package Server;

import java.util.ArrayList;
import java.util.List;

public class Round {
    List<Player> players;
    List<Player> activePlayer;
    List<Pot> pots = new ArrayList<>();
    static int basicBet;
    boolean roundFirst = false;

    public Round(List<Player> players) {
        this.players = players;
        activePlayer = new ArrayList<>(players);
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

        while (turn < activePlayer.size()) {
            currentPlayerIndex = (currentPlayerIndex + 1) % activePlayer.size();
            Player currentPlayer = activePlayer.get(currentPlayerIndex);

            currentPlayer.sendMessage("Your Turn");
            currentPlayer.sendMessage("Current Minimum Bet >> " + basicBet);

            if (roundFirst) {
                currentPlayer.takeTurn(0);
            } else {
                currentPlayer.sendMessage("Minimum Raise >> " + (2 * basicBet - currentPlayer.getCurrentBet()));
                currentPlayer.takeTurn(basicBet);
            }

            if (currentPlayer.getState() == Player.State.FOLD || currentPlayer.getState() == Player.State.BANKRUPTCY) {
                activePlayer.remove(currentPlayer);
                currentPlayerIndex--;
            } else if (currentPlayer.getState() == Player.State.ALLIN) {
                activePlayer.remove(currentPlayer);
                currentPlayerIndex = createSubPot(currentPlayerIndex);
            } else if (currentPlayer.getState() == Player.State.RAISE) turn = 0;
            else turn++;
        }
        for (Player p : activePlayer) {
            pots.get(0).plusPot(p.getCurrentBet());
            p.minusMoney(p.getCurrentBet());
            p.setCurrentBet(0);
        }
        return currentPlayerIndex;
    }

    public static void setBasicBet(int money) {
        basicBet = money;
    }

    public int createSubPot(int curPlayerIdx) {
        int turn = 0;
        int min = searchMin();
        Pot pot = null;
        while (turn < activePlayer.size()) {
            curPlayerIdx = (curPlayerIdx + 1) % activePlayer.size();
            Player curPlayer = activePlayer.get(curPlayerIdx);
            if (curPlayer.getMoney() == min) {
                curPlayer.sendMessage("Call Or Fold");
                curPlayer.takeTurn(min);
            } else if (curPlayer.getMoney()  > min) {
                curPlayer.sendMessage("Call Or Fold Or Raise");
                curPlayer.takeTurn(min);
            }

            if (curPlayer.getState() == Player.State.ALLIN) {
                getOpenPot().plusPot(min);
                activePlayer.remove(curPlayer);
                curPlayerIdx--;
            } else if (curPlayer.getState() == Player.State.RAISE) {
                turn = 0;
                getOpenPot().plusPot(min);
            } else if (curPlayer.getState() == Player.State.FOLD || curPlayer.getState() == Player.State.BANKRUPTCY) {
                activePlayer.remove(curPlayer);
                curPlayerIdx--;
            } else turn++;
        }
        pot.setClosed();
        pots.add(new Pot(activePlayer));
        for (Player p : activePlayer) {
            getOpenPot().plusPot(p.getCurrentBet() - min);
            p.minusMoney(p.getCurrentBet());
            p.setCurrentBet(0);
        }
        return curPlayerIdx;
    }

    public int searchMin() {
        int min = activePlayer.get(0).getMoney();
        for (Player p : activePlayer) {
            if (p.getMoney() < min) min = p.getMoney();
        }
        return min;
    }

    public Pot getOpenPot() {
        for (Pot pot : pots)
            if (!pot.getClosed()) return pot;
        return null;
    }
}
