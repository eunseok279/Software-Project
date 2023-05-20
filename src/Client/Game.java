package Client;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Game {
    Scanner scanner = new Scanner(System.in);

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
        Socket socket = new Socket(serverAddress, port);
        System.out.println("Connect Success!!");
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        System.out.print("Insert name >> ");
        String name = scanner.next();
        out.println(name);

        MessageReceiver messageReceiver = new MessageReceiver(socket);
        MessageSender messageSender = new MessageSender(socket);
        Thread messageReceive = new Thread(messageReceiver);
        messageReceive.start();
        Thread messageSend = new Thread(messageSender);
        messageSend.start();
        System.out.println("Enter a command /+ready/unready/quit >> ");
    }
}

class MessageReceiver implements Runnable {
    private final ObjectInputStream ois;
    private final List<Card> cardList = new ArrayList<>();

    public MessageReceiver(Socket socket) throws IOException {
        this.ois = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        Object obj;
        while (true) {
            try {
                obj = ois.readObject();
                if (obj == null) break;
                if (obj instanceof Card) {
                    Card card = (Card) obj;
                    cardList.add(card);
                    System.out.println(card);
                } else if (obj instanceof String) {
                    String message = (String) obj;
                    if (message.startsWith("/init")) {
                        cardList.clear();
                    } else {
                        System.out.println(message);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Connection lost");
                break;
            }
        }
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
        while (true) {
            message = scanner.nextLine();
            if (message.equalsIgnoreCase("/quit")) {
                out.println("/quit");
                System.out.println("연결이 끊어졌습니다.");
                try {
                    socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            }
            out.println(message);
            if(socket.isClosed()) break;
        }
    }
}

