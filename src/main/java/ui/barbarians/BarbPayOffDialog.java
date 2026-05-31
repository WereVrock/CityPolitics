package ui.barbarians;

import main.barbarians.BarbArmy;
import main.parameters.GameParameters;
import main.resources.ResourcePool;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import ui.UITheme;

/**
 * Modal dialog asking the player whether to pay off a barbarian army.
 * Shown mid-turn when noble AI has declined to pay.
 */
public class BarbPayOffDialog {

    private BarbPayOffDialog() {}

    /**
     * Shows the dialog synchronously on the EDT.
     * @return true if player chose to pay.
     */
    public static boolean show(BarbArmy army, ResourcePool resources, java.awt.Window parent) {
        int goldCost = army.getSize() * GameParameters.BARB_PAYOFF_GOLD_PER_MAN;
        int foodCost = army.getSize() * GameParameters.BARB_PAYOFF_FOOD_PER_MAN;
        int fullCost = army.getSize() * GameParameters.BARB_DISMISS_GOLD_PER_MAN;

        boolean canAffordCheap = resources.getMoney() >= goldCost
                              && resources.getFood()  >= foodCost;
        boolean canAffordFull  = resources.getMoney() >= fullCost;

        JDialog dialog = new JDialog(
                parent instanceof Frame ? (Frame) parent : null,
                "Barbarian Threat", true);
        dialog.setSize(420, 320);
        dialog.setLocationRelativeTo(parent);
        dialog.getContentPane().setBackground(UITheme.BG_PANEL);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(UITheme.BG_PANEL);
        content.setBorder(new EmptyBorder(16, 16, 16, 16));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.weightx = 1.0;
        gc.fill  = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(4, 0, 4, 0);

        String typeLabel = switch (army.getType()) {
            case WARBOSS -> "The Warboss";
            case RAIDER  -> "Barbarian Raiders";
            case RAVAGER -> "Barbarian Ravagers";
        };

        gc.gridy = 0;
        content.add(label("☠ " + typeLabel + " at " + army.getZoneId(),
                UITheme.TEXT_RED, UITheme.FONT_HEADER), gc);

        gc.gridy = 1;
        content.add(label(army.getSize() + " warriors demand tribute or blood.",
                UITheme.TEXT_PRIMARY, UITheme.FONT_BODY), gc);

        gc.gridy = 2;
        content.add(label("The noble defender refused to pay.",
                UITheme.TEXT_SECONDARY, UITheme.FONT_SMALL), gc);

        gc.gridy = 3;
        JSeparator sep = new JSeparator();
        sep.setForeground(UITheme.BORDER_COLOR);
        content.add(sep, gc);

        gc.gridy = 4;
        content.add(label("Pay off (stand down 1 turn):  "
                + goldCost + " gold + " + foodCost + " food",
                canAffordCheap ? new Color(210,170,80) : UITheme.TEXT_RED,
                UITheme.FONT_BODY), gc);

        gc.gridy = 5;
        content.add(label("Dismiss entirely:  " + fullCost + " gold",
                canAffordFull ? new Color(120,200,100) : UITheme.TEXT_RED,
                UITheme.FONT_BODY), gc);

        // Result holder
        boolean[] result = {false};

        JButton payOffBtn = styledButton("PAY OFF  (" + goldCost + "g + " + foodCost + "f)");
        payOffBtn.setEnabled(canAffordCheap);
        payOffBtn.addActionListener(e -> {
            resources.addMoney(-goldCost);
            resources.addFood(-foodCost);
            result[0] = true;
            army.setPaidOff(true);
            dialog.dispose();
        });

        JButton dismissBtn = styledButton("DISMISS  (" + fullCost + "g)");
        dismissBtn.setEnabled(canAffordFull);
        dismissBtn.addActionListener(e -> {
            resources.addMoney(-fullCost);
            result[0] = true;
            army.dismiss();
            dialog.dispose();
        });

        JButton fightBtn = styledButton("FIGHT");
        fightBtn.setForeground(UITheme.TEXT_RED);
        fightBtn.addActionListener(e -> {
            result[0] = false;
            dialog.dispose();
        });

        JPanel buttons = new JPanel(new GridLayout(1, 3, 8, 0));
        buttons.setBackground(UITheme.BG_PANEL);
        buttons.add(payOffBtn);
        buttons.add(dismissBtn);
        buttons.add(fightBtn);

        dialog.setLayout(new BorderLayout());
        dialog.add(content, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.setVisible(true);

        return result[0];
    }

    private static JLabel label(String text, Color color, Font font) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(font);
        return l;
    }

    private static JButton styledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(UITheme.FONT_BUTTON);
        btn.setForeground(UITheme.TEXT_SECONDARY);
        btn.setBackground(UITheme.BUTTON_BG);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 42));
        return btn;
    }
}