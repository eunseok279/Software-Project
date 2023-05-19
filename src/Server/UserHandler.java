package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class UserHandler implements Runnable {
    private final User user;
    private final List<User> users;
    BufferedReader in;
    Dealer dealer;

    public UserHandler(User user, List<User> users, Dealer dealer) throws IOException {
        this.user = user;
        this.users = users;
        this.dealer = dealer;
        in = new BufferedReader(new InputStreamReader(user.getSocket().getInputStream()));
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                User currentUser = dealer.getCurrentUser();
                if (message.startsWith("/")) {
                    if (message.startsWith("/quit")) {
                        System.out.println("연결이 끊어졌습니다: " + user.getName());
                        sendAll(user.getName() + "님의 연결이 끊어졌습니다.");
                        break;
                    }
                    else if (message.startsWith("/ready")) {
                        user.setReady(true);
                        sendAll(user.getName() + " is ready");
                    } else if (message.equals("/unready")) {
                        user.setReady(false);
                        sendAll(user.getName() + " is unready");
                    }
                    if (user.equals(currentUser)) {
                        handleCommand(message);
                    } else user.sendMessage(currentUser.getName() + "'s turn");
                } else {
                    handleChat(message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                user.getSocket().close();
                users.remove(user);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleCommand(String command) throws IOException {
        do{
            user.setCommand(command);
        } while(!user.getResult());

    }

    private void handleChat(String message) {
        sendAll(user.getName()+" >> "+message);
    }

    private void sendAll(String message) {
        for (User user : users) {
            user.sendMessage(message);
        }
    }
}
