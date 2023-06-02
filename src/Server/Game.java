package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.SQLException;

public class Game {
    Dealer dealer = new Dealer();
    public Game() throws ClassNotFoundException, SQLException, IOException {
        int port = 8080;
        dealer.setUpGame(port);
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException, IOException {
        new Game();
    }
}

