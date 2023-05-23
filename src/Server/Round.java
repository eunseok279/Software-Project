package Server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Round {
    List<User> users;
    List<Pot> pots = new ArrayList<>();
    CurrentPlayerTracker currentPlayerTracker;
    static int basicBet;
    private int baseBet;
    boolean noBetting = false;
    int userCount;

    public Round(List<User> users, int baseBet, CurrentPlayerTracker currentPlayerTracker) {
        this.users = users;
        pots.add(new Pot());
        this.currentPlayerTracker = currentPlayerTracker;

        this.baseBet = baseBet;
        this.userCount = users.size();
    }

    public void smallBlind() throws IOException { // 시작 강제 베팅
        User user;
        if (userCount == 2) user = users.get(0);
        else user = users.get(1);
        user.sendMessage("You are Small Blind!! Basic Betting >> 2");
        user.betMoney(baseBet / 2);
    }

    public void bigBlind() throws IOException {
        User user;
        if (userCount == 2) user = users.get(1);
        else user = users.get(2);
        user.sendMessage("You are Big Blind!! Basic Betting >> 4");
        user.betMoney(baseBet);
    }

    public void freeFlop() throws IOException { // 개인 카드 2장 분배 후 첫 배팅
        basicBet = baseBet;
        playRound();
    }

    public void flop() throws IOException { // 테이블 카드 3장 공개 후 2번째 배팅
        if (checkRemainUser(users)) {
            basicBet = baseBet;
            noBetting = true;
            currentPlayerTracker.index = findIfSBFold();
            playRound();
        }
    }

    public void turn() throws IOException { // 4번째 테이블 카드 1장 공개 후 3번째 배팅
        if (checkRemainUser(users)) {
            basicBet = baseBet;
            noBetting = true;
            currentPlayerTracker.index = findIfSBFold();
            playRound();
        }
    }

    public void river() throws IOException { // 5번째 테이블 카드 1장 공개 후 마지막 배팅
        if (checkRemainUser(users)) {
            basicBet = baseBet;
            noBetting = true;
            currentPlayerTracker.index = findIfSBFold();
            playRound();
            calculatePot();
        }
    }

    public void playRound() throws IOException {
        int turn = 0;
        int allinMoney = 0;
        boolean allin = false;
        while (turn < userCount) {
            User currentUser = users.get(currentPlayerTracker.index);
            if (canUserBet(currentUser)) {
                currentUser.sendMessage("Your Turn");
                if (allin) { // 올인 상태 발생
                    int ownMoney = currentUser.getMoney() + currentUser.getCurrentBet();
                    if (ownMoney < allinMoney) { //올인 금액보다 적다면 -> 콜할 능력X -> 폴드 or 올인
                        currentUser.sendMessage("Fold or All-In");
                        currentUser.respondToAllIn(true, 0);
                    } else { // 올인 금액보다 같거나 크다면 -> 콜, 폴드, 레이즈
                        currentUser.sendMessage("Call or Fold or Raise");
                        currentUser.respondToAllIn(false, allinMoney);
                    }
                } else if (noBetting) { // 모두가 체크거나 현재플레이어가 라운드 첫 베팅(프리플랍 제외)
                    currentUser.sendMessage("Current Minimum Bet >> " + basicBet);
                    currentUser.chooseBetAction(basicBet, true);
                } else {
                    currentUser.sendMessage("Minimum Raise >> " + 2 * (basicBet - currentUser.getCurrentBet()));
                    currentUser.chooseBetAction(basicBet, false);
                }

                if (currentUser.getState() == User.State.ALLIN) break; // 올인 발생

                switch (currentUser.getState()) {
                    case CALL, CHECK -> turn++;
                    case RAISE -> { // 첫 배팅도 레이즈 상태
                        noBetting = false;
                        turn = 1; // 다음 플레이어부터 다시 카운팅 -> 레이즈한 유저가 0
                    }
                    case FOLD -> userCount--;// 베팅 인원에서 제외 폴드 하는 순간 팟에서 제외
                    case ALLIN -> {
                        turn = 0;
                        userCount--;
                        if (allinMoney == 0 || allinMoney < currentUser.getCurrentBet()) {
                            allinMoney = currentUser.getCurrentBet();
                            pots.add(new Pot());
                            allin = true;
                        } else allin = false;
                        currentUser.setState(User.State.DEPLETED);
                    }
                }
            }
            currentUser.setCommand(null);
            currentPlayerTracker.index = (currentPlayerTracker.index + 1) % users.size();
        }
    }

    public void calculatePot() {
        if (pots.size() == 1) {
            Pot pot = pots.get(0);
            for (User user : users) {
                pot.plusPot(user.getCurrentBet(), user);
                if (!(user.getState() == User.State.FOLD)) pot.potUser.add(user);
            }
        } else {
            for (Pot pot : pots) {
                int min = minAllin();
                for (User user : users) {
                    // 올인한 돈보다 크면 현재팟에 min 만큼 저장하고 다음 팟에 저장
                    if (user.getCurrentBet() == 0) continue; // 돈 없으면 스킵
                    else pot.plusPot(Math.min(user.getCurrentBet(), min), user); // 올인한 돈보다 작으면 현재팟에
                    if (!(user.getState() == User.State.FOLD)) pot.potUser.add(user);
                }
            }
        }
    }

    public boolean checkRemainUser(List<User> users) { // 배팅할 수 있는 사람이 2명 미만이면 라운드 스킵
        int count = 0;
        for (User user : users) {
            if (canUserBet(user)) count++;
        }
        return count >= 2; // 2명 미만이면 false
    }

    public static void setBasicBet(int money) {
        basicBet = money;
    }

    public int findIfSBFold() {
        int idx = 1;
        while (users.get(idx).getState() == User.State.FOLD) {
            idx--;
            if (idx == -1) idx = users.size() - 1;
        }
        return idx;
    }

    public int minAllin() {
        int min = 99999;
        for (User user : users) {
            if (user.getState() == User.State.DEPLETED) {
                int allin = user.getCurrentBet();
                if (allin > 0)
                    if (allin < min) min = allin;
            }
        }
        return min;
    }

    public boolean canUserBet(User user) {
        User.State state = user.getState();
        return !(state == User.State.FOLD) && !(state == User.State.DEPLETED);
    }
}
