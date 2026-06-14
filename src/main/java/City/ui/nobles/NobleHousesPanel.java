// NobleHousesPanel.java
package City.ui.nobles;

import City.main.core.GameState;
import City.main.nobles.NobleHouse;
import City.main.nobles.NobleHouseManager;
import City.ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Overview panel for all noble houses.
 * Styled to match PartiesOverviewPanel.
 */
public class NobleHousesPanel extends JPanel {

    private final GameState gameState;
    private final Runnable  onBack;
    private final JPanel    listPanel;

    public NobleHousesPanel(GameState gameState, Runnable onBack) {
        this.gameState = gameState;
        this.onBack    = onBack;

        setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));

        add(buildHeader(), BorderLayout.NORTH);

        listPanel = new JPanel(new GridBagLayout());
        listPanel.setBackground(UITheme.BG_DARK);

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.setBackground(UITheme.BG_DARK);
        scroll.getViewport().setBackground(UITheme.BG_DARK);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel title = new JLabel("NOBLE HOUSES");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_GOLD);

        JButton back = new JButton("← BACK");
        back.setFont(UITheme.FONT_BUTTON);
        back.setForeground(UITheme.TEXT_SECONDARY);
        back.setBackground(UITheme.BUTTON_BG);
        back.setBorderPainted(false);
        back.setFocusPainted(false);
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.addActionListener(e -> onBack.run());

        panel.add(title, BorderLayout.WEST);
        panel.add(back,  BorderLayout.EAST);
        return panel;
    }

public void refresh() {
    listPanel.removeAll();
    NobleHouseManager manager = gameState.getNobleHouseManager();
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx   = 0;
    gbc.weightx = 1.0;
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.insets  = new Insets(0, 0, 8, 0);

    int row = 0;
    for (NobleHouse house : manager.getHouses()) {
        gbc.gridy = row++;
        gbc.weighty = 0;
        listPanel.add(buildHouseCard(house), gbc);
    }
    // push everything up
    gbc.gridy   = row;
    gbc.weighty = 1.0;
    gbc.fill    = GridBagConstraints.BOTH;
    listPanel.add(Box.createVerticalGlue(), gbc);

    listPanel.revalidate();
    listPanel.repaint();
}

private JPanel buildHouseCard(NobleHouse house) {
    JPanel card = new JPanel(new BorderLayout(12, 0));
    card.setBackground(UITheme.BG_PANEL);
    card.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
        new EmptyBorder(10, 12, 10, 12)
    ));
    card.add(buildPortrait(house),  BorderLayout.WEST);
    card.add(buildInfo(house),      BorderLayout.CENTER);
    card.add(buildStats(house),     BorderLayout.EAST);
    return card;
}

private JPanel buildPortrait(NobleHouse house) {
        JPanel portrait = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_PANEL_LIGHT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(raceColor(house.getRace()));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                int cx = getWidth() / 2;
                g2.setColor(UITheme.TEXT_SECONDARY);
                g2.fillOval(cx - 14, 10, 28, 28);
                g2.fillRoundRect(cx - 18, 42, 36, 30, 6, 6);
                g2.setColor(raceColor(house.getRace()));
                g2.setFont(UITheme.FONT_SMALL);
                String init = house.getName().substring(6, 7).toUpperCase(); // first letter after "House "
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(init, cx - fm.stringWidth(init)/2, 30);
                // race label
                g2.setFont(new Font("Serif", Font.ITALIC, 9));
                String race = house.getRace().name();
                fm = g2.getFontMetrics();
                g2.drawString(race, cx - fm.stringWidth(race)/2, 84);
            }
        };
        portrait.setPreferredSize(new Dimension(70, 95));
        portrait.setBackground(UITheme.BG_PANEL);
        return portrait;
    }

    private JPanel buildInfo(NobleHouse house) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.BG_PANEL);

        JLabel name = new JLabel(house.getName());
        name.setFont(UITheme.FONT_HEADER);
        name.setForeground(UITheme.TEXT_GOLD);

        JLabel leader = new JLabel(house.getLeaderName());
        leader.setFont(UITheme.FONT_SMALL);
        leader.setForeground(UITheme.TEXT_SECONDARY);

        JLabel zones = new JLabel("Zones: " + house.getZoneIds().size());
        zones.setFont(UITheme.FONT_SMALL);
        zones.setForeground(UITheme.TEXT_PRIMARY);

        String capitalName = house.getCapitalZoneId() != null
            ? house.getCapitalZoneId().replace("_", " ") : "None";
        JLabel capital = new JLabel("Capital: " + capitalName);
        capital.setFont(UITheme.FONT_SMALL);
        capital.setForeground(new Color(255, 210, 80));

        int totalGarrison  = house.getTotalGarrisonSize();
        int recruitedTotal = gameState.getNobleArmyManager()
            .getArmiesForHouse(house.getId())
            .stream().mapToInt(City.main.nobles.NobleArmy::getSize).sum();
        JLabel army = new JLabel("Garrison: " + totalGarrison
            + "  Armies: " + recruitedTotal
            + "  Pool: " + house.getNobleManpower());
        army.setToolTipText("Garrison = zone defenders. Armies = raised field armies. Pool = available manpower.");
        army.setFont(UITheme.FONT_SMALL);
        army.setForeground(UITheme.TEXT_PRIMARY);

        JTextArea personality = new JTextArea(house.getLeaderPersonality());
        personality.setFont(new Font("Serif", Font.ITALIC, 12));
        personality.setForeground(UITheme.TEXT_SECONDARY);
        personality.setBackground(UITheme.BG_PANEL);
        personality.setEditable(false);
        personality.setLineWrap(true);
        personality.setWrapStyleWord(true);
        personality.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        // Zone list
        StringBuilder sb = new StringBuilder("Territories: ");
        for (int i = 0; i < house.getZoneIds().size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(house.getZoneIds().get(i).replace("_", " "));
        }
        JTextArea zoneList = new JTextArea(sb.toString());
        zoneList.setFont(UITheme.FONT_SMALL);
        zoneList.setForeground(new Color(180, 200, 160));
        zoneList.setBackground(UITheme.BG_PANEL);
        zoneList.setEditable(false);
        zoneList.setLineWrap(true);
        zoneList.setWrapStyleWord(true);
        zoneList.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        panel.add(name);
        panel.add(Box.createVerticalStrut(2));
        panel.add(leader);
        panel.add(Box.createVerticalStrut(2));
        panel.add(zones);
        panel.add(capital);
        panel.add(army);
        panel.add(Box.createVerticalStrut(4));
        panel.add(personality);
        panel.add(Box.createVerticalStrut(4));
        panel.add(zoneList);
        return panel;
    }

    private JPanel buildStats(NobleHouse house) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.BG_PANEL);
        panel.setPreferredSize(new Dimension(120, 0));

        panel.add(makeStatLabel("Opinion", house.getPlayerOpinion() + " / 100",
            opinionColor(house.getPlayerOpinion())));
        panel.add(Box.createVerticalStrut(6));
        panel.add(makeStatLabel("Gold",      String.valueOf(house.getGold()),
            new Color(210, 170, 80)));
        panel.add(Box.createVerticalStrut(6));
        panel.add(makeStatLabel("Food",      String.valueOf(house.getFood()),
            new Color(120, 200, 100)));
        panel.add(Box.createVerticalStrut(6));
        panel.add(makeStatLabel("Manpower",  String.valueOf(house.getManpower()),
            UITheme.TEXT_PRIMARY));
        panel.add(Box.createVerticalStrut(6));
        panel.add(makeStatLabel("Influence", String.valueOf(house.getInfluence()),
            UITheme.ACCENT_FROST));
        panel.add(Box.createVerticalStrut(6));
        panel.add(makeStatLabel("MP/turn",
            "+" + house.getManpowerPerTurn(),
            UITheme.TEXT_SECONDARY));
        return panel;
    }

    private JPanel makeStatLabel(String key, String value, Color valueColor) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBackground(UITheme.BG_PANEL);
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel keyLabel = new JLabel(key);
        keyLabel.setFont(UITheme.FONT_SMALL);
        keyLabel.setForeground(UITheme.TEXT_SECONDARY);

        JLabel valLabel = new JLabel(value);
        valLabel.setFont(UITheme.FONT_BODY);
        valLabel.setForeground(valueColor);

        row.add(keyLabel);
        row.add(valLabel);
        return row;
    }

    private Color opinionColor(int v) {
        if (v >= 70) return UITheme.TEXT_GREEN;
        if (v <= 30) return UITheme.TEXT_RED;
        return UITheme.TEXT_PRIMARY;
    }

    private Color raceColor(NobleHouse.Race race) {
        return switch (race) {
            case HUMAN -> new Color(180, 160, 120);
            case ELF   -> new Color(120, 200, 150);
            case DWARF -> new Color(160, 120, 80);
            case ORC   -> new Color(120, 160, 80);
        };
    }
}