package Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class User { // 플레이어의 정보가 담긴 클래스
    enum State {
        LIVE, FOLD, CALL, RAISE, ALLIN, CHECK,DEPLETED
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

    public User(Socket socket, String name) throws IOException {
        this.socket = socket;
        this.name = name;
        this.ready = false;
        this.state = State.LIVE;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        bet = new Bet(this);
        hand = new Hand();
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
        this.money -= money;
    }

    public int getMoney() {
        return money;
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

    public void chooseBetAction(int basicBet, boolean noBet) { // basicBet = 앞 사람의 배팅금
        command = null;                  // currentBet = 현재 내놓은 배팅금
        while (command == null) {
            try {
                Thread.sleep(100); // 0.1초마다 확인
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (command != null) {
                if (noBet) {
                    if (command.startsWith("/bet")) {
                        String[] parts = command.split(" ");
                        if (parts.length == 2) {
                            int betMoney = Integer.parseInt(parts[1]);
                            result = bet.bet(betMoney);
                            if (!result) command = null;
                            else break;
                        }
                    } else if (command.startsWith("/fold")) {
                        bet.fold();
                        break;
                    } else if (command.startsWith("/check")) {
                        break;
                    } else {
                        sendMessage("Wrong Choice");
                        command = null;
                    }
                } else {
                    if (command.startsWith("/call")) {
                        result = bet.call(basicBet - currentBet); // 기본 배팅 - 나의 배팅금
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
                    } else {
                        sendMessage("Wrong Choice");
                        command = null;
                    }
                }
            }
        }
    }

    public void respondToAllIn(boolean result){
        command = null;
        while (command == null) {
            try {
                Thread.sleep(100); // 0.1초마다 확인
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (command != null) {
                if (result) {
                    if(command.startsWith("/allin")){
                        bet.allIn(); break;
                    }else if(command.startsWith("fold")){
                        bet.fold(); break;
                    }else{
                        sendMessage("Wrong Choice");
                        command = null;
                    }
                }
                else{

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


