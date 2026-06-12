package ui;

import main.actions.SendResourcesToNoblesAction;
import main.nobles.NobleHouse;
import main.nobles.NobleHouseManager;
import main.parameters.GameParameters;
import main.resources.ResourcePool;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialog for sending gold to a noble house.
 * Opened by SendResourcesToNoblesAction.
 */
public class SendResourcesToNoblesDialog {

    private SendResourcesToNoblesDialog() {}

    public static void show(Window parent,
                            SendResourcesToNoblesAction action,
                            NobleHouseManager nobleHouseManager,
                            ResourcePool resources,
                            main.ledger.Ledger ledger,
                            Runnable onSent) {
        List<NobleHouse> houses = nobleHouseManager.getHouses().stream()
                .filter(h -> !h.isEliminated())
                .collect(Collectors.toList());

        if (houses.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No noble houses available.");
            return;
        }

        JDialog dialog = new JDialog(
                parent instanceof Frame ? (Frame) parent : null,
                "Send Resources to Nobles", true);
        dialog.setSize(480, 400);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);
        dialog.getContentPane().setBackground(UITheme.BG_PANEL);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(UITheme.BG_PANEL);
        content.setBorder(new EmptyBorder(16, 16, 8, 16));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.weightx = 1.0; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(4, 0, 4, 0);

        gc.gridy = 0;
        JLabel title = new JLabel("Send Resources to Nobles");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_GOLD);
        content.add(title, gc);

        gc.gridy = 1;
        JLabel info = new JLabel("<html>Send gold to a noble house to improve your standing with them.<br>"
                + "Every " + GameParameters.SEND_RESOURCES_OPINION_DIVISOR + " gold grants +"
                + GameParameters.SEND_RESOURCES_OPINION_PER_GOLD + " opinion.</html>");
        info.setFont(UITheme.FONT_SMALL);
        info.setForeground(UITheme.TEXT_SECONDARY);
        content.add(info, gc);

        gc.gridy = 2;
        JLabel houseLabel = new JLabel("Choose a noble house:");
        houseLabel.setFont(UITheme.FONT_BODY);
        houseLabel.setForeground(UITheme.TEXT_PRIMARY);
        content.add(houseLabel, gc);

        gc.gridy = 3;
        String[] houseNames = houses.stream().map(h ->
                h.getName() + "  (opinion: " + h.getPlayerOpinion() + ")")
                .toArray(String[]::new);
        JComboBox<String> houseBox = new JComboBox<>(houseNames);
        houseBox.setFont(UITheme.FONT_BODY);
        houseBox.setBackground(UITheme.BG_PANEL_LIGHT);
        houseBox.setForeground(UITheme.TEXT_PRIMARY);
        content.add(houseBox, gc);

        gc.gridy = 4;
        JLabel amountLabel = new JLabel("Amount (gold):");
        amountLabel.setFont(UITheme.FONT_BODY);
        amountLabel.setForeground(UITheme.TEXT_PRIMARY);
        content.add(amountLabel, gc);

        int maxGold = resources.getMoney();
        final int[] amount = {Math.min(50, maxGold)};

        JLabel amountDisplay = new JLabel(String.valueOf(amount[0]));
        amountDisplay.setFont(amountDisplay.getFont().deriveFont(Font.BOLD, 20f));
        amountDisplay.setForeground(UITheme.TEXT_GOLD);

        JLabel opinionDisplay = new JLabel(buildOpinionString(amount[0]));
        opinionDisplay.setFont(UITheme.FONT_SMALL);
        opinionDisplay.setForeground(UITheme.TEXT_GREEN);

        JSlider slider = new JSlider(10, Math.max(10, maxGold), amount[0]);
        slider.setMajorTickSpacing(Math.max(10, maxGold / 5));
        slider.setBackground(UITheme.BG_PANEL);
        slider.setForeground(UITheme.TEXT_SECONDARY);
        slider.addChangeListener(e -> {
            amount[0] = slider.getValue();
            amountDisplay.setText(String.valueOf(amount[0]));
            opinionDisplay.setText(buildOpinionString(amount[0]));
        });

        gc.gridy = 5; content.add(amountDisplay,  gc);
        gc.gridy = 6; content.add(slider,          gc);
        gc.gridy = 7; content.add(opinionDisplay,  gc);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btnRow.setBackground(UITheme.BG_PANEL);

        JButton sendBtn = new JButton("SEND");
        sendBtn.setFont(UITheme.FONT_BUTTON);
        sendBtn.setForeground(UITheme.TEXT_GOLD);
        sendBtn.setBackground(UITheme.BUTTON_BG);
        sendBtn.setBorderPainted(false);
        sendBtn.setFocusPainted(false);
        sendBtn.addActionListener(e -> {
            NobleHouse selected = houses.get(houseBox.getSelectedIndex());
            main.actions.ActionResult result =
                    action.sendGold(selected, amount[0], resources, ledger);
            JOptionPane.showMessageDialog(dialog,
                    result.getMessage(),
                    result.isSuccess() ? "Resources Sent" : "Error",
                    result.isSuccess()
                            ? JOptionPane.INFORMATION_MESSAGE
                            : JOptionPane.ERROR_MESSAGE);
            if (result.isSuccess()) {
                onSent.run();
                dialog.dispose();
            }
        });

        JButton cancelBtn = new JButton("CANCEL");
        cancelBtn.setFont(UITheme.FONT_BUTTON);
        cancelBtn.setForeground(UITheme.TEXT_SECONDARY);
        cancelBtn.setBackground(UITheme.BUTTON_BG);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setFocusPainted(false);
        cancelBtn.addActionListener(e -> dialog.dispose());

        btnRow.add(cancelBtn);
        btnRow.add(sendBtn);

        dialog.setLayout(new BorderLayout());
        dialog.add(content, BorderLayout.CENTER);
        dialog.add(btnRow,  BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private static String buildOpinionString(int gold) {
        int opinion = (gold / GameParameters.SEND_RESOURCES_OPINION_DIVISOR)
                * GameParameters.SEND_RESOURCES_OPINION_PER_GOLD;
        opinion = Math.max(1, opinion);
        return "Opinion gain: +" + opinion;
    }
}