package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class GameState {
    int index = 0;
    boolean game = false;
    List<User> gameUser;
    List<User> users = Collections.synchronizedList(new ArrayList<>());

    public synchronized void remove(User user) {
        users.remove(user);
    }
}

public class Dealer { // 판을 깔아줄 컴퓨터 및 시스템
    List<Card> tableCard = new ArrayList<>(); // table 있는 패
    Deck deck = new Deck();
    ExecutorService executorService = Executors.newCachedThreadPool();
    GameState gameState = new GameState();
    int gameCount = 1;
    int baseBet = 4;
    boolean dealerButton = false;
    Database db = new Database();
    private Socket clientSocket;

    public void setUpGame(int port) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        db.con = DriverManager.getConnection(db.url, db.user, db.passwd);
        db.stmt = db.con.createStatement();

        // 클라이언트 연결을 받는 스레드

        Runnable connectionHandler = () -> {
            System.out.println("서버가 열렸습니다");
            System.out.println("유저 연결을 기다리는 중...");
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (true) {
                    synchronized (gameState.users) {
                        if (gameState.users.size() >= 8) {
                            continue;
                        }
                    }
                    clientSocket = serverSocket.accept();
                    executorService.submit(() -> createUser(clientSocket));
                }
            } catch (IOException e) {
                for (User user : gameState.users) {
                    try {
                        user.getSocket().close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        };

        executorService.submit(connectionHandler);

// 준비 상태를 확인하는 스레드
        Runnable readyChecker = () -> {
            while (true) {
                if (gameState.users.size() >= 2) {
                    boolean allReady = true;
                    for (User user : gameState.users) {
                        if (!user.isReady()) {
                            allReady = false;
                            break;
                        }
                    }
                    if (allReady) {
                        try {
                            sendAll("모든 플레이어가 준비가 완료되었습니다...");
                            System.out.println("game is start");
                            for (User user : gameState.users) {
                                user.setReady(false);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        gameStart();
                    }
                } else dealerButton = false;
                try {
                    Thread.sleep(1000);  // 1초마다 준비 상태 확인
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        executorService.submit(readyChecker);

        Runnable checkConnect = () -> {
            while (!Thread.currentThread().isInterrupted()) {
                synchronized (gameState.users) {
                    for (User user : gameState.users) {
                        if (user.isConnection()) gameState.remove(user);
                    }
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        executorService.submit(checkConnect);
    }

    public void createUser(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter((clientSocket.getOutputStream()));
            String receiveName = in.readLine();
            if (!receiveName.startsWith("/name")) {
                in.close();
                out.close();
                clientSocket.close();
                return;
            }
            String name = receiveName.substring(5);
            User user;
            if (db.checkUser(name)) {
                synchronized (gameState.users) {
                    for (User u : gameState.users) {
                        if (u.getName().equals(name)) {
                            out.println("중복 접속입니다.");
                            in.close();
                            out.close();
                            clientSocket.close();
                            return;
                        }
                    }
                }
                int userMoney = db.getUserMoney(name);
                user = new User(clientSocket, name, out, userMoney);
            } else {
                db.insertUser(name);
                user = new User(clientSocket, name, out);
                System.out.println(user.getName() + " is added");
            }
            gameState.users.add(user);
            UserHandler handler = new UserHandler(user, in, gameState);
            executorService.submit(handler);
            System.out.println(user.getName() + " is joined!");
            sendAll(user.getName() + "님이 들어왔습니다.");
            user.sendMessage("환영합니다!!");
            StringBuilder names = new StringBuilder();
            for (User u : gameState.users) {
                names.append("/name").append(u.getName()).append(" ");
            }
            sendAll(names.toString());
            user.sendMessage("/money" + user.getMoney());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 게임 시작
    public void gameStart() {
        try {
            gameState.game = true;
            gameState.gameUser = new ArrayList<>(gameState.users);
            if (gameCount == 3) {
                gameCount = 1;
                baseBet *= 2;
            }

            Iterator<User> iterator = gameState.gameUser.iterator();
            while (iterator.hasNext()) {
                User user = iterator.next();
                user.sendMessage("/money" + user.getMoney());
                if (user.getMoney() < baseBet) {
                    user.sendMessage("기본 배팅금이 부족합니다.");
                    iterator.remove();
                }
            }

            if (gameState.gameUser.size() < 2) return;
            if (!dealerButton) {
                System.out.println("Set Dealer Button");
                setDealerButton(gameState.gameUser);
            } else rearrangeOrder(gameState.gameUser, 1);

            if (gameState.gameUser.size() == 2) gameState.index = 0;
            else if (gameState.gameUser.size() == 3) gameState.index = 0;
            else gameState.index = 3;
            Round round = new Round(gameState.gameUser, baseBet, gameState);
            round.smallBlind();
            round.bigBlind();
            givePersonalCard(gameState.gameUser);
            round.freeFlop(); // 빅블라인드 다음 사람부터 시작
            addCard(3, gameState.gameUser);
            round.flop();
            addCard(1, gameState.gameUser);
            round.turn();
            addCard(1, gameState.gameUser);
            round.river();
            gameCount++;

            for (int i = 0; i < round.pots.size(); i++) {
                Pot pot = round.pots.get(i);
                if (pot.potUser.size() == 0) continue;
                determineWinners(pot, i);
            }
            deck.initCard();
            deck.shuffle();
            gameState.game = false;
            init(gameState.gameUser);
        } catch (SQLException | InterruptedException | ExecutionException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void init(List<User> users) throws IOException, InterruptedException, SQLException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(users.size());
        List<Future<?>> futures = new ArrayList<>();
        for (User user : users) {
            futures.add(executorService.submit(() -> {
                try {
                    user.sendMessage("/money" + user.getMoney());
                    if (user.getState() == User.State.FOLD) sendAll(user.getName() + "님 >> FOLD");
                    else if(user.hand.kicker == 0) sendAll(user.getName() + "님의 족보 : " + user.hand.handRank.name());
                    else sendAll(user.getName() + "님의 족보 : " + user.hand.handRank.name()+" 키커 : "+user.hand.kicker);
                    user.setState(User.State.LIVE);
                    user.hand.cards.clear();
                    user.hand.kicker = 0;
                    db.updateUserBalance(user.getName(), user.getMoney());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        for (Future<?> future : futures) {
            future.get(); // Blocks until the task is completed
        }
        executorService.shutdown();
    }


    public void givePersonalCard(List<User> users) throws IOException, InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(users.size());
        for (int i = 0; i < 2; i++) {
            for (User user : users) {
                Card card = deck.drawCard();
                user.hand.cards.add(card);
            }
        }
        List<Future<?>> futures = new ArrayList<>();
        for (User user : users) {
            futures.add(executorService.submit(() -> {
                try {
                    user.sendPersonalCard();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
        }
        for (Future<?> future : futures) {
            future.get(); // Blocks until the task is completed
        }
        executorService.shutdown();
    }

    public void addCard(int count, List<User> users) throws IOException, InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(users.size());

        StringBuilder cards = new StringBuilder();
        cards.append("/card").append(" ");
        for (int i = 0; i < count; i++) {
            Card card = deck.drawCard();
            tableCard.add(card);
            for (User user : users) {
                user.hand.cards.add(card);
            }
            cards.append(card.showCard()).append(" ");
        }
        String message = cards.toString();
        List<Future<?>> futures = new ArrayList<>();
        for (User user : users) {
            futures.add(executorService.submit(() -> {
                try {
                    user.hand.handRank = user.hand.determineHandRank();
                    user.sendMessage(message + "/rank" + user.hand.handRank.name());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
        }
        for (Future<?> future : futures) {
            future.get(); // Blocks until the task is completed
        }
        executorService.shutdown();
    }

    public int compareHands(User p1, User p2) {
        Hand.HandRank p1HandRank = p1.hand.determineHandRank();
        Hand.HandRank p2HandRank = p2.hand.determineHandRank();

        if (p1HandRank.ordinal() < p2HandRank.ordinal()) {
            return 1; // 이 포커 손이 더 강함
        } else if (p1HandRank.ordinal() > p2HandRank.ordinal()) {
            return -1; // 다른 포커 손이 더 강함
        } else {
            return switch (p1HandRank) {
                case ROYAL_FLUSH ->
                    // 로열 플러시는 항상 동점입니다.
                        0;
                case FLUSH, STRAIGHT_FLUSH, STRAIGHT ->
                    // 가장 높은 숫자의 카드를 비교합니다.
                        compareHighestCard(p1.hand, p2.hand);
                case FOUR_OF_A_KIND ->
                    // 같은 숫자 카드 네 장의 숫자를 비교합니다.
                        compareQuads(p1.hand, p2.hand);
                case FULL_HOUSE ->
                    // 세 장의 같은 숫자 카드를 비교하고 동점이면, 한 쌍의 같은 숫자 카드를 비교합니다.
                        compareTripsAndPair(p1.hand, p2.hand);
                case HIGH_CARD ->
                    // 가장 높은 숫자의 카드부터 차례대로 비교합니다.
                        compareHighestCardInTurn(p1.hand, p2.hand);
                case THREE_OF_A_KIND -> compareThreePairsAndKicker(p1.hand, p2.hand);
                case TWO_PAIR ->
                    // 높은 숫자의 쌍을 비교하고, 동점이면 낮은 숫자의 쌍을 비교한 다음, 동점이면 킥커(남은 카드)를 비교합니다.
                        compareTwoPairsAndKicker(p1.hand, p2.hand);
                case ONE_PAIR ->
                    // 같은 숫자 카드 쌍을 비교하고, 동점이면 킥커를 차례대로 비교합니다.
                        comparePairAndKickers(p1.hand, p2.hand);
            };
        }
    }

    private int compareThreePairsAndKicker(Hand h1, Hand h2) {
        List<Integer> h1Three = h1.getThreePairRanks();
        List<Integer> h2Three = h2.getThreePairRanks();

        int higherPairComparison = h1Three.get(0).compareTo(h2Three.get(0));
        if (higherPairComparison != 0) {
            return higherPairComparison;
        }
        h1.kicker = h1.getKickerRank(h1Three);
        h2.kicker = h2.getKickerRank(h2Three);
        return Integer.compare( h1.kicker, h2.kicker);
    }

    private int compareHighestCardInTurn(Hand h1, Hand h2) {
        Card h1HighestCard = h1.findHighestCardInAll(); // h1의 가장 높은 카드를 찾는 메서드
        Card h2HighestCard = h2.findHighestCardInAll(); // h2의 가장 높은 카드를 찾는 메서드

        return Integer.compare(h1HighestCard.rank().ordinal(), h2HighestCard.rank().ordinal());
    }

    private int compareHighestCard(Hand h1, Hand h2) {
        Card h1HighestCard = h1.getHighestCardInCombination(); // 스트레이트에 따라 h1의 가장 높은 카드를 얻는 메서드
        Card h2HighestCard = h2.getHighestCardInCombination(); // 스트레이트에 따라 h2의 가장 높은 카드를 얻는 메서드

        return Integer.compare(h1HighestCard.rank().ordinal(), h2HighestCard.rank().ordinal());
    }

    private int compareQuads(Hand h1, Hand h2) {
        List<Integer> h1Quad = h1.getQuadsRank();
        List<Integer> h2Quad = h2.getQuadsRank();

        int higherPairComparison = h1Quad.get(0).compareTo(h2Quad.get(0));
        if (higherPairComparison != 0) {
            return higherPairComparison;
        }
        h1.kicker = h1.getKickerRank(h1Quad);
        h2.kicker = h2.getKickerRank(h2Quad);
        return Integer.compare( h1.kicker, h2.kicker);
    }

    private int compareTripsAndPair(Hand h1, Hand h2) {
        List<Integer> h1Three = h1.getThreePairRanks();
        List<Integer> h2Three = h2.getThreePairRanks();

        int higherTPairComparison = h1Three.get(0).compareTo(h2Three.get(0));
        if (higherTPairComparison != 0) {
            return higherTPairComparison;
        }

        List<Integer> h1PairRanks = h1.getPairRanks();
        List<Integer> h2PairRanks = h2.getPairRanks();

        return h1PairRanks.get(0).compareTo(h2PairRanks.get(0));
    }

    private int compareTwoPairsAndKicker(Hand h1, Hand h2) {
        List<Integer> h1PairRanks = h1.getTwoPairRanks();
        List<Integer> h2PairRanks = h2.getTwoPairRanks();

        // 높은 숫자의 쌍을 비교합니다.
        int higherPairComparison = h1PairRanks.get(0).compareTo(h2PairRanks.get(0));
        if (higherPairComparison != 0) {
            return higherPairComparison;
        }

        // 낮은 숫자의 쌍을 비교합니다.
        int lowerPairComparison = h1PairRanks.get(1).compareTo(h2PairRanks.get(1));
        if (lowerPairComparison != 0) {
            return lowerPairComparison;
        }

        // 킥커(남은 카드)를 비교합니다.
        h1.kicker = h1.getKickerRank(h1PairRanks);
        h2.kicker = h2.getKickerRank(h2PairRanks);
        return Integer.compare( h1.kicker, h2.kicker);
    }

    private int comparePairAndKickers(Hand h1, Hand h2) {
        List<Integer> h1PairRanks = h1.getPairRanks();
        List<Integer> h2PairRanks = h2.getPairRanks();

        int higherPairComparison = h1PairRanks.get(0).compareTo(h2PairRanks.get(0));
        if (higherPairComparison != 0) {
            return higherPairComparison;
        }
        h1.kicker = h1.getKickerRank(h1PairRanks);
        h2.kicker = h2.getKickerRank(h2PairRanks);
        return Integer.compare(h1.kicker, h2.kicker);
    }

    public void determineWinners(Pot pot, int index) throws IOException {
        List<User> currentWinners = new ArrayList<>();
        Iterator<User> itr = pot.potUser.iterator();
        User currentUser = itr.next();

        currentWinners.add(currentUser);

        while (itr.hasNext()) {
            currentUser = itr.next();
            int comparisonResult = compareHands(currentWinners.get(0), currentUser);

            if (comparisonResult < 0) { // 현재 플레이어를 다른 플레이어가 이긴 경우
                currentWinners.clear();
                currentWinners.add(currentUser);
            } else if (comparisonResult == 0) { // 동점인 경우
                currentWinners.add(currentUser);
            }
        }

        for (User user : gameState.users) {
            if (currentWinners.contains(user)) {
                user.sendMessage("/win" + index);
                int money = pot.getPotMoney() / currentWinners.size();
                user.plusMoney(money);
                sendAll("승자는 " + user.getName());
            } else
                user.sendMessage("/lose" + index);
        }
    }

    private void setDealerButton(List<User> users) throws IOException {
        deck.shuffle();
        sendAll("Set Dealer Button");
        Map<Integer, Card> cardMap = new HashMap<>();
        for (int i = 0; i < users.size(); i++) {
            cardMap.put(i, deck.drawCard());
            sendAll(users.get(i).getName() + "님 카드 >> " + cardMap.get(i).showCard());
        }
        List<Map.Entry<Integer, Card>> entry = new ArrayList<>(cardMap.entrySet());
        entry.sort((o1, o2) -> {
            int rankComp = o2.getValue().rank().compareTo(o1.getValue().rank());
            if (rankComp == 0) return o2.getValue().suit().compareTo(o1.getValue().suit());
            return rankComp;
        });
        rearrangeOrder(users, entry.get(0).getKey());
        initUserCard();
        deck.initCard();
        deck.shuffle();
        dealerButton = true;
    }

    public void rearrangeOrder(List<User> userOrder, int dealerButtonIndex) throws IOException {
        List<User> newOrder = new ArrayList<>();
        for (int i = dealerButtonIndex; i < userOrder.size(); i++) {
            newOrder.add(userOrder.get(i));
        }
        for (int i = 0; i < dealerButtonIndex; i++) {
            newOrder.add(userOrder.get(i));
        }
        userOrder = newOrder;
        StringBuilder names = new StringBuilder();
        for (User user : userOrder) {
            sendMsg("/game", user);
            names.append("/user").append(user.getName()).append(" ");
        }
        for (User user : userOrder) {
            sendMsg(names.toString(), user);
        }
    }

    public void sendMsg(String message, User user) throws IOException {
        user.sendMessage(message);
    }

    public void sendAll(String message) throws IOException {
        synchronized (gameState.users) {
            for (User user : gameState.users) {
                user.sendMessage(message);
            }
        }
    }

    public void initUserCard() {
        for (User user : gameState.users)
            user.hand.cards.clear();
    }
}