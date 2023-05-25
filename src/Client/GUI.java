package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class GUI {

    private JFrame frame;
    private JTextField nicknameField;
    private JTextField serverIPField;
    private JButton confirmButton;
    private boolean result =false;

    public GUI() {
        // 프레임 초기화
        frame = new JFrame("Poker Client");

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

        // 확인 버튼 추가
        confirmButton = new JButton("Confirm");
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

    public void openChat() {
        Runnable checkConnect = () -> {
            while (true) {
                if (result) break;
                try{
                    Thread.sleep(1000);
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            }
            JOptionPane.showMessageDialog(frame, "Connecting!!");
            System.out.println("Thread out");
        }; new Thread(checkConnect).start();
    }

    // 채팅 창을 연다
    private void openChatWindow(String nickname, String serverIP) {
        // ... 여기서 채팅 창 구현
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

    public void addConfirmButtonListener(ActionListener listener) {
        confirmButton.addActionListener(listener);
    }
}