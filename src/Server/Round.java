package Server;

import java.util.ArrayList;
import java.util.List;

public class Round {
    List<Player> players;
    List<Pot> pots = new ArrayList<>();
    static int basicBet;
    boolean roundFirst = false;

    public Round(List<Player> players) {
        this.players = players;
        pots.add(new Pot(players));
    }

    public void smallBlind() { // 시작 강제 베팅
        Player player = players.get(1);
        player.sendMessage("You are Small Blind!! Basic Betting >> 1000");
        player.betMoney(1000);
    }

    public void bigBlind() {
        Player player = players.get(2);
        player.sendMessage("You are Big Blind!! Basic Betting >> 2000");
        player.betMoney(2000);
    }

    public int freeFlop(int currentPlayerIndex) { // 개인 카드 2장 분배 후 첫 배팅
        basicBet = 2000;
        return playRound(currentPlayerIndex);
    }

    public int flop(int currentPlayerIndex) { // 테이블 카드 3장 공개 후 2번째 배팅
        basicBet = 2000;
        roundFirst = true;
        return playRound(currentPlayerIndex);
    }

    public int turn(int currentPlayerIndex) { // 4번째 테이블 카드 1장 공개 후 3번째 배팅
        basicBet = 2000;
        roundFirst = true;
        return playRound(currentPlayerIndex);
    }

    public int river(int currentPlayerIndex) { // 5번째 테이블 카드 1장 공개 후 마지막 배팅
        basicBet = 4000;
        roundFirst = true;
        return playRound(currentPlayerIndex);
    }

    public int playRound(int currentPlayerIndex) {
        int turn = 0;
        if (!checkFolds(players)) return -1;
        while (turn < players.size()) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            Player currentPlayer = players.get(currentPlayerIndex);
            int betting = currentPlayer.getCurrentBet();

            currentPlayer.sendMessage("Your Turn");
            currentPlayer.sendMessage("Current Minimum Bet >> " + basicBet);

            if (roundFirst) {
                currentPlayer.takeTurn(0);
            } else {
                currentPlayer.sendMessage("Minimum Raise >> " + (2 * basicBet - betting));
                currentPlayer.takeTurn(basicBet);
            }

            if (currentPlayer.getState() == Player.State.FOLD) pots.get(0).plusPot(betting);
            else if (currentPlayer.getState() == Player.State.ALLIN)
                createSubPot(currentPlayerIndex);
            else if (currentPlayer.getState() == Player.State.RAISE) {
                pots.get(0).plusPot(betting);
                turn = 0;
            } else {
                pots.get(0).plusPot(betting);
                turn++;
            }
        }
        for (Player player : players) {
            player.setCurrentBet(0);
        }
        return currentPlayerIndex;
    }


    public void createSubPot(int curPlayerIdx) {
        int turn = 0;
        pots.add(new Pot(players));
        int min = searchMin(players);
        getOpenPot().plusPot(min);
        while (turn < players.size()) {
            curPlayerIdx = (curPlayerIdx + 1) % players.size();
            Player curPlayer = players.get(curPlayerIdx);
            if(curPlayer.getState()== Player.State.FOLD) continue;
            if (curPlayer.getMoney() == min) {
                curPlayer.sendMessage("Call Or Fold");
                curPlayer.takeTurn(min);
                if (curPlayer.getState() == Player.State.ALLIN) getOpenPot().plusPot(min);
            } else if (curPlayer.getMoney() > min) {
                curPlayer.sendMessage("Call Or Fold Or Raise");
                curPlayer.takeTurn(min);
            }

            if (curPlayer.getState() == Player.State.ALLIN) {
                getOpenPot().plusPot(min);
            } else if (curPlayer.getState() == Player.State.RAISE) {
                turn = 0;
                getOpenPot().plusPot(curPlayer);
            } else if (curPlayer.getState() == Player.State.FOLD || curPlayer.getState() == Player.State.BANKRUPTCY) {
                getOpenPot().plusPot(curPlayer);
            } else turn++;
        }
        getOpenPot().setClosed();
        pots.add(new Pot(activePlayer));
        for (Player p : activePlayer) {
            getOpenPot().plusPot(p.getCurrentBet() - min);
            p.minusMoney(p.getCurrentBet());
            p.setCurrentBet(0);
        }
    }

    public int searchMin(List<Player> players) {
        int min = players.get(0).getMoney();
        for (Player p : players) {
            if (p.getMoney() < min) min = p.getMoney();
        }
        return min;
    }

    public Pot getOpenPot() {
        for (Pot pot : pots)
            if (!pot.getClosed()) return pot;
        return null;
    }

    public boolean checkFolds(List<Player> players) {
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
}
