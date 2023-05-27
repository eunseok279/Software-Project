package Client;

import javax.swing.*;
import java.io.IOException;

public class Controller {
    GUI gui;
    Client client;

    public Controller(Client client, GUI gui) {
        this.client = client;
        this.gui = gui;


        this.gui.addButtonListener(gui.getConfirmButton(), e -> {
            if (gui.onConfirm()) {
                String name = gui.getInputName();
                String ip = gui.getIP();
                boolean result = client.access(ip, name);
                if (!result) {
                    JOptionPane.showMessageDialog(gui.getFrame(), "Failed Connection");
                } else {
                    gui.checkConnection();
                    gui.setResult(true);
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
    }

    public void appendMsg(String msg) {
        gui.getChatArea().append(msg + "\n");
        gui.getChatInput().setText("");
    }
}
