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
    List<Player> players = Collections.synchronizedList(new ArrayList<>());
    List<Player> winners;
    int currentPlayerIndex;
    int betting;



    public void setUpGame(int port) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<Player> players = Collections.synchronizedList(new ArrayList<>());

// 클라이언트 연결을 받는 스레드
        Runnable connectionHandler = () -> {
            while (true) {
                try (ServerSocket serverSocket = new ServerSocket(port)) {
                    if (players.size() < 6) {
                        Socket clientSocket = serverSocket.accept();

                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        String receiveName = in.readLine();

                        Player player = new Player(clientSocket, receiveName);
                        players.add(player);

                        PlayerHandler handler = new PlayerHandler(player, players, this);
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
                if (players.size() >= 2) {
                    boolean allReady = true;
                    for (Player player : players) {
                        if (!player.isReady()) {
                            allReady = false;
                            break;
                        }
                    }
                    if (allReady) {
                        sendAll("All players are ready. Starting the game...");
                        try {
                            gameStart();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        for (Player player : players) {
                            player.setReady(false);
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
        setDealerButton(players);
        Round round = new Round(players);
        round.smallBlind();
        round.bigBlind();
        for(Player player:players)
            getPersonalCard(player.hand.cards);
        currentPlayerIndex = 2; // 0번 - 딜러 버튼 | 1번 - 스몰 블라인드 | 2번 - 빅블라인드
        currentPlayerIndex = round.freeFlop(currentPlayerIndex);
        currentPlayerIndex = round.flop(currentPlayerIndex);
        currentPlayerIndex = round.turn(currentPlayerIndex);
        currentPlayerIndex = round.river(currentPlayerIndex);
        for(Player player : players)
            if(player.getMoney()<2000){
                player.sendMessage("Your Base Money is not enough");
                player.getSocket().close();
                players.remove(player);
            }
    }

    public synchronized Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }
    public void getPersonalCard(List<Card> cards) {
        for (int i = 0; i < 2; i++) {
            cards.add(deck.drawCard());
        }
    }
    public void addCard(Card card) {
        tableCard.add(card);

        for (Player player : players)
            player.hand.cards.add(card);
    }
    public int compareHands(Player p1, Player p2) {
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

    public List<Player> determineWinners(List<Player> players) {
        List<Player> currentWinners = new ArrayList<>();
        currentWinners.add(players.get(0)); //1번 플레이어

        for (int i = 1; i < players.size(); i++) { // 234 플레이어
            Player currentPlayer = players.get(i);
            int comparisonResult = compareHands(currentWinners.get(0), currentPlayer);

            if (comparisonResult < 0) { // 현재 플레이어를 다른 플레이어가 이긴 경우
                currentWinners.clear();
                currentWinners.add(currentPlayer);
            } else if (comparisonResult == 0) { // 동점인 경우
                currentWinners.add(currentPlayer);
            }
        }
        return currentWinners;
    }

    private void setDealerButton(List<Player> players) {
        sendAll("Set Dealer Button");
        Map<Integer, Card> cardMap = new HashMap<>();
        for (int i = 0; i < players.size(); i++) {
            cardMap.put(i, deck.drawCard());
            sendAll(players.get(i).getName() + "'s card >> " + cardMap.get(i).showCard());
        }
        List<Map.Entry<Integer, Card>> entry = new ArrayList<>(cardMap.entrySet());
        entry.sort((o1, o2) -> {
            int rankComp = o2.getValue().rank().compareTo(o1.getValue().rank());
            if (rankComp == 0) return o2.getValue().suit().compareTo(o1.getValue().suit());
            return rankComp;
        });
        sendAll(players.get(entry.get(0).getKey()).getName() + "is DealerButton!!");
        rearrangeOrder(players, entry.get(0).getKey());
        initUserCard();
        deck.initCard();
        deck.shuffle();
    }

    public List<Player> rearrangeOrder(List<Player> playerOrder, int dealerButtonIndex) {
        List<Player> newOrder = new ArrayList<>();
        for (int i = dealerButtonIndex; i < playerOrder.size(); i++) {
            newOrder.add(playerOrder.get(i));
        }
        for (int i = 0; i < dealerButtonIndex; i++) {
            newOrder.add(playerOrder.get(i));
        }
        return newOrder;
    }

    public void sendMsg(String message, Player player) {
        player.sendMessage(message);
    }

    public void sendAll(String message) {
        for (Player player : players) {
            player.sendMessage(message);
        }
    }
    public void initUserCard(){
        for(Player player:players)
            player.hand.cards.clear();
    }
}

