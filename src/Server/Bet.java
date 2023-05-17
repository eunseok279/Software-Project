package Server;

public class Bet { // 배팅 방식
    Player player;

    public Bet(Player player) {
        this.player = player;
    }

    public boolean firstBet(int betMoney){
        if(betMoney>player.getMoney()) {
            player.sendMessage("Not Enough Money!!");
            return false;
        }
       else if(betMoney == player.getMoney()) {
            player.sendMessage("First betting can't do all in");
            return false;
        }
        if(betMoney<Round.basicBet){
            player.sendMessage("Minimum is >> "+Round.basicBet);
            return false;
        }
        player.betMoney(betMoney);
        Round.setBasicBet(betMoney);
        player.setState(Player.State.CALL);
        return true;
    }

    public boolean call(int callMoney) {
        if (callMoney > player.getMoney()) {
            player.sendMessage("Not Enough Money!! You Should All-In or Fold");
            return false;
        }
        else if (callMoney == player.getMoney()) {
            player.sendMessage("Your Money is All In");
            return allIn();
        }
        if(player.getCurrentBet() == Round.basicBet) {
            player.sendMessage("You don't have to call!");
            return false;
        }
        player.betMoney(callMoney);
        player.setState(Player.State.CALL);
        return true;
    }

    public boolean raise(int raiseMoney, int currentBet, int basicBet) {
        int minimumRaise = basicBet * 2 - currentBet; // 최소 배팅금은 전 사람의 배팅금의 2배(자신의 배팅금 포함)
        if (minimumRaise > raiseMoney) {
            player.sendMessage("Minimum is >> " + minimumRaise);
            return false;
        }
        if (raiseMoney > player.getMoney()) {
            player.sendMessage("Not Enough Money!!");
            return false;
        }
        else if (raiseMoney == player.getMoney()) {
            player.sendMessage("Your Money is All In");
            return allIn();
        }
        if (raiseMoney + currentBet < basicBet) {
            player.sendMessage("Not Enough Money!! You Should All-In or Fold");
        } else if (raiseMoney + currentBet == basicBet) {
            player.sendMessage("Call Is Execution!");
            return call(raiseMoney);
        }
        player.betMoney(raiseMoney);
        Round.setBasicBet(raiseMoney + currentBet);
        player.setState(Player.State.RAISE);
        return true;
    }

    public void fold() {
        player.setState(Player.State.FOLD);
    }

    public boolean allIn() {
        int money = player.getMoney();
        player.betMoney(money);
        player.setState(Player.State.ALLIN);
        return true;
    }

    public boolean check(int basicBet) {
        if (player.getCurrentBet() < basicBet) {
            player.sendMessage("You must bet!");
            return false;
        }
        player.setState(Player.State.CHECK);
        return true;
    }
}
