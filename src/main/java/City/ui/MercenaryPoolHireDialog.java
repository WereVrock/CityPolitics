package City.ui;

import City.main.mercenaries.MercenaryArmy;
import City.main.mercenaries.MercenaryHirePool;
import City.main.mercenaries.MercenaryManager;
import City.main.resources.ResourcePool;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import static java.awt.Component.LEFT_ALIGNMENT;
import java.util.List;

/**
 * Dialog showing the turn's mercenary hire pool (1–4 companies of random size).
 * Player may hire any available company if they can afford it.
 */
public class MercenaryPoolHireDialog {

    private MercenaryPoolHireDialog() {}

    public static void show(Window parent,
                            MercenaryManager mercenaryManager,
                            ResourcePool resources,
                            City.main.ledger.Ledger ledger,
                            Runnable onHired) {

        MercenaryHirePool pool = mercenaryManager.getHirePool();
        List<MercenaryHirePool.MercenaryOffer> available = pool.getAvailableOffers();

        JDialog dialog = new JDialog(
                parent instanceof Frame ? (Frame) parent : null,
                "Hire Mercenaries — Available Companies", true);
        dialog.setSize(540, 460);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);
        dialog.getContentPane().setBackground(UITheme.BG_PANEL);
        dialog.setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.BG_PANEL_LIGHT);
        header.setBorder(new EmptyBorder(14, 16, 10, 16));

        JLabel title = new JLabel("Mercenary Companies Available This Season");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_GOLD);

        JLabel goldLabel = new JLabel("Gold: " + resources.getMoney());
        goldLabel.setFont(UITheme.FONT_BODY);
        goldLabel.setForeground(new Color(210, 170, 80));

        JLabel info = new JLabel("<html>"
                + "Companies not hired this turn will disband. Upkeep is paid each turn.<br>"
                + "<font color='#C84646'>Unsupervised mercenaries may raid (30% chance/turn).</font>"
                + "</html>");
        info.setFont(UITheme.FONT_SMALL);
        info.setForeground(UITheme.TEXT_SECONDARY);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        headerRight.setBackground(UITheme.BG_PANEL_LIGHT);
        headerRight.add(goldLabel);

        header.add(title, BorderLayout.CENTER);
        header.add(headerRight, BorderLayout.EAST);

        JPanel headerFull = new JPanel(new BorderLayout(0, 6));
        headerFull.setBackground(UITheme.BG_PANEL_LIGHT);
        headerFull.setBorder(new EmptyBorder(10, 16, 10, 16));
        headerFull.add(title,    BorderLayout.NORTH);
        headerFull.add(info,     BorderLayout.CENTER);
        headerFull.add(headerRight, BorderLayout.EAST);

        dialog.add(headerFull, BorderLayout.NORTH);

        // Offer list
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(UITheme.BG_DARK);
        listPanel.setBorder(new EmptyBorder(8, 12, 8, 12));

        if (available.isEmpty()) {
            JLabel none = new JLabel("  No companies available — all hired or none generated.");
            none.setFont(UITheme.FONT_BODY);
            none.setForeground(UITheme.TEXT_SECONDARY);
            listPanel.add(none);
        } else {
            for (MercenaryHirePool.MercenaryOffer offer : available) {
                listPanel.add(buildOfferCard(offer, resources, ledger,
                        mercenaryManager, goldLabel, listPanel, onHired, dialog));
                listPanel.add(Box.createVerticalStrut(8));
            }
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.setBackground(UITheme.BG_DARK);
        scroll.getViewport().setBackground(UITheme.BG_DARK);
        dialog.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        footer.setBackground(UITheme.BG_PANEL);
        JButton closeBtn = new JButton("CLOSE");
        closeBtn.setFont(UITheme.FONT_BUTTON);
        closeBtn.setForeground(UITheme.TEXT_SECONDARY);
        closeBtn.setBackground(UITheme.BUTTON_BG);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> dialog.dispose());
        footer.add(closeBtn);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private static JPanel buildOfferCard(MercenaryHirePool.MercenaryOffer offer,
                                          ResourcePool resources,
                                          City.main.ledger.Ledger ledger,
                                          MercenaryManager mercenaryManager,
                                          JLabel goldLabel,
                                          JPanel listPanel,
                                          Runnable onHired,
                                          JDialog dialog) {

        JPanel card = new JPanel(new BorderLayout(12, 0));
        boolean canAfford = resources.getMoney() >= offer.getGoldCost();
        card.setBackground(canAfford ? UITheme.BG_PANEL_LIGHT : new Color(35, 22, 22));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        canAfford ? UITheme.BORDER_COLOR : new Color(100, 50, 50), 1),
                new EmptyBorder(10, 12, 10, 12)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        card.setAlignmentX(LEFT_ALIGNMENT);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(card.getBackground());

        JLabel nameLabel = new JLabel("⚔ " + offer.getName());
        nameLabel.setFont(UITheme.FONT_BUTTON);
        nameLabel.setForeground(canAfford ? new Color(200, 165, 80) : UITheme.TEXT_SECONDARY);

        JLabel sizeLabel = new JLabel("Size: " + offer.getSize() + " soldiers");
        sizeLabel.setFont(UITheme.FONT_SMALL);
        sizeLabel.setForeground(UITheme.TEXT_SECONDARY);

        JLabel costLabel = new JLabel("Hire cost: " + offer.getGoldCost() + " gold"
                + (canAfford ? "" : "  ⚠ not enough gold"));
        costLabel.setFont(UITheme.FONT_SMALL);
        costLabel.setForeground(canAfford ? UITheme.TEXT_SECONDARY : UITheme.TEXT_RED);

        JLabel upkeepLabel = new JLabel(String.format(
                "Upkeep: %.1f gold/turn", offer.getUpkeepPerTurn()));
        upkeepLabel.setFont(UITheme.FONT_SMALL);
        upkeepLabel.setForeground(UITheme.TEXT_SECONDARY);

        info.add(nameLabel);
        info.add(Box.createVerticalStrut(3));
        info.add(sizeLabel);
        info.add(costLabel);
        info.add(upkeepLabel);

        JButton hireBtn = new JButton("HIRE");
        hireBtn.setFont(UITheme.FONT_BUTTON);
        hireBtn.setForeground(canAfford ? UITheme.TEXT_GOLD : UITheme.TEXT_SECONDARY);
        hireBtn.setBackground(canAfford ? UITheme.BUTTON_BG : UITheme.BUTTON_DISABLED);
        hireBtn.setBorderPainted(false);
        hireBtn.setFocusPainted(false);
        hireBtn.setEnabled(canAfford);
        hireBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        hireBtn.addActionListener(e -> {
            if (resources.getMoney() < offer.getGoldCost()) {
                JOptionPane.showMessageDialog(dialog,
                        "Not enough gold. Need " + offer.getGoldCost() + ".",
                        "Cannot Hire", JOptionPane.WARNING_MESSAGE);
                return;
            }
            ledger.applyOneTime(City.main.resources.ResourceType.GOLD,
                    "mercenaries", "Hire " + offer.getName(),
                    -offer.getGoldCost(), resources);
            MercenaryArmy army = new MercenaryArmy(
                    offer.getName(), offer.getSize(),
                    City.main.army.Army.HEARTLAND_ID);
            mercenaryManager.addFromArmy(army);
            offer.markHired();

            goldLabel.setText("Gold: " + resources.getMoney());
            // Disable and grey out this card
            hireBtn.setEnabled(false);
            hireBtn.setText("HIRED ✓");
            hireBtn.setForeground(UITheme.TEXT_GREEN);
            card.setBackground(new Color(20, 35, 20));
            nameLabel.setForeground(UITheme.TEXT_GREEN);

            onHired.run();
            City.debug.Debug.log("merc-pool-dialog", "hired",
                    offer.getName() + " size=" + offer.getSize()
                    + " cost=" + offer.getGoldCost());
        });

        card.add(info,    BorderLayout.CENTER);
        card.add(hireBtn, BorderLayout.EAST);
        return card;
    }
}