package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
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
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        MessageReceiver messageReceiver = new MessageReceiver(socket);
        System.out.print("이름을 입력하세요 >> ");
        String name = scanner.next();
        out.println(name);

        Thread messageReceive = new Thread(messageReceiver);
        messageReceive.start();
        while (true) {
            System.out.println("Enter a command (READY/UNREADY):");
            String command = scanner.nextLine();

            if (command.equals("UNREADY")) {
                out.println("UNREADY");
            } else if (command.equalsIgnoreCase("READY")) {
                out.println("READY");
            } else if (command.equalsIgnoreCase("QUIT")) {
                out.println("QUIT");
                System.out.println("연결이 끊어졌습니다.");
                socket.close();
                break;
            }
        }
    }

}

class MessageReceiver implements Runnable {
    private BufferedReader in;

    public MessageReceiver(Socket socket) throws IOException {
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void run() {
        String message;
        try {
            while ((message = in.readLine()) != null) {
                System.out.println(message);
            }
        } catch (IOException e) {
            // 에러 처리
        }
    }
}