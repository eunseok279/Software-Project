package Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class User {
    // 플레이어의 정보가 담긴 클래스
    enum State {
        LIVE, FOLD, CALL, RAISE, ALLIN, CHECK, DEPLETED
    }

    Hand hand;
    Bet bet;
    private final String name;
    private int money;
    private int betting;
    private int currentBet;
    private boolean connection;
    private State state = State.LIVE;
    private boolean ready = false;
    private final Socket socket;
    PrintWriter out;
    private String command = null;
    private boolean result=false;
    private volatile boolean ack = false;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledFuture;


    public User(Socket socket, String name, PrintWriter out, int money) throws IOException {
        this(socket, name, out);
        this.money = money;
        this.betting = 0;
        this.connection = true;
        bet = new Bet(this);
        hand = new Hand();
    }

    public User(Socket socket, String name, PrintWriter out) throws IOException {
        this.socket = socket;
        this.name = name;
        this.out = out;
        this.money = 200;
        this.betting = 0;
        this.connection = true;
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

    public int getCurrentBet() {
        return currentBet;
    }

    public void setCurrentBet() {
        betting += currentBet;
        this.currentBet = 0;
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

    public void setReady(boolean ready) throws IOException {
        this.ready = ready;
        if (ready) sendMessage("/ready");
        else sendMessage("/unready");
    }

    public Socket getSocket() {
        return socket;
    }

    public void sendMessage(String message) throws IOException {
        out.write(message + "\n");
        out.flush();
    }

    public void sendPersonalCard() throws IOException, InterruptedException {
        String message = "/card" + " " + hand.cards.get(0).showCard() + " " + hand.cards.get(1).showCard() + " " + "/rank" + hand.determineHandRank().name();
        sendMessage(message);
    }

    public void setAck(boolean ack) {
        this.ack = ack;
    }

    private void commandACK() throws InterruptedException {
        Runnable foldTask = () -> {
            setCommand("/fold");
            ack = true;
        };
        scheduledFuture = executorService.schedule(foldTask, 30, TimeUnit.SECONDS);
        while (!ack) {
            Thread.sleep(100);
        }
        ack = false;
    }

    public void setConnection(boolean connection) {
        this.connection = connection;
    }

    public boolean isConnection() {
        return connection;
    }

    public void chooseBetAction(int basicBet, boolean noBet) throws IOException, InterruptedException { // basicBet = 앞 사람의 배팅금// currentBet = 현재 내놓은 배팅금
        result = false;
        commandACK();
        while (true) {
            Thread.sleep(100);
            if (command != null) {  // 입력이 됐으면
                synchronized (command) {
                    if (noBet) { // 아무도 배팅을 안한 상태
                        if (command.startsWith("/raise")) { // 배팅
                            int betMoney = Integer.parseInt(command.substring(6));
                            result = bet.bet(betMoney);
                            if (result) break;
                        } else if (command.startsWith("/fold")) { // 폴드
                            result = bet.fold();
                            break;
                        } else if (command.startsWith("/check")) { // 체크
                            result = bet.check();
                            break;
                        } else {
                            sendMessage("/result잘못된 선택입니다.");
                        }
                    } else {    // 누가 배팅한 상태 -> 콜하거나 레이즈 혹은 폴드
                        if (command.startsWith("/call")) { // 콜
                            result = bet.call(basicBet - currentBet); // 기본 배팅 - 나의 배팅금
                            if (result) break;
                        } else if (command.startsWith("/raise")) { // 레이즈
                            int raiseMoney = Integer.parseInt(command.substring(6));
                            result = bet.raise(raiseMoney, currentBet, basicBet);
                            if (result) break;
                        } else if (command.startsWith("/fold")) { // 폴드
                            result = bet.fold();
                            break;
                        } else if (command.startsWith("/check")) { // 체크 (빅블라인드만 예외적으로 사용가능)
                            result = bet.check();
                            if (result) break;
                        } else {
                            sendMessage("/result잘못된 선택입니다.");
                        }
                    }
                }
                command = null;
            }
        }
        scheduledFuture.cancel(false);
    }

    public void respondToAllIn(boolean allin, int basicBet) throws IOException, InterruptedException {
        result = false;
        commandACK();
        while (true) {
            try {
                Thread.sleep(100); // 0.1초마다 확인
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (command != null) {
                synchronized (command) {
                    if (allin) {
                        if (command.startsWith("/allin")) {
                            result = bet.allIn();
                            break;
                        } else if (command.startsWith("/fold")) {
                            result = bet.fold();
                            break;
                        } else {
                            sendMessage("/result잘못된 선택입니다.");
                        }
                    } else {
                        if (command.startsWith("/call")) {
                            result = bet.call(basicBet - betting); // 기본 배팅 - 나의 배팅금
                            if (result) break;
                        } else if (command.startsWith("/fold")) {
                            result = bet.fold();
                            break;
                        } else if (command.startsWith("/raise")) {
                            int raiseMoney = Integer.parseInt(command.substring(6));
                            result = bet.raise(raiseMoney, betting, basicBet);
                            if (result) break;
                        }else {
                            sendMessage("/result잘못된 선택입니다.");
                        }
                    }
                }
                command = null;
            }
        }
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean getResult() {
        return result;
    }

    public void setBetting(int amt) {
        betting = amt;
    }

    public int getBetting() {
        return betting;
    }
}


