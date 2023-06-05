package Server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Round {
    List<User> users;
    List<Pot> pots = new ArrayList<>();
    CurrentTracker currentTracker;
    ExecutorService service = Executors.newSingleThreadExecutor();
    private int basicBet;
    private final int baseBet;
    private boolean noBetting = false;
    private boolean freeFlop = true;
    private StringBuilder info;
    private int userCount;
    private int potMoney;

    public Round(List<User> users, int baseBet, CurrentTracker currentTracker) throws IOException {
        this.users = users;
        pots.add(new Pot());
        this.currentTracker = currentTracker;

        this.baseBet = baseBet;
        this.userCount = users.size();
        potMoney = 0;
        service.submit(checkConnect);
    }

    public void smallBlind() throws IOException { // 시작 강제 베팅
        User user;
        if (userCount == 2) user = users.get(0);
        else user = users.get(1);
        user.sendMessage("스몰 블라인드입니다! 기본으로 " + baseBet / 2 + "달러 자동 배팅되었습니다.");
        user.betMoney(baseBet / 2);
        potMoney+= baseBet/2;
        sendTurnEnd(user);
    }

    public void bigBlind() throws IOException {
        User user;
        if (userCount == 2) user = users.get(1);
        else user = users.get(2);
        user.sendMessage("빅 블라인드입니다! 기본으로 " + baseBet + "달러 자동 배팅되었습니다.");
        user.betMoney(baseBet);
        potMoney += baseBet;
        sendTurnEnd(user);
    }

    public void freeFlop() throws IOException, InterruptedException { // 개인 카드 2장 분배 후 첫 배팅
        for (User user : users)
            sendInfo(user);
        basicBet = baseBet;
        playRound();
    }

    public void flop() throws IOException, InterruptedException { // 테이블 카드 3장 공개 후 2번째 배팅
        freeFlop = false;
        if (checkRemainUser(users)) {
            basicBet += baseBet;
            noBetting = true;
            currentTracker.index = findIfSBFold();
            playRound();
        }
    }

    public void turn() throws IOException, InterruptedException { // 4번째 테이블 카드 1장 공개 후 3번째 배팅
        if (checkRemainUser(users)) {
            basicBet += baseBet;
            noBetting = true;
            currentTracker.index = findIfSBFold();
            playRound();
        }
    }

    public void river() throws IOException, InterruptedException { // 5번째 테이블 카드 1장 공개 후 마지막 배팅
        if (checkRemainUser(users)) {
            basicBet += baseBet;
            noBetting = true;
            currentTracker.index = findIfSBFold();
            playRound();
        }
        calculatePot();
    }

    public void playRound() throws IOException, InterruptedException {
        int turn = 0;
        int allinMoney = 0;
        int minimumBet;
        boolean allin = false;
        sendState();
        while (turn < userCount) {
            User currentUser = users.get(currentTracker.index);
            if (canUserBet(currentUser)) {
                minimumBet = basicBet-currentUser.getAlreadyBet();
                if(freeFlop) potMoney-= currentUser.getCurrentBet();
                currentUser.sendMessage("당신의 차례입니다");
                sendTurn(currentUser);
                if (allin) { // 올인 상태 발생
                    int ownMoney = currentUser.getMoney() + currentUser.getAlreadyBet();
                    if (ownMoney < allinMoney) { //올인 금액보다 적다면 -> 콜할 능력X -> 폴드 or 올인
                        currentUser.sendMessage("/result폴드 혹은 올인만 가능");
                        currentUser.respondToAllIn(true, 0);
                    } else { // 올인 금액보다 같거나 크다면 -> 콜, 폴드, 레이즈
                        currentUser.sendMessage("/result콜, 폴드, 레이즈만 가능");
                        currentUser.respondToAllIn(false, allinMoney);
                    }
                } else if (noBetting) { // 모두가 체크거나 현재플레이어가 라운드 첫 베팅(프리플랍 제외)
                    currentUser.sendMessage("/result최소 배팅금 >> " + minimumBet + " [베팅, 폴드, 체크]");
                    currentUser.chooseBetAction(minimumBet,basicBet, true);
                } else {
                    currentUser.sendMessage("/result최소 레이즈 금액 >> " + 2 * minimumBet+ " [콜, 레이즈, 폴드, 체크]");
                    currentUser.chooseBetAction(minimumBet, basicBet, false);
                }

                potMoney += currentUser.getCurrentBet();

                sendInfo(currentUser);
                switch (currentUser.getState()) {
                    case CALL, CHECK -> turn++;
                    case RAISE -> { // 첫 배팅도 레이즈 상태
                        basicBet = currentUser.getAlreadyBet();
                        noBetting = false;
                        turn = 1; // 다음 플레이어부터 다시 카운팅 -> 레이즈한 유저가 0
                    }
                    case FOLD -> userCount--;// 베팅 인원에서 제외 폴드 하는 순간 팟에서 제외
                    case ALLIN -> {
                        turn = 0;
                        userCount--;
                        if (allinMoney == 0 || allinMoney < currentUser.getAlreadyBet()) {
                            allinMoney = currentUser.getAlreadyBet();
                            basicBet = allinMoney;
                            pots.add(new Pot());
                            allin = true;
                        } else allin = false;
                        currentUser.setState(User.State.DEPLETED);
                    }
                }
            }
            currentUser.setCommand(null);
            sendTurnEnd(currentUser);
            currentTracker.index = (currentTracker.index + 1) % users.size();
        }
        for (User user : users) {
            if (!(user.getState() == User.State.FOLD)&& !(user.getState() == User.State.DEPLETED)) {
                user.setState(User.State.LIVE);
            }
            user.setCurrentBet(0);
            sendInfo(user);
        }
    }

    private void sendInfo(User currentUser) throws IOException { // 개인 유저에게 소지금, 현재 베팅금, 팟에 들어간 금액
        info = new StringBuilder();
        info.append("/info ").append("/money").append(currentUser.getMoney()).append(" ").append("/bet").append(currentUser.getAlreadyBet());
        currentUser.sendMessage(info.toString());
    }
    private void sendTurn(User currentUser) throws IOException {
        sendAll("/turn"+currentTracker.index);
        currentUser.sendMessage("/time");
    }
    private void sendTurnEnd(User currentUser) throws IOException {
        info = new StringBuilder();
        info.append("/end").append(users.indexOf(currentUser)).append(" ").append("/bet").append(currentUser.getAlreadyBet()).append(" ").append("/state").append(currentUser.getState().name()).append(" ").append("/pot").append(potMoney);
        sendAll(info.toString());
    }
    private void sendState() throws IOException {
        info = new StringBuilder();
        for(User user: users){
            info.append("/state").append(user.getState().name()).append(" ");
        }
        sendAll(info.toString());
    }
    // 유저의 베팅 금액 -> 배팅하면 모든 유저에게 전송, 유저의 상태 -> 모든 유저에게 전송, 팟 -> 모든 유저
    private void calculatePot() {
        if (pots.size() == 1) {
            Pot pot = pots.get(0);
            for (User user : users) {
                pot.plusPot(user.getAlreadyBet(), user);
                if (!(user.getState() == User.State.FOLD)) pot.potUser.add(user);
            }
        } else { // 올인이 발생한 경우에만
            for (Pot pot : pots) {
                int min = minAllin();
                for (User user : users) {
                    // 올인한 돈보다 크면 현재팟에 min 만큼 저장하고 다음 팟에 저장
                    if (user.getAlreadyBet() == 0) continue; // 돈 없으면 스킵
                    else pot.plusPot(Math.min(user.getAlreadyBet(), min), user); // 올인한 돈보다 작으면 현재팟에
                    if (!(user.getState() == User.State.FOLD)) pot.potUser.add(user);
                }
            }
        }
    }

    private boolean checkRemainUser(List<User> users) { // 배팅할 수 있는 사람이 2명 미만이면 라운드 스킵
        int count = 0;
        for (User user : users) {
            if (canUserBet(user)) count++;
        }
        return count > 1; // 2명 미만이면 false
    }

    private int findIfSBFold() {
        int idx;
        for (idx = 1; idx < users.size(); idx++) {
            if (!(users.get(idx).getState() == User.State.FOLD)) {
                return idx;
            }
            idx = (idx - 1) % users.size();
            if (idx == -1) idx = users.size() - 1;
        }
        return users.size();
    }

    private int minAllin() {
        int min = 99999;
        for (User user : users) {
            if (user.getState() == User.State.DEPLETED) {
                int allin = user.getAlreadyBet();
                if (allin > 0) if (allin < min) min = allin;
            }
        }
        return min;
    }

    private boolean canUserBet(User user) {
        User.State state = user.getState();
        return !(state == User.State.FOLD) && !(state == User.State.DEPLETED);
    }

    private void sendAll(String message) throws IOException {
        for (User user : users) {
            user.sendMessage(message);
        }
    }

    Runnable checkConnect = () -> {
        while (true) {
            for (User user : users) {
                if (!user.isConnection()) {
                    user.setCommand("/fold");
                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    };
}


