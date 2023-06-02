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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class CurrentTracker {
    int index = 0;
    boolean game = false;
}

public class Dealer { // 판을 깔아줄 컴퓨터 및 시스템
    List<Card> tableCard = new ArrayList<>(); // table 있는 패
    Deck deck = new Deck();
    List<User> users = Collections.synchronizedList(new ArrayList<>());
    ExecutorService executorService = Executors.newCachedThreadPool();
    CurrentTracker currentTracker = new CurrentTracker();
    int gameCount = 1;
    int baseBet = 4;
    boolean dealerButton = false;
    private Socket clientSocket;
    Database db = new Database();

    public void setUpGame(int port) throws ClassNotFoundException, SQLException, IOException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        db.con = DriverManager.getConnection(db.url, db.user, db.passwd);
        db.stmt = db.con.createStatement();
        System.out.println("wait for Users...");

        // 클라이언트 연결을 받는 스레드

        Runnable connectionHandler = () -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (true) {
                    synchronized (users) {
                        if (users.size() >= 8) {
                            continue;
                        }
                    }
                    clientSocket = serverSocket.accept();
                    executorService.submit(() -> {
                        createUser(clientSocket);
                    });
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        executorService.submit(connectionHandler);

// 준비 상태를 확인하는 스레드
        Runnable readyChecker = () -> {
            while (true) {
                if (users.size() >= 2) {
                    boolean allReady = true;
                    for (User user : users) {
                        if (!user.isReady()) {
                            allReady = false;
                            break;
                        }
                    }
                    if (allReady) {
                        try {
                            sendAll("All users are ready. Starting the game...");
                            sendAll("/game");
                            System.out.println("game is start");
                            for (User user : users) {
                                user.setReady(false);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        gameStart();
                    }
                }
                try {
                    Thread.sleep(1000);  // 1초마다 준비 상태 확인
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        executorService.submit(readyChecker);
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
                synchronized (users) {
                    for (User u : users) {
                        if (u.getName().equals(name)) {
                            out.println("You need to log in with a different nickname");
                            in.close();
                            out.close();
                            clientSocket.close();
                            users.remove(u);
                            return;
                        }
                    }
                }
                int userMoney = db.getUserMoney(name);
                user = new User(clientSocket, name, out, userMoney);
            } else {
                db.insertUser(name, 200);
                user = new User(clientSocket, name, out);
                System.out.println(user.getName() + " is added");
            }
            users.add(user);
            System.out.println(user.getName() + " is joined!");
            sendAll(user.getName() + " is joined");
            UserHandler handler = new UserHandler(user, users, in, currentTracker);
            executorService.submit(handler);
            sendMsg("Welcome!!", user);
            for (User u : users) {
                sendAll("/name" + u.getName());
            }
            sendAll("/finish");
            user.sendMessage("/money" + user.getMoney());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 게임 시작
    public void gameStart (){
        try {
            currentTracker.game = true;
            List<User> gameUsers = new ArrayList<>(users);
            if (gameCount == 3) {
                gameCount = 1;
                baseBet *= 2;
            }
            for (User user : gameUsers) {
                user.sendMessage("/money"+user.getMoney());
                if (user.getMoney() < baseBet) {
                    user.sendMessage("Your Base Money is not enough");
                    gameUsers.remove(user);
                }
            }
            if (gameUsers.size() < 2) return;
            if (!dealerButton) {
                System.out.println("Set Dealer Button");
                setDealerButton(gameUsers);
            } else rearrangeOrder(gameUsers, 1);

            if (gameUsers.size() == 2) currentTracker.index = 0;
            else currentTracker.index = 3;
            Round round = new Round(gameUsers, baseBet, currentTracker);
            round.smallBlind();
            round.bigBlind();
            givePersonalCard(gameUsers);
            sendRank(gameUsers);
            round.freeFlop(); // 빅블라인드 다음 사람부터 시작
            addCard(3,gameUsers);
            sendRank(gameUsers);
            round.flop();
            addCard(1,gameUsers);
            sendRank(gameUsers);
            round.turn();
            addCard(1,gameUsers);
            sendRank(gameUsers);
            round.river();
            gameCount++;
            for (int i = 0; i < round.pots.size(); i++) {
                Pot pot = round.pots.get(i);
                determineWinners(pot, i);
            }
            deck.initCard();
            currentTracker.game = false;
            sendMoney(gameUsers);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void sendRank(List<User> users) throws IOException {
        for (User user : users)
            if (!(user.getState() == User.State.FOLD)) user.sendMessage("/rank" + user.hand.determineHandRank().name());
    }

    public void sendMoney(List<User> users) throws IOException {
        for (User user : users)
            user.sendMessage("/money" + user.getMoney());
    }

    public void givePersonalCard(List<User> users) throws IOException, ClassNotFoundException {
        for (int i = 0; i < 2; i++)
            for (User user : users) {
                Card card = deck.drawCard();
                user.hand.cards.add(card);
                user.sendMessage("/card" + card.showCard());
            }
    }

    public void addCard(int count,List<User> users) throws IOException {
        for (int i = 0; i < count; i++) {
            Card card = deck.drawCard();

            tableCard.add(card);
            for (User user : users) {
                user.hand.cards.add(card);
                user.sendMessage("/card" + card.showCard());
            }
        }
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
        int h1Kicker = h1.getKickerRank(h1Three);
        int h2Kicker = h2.getKickerRank(h2Three);
        return Integer.compare(h1Kicker, h2Kicker);
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
        int h1Kicker = h1.getKickerRank(h1Quad);
        int h2Kicker = h2.getKickerRank(h2Quad);
        return Integer.compare(h1Kicker, h2Kicker);
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
        int h1Kicker = h1.getKickerRank(h1PairRanks);
        int h2Kicker = h2.getKickerRank(h2PairRanks);
        return Integer.compare(h1Kicker, h2Kicker);
    }

    private int comparePairAndKickers(Hand h1, Hand h2) {
        List<Integer> h1PairRanks = h1.getPairRanks();
        List<Integer> h2PairRanks = h2.getPairRanks();

        int higherPairComparison = h1PairRanks.get(0).compareTo(h2PairRanks.get(0));
        if (higherPairComparison != 0) {
            return higherPairComparison;
        }
        int h1Kicker = h1.getKickerRank(h1PairRanks);
        int h2Kicker = h2.getKickerRank(h2PairRanks);
        return Integer.compare(h1Kicker, h2Kicker);
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

        for (User user : currentWinners) {
            user.sendMessage("/win" + index);
            int money = pot.getPotMoney() / currentWinners.size();
            user.plusMoney(money);
        }
    }

    private void setDealerButton(List<User> users) throws IOException {
        deck.shuffle();
        sendAll("Set Dealer Button");
        Map<Integer, Card> cardMap = new HashMap<>();
        for (int i = 0; i < users.size(); i++) {
            cardMap.put(i, deck.drawCard());
            sendAll(users.get(i).getName() + "'s card >> " + cardMap.get(i).showCard());
        }
        List<Map.Entry<Integer, Card>> entry = new ArrayList<>(cardMap.entrySet());
        entry.sort((o1, o2) -> {
            int rankComp = o2.getValue().rank().compareTo(o1.getValue().rank());
            if (rankComp == 0) return o2.getValue().suit().compareTo(o1.getValue().suit());
            return rankComp;
        });
        sendAll(users.get(entry.get(0).getKey()).getName() + " >> DealerButton!!");
        rearrangeOrder(users, entry.get(0).getKey());
        initUserCard();
        deck.initCard();
        deck.shuffle();
        dealerButton = true;
    }

    public void rearrangeOrder(List<User> userOrder, int dealerButtonIndex) {
        List<User> newOrder = new ArrayList<>();
        for (int i = dealerButtonIndex; i < userOrder.size(); i++) {
            newOrder.add(userOrder.get(i));
        }
        for (int i = 0; i < dealerButtonIndex; i++) {
            newOrder.add(userOrder.get(i));
        }
        users = newOrder;
    }

    public void sendMsg(String message, User user) throws IOException {
        user.sendMessage(message);
    }

    public void sendAll(String message) throws IOException {
        for (User user : users) {
            user.sendMessage(message);
        }
    }

    public void initUserCard() {
        for (User user : users)
            user.hand.cards.clear();
    }
}