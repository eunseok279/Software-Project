package Server;

import java.util.List;

public class Pot {
    private int potMoney;
    private boolean closed = false;
    List<Player> potPlayer;

    public Pot() {
        potMoney = 0;
    }

    public void plusPot(int money,Player player) {
        potMoney += money;
        player.setCurrentBet(player.getCurrentBet()-money);
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
