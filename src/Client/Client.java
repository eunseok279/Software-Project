package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Client {
    GUI gui;
    Controller controller;
    Socket socket;
    BufferedReader in;
    PrintWriter out;

    public Client() {
        gui = new GUI();
        controller = new Controller(this, gui);
    }

    public boolean access(String serverAddress, String name) {
        try {
            socket = new Socket(serverAddress, 8080);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream());
            sendMessage("/name" + name);

            MessageReceiver messageReceiver = new MessageReceiver(socket, in, controller);
            Thread messageReceive = new Thread(messageReceiver);
            messageReceive.start();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void sendMessage(String message) throws IOException {
        out.write(message + "\n");
        out.flush();
        executeCommand(message);
    }

    private void executeCommand(String message) {
        if (message.startsWith("//")) {
            if (message.startsWith("//quit")) {
                System.out.println("Connection Lost");
                try {
                    socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (message.startsWith("//ready")) {
                gui.setReady(true);
            } else if (message.startsWith("//unready")) {
                gui.setReady(false);
            }
        }
    }

    public static void main(String[] args) {
        new Client();
    }
}

class MessageReceiver implements Runnable {
    private final Socket socket;
    Controller controller;
    BufferedReader in;
    List<Card> cards = new ArrayList<>();

    public MessageReceiver(Socket socket, BufferedReader in, Controller controller) {
        this.socket = socket;
        this.in = in;
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("/")) command(message);
                else {
                    controller.appendMsg(message);
                }
            }
            in.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void command(String message) throws IOException {
        if (message.startsWith("/init")) cards.clear();
        else if (message.contains("/name")) {
            String name = message.substring(5);
            controller.nameList.add(name);
        } else if (message.contains("/finish")) {
            controller.setUserList();
        } else if (message.contains("/quit")) {
            String name = message.substring(5);
            controller.nameList.remove(name);
            controller.setUserList();
        } else if (message.startsWith("/unready")) {
            controller.gui.setReady(false);
        } else if (message.startsWith("/ready")) {
            controller.gui.setReady(true);
        } else if (message.startsWith("/game")) {
            controller.startGame();
        } else if(message.startsWith("/info")){
            List<String> split = new ArrayList<>(Arrays.asList(message.split(" ")));
            split.remove(0);
            for(String info: split){
                controller.appendInfo(info);
            }
        } else if (message.startsWith("/rank")) {
            controller.appendInfo(message);
        } else if (message.startsWith("/win")) {
            String index = message.substring(4);
            controller.winner(index);
        } else if (message.startsWith("/lose")) {
            String index = message.substring(5);
            controller.loser(index);
        } else if (message.startsWith("/card")) {
            List<String> split = new ArrayList<>(Arrays.asList(message.split(" ")));
            split.remove(0);
            for (String card : split) {
                if (card.startsWith("/rank")) controller.appendInfo(card);
                else {
                    String suit = card.substring(0, 1);
                    String rank = card.substring(1);
                    controller.addCard(suit, rank);
                }
            }
            controller.client.sendMessage("//ack");
        }
    }
}