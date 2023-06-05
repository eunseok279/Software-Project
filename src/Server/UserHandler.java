package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;
import java.util.List;

public class UserHandler implements Runnable {
    private final User user;
    private final List<User> users;
    BufferedReader in;
    CurrentTracker currentTracker;

    public UserHandler(User user, List<User> users, BufferedReader in, CurrentTracker currentTracker) throws IOException {
        this.user = user;
        this.users = users;
        this.in = in;
        this.currentTracker = currentTracker;
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("/")) {
                    handleCommand(message);
                } else {
                    handleChat(message);
                }
            }
        } catch (SocketException e) {
            currentTracker.index--;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                sendAll("/quit" + user.getName());
                in.close();
                user.getSocket().close();
                System.out.println(user.getName() + " is exit");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleCommand(String command) throws IOException {
        if (command.startsWith("//")) {
            if (command.startsWith("//quit")) {
                System.out.println("연결 종료 >> " + user.getName());
                sendAll("/quit" + user.getName());
                sendAll(user.getName() + "님이 나가셨습니다");
                user.setConnection(false);
                in.close();
                user.out.close();
                user.getSocket().close();
            } else if (command.startsWith("//ready")) {
                user.setReady(true);
                sendAll(user.getName() + " 레디");
                System.out.println(user.getName() + " is ready");
            } else if (command.startsWith("//unready")) {
                user.setReady(false);
                sendAll(user.getName() + " 언레디");
                System.out.println(user.getName() + " is unready");
            } else if (command.startsWith("//money")) {
                user.sendMessage(Integer.toString(user.getMoney()));
            } else if (command.startsWith("//ack")) {
                user.setAck(true);
            }
        } else {
            if (currentTracker.game) {
                User currentUser = users.get(currentTracker.index);
                if (user.equals(currentUser)) {
                    user.setCommand(command);
                    user.setAck(true);
                } else {
                    user.sendMessage("/error" + currentUser.getName() + "님의 차례입니다");
                    user.setCommand(null);
                }
            }
        }
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

