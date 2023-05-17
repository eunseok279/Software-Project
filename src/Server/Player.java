package Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class Player { // 플레이어의 정보가 담긴 클래스
    enum State {
        BANKRUPTCY, LIVE, WINNER, FOLD, CALL, RAISE, ALLIN, CHECK
    }

    Hand hand;
    Bet bet;
    private String name;
    private int money;
    private int currentBet = 0;
    private State state;
    private boolean ready;
    private Socket socket;
    private PrintWriter out;
    private String command;
    private boolean result;

    public Player(Socket socket, String name) throws IOException {
        this.socket = socket;
        this.name = name;
        this.ready = false;
        this.state = State.LIVE;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        bet = new Bet(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void betMoney(int money) {
        this.currentBet += money;
    }

    public int getMoney() {
        return money;
    }

    public void minusMoney(int money) {
        this.money -= money;
    }

    public void plusMoney(int money) {
        this.money += money;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public Socket getSocket() {
        return socket;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void takeTurn(int basicBet) { // basicBet = 앞 사람의 배팅금
        command = null;                  // currentBet = 현재 내놓은 배팅금
        while (command == null) {
            try {
                Thread.sleep(100); // 0.1초마다 확인
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (command != null) {
                if (basicBet == 0) {
                    if (command.startsWith("/bet")) {
                        String[] parts = command.split(" ");
                        if (parts.length == 2) {
                            int betMoney = Integer.parseInt(parts[1]);
                            result = bet.firstBet(betMoney);
                            if (!result) command = null;
                            else break;
                        }
                    }
                } else {
                    if (command.startsWith("/bet")) command = null;
                    else if (command.startsWith("/call")) {
                        result = bet.call(basicBet - currentBet); // 전 사람의 배팅금 - 나의 배팅금
                        if (!result) command = null;
                        else break;
                    } else if (command.startsWith("/raise")) {
                        String[] parts = command.split(" ");
                        if (parts.length == 2) {
                            int raiseMoney = Integer.parseInt(parts[1]);
                            result = bet.raise(raiseMoney, currentBet, basicBet);
                            if (!result) command = null;
                            else break;
                        }
                    } else if (command.startsWith("/fold")) {
                        bet.fold();
                        break;
                    } else if (command.startsWith("/check")) {
                        if (!bet.check(basicBet)) command = null;
                        else break;
                    } else if (command.startsWith("/allin")) {
                        bet.allIn();
                        state = State.ALLIN;
                        break;
                    }
                }
            }
        }
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean getResult() {
        return result;
    }

    public void setCurrentBet(int amt) {
        currentBet = amt;
    }

    public int getCurrentBet() {
        return currentBet;
    }
}


