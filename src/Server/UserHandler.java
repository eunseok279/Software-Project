package Server;

import java.io.*;
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
        }
        catch (SocketException e) {
            currentTracker.index--;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                sendAll("/quit"+user.getName());
                sendAll(user.getName() + " is exit");
                in.close();
                user.getSocket().close();
                users.remove(user);
                System.out.println(user.getName() + " is exit");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleCommand(String command) throws IOException {
        if (command.startsWith("//")) {
            if (command.startsWith("//quit")) {
                System.out.println("Connection Lost >> " + user.getName());
                sendAll("/quit"+user.getName());
                sendAll(user.getName() + " is exit");
                user.getSocket().close();
            } else if (command.startsWith("//ready")) {
                user.setReady(true);
                sendAll(user.getName() + " is ready");
                System.out.println(user.getName() + " is ready");
            } else if (command.startsWith("//unready")) {
                user.setReady(false);
                sendAll(user.getName() + " is unready");
                System.out.println(user.getName() + " is unready");
            } else if (command.startsWith("//money")) {
                user.sendMessage(Integer.toString(user.getMoney()));
            }
        } else {
            if (currentTracker.game) {
                User currentUser = users.get(currentTracker.index);
                if (user.equals(currentUser)) {
                    do {
                        user.setCommand(command);
                    } while (!user.getResult());
                    user.sendMessage("Turn End");
                } else user.sendMessage("/error"+currentUser.getName() + "'s turn");
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

