package Server;

public class Bet { // 배팅 방식
    Player player;

    public Bet(Player player) {
        this.player = player;
        int betting = player.getCurrentBet();
    }

    public boolean bet(int betMoney) { // 각 라운드 첫 배팅(프리플랍 제외)
        if (betMoney > player.getMoney()) { // 배팅금 > 소지금
            player.sendMessage("Not Enough Money!!");
            return false;
        }
        else if(betMoney == player.getMoney()){
            player.sendMessage("All-In");
            return allIn();
        }
        if (betMoney < Round.basicBet) { // 배팅금 < 최소 배팅
            player.sendMessage("Minimum is >> " + Round.basicBet);
            return false;
        }
        player.betMoney(betMoney);
        Round.setBasicBet(betMoney); // 첫 배팅 => 최소 배팅금
        player.setState(Player.State.RAISE);
        return true;
    }

    public boolean call(int callMoney) { // 앞 사람의 배팅금을 콜
        if (callMoney > player.getMoney()) { // 콜 > 소지금 -> 폴드 혹은 allin
            player.sendMessage("Not Enough Money!! You Should All-In or Fold");
            return false;
        } else if (callMoney == player.getMoney()) { // 콜 = 소지금 -> 올인
            player.sendMessage("All-In");
            return allIn();
        }
        if (callMoney == 0) { // 기본 배팅금 = 배팅금 -> 체크
            player.sendMessage("Check");
            return check();
        }
        player.betMoney(callMoney);
        player.setState(Player.State.CALL);
        return true;
    }

    public boolean raise(int raiseMoney, int currentBet, int basicBet) { // 앞 사람의 레이즈
        int minimumRaise = 2*(basicBet - currentBet); // 기본 배팅금 - 나의 배팅금의 두배
        if (raiseMoney  < minimumRaise) { // 민레이즈
            player.sendMessage("Minimum raise is >> " + minimumRaise);
            return false;
        }
        if (raiseMoney > player.getMoney()) { // 배팅금 > 소지금
            player.sendMessage("Not Enough Money!!");
            return false;
        } else if (raiseMoney == player.getMoney()) { // 배팅금 = 소지금 -> 올인
            player.sendMessage("All In");
            return allIn();
        }
        if (raiseMoney + currentBet < basicBet) { // 전체 배팅금 < 기본 배팅금
            player.sendMessage("Minimum Betting >> " + basicBet);
            return false;
        } else if (raiseMoney + currentBet == basicBet) { // 전체 배팅금 == 기본 배팅금 -> 콜
            player.sendMessage("Call");
            return call(raiseMoney);
        }
        player.betMoney(raiseMoney);
        Round.setBasicBet(player.getCurrentBet()); // 레이즈에 성공하면 기본 배팅금 = 전체 배팅금
        player.setState(Player.State.RAISE);
        return true;
    }

    public void fold() {
        player.setState(Player.State.FOLD);
    }

    public boolean allIn() {
        int money = player.getMoney();
        player.betMoney(money);
        Round.setBasicBet(money);
        player.setState(Player.State.ALLIN);
        return true;
    }

    public boolean check() { // 턴을 넘긴다 배팅x
        if (player.getCurrentBet() < Round.basicBet) { // 현재 배팅금 < 기본 배팅금 -> 콜
            player.sendMessage("You Can't Check!");
            return false;
        }
        player.setState(Player.State.CHECK);
        return true;
    }
}
