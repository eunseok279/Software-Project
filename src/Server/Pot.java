package Server;

import java.util.List;

public class Pot {
    private int potMoney;
    private boolean closed = false;
    List<Player> potPlayer;

    public Pot(List<Player> potPlayer) {
        this.potPlayer = potPlayer;
        potMoney = 0;
    }

    public void plusPot(int money) {
        potMoney += money;
    }

    public int getPot() {
        return potMoney;
    }
    public void setClosed(){
        this.closed = true;
    }
    public boolean getClosed(){
        return closed;
    }
}
