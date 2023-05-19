package Server;

import java.util.List;

public class Round {
    List<User> users;
    List<Pot> pots;
    static int basicBet;
    private int baseBet;
    static int currentUserIndex;
    boolean noBetting = false;
    int userCount;

    public Round(List<User> users, int baseBet) {
        this.users = users;
        pots.add(new Pot());
        this.currentUserIndex = 3;
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
        if (checkRemainUser(users)) {
            basicBet = baseBet;
            noBetting = true;
            currentUserIndex = findIfSBFold();
            playRound();
        }
    }

    public void turn() { // 4번째 테이블 카드 1장 공개 후 3번째 배팅
        if (checkRemainUser(users)) {
            basicBet = baseBet;
            noBetting = true;
            currentUserIndex = findIfSBFold();
            playRound();
        }
    }

    public void river() { // 5번째 테이블 카드 1장 공개 후 마지막 배팅
        if (checkRemainUser(users)) {
            basicBet = baseBet;
            noBetting = true;
            currentUserIndex = findIfSBFold();
            playRound();
        }
    }

    public void playRound() {
        int turn = 0;
        Pot pot = getOpenPot();
        int allinMoney = 0;
        boolean allin = false;
        while (turn < userCount) {
            User currentUser = users.get(currentUserIndex);
            if (!canUserBet(currentUser)) continue;

            currentUser.sendMessage("Your Turn");
            currentUser.sendMessage("Current Minimum Bet >> " + basicBet);

            if(allin){ // 올인 상태 발생
                int ownMoney = currentUser.getMoney() + currentUser.getCurrentBet();
                if (ownMoney < allinMoney) { //올인 금액보다 적다면 -> 콜할 능력X -> 폴드 or 올인
                    currentUser.sendMessage("Fold or All-In");
                    currentUser.respondToAllIn(true, 0);
                } else { // 올인 금액보다 같거나 크다면 -> 콜, 폴드, 레이즈
                    currentUser.sendMessage("Your Turn");
                    currentUser.sendMessage("Call or Fold or Raise");
                    currentUser.respondToAllIn(false, allinMoney);
                }
            }
            else if (noBetting) { // 모두가 체크거나 현재플레이어가 라운드 첫 베팅(프리플랍 제외)
                currentUser.sendMessage("Current Minimum Bet >> " + basicBet);
                currentUser.chooseBetAction(basicBet, true);
            } else {
                currentUser.sendMessage("Minimum Raise >> " + 2 * (basicBet - currentUser.getCurrentBet()));
                currentUser.chooseBetAction(basicBet, false);
            }

            if (currentUser.getState() == User.State.ALLIN) break; // 올인 발생

            currentUserIndex = (currentUserIndex + 1) % users.size();
            switch (currentUser.getState()) {
                case CALL, CHECK -> {
                    pot.addIfNew(currentUser);
                    turn++;
                }
                case RAISE -> { // 첫 배팅도 레이즈 상태
                    noBetting = false;
                    turn = 1; // 다음 플레이어부터 다시 카운팅 -> 레이즈한 유저가 0
                    pot.addIfNew(currentUser);
                }
                case FOLD -> {userCount--;pot.plusPot(); }// 베팅 인원에서 제외 폴드 하는 순간 팟에서 제외
                case ALLIN -> {
                    if (allinMoney == 0) {
                        pots.add(new Pot());
                        allinMoney = currentUser.getCurrentBet();
                        turn = 0;
                        userCount--;
                        pot.addIfNew(currentUser);
                        allin = true;
                    }
                    if (allinMoney > currentUser.getCurrentBet()) {
                        allinMoney = currentUser.getCurrentBet();

                    } else {
                        pots.add(new Pot());

                    }
                }
            }
        }
//        if (turn != userCount) {
//            // 라운드 끝나고 올인 없이 끝나면 열린 팟에
//            for (User user : users) { // 배팅 금액 모으기
//                pot.plusPot(user.getCurrentBet(), user);
//            }
//        } else {// 만약 라운드에서 올인이 발생?
//            createSidePot(users.get((currentUserIndex++) % users.size()));// 올인 유저
//            userCount--; // 배팅 인원에서 제외
//        }
    }

    public void calculatePot() {
        for(Pot pot : pots){
            int min = minBet();
            for(User user : users){
                if(user.getState() == User.State.FOLD)
            }
        }
    }

    public void createSidePot(User allinUser) {
        pots.add(new Pot());
        Pot pot = getOpenPot();
        Pot sidePot = getSidePot();
        int min;
        int turn = 0; //올인한 유저 다음 부터 recount
        allinUser.setState(User.State.DEPLETED);
        int allinMoney = allinUser.getCurrentBet();
        while (turn < userCount) { //일단 배팅을 먼저 하고 가장 작은 놈 찾고 min 만큼 모두 배팅 -> 큰놈들은 따로 설정
            User currentUser = users.get(currentUserIndex);
            if (canUserBet(currentUser)) continue; // Fold 상태면 스킵
            currentUser.sendMessage("Your Turn");
            int ownMoney = currentUser.getMoney() + currentUser.getCurrentBet();

            if (ownMoney < allinMoney) { //올인 금액보다 적다면 -> 콜할 능력X -> 폴드 or 올인
                currentUser.sendMessage("Fold or All-In");
                currentUser.respondToAllIn(true, 0);
            } else { // 올인 금액보다 같거나 크다면 -> 콜, 폴드, 레이즈
                currentUser.sendMessage("Your Turn");
                currentUser.sendMessage("Call or Fold or Raise");
                currentUser.respondToAllIn(false, allinMoney);
            }
            switch (currentUser.getState()) {
                case FOLD -> {
                    userCount--;
                    pot.plusPot(currentUser.getCurrentBet(), currentUser);
                }
                case CALL -> {
                    turn++;
                    sidePot.addIfNew(currentUser);
                }
                case RAISE -> {
                    turn = 1;
                    sidePot.addIfNew(currentUser);
                }
                case ALLIN -> currentUser.setState(User.State.DEPLETED);
            }
            currentUserIndex = (currentUserIndex + 1) % users.size();
        }
        min = min();
        for (User user : users) {
            pot.plusPot(min, user);
        }
        pot.setClosed();
    }

    public boolean checkRemainUser(List<User> users) { // 배팅할 수 있는 사람이 2명 미만이면 라운드 스킵
        int count = 0;
        for (User user : users) {
            if (canUserBet(user)) count++;
        }
        if (count < 2) return false; // 2명 미만이면 false
        else return true;
    }

    public static void setBasicBet(int money) {
        basicBet = money;
    }

    public Pot getOpenPot() { // 현재 열린 팟 반환
        for (Pot pot : pots)
            if (!pot.getClosed()) return pot;
        return null;
    }

    public Pot getSidePot() { // 사이드 팟 반환
        Pot pot = getOpenPot();
        return pots.get(pots.indexOf(pot) + 1);
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

    public int minBet() {
        int min = users.get(0).getCurrentBet();
        for (User user : users) {
            if (user.getState() == User.State.FOLD) continue;
            int betting = user.getCurrentBet();
            if (betting < min) min = betting;
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

    public boolean canUserBet(User user) {
        User.State state = user.getState();
        return !(state == User.State.FOLD) && !(state == User.State.DEPLETED);
    }


}
