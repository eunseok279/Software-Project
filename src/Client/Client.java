package Client;

import Server.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client {
    GUI gui;
    Controller controller;
    Socket socket;
    ObjectOutputStream oos;
    ObjectInputStream ois;
    public Client(){
        gui = new GUI();
        controller = new Controller(this,gui);
    }
    public boolean access(String serverAddress,String name) {
        try {
            socket = new Socket(serverAddress, 8080);
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
            oos.writeObject("/name " + name);

            MessageReceiver messageReceiver = new MessageReceiver(socket, ois,controller);
            Thread messageReceive = new Thread(messageReceiver);
            messageReceive.start();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    public void sendMessage(String message) throws IOException {
        oos.writeObject(message);
        executeCommand(message);
        oos.flush();
    }

    private void executeCommand(String message) {
        if (message.startsWith("//")) {
            if (message.startsWith("//quit")) {
                System.out.println("Connection Lost");
                try {
                    socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (message.startsWith("//ready")) {
                gui.setReady(true);
            } else if (message.startsWith("//unready")) {
                gui.setReady(false);
            }
        }
    }

    public static void main(String[] args) {
        new Client();
    }
}
class MessageReceiver implements Runnable {
    private final Socket socket;
    Controller controller;
    ObjectInputStream ois;
    List<Card> cards = new ArrayList<>();

    public MessageReceiver(Socket socket, ObjectInputStream ois,Controller controller) {
        this.socket = socket;
        this.ois = ois;
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            Object receivedObject;
            while ((receivedObject = ois.readObject()) != null) {
                if (receivedObject instanceof Card card) {

                } else if (receivedObject instanceof String message) {
                    if(message.startsWith("/"))
                        command(message);
                    else {
                        controller.appendMsg(message);
                    }
                }
            }
            ois.close();
            socket.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void command(String message) {
        if (message.startsWith("/init")) cards.clear();
        else if(message.contains("/name")){
            String name = message.substring(5);
            controller.nameList.add(name);
        }
        else if(message.contains("/finish")){
            controller.setUserList();
        }
        else if(message.contains("/quit")){
            String name = message.substring(5);
            controller.nameList.remove(name);
            controller.setUserList();
        }
        else if(message.startsWith("/unready")){
            controller.gui.setReady(false);
        }
        else if(message.startsWith("/ready")){
            controller.gui.setReady(true);
        }
        else if(message.startsWith("/game")){
            controller.gui.startGame();
        }
    }
}