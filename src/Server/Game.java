package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.SQLException;

public class Game {
    Dealer dealer = new Dealer();
    public Game() throws IOException, ClassNotFoundException, SQLException {
        int port = 8080;
        dealer.setUpGame(port);
    }

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        new Game();
    }
}

