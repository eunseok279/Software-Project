package Server;

import java.io.IOException;

public class Bet { // ���� ���
    User user;

    public Bet(User user) {
        this.user = user;
        int betting = user.getCurrentBet();
    }

    public boolean bet(int betMoney) throws IOException { // �� ���� ù ����(�����ö� ����)
        if (betMoney > user.getMoney()) { // ���ñ� > ������
            user.sendMessage("Not Enough Money!!");
            return false;
        }
        else if(betMoney == user.getMoney()){
            user.sendMessage("All-In");
            return allIn();
        }
        if (betMoney < Round.basicBet) { // ���ñ� < �ּ� ����
            user.sendMessage("Minimum is >> " + Round.basicBet);
            return false;
        }
        user.betMoney(betMoney);
        Round.setBasicBet(betMoney); // ù ���� => �ּ� ���ñ�
        user.setState(User.State.RAISE);
        return true;
    }

    public boolean call(int callMoney) throws IOException { // �� ����� ���ñ��� ��
        if (callMoney > user.getMoney()) { // �� > ������ -> ���� Ȥ�� allin
            user.sendMessage("Not Enough Money!! You Should All-In or Fold");
            return false;
        } else if (callMoney == user.getMoney()) { // �� = ������ -> ����
            user.sendMessage("All-In");
            return allIn();
        }
        if (callMoney == 0) { // �⺻ ���ñ� = ���ñ� -> üũ
            user.sendMessage("Check");
            return check();
        }
        user.betMoney(callMoney);
        user.setState(User.State.CALL);
        return true;
    }

    public boolean raise(int raiseMoney, int currentBet, int basicBet) throws IOException { // �� ����� ������
        int minimumRaise = 2*(basicBet - currentBet); // �⺻ ���ñ� - ���� ���ñ��� �ι�
        if (raiseMoney  < minimumRaise) { // �η�����
            user.sendMessage("Minimum raise is >> " + minimumRaise);
            return false;
        }
        if (raiseMoney > user.getMoney()) { // ���ñ� > ������
            user.sendMessage("Not Enough Money!!");
            return false;
        } else if (raiseMoney == user.getMoney()) { // ���ñ� = ������ -> ����
            user.sendMessage("All In");
            return allIn();
        }
        if (raiseMoney + currentBet < basicBet) { // ��ü ���ñ� < �⺻ ���ñ�
            user.sendMessage("Minimum Betting >> " + basicBet);
            return false;
        } else if (raiseMoney + currentBet == basicBet) { // ��ü ���ñ� == �⺻ ���ñ� -> ��
            user.sendMessage("Call");
            return call(raiseMoney);
        }
        user.betMoney(raiseMoney);
        Round.setBasicBet(user.getCurrentBet()); // ����� �����ϸ� �⺻ ���ñ� = ��ü ���ñ�
        user.setState(User.State.RAISE);
        return true;
    }

    public void fold() {
        user.setState(User.State.FOLD);
    }

    public boolean allIn() {
        int money = user.getMoney();
        user.betMoney(money);
        user.setState(User.State.ALLIN);
        return true;
    }

    public boolean check() throws IOException { // ���� �ѱ�� ����x
        if (user.getCurrentBet() < Round.basicBet) { // ���� ���ñ� < �⺻ ���ñ� -> ��
            user.sendMessage("You Can't Check!");
            return false;
        }
        user.setState(User.State.CHECK);
        return true;
    }
}
