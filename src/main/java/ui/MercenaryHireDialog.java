package ui;

import main.mercenaries.MercenaryArmy;
import main.mercenaries.MercenaryManager;
import main.parameters.GameParameters;
import main.resources.ResourcePool;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Dialog for hiring mercenary companies.
 * Opens when player clicks Hire Mercenaries.
 */
public class MercenaryHireDialog {

    private MercenaryHireDialog() {}

    public static void show(Window parent, MercenaryManager manager,
                            ResourcePool resources, String heartlandId,
                            Runnable onHired) {
        JDialog dialog = new JDialog(
                parent instanceof Frame ? (Frame) parent : null,
                "Hire Mercenaries", true);
        dialog.setSize(460, 380);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);
        dialog.getContentPane().setBackground(ui.UITheme.BG_PANEL);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(ui.UITheme.BG_PANEL);
        content.setBorder(new EmptyBorder(16, 16, 8, 16));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.weightx = 1.0; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(4, 0, 4, 0);

        gc.gridy = 0;
        JLabel title = new JLabel("Hire Mercenaries");
        title.setFont(ui.UITheme.FONT_TITLE);
        title.setForeground(ui.UITheme.TEXT_GOLD);
        content.add(title, gc);

        gc.gridy = 1;
        double costMultiplier = GameParameters.MERCENARY_COST_MULTIPLIER;
        int goldPerSoldier = (int)(GameParameters.SOLDIER_RECRUIT_GOLD_COST * costMultiplier);
        int manPerSoldier  = GameParameters.SOLDIER_RECRUIT_MANPOWER_COST;
        double upkeepPerSoldier = GameParameters.SOLDIER_UPKEEP_GOLD * costMultiplier;
        JLabel infoLabel = new JLabel("<html>Cost: " + goldPerSoldier
                + " gold + " + manPerSoldier + " manpower per soldier.<br>"
                + "Upkeep: " + String.format("%.1f", upkeepPerSoldier) + " gold/turn per soldier.<br>"
                + "Warning: unsupervised mercenaries may raid zones (30% chance).</html>");
        infoLabel.setFont(ui.UITheme.FONT_SMALL);
        infoLabel.setForeground(ui.UITheme.TEXT_SECONDARY);
        content.add(infoLabel, gc);

        gc.gridy = 2;
        JLabel nameLabel = new JLabel("Company Name:");
        nameLabel.setFont(ui.UITheme.FONT_BODY);
        nameLabel.setForeground(ui.UITheme.TEXT_PRIMARY);
        content.add(nameLabel, gc);

        gc.gridy = 3;
        JTextField nameField = new JTextField(generateMercName());
        nameField.setFont(ui.UITheme.FONT_BODY);
        nameField.setBackground(ui.UITheme.BG_PANEL_LIGHT);
        nameField.setForeground(ui.UITheme.TEXT_PRIMARY);
        nameField.setCaretColor(ui.UITheme.TEXT_PRIMARY);
        content.add(nameField, gc);

        gc.gridy = 4;
        JLabel sizeLabel = new JLabel("Size (soldiers):");
        sizeLabel.setFont(ui.UITheme.FONT_BODY);
        sizeLabel.setForeground(ui.UITheme.TEXT_PRIMARY);
        content.add(sizeLabel, gc);

        int maxByGold     = resources.getMoney()    / goldPerSoldier;
        int maxByManpower = resources.getManpower() / manPerSoldier;
        int maxSize       = Math.max(0, Math.min(maxByGold, maxByManpower));

        final int[] amount = {Math.min(50, maxSize)};

        JLabel amountDisplay = new JLabel(String.valueOf(amount[0]));
        amountDisplay.setFont(amountDisplay.getFont().deriveFont(Font.BOLD, 22f));
        amountDisplay.setForeground(ui.UITheme.TEXT_GOLD);

        JLabel costDisplay = new JLabel(buildCostString(amount[0], goldPerSoldier, manPerSoldier, upkeepPerSoldier));
        costDisplay.setFont(ui.UITheme.FONT_SMALL);
        costDisplay.setForeground(ui.UITheme.TEXT_SECONDARY);

        JSlider slider = new JSlider(0, Math.max(1, maxSize), amount[0]);
        slider.setBackground(ui.UITheme.BG_PANEL);
        slider.setForeground(ui.UITheme.TEXT_SECONDARY);

        slider.addChangeListener(e -> {
            amount[0] = slider.getValue();
            amountDisplay.setText(String.valueOf(amount[0]));
            costDisplay.setText(buildCostString(amount[0], goldPerSoldier, manPerSoldier, upkeepPerSoldier));
        });

        gc.gridy = 5;
        content.add(amountDisplay, gc);
        gc.gridy = 6;
        content.add(slider, gc);
        gc.gridy = 7;
        content.add(costDisplay, gc);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btnRow.setBackground(ui.UITheme.BG_PANEL);

        JButton hireBtn = new JButton("HIRE");
        hireBtn.setFont(ui.UITheme.FONT_BUTTON);
        hireBtn.setForeground(ui.UITheme.TEXT_GOLD);
        hireBtn.setBackground(ui.UITheme.BUTTON_BG);
        hireBtn.setBorderPainted(false);
        hireBtn.setFocusPainted(false);
        hireBtn.addActionListener(e -> {
            if (amount[0] <= 0) {
                JOptionPane.showMessageDialog(dialog, "Must hire at least 1 soldier.");
                return;
            }
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = generateMercName();
            MercenaryArmy hired = manager.hire(name, amount[0], heartlandId, resources);
            if (hired == null) {
                JOptionPane.showMessageDialog(dialog, "Cannot afford. Check gold and manpower.");
            } else {
                onHired.run();
                dialog.dispose();
            }
        });

        JButton cancelBtn = new JButton("CANCEL");
        cancelBtn.setFont(ui.UITheme.FONT_BUTTON);
        cancelBtn.setForeground(ui.UITheme.TEXT_SECONDARY);
        cancelBtn.setBackground(ui.UITheme.BUTTON_BG);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setFocusPainted(false);
        cancelBtn.addActionListener(e -> dialog.dispose());

        btnRow.add(cancelBtn);
        btnRow.add(hireBtn);

        dialog.setLayout(new BorderLayout());
        dialog.add(content, BorderLayout.CENTER);
        dialog.add(btnRow,  BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private static String buildCostString(int n, int goldPer, int manPer, double upkeepPer) {
        return "Recruitment: " + (n * goldPer) + " gold + " + (n * manPer)
                + " manpower  |  Upkeep: " + String.format("%.1f", n * upkeepPer) + " gold/turn";
    }

    private static final String[] MERC_ADJECTIVES = {
        "Iron", "Silver", "Bloody", "Steel", "Black", "Golden", "Shadow", "Cursed"
    };
    private static final String[] MERC_NOUNS = {
        "Fang", "Claw", "Sword", "Shield", "Band", "Company", "Dogs", "Ravens"
    };

    private static String generateMercName() {
        String adj  = MERC_ADJECTIVES[(int)(Math.random() * MERC_ADJECTIVES.length)];
        String noun = MERC_NOUNS[(int)(Math.random() * MERC_NOUNS.length)];
        return "The " + adj + " " + noun;
    }
}