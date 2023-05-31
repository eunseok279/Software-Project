package Client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Controller {
    GUI gui;
    GameGUI gameGUI;
    Client client;
    Set<String> nameList = new HashSet<>();
    Map<String, Image> cardImages = new HashMap<>();

    boolean connectResult = false;

    public Controller(Client client, GUI gui) {
        this.client = client;
        this.gui = gui;

        try {
            for (String suit : new String[]{"C", "D", "H", "S"}) {  // Clubs, Diamonds, Hearts, Spades
                for (int i = 2; i <= 14; i++) {  // Card ranks from 1 (Ace) to 13 (King)
                    BufferedImage originalImage = ImageIO.read(new File("card/" + i + suit + ".png"));
                    int width = 100;  // the width you want
                    int height = 120;  // the height you want
                    Image image = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    cardImages.put(i + suit, image);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        this.gui.addButtonListener(gui.getConfirmButton(), e -> {
            if (gui.onConfirm()) {
                String name = gui.getInputName();
                String ip = gui.getIP();
                connectResult = client.access(ip, name);
                if (!connectResult) {
                    JOptionPane.showMessageDialog(gui.getLoginFrame(), "connection has failed.");
                } else {
                    gui.checkConnection();
                    gui.setResult(connectResult);
                }
            }
        });
        this.gui.addTextFieldListener(gui.getChatInput(), e -> {
            String text = gui.getChatInput().getText().trim();
            try {
                client.sendMessage(text);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        this.gui.addButtonListener(gui.getReadyButton(), e -> {
            try {
                if (gui.isReady()) client.sendMessage("//unready");
                else client.sendMessage("//ready");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        this.gui.addButtonListener(gui.getQuitButton(), e -> {
            try {
                client.sendMessage("//quit");
                gui.getChatFrame().dispose();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        this.gui.addWindowListener(gui.getChatFrame(), new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    client.socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                System.exit(0);
            }
        });
    }

    public void appendMsg(String msg) {
        if(gui.isGame()) {
           gameGUI.getMiniChatArea().append(msg + "\n");
           gameGUI.getMiniChatField().setText("");
        }
        else{
            gui.getChatList().append(msg + "\n");
            gui.getChatInput().setText("");
        }
    }

    public void setUserList() {
        gui.getUserList().setText(null);
        for (String s : nameList) {
            gui.getUserList().append(s + "\n");
        }
    }
}
