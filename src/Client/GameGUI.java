package Client;

import javax.swing.*;
import java.awt.*;

public class GameGUI {
    String[] hands = {"Royal Straight Flush", "Straight Flush", "Four of a kind",
            "Full house", "Flush", "Straight", "Three of a kind",
            "Two pair", "One pair", "High card"};

    String[] handDescriptions = {"You are Winner Go ahead!!!",
            "Description for Straight Flush",
            "Description for Four of a kind",
            "Description for Full house",
            "Description for Flush",
            "Description for Straight",
            "Description for Three of a kind",
            "Description for Two pair",
            "Description for One pair",
            "Description for High card"};
    public GameGUI(){
        JFrame frame = new JFrame("Poker Game");

        // Set the layout
        frame.setLayout(new BorderLayout());

        // Main Panel
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Add the components
        // Top
        JPanel topPanel = new JPanel(new GridLayout(1, 3));
        topPanel.add(new JLabel("Current Hand Rank"));
        topPanel.add(new JLabel("Total Pot"));
        topPanel.add(new JLabel("Your Money"));
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center
        JPanel centerPanel = new JPanel(new GridLayout(1, 2));
        JLabel communityCards = new JLabel(new ImageIcon("path_to_community_cards_image.jpg"));
        centerPanel.add(communityCards);
        JLabel personalCards = new JLabel(new ImageIcon("path_to_personal_cards_image.jpg"));
        centerPanel.add(personalCards);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom
        JPanel bottomPanel = new JPanel();
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JTextField betField = new JTextField(10);
        JButton foldButton = new JButton("Fold");
        JButton checkButton = new JButton("Check");
        JButton callButton = new JButton("Call");
        JButton raiseButton = new JButton("Bet/Raise");

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
        handRankingPanel.add(new JLabel("Hand Ranking"),BorderLayout.CENTER);
        handRankingPanel.add(Box.createVerticalStrut(10));

// Add each hand as a separate label to the panel.
        for (int i = 0; i < hands.length; i++) {
            JLabel handLabel = new JLabel(hands[i]);
            handLabel.setFont(new Font("맑은 고딕",Font.BOLD,20));
            handLabel.setToolTipText(handDescriptions[i]); // Set the tool tip
            handRankingPanel.add(handLabel);
        }
        handRankingPanel.add(Box.createVerticalStrut(10));
        handRankingPanel.add(new JLabel("If you put your mouse over the letter"));
        handRankingPanel.add(new JLabel("the information will appear."));

// Add the hand ranking panel to the top of the side panel.
        sidePanel.add(handRankingPanel, BorderLayout.NORTH);
        JPanel chatPanel = new JPanel(new BorderLayout()); // Here set layout to BorderLayout
        JTextArea chatArea = new JTextArea(10, 20);
        JTextField chatField = new JTextField(20);
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        chatPanel.add(scrollPane, BorderLayout.CENTER); // Here change to CENTER
        chatPanel.add(chatField, BorderLayout.SOUTH);
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
}
