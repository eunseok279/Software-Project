package Client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
    int cardCount = 0;
    boolean connectResult = false;

    public Controller(Client client, GUI gui) {
        this.client = client;
        this.gui = gui;

        try {
            for (String suit : new String[]{"C", "D", "H", "S"}) {  // Clubs, Diamonds, Hearts, Spades
                for (int i = 2; i <= 14; i++) {  // Card ranks from 1 (Ace) to 13 (King)
                    BufferedImage originalImage = ImageIO.read(new File("card/" + suit + i + ".png"));
                    int width = 100;  // the width you want
                    int height = 120;  // the height you want
                    Image image = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    cardImages.put(suit + i, image);
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
                    client.sendMessage("//quit");
                    client.socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                System.exit(0);
            }
        });
    }

    public void setGameGUI() {
        this.gameGUI.getCallButton().addActionListener(e -> {
            try {
                client.sendMessage("/call");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        this.gameGUI.getCheckButton().addActionListener(e -> {
            try {
                client.sendMessage("/check");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        this.gameGUI.getRaiseButton().addActionListener(e -> {
            String money = gameGUI.getBetField().getText();
            if (!isNumeric(money) || money.isEmpty()) JOptionPane.showMessageDialog(gameGUI.getFrame(), "숫자를 입력하세요!!");
            else {
                try {
                    client.sendMessage("/raise" + money);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            gameGUI.getBetField().setText("");
        });
        this.gameGUI.getFoldButton().addActionListener(e -> {
            try {
                client.sendMessage("/fold");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        this.gui.addWindowListener(gameGUI.getFrame(), new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    client.sendMessage("//quit");
                    client.socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                System.exit(0);
            }
        });
        this.gameGUI.getMiniChatField().addActionListener(e -> {
            String text = gameGUI.getMiniChatField().getText().trim();
            try {
                client.sendMessage(text);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public void appendMsg(String msg) {
        if (gui.isGame()) {
            gameGUI.getMiniChatArea().append(msg + "\n");
            gameGUI.getMiniChatField().setText("");
        } else {
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

    public void appendInfo(String message) {
        if (gui.isGame()) {
            if (message.startsWith("/money")) {
                gameGUI.getMoneyLabel().setText(message.substring(6));
            } else if (message.startsWith("/bet")) {
                gameGUI.getBetLabel().setText(message.substring(4));
            } else if (message.startsWith("/pot")) gameGUI.getPotLabel().setText(message.substring(4));
            else if (message.startsWith("/rank")) gameGUI.getRankLabel().setText(message.substring(5));
        } else {
            if (message.startsWith("/money")) {
                gui.getMoneyLabel().setText(message.substring(6));
            }
        }
    }

    public void addCard(String suit, String rank) {
        if (cardCount < 2) gameGUI.getPersonalPanel().add(new JLabel(new ImageIcon(cardImages.get(suit + rank))));
        else gameGUI.getCommunityPanel().add(new JLabel(new ImageIcon(cardImages.get(suit + rank))));

        cardCount++;
        if (cardCount == 7) cardCount = 0;
    }

    public void winner(String index) {
        JOptionPane.showMessageDialog(gameGUI.getFrame(), index + "번째 팟을 이겼습니다!!");
        gameGUI.getFrame().dispose();
        gui.getChatFrame().setVisible(true);
        gui.setGame(false);
    }

    public void startGame() {
        gui.getChatFrame().setVisible(false);
        gui.setGame(true);
        gameGUI = new GameGUI();
        setGameGUI();
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
