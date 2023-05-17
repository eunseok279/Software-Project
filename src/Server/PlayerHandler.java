package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class PlayerHandler implements Runnable {
    private final Player player;
    private final List<Player> players;
    BufferedReader in;
    Dealer dealer;

    public PlayerHandler(Player player, List<Player> players, Dealer dealer) throws IOException {
        this.player = player;
        this.players = players;
        this.dealer = dealer;
        in = new BufferedReader(new InputStreamReader(player.getSocket().getInputStream()));
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                Player currentPlayer = dealer.getCurrentPlayer();
                if (message.startsWith("/")) {
                    if (message.startsWith("/quit")) {
                        System.out.println("연결이 끊어졌습니다: " + player.getName());
                        sendAll(player.getName() + "님의 연결이 끊어졌습니다.");
                        break;
                    }
                    else if (message.startsWith("/ready")) {
                        player.setReady(true);
                        sendAll(player.getName() + " is ready");
                    } else if (message.equals("/unready")) {
                        player.setReady(false);
                        sendAll(player.getName() + " is unready");
                    }
                    if (player.equals(currentPlayer)) {
                        handleCommand(message);
                    } else player.sendMessage(currentPlayer.getName() + "'s turn");
                } else {
                    handleChat(message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                player.getSocket().close();
                players.remove(player);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleCommand(String command) throws IOException {
        do{
            player.setCommand(command);
        } while(!player.getResult());

    }

    private void handleChat(String message) {
        sendAll(player.getName()+" >> "+message);
    }

    private void sendAll(String message) {
        for (Player player : players) {
            player.sendMessage(message);
        }
    }
}
