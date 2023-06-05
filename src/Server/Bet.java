package Server;

import java.io.IOException;

public class Bet { // 배팅 방식
    User user;

    public Bet(User user) {
        this.user = user;
    }

    //currentBet = 0
    public boolean bet(int betMoney,int basicBet) throws IOException { // 각 라운드 첫 배팅(프리플랍 제외)
        if (betMoney > user.getMoney()) { // 배팅금 > 소지금
            user.sendMessage("/error돈이 부족합니다");
            return false;
        } else if (betMoney == user.getMoney()) {
            user.sendMessage("/result올인");
            return allIn();
        }
        else if (betMoney < basicBet) { // 배팅금 < 최소 배팅
            user.sendMessage("/error최소 배팅금은 >> " + basicBet);
            return false;
        }
        user.betMoney(betMoney);
        user.setState(User.State.RAISE);
        return true;
    }

    public boolean call(int callMoney) throws IOException { // 앞 사람의 배팅금을 콜
        if (callMoney > user.getMoney()) { // 콜 > 소지금 -> 폴드 혹은 allin
            user.sendMessage("/error콜하기엔 충분하지 않습니다!!");
            return false;
        } else if (callMoney == user.getMoney()) { // 콜 = 소지금 -> 올인
            user.sendMessage("/result올인");
            return allIn();
        }
        if (callMoney == 0) { // 기본 배팅금 = 배팅금 -> 체크
            user.sendMessage("/result체크");
            return check(callMoney);
        }
        user.betMoney(callMoney);
        user.setState(User.State.CALL);
        return true;
    }

    // currentBet 현재 라운드에서 배팅한 돈
    // alreadyBet 전체 배팅돈
    public boolean raise(int raiseMoney, int basicBet) throws IOException { // 앞 사람의 레이즈
        int minimumRaise = 2 * basicBet; // 기본 배팅금 - 나의 배팅금의 두배
        if (raiseMoney < minimumRaise) { // 민레이즈
            user.sendMessage("/error최소 레이즈 금액 >> " + minimumRaise);
            return false;
        } else if (raiseMoney > user.getMoney()) { // 배팅금 > 소지금
            user.sendMessage("/error돈이 부족합니다");
            return false;
        } else if (raiseMoney == user.getMoney()) { // 배팅금 = 소지금 -> 올인
            user.sendMessage("/result올인");
            return allIn();
        } else if (raiseMoney == basicBet) { // 전체 배팅금 == 기본 배팅금 -> 콜
            user.sendMessage("/result콜");
            return call(raiseMoney);
        }
        user.betMoney(raiseMoney);
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

    public boolean check(int basicBet) throws IOException { // 턴을 넘긴다 배팅x
        if (user.getAlreadyBet()<basicBet) { // 현재 배팅금 < 기본 배팅금 -> 콜
            user.sendMessage("/error체크할 수 없습니다");
            return false;
        }
        user.setState(User.State.CHECK);
        return true;
    }
}
