package Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Game {
    Scanner scanner = new Scanner(System.in);
    Socket socket;
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
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        System.out.print("Insert name >> ");
        String name = scanner.next();
        oos.writeObject(name);

        MessageReceiver messageReceiver = new MessageReceiver(socket, ois);
        MessageSender messageSender = new MessageSender(socket, oos);
        Thread messageSend = new Thread(messageSender);
        messageSend.start();
        Thread messageReceive = new Thread(messageReceiver);
        messageReceive.start();
        System.out.println("Enter a command /+ready/unready/quit >> ");
    }
}

class MessageReceiver implements Runnable {
    private final Socket socket;
    ObjectInputStream ois;
    List<Card> cards = new ArrayList<>();

    public MessageReceiver(Socket socket, ObjectInputStream ois) {
        this.socket = socket;
        this.ois = ois;
    }

    @Override
    public void run() {
        try {
            Object receivedObject;
            while ((receivedObject = ois.readObject()) != null) {
                if (receivedObject instanceof Card card) {
                    cards.add(card);
                } else if (receivedObject instanceof String message) {
                    if (message.startsWith("/init")) cards.clear();
                    else System.out.println(message);
                }
            }

            ois.close();
            socket.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}

class MessageSender implements Runnable {
    Scanner scanner = new Scanner(System.in);
    private final ObjectOutputStream oos;
    Socket socket;

    public MessageSender(Socket socket, ObjectOutputStream oos) throws IOException {
        this.socket = socket;
        this.oos = oos;
    }

    public void run() {
        String message;
        try {
            while (!socket.isClosed()) {
                message = scanner.nextLine();
                if (message.equalsIgnoreCase("/quit")) {
                    oos.writeObject("/quit");
                    System.out.println("Connection Lost");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                }
                oos.writeObject(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Thread out");
    }
}