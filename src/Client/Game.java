package Client;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Game {
    Scanner scanner = new Scanner(System.in);

    public Game() throws IOException {
        System.out.print("서버 포트와 IP를 입력 >> ");
        int port = scanner.nextInt();
        String ip = scanner.next();
        openServer(ip, port);
    }

    public static void main(String[] args) throws IOException {
        new Game();
    }

    public void openServer(String serverAddress, int port) throws IOException {
        Socket socket = new Socket(serverAddress, port);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        MessageReceiver messageReceiver = new MessageReceiver(socket);
        MessageSender messageSender = new MessageSender(socket);
        ObjectReceiver objectReceiver = new ObjectReceiver(socket);
        System.out.print("이름을 입력하세요 >> ");
        String name = scanner.next();
        out.println(name);

        Thread messageReceive = new Thread(messageReceiver);
        messageReceive.start();
        Thread messageSend = new Thread(messageSender);
        messageSend.start();
        Thread objectReceive = new Thread(objectReceiver);
        objectReceive.start();
        System.out.println("Enter a command /+ready/unready/quit >> ");
    }
}

class MessageReceiver implements Runnable {
    private final BufferedReader in;

    public MessageReceiver(Socket socket) throws IOException {
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void run() {
        String message;
        try {
            while ((message = in.readLine()) != null) {
                if (message.startsWith("/init")) {
                    ObjectReceiver.cardList.clear();
                } else System.out.println(message);
            }
        } catch (IOException e) {
            System.out.println("Connection lost");
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
        }
    }
}

class ObjectReceiver implements Runnable {
    private final ObjectInputStream ois;
    private final Socket socket;
    static final List<Card> cardList = new ArrayList<>();

    public ObjectReceiver(Socket socket) throws IOException {
        this.socket = socket;
        InputStream is = socket.getInputStream();
        this.ois = new ObjectInputStream(is);
    }

    @Override
    public void run() {
        Card card;
        while (true) {
            try {
                if ((card = (Card) ois.readObject()) == null) break;
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            cardList.add(card);
            System.out.println(card);
        }
    }

//    public void sendAck(String message) throws IOException {
//        OutputStream os = socket.getOutputStream();
//        ObjectOutputStream oos = new ObjectOutputStream(os);
//
//        oos.writeObject(message);
//        oos.flush();
//    }
}