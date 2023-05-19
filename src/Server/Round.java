package Server;

import java.util.List;

public class Round {
    List<User> users;
    List<Pot> pots;
    static int basicBet;
    private int baseBet;
    int currentUserIndex;
    boolean noBetting = false;
    int userCount;

    public Round(List<User> users, int currentUserIndex, int baseBet) {
        this.users = users;
        pots.add(new Pot());
        this.currentUserIndex = currentUserIndex;
        this.baseBet = baseBet;
        this.userCount = users.size();
    }

    public void smallBlind() { // 시작 강제 베팅
        User user = users.get(1);
        user.sendMessage("You are Small Blind!! Basic Betting >> 2");
        user.betMoney(baseBet / 2);
        pots.get(0).plusPot(baseBet / 2, user);
    }

    public void bigBlind() {
        User user = users.get(2);
        user.sendMessage("You are Big Blind!! Basic Betting >> 4");
        user.betMoney(baseBet);
        pots.get(0).plusPot(baseBet, user);
    }

    public void freeFlop() { // 개인 카드 2장 분배 후 첫 배팅
        basicBet = baseBet;
        playRound();
    }

    public void flop() { // 테이블 카드 3장 공개 후 2번째 배팅
        basicBet = baseBet;
        noBetting = true;
        currentUserIndex = findIfSBFold();
        playRound();
    }

    public void turn() { // 4번째 테이블 카드 1장 공개 후 3번째 배팅
        basicBet = baseBet;
        noBetting = true;
        currentUserIndex = findIfSBFold();
        playRound();
    }

    public void river() { // 5번째 테이블 카드 1장 공개 후 마지막 배팅
        basicBet = baseBet;
        noBetting = true;
        currentUserIndex = findIfSBFold();
        playRound();
    }

    public void playRound() {
        int turn = 0;
        if (!checkFolds(users)) return;
        while (turn < userCount) {
            User currentUser = users.get(currentUserIndex);

            User.State state = currentUser.getState();
            if (state == User.State.FOLD||state==User.State.DEPLETED) {
                turn++;
                continue;
            }

            currentUser.sendMessage("Your Turn");
            currentUser.sendMessage("Current Minimum Bet >> " + basicBet);

            if (noBetting) { // 모두가 체크거나 현재플레이어가 라운드 첫 베팅(프리플랍 제외)
                currentUser.chooseBetAction(basicBet, true);
            } else {
                currentUser.sendMessage("Minimum Raise >> " + 2 * (basicBet - currentUser.getCurrentBet()));
                currentUser.chooseBetAction(basicBet, false);
            }

            currentUserIndex = (currentUserIndex + 1) % users.size();
            switch (currentUser.getState()) {
                case CALL, CHECK -> turn++;
                case ALLIN -> {
                    turn = 0;
                    createSidePot();
                }
                case RAISE -> { // 첫 배팅도 레이즈 상태
                    noBetting = false;
                    turn = 0;
                }
                case FOLD -> userCount--;
            }
        }
        Pot pot = getOpenPot();
        for (User user : users) {
            pot.plusPot(user.getCurrentBet(), user);
        }
    }

    public void createSidePot() {
        pots.add(new Pot());
        Pot pot = getOpenPot();
        int min = min();
        int max = max();
        int turn = 0;
        while (turn < users.size()) {
            User currentUser = users.get(currentUserIndex);
            if (currentUser.getState() == User.State.FOLD) {
                turn++;
                continue;
            }
            int ownMoney = currentUser.getMoney() + currentUser.getCurrentBet();
            boolean result = ownMoney < basicBet; // basicBet = 올인 금액
            if (result) { //올인 금액보다 적다면 -> 폴드 or 올인
                currentUser.sendMessage("Your Turn");
                currentUser.sendMessage("Fold or All-In");
                currentUser.respondToAllIn(true);
            } else { // 올인 금액보다 같거나 크다면 -> 콜, 폴드, 레이즈
                currentUser.sendMessage("Your Turn");
                currentUser.sendMessage("Call or Fold or Raise");
                currentUser.respondToAllIn(false);
                switch (currentUser.getState()) {
                    case FOLD, CALL -> pot.plusPot(currentUser.getCurrentBet(), currentUser);
                    case RAISE -> pot.plusPot(min, currentUser);
                }
            }
            currentUserIndex = (currentUserIndex + 1) % users.size();
        }
        for ()
            pot.setClosed();
    }

    public boolean checkFolds(List<User> users) { // 1명만 제외 전부 폴드면 남은 한명 자동 승리
        int count = 0;
        for (User user : users) {
            if (user.getState() == User.State.FOLD) count++;
        }
        if (users.size() - count == 1) return false;
        else return true;
    }

    public static void setBasicBet(int money) {
        basicBet = money;
    }

    public Pot getOpenPot() { // 가장 나중에 만들어진 팟
        for (Pot pot : pots)
            if (!pot.getClosed()) return pot;
        return null;
    }

    public int findIfSBFold() {
        int idx = 1;
        while (true) {
            if (users.get(idx).getState() == User.State.FOLD) idx--;
            else break;
            if (idx == -1) idx = users.size() - 1;
        }
        return idx;
    }

    public int min() {
        int min = users.get(0).getMoney() + users.get(0).getCurrentBet();
        for (User user : users) {
            if (user.getState() == User.State.FOLD) continue;
            int ownMoney = user.getMoney() + user.getCurrentBet();
            if (ownMoney < min) min = ownMoney;
        }
        return min;
    }

    public int max() {
        int max = users.get(0).getMoney() + users.get(0).getCurrentBet();
        for (User user : users) {
            if (user.getState() == User.State.FOLD) continue;
            if (user.getMoney() + user.getCurrentBet() > max) max = user.getMoney() + user.getCurrentBet();
        }
        return max;
    }
}
