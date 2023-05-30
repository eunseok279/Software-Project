package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;

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
    public void addWindowListener(JFrame frame, WindowListener listener){
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

    public void setReady(boolean ready) {
        this.ready = ready;
    }
}