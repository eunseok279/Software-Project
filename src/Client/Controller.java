package Client;

public class Controller {
    GUI gui;
    Client client;
    public Controller(Client client,GUI gui){
        this.client = client;
        this.gui = gui;

        this.gui.addConfirmButtonListener(e -> {
            if(gui.onConfirm()) {
                String name = gui.getInputName();
                String ip = gui.getIP();
                boolean result = client.access(ip, name);
                gui.openChat();
                gui.setResult(result);
            }
        });
    }
}
