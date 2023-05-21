package Client;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Game {
    Scanner scanner = new Scanner(System.in);
    Socket socket;
    SharedCard sc = new SharedCard();

    public Game() throws IOException {
        //System.out.print("Insert Server IP >> ");
        int port = 8080;
        //String ip = scanner.next();
        access("Localhost", port);
    }

    public static void main(String[] args) throws IOException {
        new Game();
    }

    public void access(String serverAddress, int port) throws IOException {
        socket = new Socket(serverAddress, port);
        System.out.println("Connect Success!!");
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        System.out.print("Insert name >> ");
        String name = scanner.next();
        out.println(name);

        MessageReceiver messageReceiver = new MessageReceiver(socket, sc);
        MessageSender messageSender = new MessageSender(socket);
        ObjectReceiver objectReceiver = new ObjectReceiver(socket,sc);
        Thread messageReceive = new Thread(messageReceiver);
        messageReceive.start();
        Thread messageSend = new Thread(messageSender);
        messageSend.start();
        Thread objectReceive =new Thread(objectReceiver);
        objectReceive.start();
        System.out.println("Enter a command /+ready/unready/quit >> ");
    }
}

class MessageReceiver implements Runnable {
    private Socket socket;
    private SharedCard sc;

    public MessageReceiver(Socket socket, SharedCard sc) {
        this.socket = socket;
        this.sc = sc;
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String receivedMessage;
            while ((receivedMessage = reader.readLine()) != null) {
                if (receivedMessage.startsWith("/init")) {
                    while (true) if (sc.isOk) break;
                    sc.cards.clear();
                    sc.isOk = false;
                } else System.out.println(receivedMessage);
            }

            reader.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ObjectReceiver implements Runnable {
    private Socket socket;
    private List<Card> cards = new ArrayList<>();
    private SharedCard sc;

    public ObjectReceiver(Socket socket, SharedCard sc) {
        this.socket = socket;
        this.sc = sc;
    }

    @Override
    public void run() {
        try {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            Object receivedObject;
            while ((receivedObject = ois.readObject()) != null) {
                if (receivedObject instanceof Card) {
                    Card card = (Card) receivedObject;
                    sc.cards.add(card);
                    sc.isOk = true;
                }
            }

            ois.close();
            socket.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void clearCard() {
        cards.clear();
    }
}


class MessageSender implements Runnable {
    Scanner scanner = new Scanner(System.in);
    private final PrintWriter out;
    Socket socket;

    public MessageSender(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void run() {
        String message;
        while (!socket.isClosed()) {
            message = scanner.nextLine();
            if (message.equalsIgnoreCase("/quit")) {
                out.println("/quit");
                System.out.println("Connection Lost");
                try {
                    socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            }
            out.println(message);
        }
        System.out.println("Thread out");
    }
}

class SharedCard {
    List<Card> cards = new ArrayList<>();
    volatile boolean isOk = false;
}
