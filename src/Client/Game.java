package Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Game {
    public Game() throws IOException {
        Client client = new Client();
        GUI gui = new GUI();
        Controller controller = new Controller(client,gui);
    }

    public static void main(String[] args) throws IOException {
        new Game();
    }
}