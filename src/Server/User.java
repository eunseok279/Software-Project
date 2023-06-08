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
    private int alreadyBet;
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
        this.alreadyBet = 0;
        this.connection = true;
        bet = new Bet(this);
        hand = new Hand();
    }

    public User(Socket socket, String name, PrintWriter out) throws IOException {
        this.socket = socket;
        this.name = name;
        this.out = out;
        this.money = 200;
        this.alreadyBet = 0;
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
    public void setCurrentBet(int money) {
        currentBet = money;
    }

    public void betMoney(int money) {
        this.currentBet += money;
        this.alreadyBet +=money;
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

    public void sendPersonalCard() throws IOException {
        hand.handRank = hand.determineHandRank();
        String message = "/card" + " " + hand.cards.get(0).showCard() + " " + hand.cards.get(1).showCard() + " " + "/rank" + hand.handRank.name();
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
            if(command!=null) break;
        }
        ack = false;
    }
    public boolean isConnection() {
        return !connection;
    }

    public void chooseBetAction(int minimumBet, int basicBet, boolean noBet) throws IOException, InterruptedException { // basicBet = 앞 사람의 배팅금// currentBet = 현재 내놓은 배팅금
        result = false;
        commandACK();
        while (true) {
            Thread.sleep(100);
            if (command != null) {  // 입력이 됐으면
                synchronized (command) {
                    if (noBet) { // 아무도 배팅을 안한 상태
                        if (command.startsWith("/raise")) { // 배팅
                            int betMoney = Integer.parseInt(command.substring(6));
                            result = this.bet.bet(betMoney,minimumBet);
                            if (result) break;
                        } else if (command.startsWith("/fold")) { // 폴드
                            this.bet.fold();
                            break;
                        } else if (command.startsWith("/check")) { // 체크
                            result = this.bet.check(0);
                            if(result)break;
                        } else if(command.startsWith("/allin")){
                            this.bet.allIn();
                            break;
                        }else {
                            sendMessage("/error잘못된 선택입니다.");
                        }
                    } else {    // 누가 배팅한 상태 -> 콜하거나 레이즈 혹은 폴드
                        if (command.startsWith("/call")) { // 콜
                            result = this.bet.call(minimumBet); // 기본 배팅 - 나의 배팅금
                            if (result) break;
                        } else if (command.startsWith("/raise")) { // 레이즈
                            int raiseMoney = Integer.parseInt(command.substring(6));
                            result = this.bet.raise(raiseMoney, minimumBet);
                            if (result) break;
                        } else if (command.startsWith("/fold")) { // 폴드
                            result = this.bet.fold();
                            break;
                        } else if (command.startsWith("/check")) { // 체크 (빅블라인드만 예외적으로 사용가능)
                            result = this.bet.check(basicBet);
                            if (result) break;
                        }else if(command.startsWith("/allin")){
                            this.bet.allIn();
                            break;
                        } else {
                            sendMessage("/error잘못된 선택입니다.");
                        }
                    }
                    command = null;
                }
            }
        }
        scheduledFuture.cancel(false);
        sendMessage("/send");
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
                            bet.allIn();
                            break;
                        } else if (command.startsWith("/fold")) {
                            bet.fold();
                            break;
                        } else {
                            sendMessage("/error잘못된 선택입니다.");
                        }
                    } else {
                        if (command.startsWith("/call")) {
                            result = bet.call(basicBet - alreadyBet); // 기본 배팅 - 나의 배팅금
                            if (result) break;
                        } else if (command.startsWith("/fold")) {
                            bet.fold();
                            break;
                        } else if (command.startsWith("/raise")) {
                            int raiseMoney = Integer.parseInt(command.substring(6));
                            result = bet.raise(raiseMoney, basicBet);
                            if (result) break;
                        } else if(command.startsWith("/allin")){
                            bet.allIn();
                            break;
                        }else {
                            sendMessage("/error잘못된 선택입니다.");
                        }
                    }
                    command = null;
                }
            }
        }
        scheduledFuture.cancel(false);
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setAlreadyBet(int amt) {
        alreadyBet = amt;
    }

    public int getAlreadyBet() {
        return alreadyBet;
    }
}


