package Client;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Controller {
    GUI gui;
    Client client;
    Set<String> nameList = new HashSet<>();
    boolean connectResult = false;

    public Controller(Client client, GUI gui) {
        this.client = client;
        this.gui = gui;


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
        gui.getChatList().append(msg + "\n");
        gui.getChatInput().setText("");
    }

    public void setUserList() {
        gui.getUserList().setText(null);
        for (String s : nameList) {
            gui.getUserList().append(s + "\n");
        }
    }
}
