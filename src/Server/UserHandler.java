package Server;

import java.io.BufferedReader;
import java.io.IOException;

public class UserHandler implements Runnable {
    private final User user;
    BufferedReader in;
    GameState gameState;

    public UserHandler(User user, BufferedReader in, GameState gameState) throws IOException {
        this.user = user;
        this.in = in;
        this.gameState = gameState;
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
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                sendAll("/quit" + user.getName());
                in.close();
                gameState.remove(user);
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
            if (gameState.game) {
                User currentUser = gameState.gameUser.get(gameState.index);
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
        for (User user : gameState.users) {
            user.sendMessage(message);
        }
    }
}

