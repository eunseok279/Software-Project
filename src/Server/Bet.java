package Server;

public class Bet { // ���� ���
    Player player;

    public Bet(Player player) {
        this.player = player;
        int betting = player.getCurrentBet();
    }

    public boolean firstBet(int betMoney) { // �� ���� ù ����(�����ö� ����)
        if (betMoney > player.getMoney()) { // ���ñ� > ������
            player.sendMessage("Not Enough Money!!");
            return false;
        } else if (betMoney == player.getMoney()) { // ���ñ� == ������
            player.sendMessage("First betting can't do all in");
            return false;
        }
        if (betMoney < Round.basicBet) { // ���ñ� < �ּ� ����
            player.sendMessage("Minimum is >> " + Round.basicBet);
            return false;
        }
        player.betMoney(betMoney);
        Round.setBasicBet(betMoney); // ù ���� => �ּ� ���ñ�
        player.setState(Player.State.CALL);
        return true;
    }

    public boolean call(int callMoney) { // �� ����� ���ñ��� ��
        if (callMoney > player.getMoney()) { // �� > ������ -> ���� Ȥ�� allin
            player.sendMessage("Not Enough Money!! You Should All-In or Fold");
            return false;
        } else if (callMoney == player.getMoney()) { // �� = ������ -> ����
            player.sendMessage("Your Money is All In");
            return allIn();
        }
        if (player.getCurrentBet() == Round.basicBet) { // �⺻ ���ñ� = ���ñ� -> üũ�� ������
            player.sendMessage("You don't have to call!");
            return false;
        }
        player.betMoney(callMoney);
        player.setState(Player.State.CALL);
        return true;
    }

    public boolean raise(int raiseMoney, int currentBet, int basicBet) { // �� ����� ������
        int minimumRaise = basicBet * 2 - currentBet; // �ּ� ���ñ��� �� ����� ���ñ��� 2��(�ڽ��� ���ñ� ����)
        if (raiseMoney  < minimumRaise) { // �ּ� ������ > ���ñ�
            player.sendMessage("Minimum raise is >> " + minimumRaise);
            return false;
        }
        if (raiseMoney > player.getMoney()) { // ���ñ� > ������
            player.sendMessage("Not Enough Money!!");
            return false;
        } else if (raiseMoney == player.getMoney()) { // ���ñ� = ������ -> ����
            player.sendMessage("Your Money is All In");
            return allIn();
        }
        if (raiseMoney + currentBet < basicBet) { // ��ü ���ñ� < �⺻ ���ñ�
            player.sendMessage("Minimum Betting >> " + basicBet);
            return false;
        } else if (raiseMoney + currentBet == basicBet) { // ��ü ���ñ� == �⺻ ���ñ� -> ��
            player.sendMessage("Call Is Execution!");
            return call(raiseMoney);
        }
        player.betMoney(raiseMoney);
        Round.setBasicBet(currentBet); // ����� �����ϸ� �⺻ ���ñ� = ��ü ���ñ�
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

    public boolean check(int basicBet) { // ���� �ѱ�� ����x
        if (player.getCurrentBet() < basicBet) { // ���� ���ñ� < �⺻ ���ñ� -> ��
            player.sendMessage("You must bet!");
            return false;
        }
        player.setState(Player.State.CHECK);
        return true;
    }
}
