package Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client {
    Scanner scanner = new Scanner(System.in);
    Socket socket;


    public boolean access(String serverAddress,String name) {
        try {
            socket = new Socket(serverAddress, 8080);
            System.out.println("Connect Success!!");
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            oos.writeObject("/name " + name);

            MessageReceiver messageReceiver = new MessageReceiver(socket, ois);
            MessageSender messageSender = new MessageSender(socket, oos);
            Thread messageSend = new Thread(messageSender);
            messageSend.start();
            Thread messageReceive = new Thread(messageReceiver);
            messageReceive.start();
            System.out.println("Enter a command /+ready/unready/quit >> ");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
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

    public MessageSender(Socket socket, ObjectOutputStream oos) {
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
                oos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Thread out");
    }
}

