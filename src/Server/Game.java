package Server;

import java.io.IOException;
import java.util.Scanner;

public class Game {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("서버 포트를 입력 >> ");
        int port = scanner.nextInt();
        Dealer dealer = new Dealer();
        dealer.setUpGame(port);
    }
}

