package Server;

import java.util.List;

public class Round {
    List<Player> players;
    List<Pot> pots;
    Pot mainPot;
    static int basicBet;
    private int baseBet;
    int currentPlayerIndex;
    boolean noBetting = false;

    public Round(List<Player> players, int currentPlayerIndex, int baseBet) {
        this.players = players;
        pots.add(new Pot());
        mainPot = pots.get(0);
        this.currentPlayerIndex = currentPlayerIndex;
        this.baseBet = baseBet;
    }

    public void smallBlind() { // 시작 강제 베팅
        Player player = players.get(1);
        player.sendMessage("You are Small Blind!! Basic Betting >> 2");
        player.betMoney(baseBet / 2);
        mainPot.plusPot(baseBet / 2, player);
    }

    public void bigBlind() {
        Player player = players.get(2);
        player.sendMessage("You are Big Blind!! Basic Betting >> 4");
        player.betMoney(baseBet);
        mainPot.plusPot(baseBet, player);
    }

    public void freeFlop() { // 개인 카드 2장 분배 후 첫 배팅
        basicBet = baseBet;
        playRound();
    }

    public void flop() { // 테이블 카드 3장 공개 후 2번째 배팅
        basicBet = baseBet;
        noBetting = true;
        currentPlayerIndex = findIfSBFold();
        playRound();
    }

    public void turn() { // 4번째 테이블 카드 1장 공개 후 3번째 배팅
        basicBet = baseBet;
        noBetting = true;
        currentPlayerIndex = findIfSBFold();
        playRound();
    }

    public void river() { // 5번째 테이블 카드 1장 공개 후 마지막 배팅
        basicBet = baseBet;
        noBetting = true;
        currentPlayerIndex = findIfSBFold();
        playRound();
    }

    public void playRound() {
        int turn = 0;
        if (!checkFolds(players)) return;
        while (turn < players.size()) {
            Player currentPlayer = players.get(currentPlayerIndex);

            Player.State state = currentPlayer.getState();
            if (state == Player.State.FOLD) {
                turn++;
                continue;
            }

            currentPlayer.sendMessage("Your Turn");
            currentPlayer.sendMessage("Current Minimum Bet >> " + basicBet);

            if (noBetting) { // 모두가 체크거나 현재플레이어가 라운드 첫 베팅(프리플랍 제외)
                currentPlayer.chooseBetAction(basicBet, true);
            } else {
                currentPlayer.sendMessage("Minimum Raise >> " + 2 * (basicBet - currentPlayer.getCurrentBet()));
                currentPlayer.chooseBetAction(basicBet, false);
            }

            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            switch (currentPlayer.getState()) {
                case CALL, CHECK -> turn++;
                case ALLIN -> {
                    turn = 0;
                    createSidePot();
                }
                case RAISE -> { // 첫 배팅도 레이즈 상태
                    noBetting = false;
                    turn = 0;
                }
            }
        }
        Pot pot = getOpenPot();
        for (Player player : players) {
            pot.plusPot(player.getCurrentBet(), player);
        }
    }

    public void createSidePot() {
        pots.add(new Pot());
        Pot pot = getOpenPot();
        int min = min();
        int max = max();
        int turn = 0;
        while (turn < players.size()) {
            Player currentPlayer = players.get(currentPlayerIndex);
            if (currentPlayer.getState() == Player.State.FOLD) {
                turn++;
                continue;
            }
            int ownMoney = currentPlayer.getMoney() + currentPlayer.getCurrentBet();
            boolean result = ownMoney < basicBet; // basicBet = 올인 금액
            if (result) { //올인 금액보다 적다면 -> 폴드 or 올인
                currentPlayer.sendMessage("Your Turn");
                currentPlayer.sendMessage("Fold or All-In");
                currentPlayer.respondToAllIn(true);
            } else { // 올인 금액보다 같거나 크다면 -> 콜, 폴드, 레이즈
                currentPlayer.sendMessage("Your Turn");
                currentPlayer.sendMessage("Call or Fold or Raise");
                currentPlayer.respondToAllIn(false);
                switch (currentPlayer.getState()) {
                    case FOLD, CALL -> pot.plusPot(currentPlayer.getCurrentBet(), currentPlayer);
                    case RAISE -> pot.plusPot(min, currentPlayer);
                }
            }
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        }
        for ()
            pot.setClosed();
    }

    public boolean checkFolds(List<Player> players) { // 1명만 제외 전부 폴드면 남은 한명 자동 승리
        int count = 0;
        for (Player player : players) {
            if (player.getState() == Player.State.FOLD) count++;
        }
        if (players.size() - count == 1) return false;
        else return true;
    }

    public static void setBasicBet(int money) {
        basicBet = money;
    }

    public Pot getOpenPot() { // 가장 나중에 만들어진 팟
        for (Pot pot : pots)
            if (!pot.getClosed()) return pot;
        return null;
    }

    public int findIfSBFold() {
        int idx = 1;
        while (true) {
            if (players.get(idx).getState() == Player.State.FOLD) idx--;
            else break;
            if (idx == -1) idx = players.size() - 1;
        }
        return idx;
    }

    public int min() {
        int min = players.get(0).getMoney() + players.get(0).getCurrentBet();
        for (Player player : players) {
            if (player.getState() == Player.State.FOLD) continue;
            int ownMoney = player.getMoney() + player.getCurrentBet();
            if (ownMoney < min) min = ownMoney;
        }
        return min;
    }

    public int max() {
        int max = players.get(0).getMoney() + players.get(0).getCurrentBet();
        for (Player player : players) {
            if (player.getState() == Player.State.FOLD) continue;
            if (player.getMoney() + player.getCurrentBet() > max) max = player.getMoney() + player.getCurrentBet();
        }
        return max;
    }
}
