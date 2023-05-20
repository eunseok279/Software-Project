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
        String message = " ";
        try {
            while (true) {
                message = in.readLine();
                if (message.startsWith("/")) {
                    if (message.startsWith("/quit")) {
                        System.out.println("Connection Lost >> " + user.getName());
                        sendAll(user.getName() + "'s Connection Lost");
                        break;
                    } else if (message.startsWith("/ready")) {
                        user.setReady(true);
                        sendAll(user.getName() + " is ready");
                        System.out.println(user.getName() + " is ready");
                    } else if (message.startsWith("/unready")) {
                        user.setReady(false);
                        sendAll(user.getName() + " is unready");
                        System.out.println(user.getName() + " is unready");
                    }
                    if (Dealer.game) {
                        User currentUser = dealer.getCurrentUser();
                        if (user.equals(currentUser)) {
                            handleCommand(message);
                        } else user.sendMessage(currentUser.getName() + "'s turn");
                    }
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
        do {
            user.setCommand(command);
        } while (!user.getResult());
    }

    private void handleChat(String message) throws IOException {
        sendAll("[" + user.getName() + "] >> " + message);
    }

    private void sendAll(String message) throws IOException {
        for (User user : users) {
            user.sendMessage(message);
        }
    }
}
