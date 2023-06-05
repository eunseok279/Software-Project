package Client;

import javax.imageio.ImageIO;
import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Controller {
    GUI gui;
    GameGUI gameGUI;
    Client client;
    List<String> nameList = new ArrayList<>();
    Map<String, Image> cardImages = new HashMap<>();
    List<JPanel> gameUser = new ArrayList<>();
    int cardCount = 0;
    boolean connectResult = false;
    boolean send = false;

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
        gui.addButtonListener(gui.getConfirmButton(), e -> {
            if (gui.onConfirm()) {
                String name = gui.getInputName();
                String ip = gui.getIP();
                connectResult = client.access(ip, name);
                if (!connectResult) {
                    JOptionPane.showMessageDialog(gui.getLoginFrame(), "connection has failed.");
                } else {
                    gui.checkConnection();
                    gui.setResult(connectResult);
                    gui.openChatWindow();
                    setGui();
                }
            }
        });
    }

    public void setGui() {

        gui.addTextFieldListener(gui.getChatInput(), e -> {
            String text = gui.getChatInput().getText().trim();
            try {
                client.sendMessage(text);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        gui.addButtonListener(gui.getReadyButton(), e -> {
            try {
                if (gui.isReady()) client.sendMessage("//unready");
                else client.sendMessage("//ready");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        gui.addButtonListener(gui.getQuitButton(), e -> {
            try {
                client.sendMessage("//quit");
                gui.getChatFrame().dispose();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        gui.addWindowListener(gui.getChatFrame(), new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    client.sendMessage("//quit");
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
        this.gameGUI.getAllInButton().addActionListener(e -> {
            try {
                client.sendMessage("/allin");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        this.gameGUI.addWindowListener(gameGUI.getFrame(), new WindowAdapter() {
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

    public void appendInfo(String message) throws IOException {
        if (gui.isGame()) {
            if (message.startsWith("/money")) {
                message = message + "$";
                gameGUI.getMoneyLabel().setText(message.substring(6));
            } else if (message.startsWith("/bet")) {
                gameGUI.getBetLabel().setText(message.substring(4) + "$");
            } else if (message.startsWith("/pot")) gameGUI.getPotLabel().setText(message.substring(4) + "$");
            else if (message.startsWith("/rank")) gameGUI.getRankLabel().setText(message.substring(5));
        } else {
            if (message.startsWith("/money")) {
                message = message + "$";
                gui.getMoneyLabel().setText(message.substring(6));
            }
        }
        client.sendMessage("//ack");
    }

    public void addCard(String suit, String rank) throws IOException {
        SwingUtilities.invokeLater(() -> {
            if (cardCount < 2) gameGUI.getPersonalPanel().add(new JLabel(new ImageIcon(cardImages.get(suit + rank))));
            else gameGUI.getCommunityPanel().add(new JLabel(new ImageIcon(cardImages.get(suit + rank))));

            cardCount++;
            if (cardCount == 7) cardCount = 0;
        });
        client.sendMessage("//ack");
    }

    public void winner(String index) {
        if (Integer.parseInt(index) == 0) {
            gameGUI.getFrame().dispose();
            gui.setGame(false);
            gui.openChatWindow();
            JOptionPane.showMessageDialog(gameGUI.getFrame(), "메인 팟을 이겼습니다!!");
        }
        else JOptionPane.showMessageDialog(gameGUI.getFrame(), "사이드 팟 " + index + "을 이겼습니다!!");
        gameUser.clear();
    }

    public void loser(String index) {
        if (Integer.parseInt(index) == 0) {
            gameGUI.getFrame().dispose();
            gui.setGame(false);
            gui.openChatWindow();
            JOptionPane.showMessageDialog(gameGUI.getFrame(), "메인 팟에 졌습니다");
        }
        else JOptionPane.showMessageDialog(gameGUI.getFrame(), "사이드 팟 " + index + "에 졌습니다");
        gameUser.clear();
    }

    public void startGame() {
        gui.getChatFrame().dispose();
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

    public void addGameUser(List<String> users) {
        for (String user : users) {
            String name = user.substring(5);
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            JLabel nameLabel = new JLabel(name);
            JLabel betLabel = new JLabel("0$");
            JLabel stateLabel = new JLabel("LIVE");
            panel.add(nameLabel);
            panel.add(betLabel);
            panel.add(stateLabel);
            gameUser.add(panel);
            panel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK));
            gameGUI.getPlayerPanel().add(panel);
        }
    }

    public void setUserTurn(int userIndex) {
        for (Component component : gameUser.get(userIndex).getComponents()) {
            if (component instanceof JLabel label) {
                label.setFont(new Font("arial", Font.BOLD, 12));
                label.setForeground(Color.GREEN);
                break;
            }
        }
    }

    public void setUserTurnEnd(int userIndex, String bet, String state, String pot) {
        int count = 0;
        for (Component component : gameUser.get(userIndex).getComponents()) {
            if (component instanceof JLabel label) {
                if (count == 0) {
                    label.setFont(new Font("arial", Font.PLAIN, 12));
                    label.setForeground(Color.BLACK);
                } else if (count == 1) label.setText(bet + "$");
                else {
                    if(state.equals("DEPLETED")) state = "ALLIN";
                    label.setText(state);
                }
            }
            count++;
        }
        gameGUI.getPotLabel().setText(pot);
        gameGUI.getResultLabel().setText("다른 사람의 턴입니다...");
    }
    public void setState(List<String> state){
        for(int i = 0;i<gameUser.size();i++){
            int count =0;
            for(Component component: gameUser.get(i).getComponents()){
                if(component instanceof  JLabel label){
                    if(count == 2){
                        label.setText(state.get(i));
                    }
                }
                count++;
            }
        }
    }

    public void startTime() throws IOException {
        client.sendMessage("//ack");
        AtomicInteger time = new AtomicInteger(30);

        Timer timer = new Timer(1000, e -> {
            int remainingTime = time.decrementAndGet();
            gameGUI.getTimeLabel().setText(Integer.toString(remainingTime));
            if (remainingTime < 0 || send) {
                ((Timer) e.getSource()).stop();
            }
        });
        timer.start();
        send = false;
    }

    public void errorOccur(String message) {
        String result = gameGUI.getResultLabel().getText();
        gameGUI.getResultLabel().setText(message.substring(6));
        gameGUI.getResultLabel().setForeground(Color.RED);
        Timer error = new Timer(1000, new ActionListener() {
            int time = 1;

            @Override
            public void actionPerformed(ActionEvent e) {
                time--;
                if (time < 0) {
                    ((Timer) e.getSource()).stop();  // Stop the timer
                    gameGUI.getResultLabel().setText(result);
                    gameGUI.getResultLabel().setForeground(Color.BLACK);
                }
            }
        });
        error.start();
    }
}