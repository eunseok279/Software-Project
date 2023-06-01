package Client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GUI {

    private final JFrame frame = new JFrame("Connect");
    private final JFrame chatFrame = new JFrame("User");
    private final JTextField nicknameField;
    private final JTextField serverIPField;
    private static final long serialVersionUID = 1L;
    private JTextArea chatArea = new JTextArea(40, 25);
    private TextArea ChatList = new TextArea(30, 50);
    private TextArea UserList = new TextArea(30, 15);
    private JTextField chatInput = new JTextField(45);
    private JButton confirmButton = new JButton("Confirm");
    private JButton readyButton = new JButton("Ready/Unready");
    private JButton quitButton = new JButton("Quit");
    JLabel UserLabel = new JLabel("유저 목록");
    JLabel User = new JLabel();
    private JScrollPane scrollPane;
    private boolean result = false;
    private boolean ready = false;
    private boolean game = false;

    public GUI() {
        // 프레임이 닫힐 때 프로그램도 종료하도록 설정
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 레이아웃 설정
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        frame.setLocationRelativeTo(null);
        // 닉네임 필드 추가
        JPanel nicknamePanel = new JPanel(new FlowLayout());
        nicknamePanel.add(new JLabel("Nickname:"));
        nicknameField = new JTextField(15); // 텍스트필드 길이 설정
        nicknamePanel.add(nicknameField);
        frame.add(nicknamePanel);

        // 서버 IP 필드 추가
        JPanel serverIPPanel = new JPanel(new FlowLayout());
        serverIPPanel.add(new JLabel("Server IP:"));
        serverIPField = new JTextField("Localhost", 15); // 텍스트필드 길이 설정
        serverIPPanel.add(serverIPField);
        frame.add(serverIPPanel);
        frame.add(confirmButton);

        // 프레임 크기 설정 및 프레임 보이게 설정
        frame.pack();
        frame.setVisible(true);
    }

    // 확인 버튼 클릭 시 실행되는 메소드
    public boolean onConfirm() {
        String nickname = nicknameField.getText();
        String serverIP = serverIPField.getText();

        // 닉네임과 서버 IP 확인
        if (nickname.isEmpty() || serverIP.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Nickname and server IP must be filled in.");
            return false;
        }
        return true;
    }

    public void checkConnection() {
        Runnable checkConnect = () -> {
            while (true) {
                if (result) break;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            JOptionPane.showMessageDialog(frame, "Connecting!!");
            frame.dispose();
            openChatWindow();
        };
        new Thread(checkConnect).start();
    }

    // 채팅 창을 연다
    private void openChatWindow() {
        JPanel textAndButtonPanel = new JPanel(new FlowLayout());

// Add components to the textAndButtonPanel
        textAndButtonPanel.add(chatInput);
        textAndButtonPanel.add(readyButton);
        textAndButtonPanel.add(quitButton);

        chatFrame.setTitle("User");
        chatFrame.setVisible(true);
        chatFrame.setSize(750, 600);
        chatFrame.setResizable(false);
        chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ChatList.setEditable(false);
        UserList.setEditable(false);

// Add components to the main panel
        JPanel ClientGUIPanel = new JPanel();
        ClientGUIPanel.add(User);
        ClientGUIPanel.add(ChatList);
        ClientGUIPanel.add(UserLabel);
        ClientGUIPanel.add(UserList);
        ClientGUIPanel.add(textAndButtonPanel);
        chatFrame.add(ClientGUIPanel);
    }



    public void startGame() {
        chatFrame.setVisible(false);
        game = true;
        new GameGUI();
    }

    public String getInputName() {
        return nicknameField.getText();
    }

    public String getIP() {
        return serverIPField.getText();
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public void addButtonListener(JButton button, ActionListener listener) {
        button.addActionListener(listener);
    }

    public void addTextFieldListener(JTextField textField, ActionListener listener) {
        textField.addActionListener(listener);
    }

    public void addWindowListener(JFrame frame, WindowListener listener) {
        frame.addWindowListener(listener);
    }


    public JFrame getLoginFrame() {
        return frame;
    }

    public JFrame getChatFrame() {
        return chatFrame;
    }

    public JTextField getChatInput() {
        return chatInput;
    }

    public TextArea getChatList() {
        return ChatList;
    }

    public TextArea getUserList() {
        return UserList;
    }

    public JButton getConfirmButton() {
        return confirmButton;
    }

    public JButton getReadyButton() {
        return readyButton;
    }

    public JButton getQuitButton() {
        return quitButton;
    }

    public boolean isReady() {
        return ready;
    }
    public boolean isGame() {
        return game;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }


}

class GameGUI {
    private final JPanel communityPanel;
    private final JPanel personalPanel;
    Map<String, Image> cardImages = new HashMap<>();
    private final JFrame frame;
    private final JButton foldButton;
    private final JButton checkButton;
    private final JButton callButton;
    private final JButton raiseButton;
    private final JLabel rankLabel;
    private  final  JLabel betLabel;
    private final JLabel potLabel;
    private final JLabel moneyLabel;
    private final JTextArea miniChatArea;
    private final JTextField miniChatField;
    String[] hands = {"Royal Straight Flush", "Straight Flush", "Four of a kind", "Full house", "Flush", "Straight", "Three of a kind", "Two pair", "One pair", "High card"};

    String[] handDescriptions = {"이미 승리하셨습니다!!!", "같은 무늬 5장, 연결된 숫자", "같은 숫자 4개", "같은 숫자 3개와 같은 숫자 2개의 조함", "같은 무늬 5장", "5장의 카드가 연속된 경우", "3장의 카드가 같은 숫자", "2가지 숫자가 페어", "2장의 카드가 같은 숫자", "5장의 카드가 숫자, 무늬 모두 다른 경우 => 가장 높은 카드로 승자 판별"};


    public GameGUI() {
        try {
            for (String suit : new String[]{"C", "D", "H", "S"}) {  // Clubs, Diamonds, Hearts, Spades
                for (int i = 2; i <= 14; i++) {  // Card ranks from 1 (Ace) to 13 (King)
                    BufferedImage originalImage = ImageIO.read(new File("card/" + i + suit + ".png"));
                    int width = 100;  // the width you want
                    int height = 130;  // the height you want
                    Image image = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    cardImages.put(i + suit, image);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        frame = new JFrame("Poker Game");

        // Set the layout
        frame.setLayout(new BorderLayout());

        // Main Panel
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Add the components
        // Top
        JPanel topPanel = new JPanel(new GridLayout(1, 3));
        JPanel rankPanel = new JPanel(new FlowLayout());
        JLabel label1 = new JLabel("현재 카드 족보:");
        rankLabel = new JLabel("로얄");
        rankPanel.add(label1);
        rankPanel.add(rankLabel);

        JPanel betPanel = new JPanel(new FlowLayout());
        JLabel label2 = new JLabel("플레이어 배팅:");
        betLabel = new JLabel("2달러");
        betPanel.add(label2);
        betPanel.add(betLabel);

        JPanel potPanel = new JPanel(new FlowLayout());
        JLabel label3 = new JLabel("현재 총 배팅금:");
        potLabel = new JLabel("5달러");
        potPanel.add(label3);
        potPanel.add(potLabel);

        JPanel moneyPanel = new JPanel(new FlowLayout());
        JLabel label4 = new JLabel("소지금:");
        moneyLabel = new JLabel("200달러");
        moneyPanel.add(label4);
        moneyPanel.add(moneyLabel);

        topPanel.add(betPanel);
        topPanel.add(potPanel);
        topPanel.add(moneyPanel);
        topPanel.add(rankPanel);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center
        JPanel centerPanel = new JPanel(new GridLayout(3, 1));
        communityPanel = new JPanel(new FlowLayout());
        personalPanel = new JPanel(new FlowLayout());
        personalPanel.add(new JLabel(new ImageIcon(cardImages.get("2C"))));
        personalPanel.add(new JLabel(new ImageIcon(cardImages.get("2C"))));
        centerPanel.add(new JPanel());
        centerPanel.add(communityPanel);
        centerPanel.add(personalPanel);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom
        JPanel bottomPanel = new JPanel();
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JTextField betField = new JTextField(10);
        foldButton = new JButton("Fold");
        checkButton = new JButton("Check");
        callButton = new JButton("Call");
        raiseButton = new JButton("Bet/Raise");

        southPanel.add(foldButton);
        southPanel.add(checkButton);
        southPanel.add(callButton);
        southPanel.add(raiseButton);
        southPanel.add(betField);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        // Right Panel
        JPanel sidePanel = new JPanel(new GridLayout(2, 1));
        JPanel handRankingPanel = new JPanel();
        handRankingPanel.setLayout(new BoxLayout(handRankingPanel, BoxLayout.Y_AXIS));

        // Add a title to the hand ranking panel.
        handRankingPanel.add(new JLabel("족보 순위"), BorderLayout.CENTER);
        handRankingPanel.add(Box.createVerticalStrut(10));

        // Add each hand as a separate label to the panel.
        for (int i = 0; i < hands.length; i++) {
            JLabel handLabel = new JLabel(hands[i]);
            handLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
            handLabel.setToolTipText(handDescriptions[i]); // Set the tool tip
            handRankingPanel.add(handLabel);
        }
        handRankingPanel.add(Box.createVerticalStrut(10));
        handRankingPanel.add(new JLabel("만약 마우스를 글자 위에 두면"));
        handRankingPanel.add(new JLabel("각 족보의 정보가 나타날 것입니다."));

        // Add the hand ranking panel to the top of the side panel.
        sidePanel.add(handRankingPanel, BorderLayout.NORTH);
        JPanel chatPanel = new JPanel(new BorderLayout()); // Here set layout to BorderLayout
        chatPanel.add(new JLabel("                          미니 채팅창"), BorderLayout.NORTH);
        miniChatArea = new JTextArea(10, 20);
        miniChatField = new JTextField(20);
        miniChatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(miniChatArea);
        chatPanel.add(scrollPane, BorderLayout.CENTER); // Here change to CENTER
        chatPanel.add(miniChatField, BorderLayout.SOUTH);
        sidePanel.add(chatPanel);
        sidePanel.setBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, Color.BLACK));

        // Add Main Panel and Side Panel to frame
        frame.add(mainPanel, BorderLayout.CENTER);
        frame.add(sidePanel, BorderLayout.EAST);

        // Set the frame properties
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1500, 800);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        new GameGUI();
    }

    public JFrame getFrame() {
        return frame;
    }

    public JButton getFoldButton() {
        return foldButton;
    }

    public JButton getCheckButton() {
        return checkButton;
    }

    public JButton getCallButton() {
        return callButton;
    }

    public JButton getRaiseButton() {
        return raiseButton;
    }

    public JLabel getRankLabel() {
        return rankLabel;
    }

    public JLabel getPotLabel() {
        return potLabel;
    }

    public JLabel getMoneyLabel() {
        return moneyLabel;
    }

    public JTextArea getMiniChatArea() {
        return miniChatArea;
    }

    public JTextField getMiniChatField() {
        return miniChatField;
    }

}