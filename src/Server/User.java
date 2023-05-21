package Server;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class User { // 플레이어의 정보가 담긴 클래스
    enum State {
        LIVE, FOLD, CALL, RAISE, ALLIN, CHECK, DEPLETED
    }

    Hand hand;
    Bet bet;
    private final String name;
    private int money;
    private int currentBet = 0;
    private State state;
    private boolean ready = false;
    private final Socket socket;
    ObjectOutputStream oos;
    PrintWriter out;
    private String command;
    private boolean result;

    public User(Socket socket, String name, int money) throws IOException {
        this(socket, name);
        this.state = State.LIVE;
        this.money = money;
        this.oos= new ObjectOutputStream(new ObjectOutputStream(socket.getOutputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        bet = new Bet(this);
        hand = new Hand();
    }

    public User(Socket socket, String name) throws IOException {
        this.socket = socket;
        this.name = name;
        this.state = State.LIVE;
        this.money = 200;
        bet = new Bet(this);
        hand = new Hand();
    }


    public String getName() {
        return name;
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
    public void sendCard(Card card) throws IOException {
        oos.writeObject(card);
        oos.flush();
    }
    public void sendMessage(String message) throws IOException {
       out.println(message);
    }

//    public String receiveAck() throws IOException, ClassNotFoundException {
//        InputStream is = socket.getInputStream();
//
//        // ObjectInputStream을 생성한다.
//        ObjectInputStream ois = new ObjectInputStream(is);
//
//        // 클라이언트로부터 메시지를 받는다.
//        String ack = (String) ois.readObject();
//
//        return ack;
//    }

    public void chooseBetAction(int basicBet, boolean noBet) throws IOException { // basicBet = 앞 사람의 배팅금
        command = null;                  // currentBet = 현재 내놓은 배팅금
        while (command == null) {
            try {
                Thread.sleep(100); // 0.1초마다 확인
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (command != null) {  // 입력이 됐으면
                if (noBet) { // 아무도 배팅을 안한 상태
                    if (command.startsWith("/bet")) { // 배팅
                        String[] parts = command.split(" ");
                        if (parts.length == 2) {
                            int betMoney = Integer.parseInt(parts[1]);
                            result = bet.bet(betMoney);
                            if (!result) command = null;
                            else break;
                        } else {
                            sendMessage("Wrong Input");
                            command = null;
                        }
                    } else if (command.startsWith("/fold")) { // 폴드
                        bet.fold();
                        break;
                    } else if (command.startsWith("/check")) { // 체크
                        break;
                    } else {
                        sendMessage("Wrong Choice");
                        result = false;
                        command = null;
                    }
                } else {    // 누가 배팅한 상태 -> 콜하거나 레이즈 혹은 폴드
                    if (command.startsWith("/call")) { // 콜
                        result = bet.call(basicBet - currentBet); // 기본 배팅 - 나의 배팅금
                        if (!result) command = null;
                        else break;
                    } else if (command.startsWith("/raise")) { // 레이즈
                        String[] parts = command.split(" ");
                        if (parts.length == 2) {
                            int raiseMoney = Integer.parseInt(parts[1]);
                            result = bet.raise(raiseMoney, currentBet, basicBet);
                            if (!result) command = null;
                            else break;
                        }
                    } else if (command.startsWith("/fold")) { // 폴드
                        bet.fold();
                        break;
                    } else if (command.startsWith("/check")) { // 체크 (빅블라인드만 예외적으로 사용가능)
                        result = bet.check();
                        if (!result) command = null;
                        else break;
                    } else {
                        sendMessage("Wrong Choice");
                        result = false;
                        command = null;
                    }
                }
            }
        }
    }

    public void respondToAllIn(boolean result, int basicBet) throws IOException {
        command = null;
        while (command == null) {
            try {
                Thread.sleep(100); // 0.1초마다 확인
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (command != null) {
                if (result) {
                    if (command.startsWith("/allin")) {
                        bet.allIn();
                        break;
                    } else if (command.startsWith("fold")) {
                        bet.fold();
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
                    } else if (command.startsWith("/fold")) {
                        bet.fold();
                        break;
                    } else if (command.startsWith("/raise")) {
                        String[] parts = command.split(" ");
                        if (parts.length == 2) {
                            int raiseMoney = Integer.parseInt(parts[1]);
                            result = bet.raise(raiseMoney, currentBet, basicBet);
                            if (!result) command = null;
                            else break;
                        }
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


