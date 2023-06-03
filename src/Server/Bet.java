package Server;

import java.io.IOException;

public class Bet { // 배팅 방식
    User user;

    public Bet(User user) {
        this.user = user;
    }

    public boolean bet(int betMoney) throws IOException { // 각 라운드 첫 배팅(프리플랍 제외)
        if (betMoney > user.getMoney()) { // 배팅금 > 소지금
            user.sendMessage("Not Enough Money!!");
            return false;
        }
        else if(betMoney == user.getMoney()){
            user.sendMessage("All-In");
            return allIn();
        }
        if (betMoney < Round.basicBet) { // 배팅금 < 최소 배팅
            user.sendMessage("Minimum is >> " + Round.basicBet);
            return false;
        }
        user.betMoney(betMoney);
        Round.setBasicBet(betMoney); // 첫 배팅 => 최소 배팅금
        user.setState(User.State.RAISE);
        return true;
    }

    public boolean call(int callMoney) throws IOException { // 앞 사람의 배팅금을 콜
        if (callMoney > user.getMoney()) { // 콜 > 소지금 -> 폴드 혹은 allin
            user.sendMessage("Not Enough Money!! You Should All-In or Fold");
            return false;
        } else if (callMoney == user.getMoney()) { // 콜 = 소지금 -> 올인
            user.sendMessage("All-In");
            return allIn();
        }
        if (callMoney == 0) { // 기본 배팅금 = 배팅금 -> 체크
            user.sendMessage("Check");
            return check();
        }
        user.betMoney(callMoney);
        user.setState(User.State.CALL);
        return true;
    }

    public boolean raise(int raiseMoney, int currentBet, int basicBet) throws IOException { // 앞 사람의 레이즈
        int minimumRaise = 2*(basicBet - currentBet); // 기본 배팅금 - 나의 배팅금의 두배
        if (raiseMoney  < minimumRaise) { // 민레이즈
            user.sendMessage("Minimum raise is >> " + minimumRaise);
            return false;
        }
        if (raiseMoney > user.getMoney()) { // 배팅금 > 소지금
            user.sendMessage("Not Enough Money!!");
            return false;
        } else if (raiseMoney == user.getMoney()) { // 배팅금 = 소지금 -> 올인
            user.sendMessage("All In");
            return allIn();
        }
        if (raiseMoney + currentBet < basicBet) { // 전체 배팅금 < 기본 배팅금
            user.sendMessage("Minimum Betting >> " + basicBet);
            return false;
        } else if (raiseMoney + currentBet == basicBet) { // 전체 배팅금 == 기본 배팅금 -> 콜
            user.sendMessage("Call");
            return call(raiseMoney);
        }
        user.betMoney(raiseMoney);
        Round.setBasicBet(user.getBetting()); // 레이즈에 성공하면 기본 배팅금 = 전체 배팅금
        user.setState(User.State.RAISE);
        return true;
    }

    public boolean fold() {
        user.setState(User.State.FOLD);
        return true;
    }

    public boolean allIn() {
        int money = user.getMoney();
        user.betMoney(money);
        user.setState(User.State.ALLIN);
        return true;
    }

    public boolean check() throws IOException { // 턴을 넘긴다 배팅x
        if (user.getBetting() < Round.basicBet) { // 현재 배팅금 < 기본 배팅금 -> 콜
            user.sendMessage("You Can't Check!");
            return false;
        }
        user.setState(User.State.CHECK);
        return true;
    }
}
