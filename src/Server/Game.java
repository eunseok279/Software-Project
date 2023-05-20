package Server;

public class Game {
    public static void main(String[] args) {
        int port = 8080;
        Dealer dealer = new Dealer();
        dealer.setUpGame(port);
    }
}

