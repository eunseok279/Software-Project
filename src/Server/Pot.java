package Server;

import java.util.HashSet;
import java.util.Set;

public class Pot {
    private int potMoney;
    Set<User> potUser;

    public Pot() {
        potMoney = 0;
        potUser = new HashSet<>();
    }

    public void plusPot(int money,User user) {
        potMoney += money;
        user.setBetting(user.getBetting()-money);
    }

    public int getPotMoney() {
        return potMoney;
    }
}
