package City.ui;

import City.main.army.Army;
import City.main.army.ArmyManager;
import City.main.mercenaries.MercenaryArmy;
import City.main.mercenaries.MercenaryManager;
import City.main.mercenaries.MercenaryRecruitmentHandler;
 
import City.main.parameters.MercenaryParams;
import City.main.parameters.PlayerArmyParams;
import City.main.resources.ResourcePool;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Dialog for hiring an existing player army as mercenaries.
 * Player picks from eligible armies (in heartland, have commander, have soldiers).
 */
public class MercenaryHireDialog {

    private MercenaryHireDialog() {}

    public static void show(Window parent, ArmyManager armyManager,
                            MercenaryManager mercenaryManager,
                            ResourcePool resources,
                            Runnable onHired) {
        List<Army> eligible = MercenaryRecruitmentHandler.getEligibleArmies(armyManager);

        JDialog dialog = new JDialog(
                parent instanceof Frame ? (Frame) parent : null,
                "Hire Mercenaries", true);
        dialog.setSize(500, 420);
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
        JLabel title = new JLabel("Hire an Army as Mercenaries");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_GOLD);
        content.add(title, gc);

        gc.gridy = 1;
        double mult = MercenaryParams.MERCENARY_COST_MULTIPLIER;
        double upkeepMult = mult;
        JLabel infoLabel = new JLabel("<html>"
                + "Select one of your armies to hire out as mercenaries.<br>"
                + "Cost: ~" + (int)(PlayerArmyParams.SOLDIER_RECRUIT_GOLD_COST * mult)
                + "× per soldier (±15% variance).<br>"
                + "Upkeep: " + String.format("%.1f", PlayerArmyParams.SOLDIER_UPKEEP_GOLD * upkeepMult)
                + " gold/turn per soldier.<br>"
                + "<font color='#C84646'>Warning: unsupervised mercenaries may raid (30% chance).</font>"
                + "</html>");
        infoLabel.setFont(UITheme.FONT_SMALL);
        infoLabel.setForeground(UITheme.TEXT_SECONDARY);
        content.add(infoLabel, gc);

        if (eligible.isEmpty()) {
            gc.gridy = 2;
            JLabel noArmies = new JLabel("No eligible armies. Need: in city, living commander, soldiers > 0.");
            noArmies.setFont(UITheme.FONT_BODY);
            noArmies.setForeground(UITheme.TEXT_RED);
            content.add(noArmies, gc);

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            btnRow.setBackground(UITheme.BG_PANEL);
            JButton closeBtn = new JButton("CLOSE");
            closeBtn.setFont(UITheme.FONT_BUTTON);
            closeBtn.setForeground(UITheme.TEXT_SECONDARY);
            closeBtn.setBackground(UITheme.BUTTON_BG);
            closeBtn.setBorderPainted(false);
            closeBtn.setFocusPainted(false);
            closeBtn.addActionListener(e -> dialog.dispose());
            btnRow.add(closeBtn);
            dialog.setLayout(new BorderLayout());
            dialog.add(content, BorderLayout.CENTER);
            dialog.add(btnRow,  BorderLayout.SOUTH);
            dialog.setVisible(true);
            return;
        }

        gc.gridy = 2;
        JLabel pickLabel = new JLabel("Choose an army:");
        pickLabel.setFont(UITheme.FONT_BODY);
        pickLabel.setForeground(UITheme.TEXT_PRIMARY);
        content.add(pickLabel, gc);

        // Army list panel
        gc.gridy = 3;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        ButtonGroup group = new ButtonGroup();
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(UITheme.BG_DARK);

        final Army[] selected = {eligible.get(0)};
        JLabel costDisplay = new JLabel();
        costDisplay.setFont(UITheme.FONT_BODY);
        costDisplay.setForeground(UITheme.TEXT_GOLD);

        for (Army army : eligible) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setBackground(UITheme.BG_PANEL_LIGHT);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                    new EmptyBorder(6, 8, 6, 8)));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

            JRadioButton rb = new JRadioButton();
            rb.setBackground(UITheme.BG_PANEL_LIGHT);
            group.add(rb);
            if (army == selected[0]) rb.setSelected(true);

            JPanel info = new JPanel();
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            info.setBackground(UITheme.BG_PANEL_LIGHT);

            String cmdName = army.hasLivingCommander()
                    ? army.getCommander().getName() + "  [Skill "
                      + army.getCommandingSkill() + "]" : "—";
            JLabel nameL = new JLabel(army.getDisplayName());
            nameL.setFont(UITheme.FONT_BUTTON);
            nameL.setForeground(UITheme.TEXT_PRIMARY);

            JLabel detailL = new JLabel("Size: " + army.getSize()
                    + "   Commander: " + cmdName
                    + "   Est. cost: ~" + MercenaryRecruitmentHandler.computeHireCostPreview(army) + "g");
            detailL.setFont(UITheme.FONT_SMALL);
            detailL.setForeground(UITheme.TEXT_SECONDARY);

            info.add(nameL);
            info.add(detailL);

            row.add(rb,   BorderLayout.WEST);
            row.add(info, BorderLayout.CENTER);

            row.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    rb.setSelected(true);
                    selected[0] = army;
                    updateCostDisplay(costDisplay, army, resources);
                }
            });
            listPanel.add(row);
            listPanel.add(Box.createVerticalStrut(4));
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.setBackground(UITheme.BG_DARK);
        scroll.getViewport().setBackground(UITheme.BG_DARK);
        content.add(scroll, gc);

        gc.gridy = 4; gc.weighty = 0; gc.fill = GridBagConstraints.HORIZONTAL;
        updateCostDisplay(costDisplay, selected[0], resources);
        content.add(costDisplay, gc);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btnRow.setBackground(UITheme.BG_PANEL);

        JButton hireBtn = new JButton("HIRE");
        hireBtn.setFont(UITheme.FONT_BUTTON);
        hireBtn.setForeground(UITheme.TEXT_GOLD);
        hireBtn.setBackground(UITheme.BUTTON_BG);
        hireBtn.setBorderPainted(false);
        hireBtn.setFocusPainted(false);
        hireBtn.addActionListener(e -> {
            if (selected[0] == null) return;
            MercenaryArmy merc = MercenaryRecruitmentHandler.hire(
                    selected[0], resources, mercenaryManager);
            if (merc == null) {
                JOptionPane.showMessageDialog(dialog,
                        "Cannot afford. Cost: ~"
                        + MercenaryRecruitmentHandler.computeHireCostPreview(selected[0])
                        + " gold. Available: " + resources.getMoney());
            } else {
                onHired.run();
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
        btnRow.add(hireBtn);

        dialog.setLayout(new BorderLayout());
        dialog.add(content, BorderLayout.CENTER);
        dialog.add(btnRow,  BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private static void updateCostDisplay(JLabel label, Army army, ResourcePool resources) {
        int preview = MercenaryRecruitmentHandler.computeHireCostPreview(army);
        int minCost = (int)(preview * (1 - MercenaryParams.MERCENARY_RECRUIT_COST_VARIANCE));
        int maxCost = (int)(preview * (1 + MercenaryParams.MERCENARY_RECRUIT_COST_VARIANCE));
        boolean canAfford = resources.getMoney() >= minCost;
        label.setText("Estimated cost: " + minCost + "–" + maxCost
                + " gold  |  Available: " + resources.getMoney()
                + "  |  Upkeep: " + String.format("%.1f",
                        army.getSize() * PlayerArmyParams.SOLDIER_UPKEEP_GOLD
                        * MercenaryParams.MERCENARY_COST_MULTIPLIER) + "/turn");
        label.setForeground(canAfford ? UITheme.TEXT_GOLD : UITheme.TEXT_RED);
    }
}