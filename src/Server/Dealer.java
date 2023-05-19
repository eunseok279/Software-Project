package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Dealer { // 판을 깔아줄 컴퓨터 및 시스템
    Pot pot;
    List<Card> tableCard = new ArrayList<>(); // table 있는 패
    Deck deck = new Deck();
    List<User> users = Collections.synchronizedList(new ArrayList<>());
    List<User> winners;
    int currentUserIndex;
    int gameCount = 1;
    int baseBet = 4;


    public void setUpGame(int port) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<User> users = Collections.synchronizedList(new ArrayList<>());

// 클라이언트 연결을 받는 스레드
        Runnable connectionHandler = () -> {
            while (true) {
                try (ServerSocket serverSocket = new ServerSocket(port)) {
                    if (users.size() < 6) {
                        Socket clientSocket = serverSocket.accept();

                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        String receiveName = in.readLine();

                        User user = new User(clientSocket, receiveName);
                        users.add(user);

                        UserHandler handler = new UserHandler(user, users, this);
                        executorService.submit(handler);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
                        sendAll("All users are ready. Starting the game...");
                        try {
                            gameStart();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        for (User user : users) {
                            user.setReady(false);
                        }
                        break;
                    }
                }
                try {
                    Thread.sleep(1000);  // 1초마다 준비 상태 확인
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        new Thread(readyChecker).start();

    }

    public void gameStart() throws IOException {
        if (gameCount == 3) {
            gameCount = 0;
            baseBet *= 2;
        }
        setDealerButton(users);
        Round round = new Round(users, currentUserIndex, baseBet);
        round.smallBlind();
        round.bigBlind();
        for (User user : users)
            getPersonalCard(user.hand.cards);
        currentUserIndex = 3; // 0번 - 딜러 버튼 | 1번 - 스몰 블라인드 | 2번 - 빅블라인드
        round.freeFlop(); // 빅블라인드 다음 사람부터 시작
        round.flop();
        round.turn();
        round.river();
        for (User user : users)
            if (user.getMoney() < baseBet) {
                user.sendMessage("Your Base Money is not enough");
                user.getSocket().close();
                users.remove(user);
            }
        if (users.size() == 1)
            sendMsg("Winner of Winner is you! Congratulation", users.get(0));
        gameCount++;
    }

    public synchronized User getCurrentUser() {
        return users.get(currentUserIndex);
    }

    public void getPersonalCard(List<Card> cards) {
        for (int i = 0; i < 2; i++) {
            cards.add(deck.drawCard());
        }
    }

    public void addCard(Card card) {
        tableCard.add(card);

        for (User user : users)
            user.hand.cards.add(card);
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

    public List<User> determineWinners(List<User> users) {
        List<User> currentWinners = new ArrayList<>();
        currentWinners.add(users.get(0)); //1번 플레이어

        for (int i = 1; i < users.size(); i++) { // 234 플레이어
            User currentUser = users.get(i);
            int comparisonResult = compareHands(currentWinners.get(0), currentUser);

            if (comparisonResult < 0) { // 현재 플레이어를 다른 플레이어가 이긴 경우
                currentWinners.clear();
                currentWinners.add(currentUser);
            } else if (comparisonResult == 0) { // 동점인 경우
                currentWinners.add(currentUser);
            }
        }
        return currentWinners;
    }

    private void setDealerButton(List<User> users) {
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
        sendAll(users.get(entry.get(0).getKey()).getName() + "is DealerButton!!");
        rearrangeOrder(users, entry.get(0).getKey());
        initUserCard();
        deck.initCard();
        deck.shuffle();
    }

    public List<User> rearrangeOrder(List<User> userOrder, int dealerButtonIndex) {
        List<User> newOrder = new ArrayList<>();
        for (int i = dealerButtonIndex; i < userOrder.size(); i++) {
            newOrder.add(userOrder.get(i));
        }
        for (int i = 0; i < dealerButtonIndex; i++) {
            newOrder.add(userOrder.get(i));
        }
        return newOrder;
    }

    public void sendMsg(String message, User user) {
        user.sendMessage(message);
    }

    public void sendAll(String message) {
        for (User user : users) {
            user.sendMessage(message);
        }
    }

    public void initUserCard() {
        for (User user : users)
            user.hand.cards.clear();
    }
}

