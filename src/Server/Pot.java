package Server;

import java.util.List;

public class Pot {
    private int potMoney;
    private boolean closed = false;
    List<User> potUser;

    public Pot() {
        potMoney = 0;
    }

    public void plusPot(int money,User user) {
        potMoney += money;
        user.setCurrentBet(user.getCurrentBet()-money);
    }

    public int getPotMoney() {
        return potMoney;
    }
    public void setClosed(){
        this.closed =true;
    }
    public boolean getClosed(){
        return closed;
    }
}
