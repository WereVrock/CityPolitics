// NobleHousesPanel.java
package City.ui.nobles;

import City.main.core.GameState;
import City.main.nobles.NobleHouse;
import City.main.nobles.NobleHouseColors;
import City.main.nobles.NobleHouseManager;
import City.main.bank.BankManager;
import City.ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Overview panel for all noble houses.
 * Styled to match PartiesOverviewPanel.
 */
public class NobleHousesPanel extends JPanel {

    private enum HouseFilter { ALL, LANDED, LANDLESS }

    private final GameState gameState;
    private final Runnable  onBack;
    private final JPanel    listPanel;

    private HouseFilter currentFilter = HouseFilter.ALL;
    private JButton      filterButton;

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
        scroll.getVerticalScrollBar().setUnitIncrement(32);
        add(scroll, BorderLayout.CENTER);
    }

private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel title = new JLabel("NOBLE HOUSES");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_GOLD);

        filterButton = new JButton(filterLabel());
        filterButton.setFont(UITheme.FONT_BUTTON);
        filterButton.setForeground(UITheme.TEXT_SECONDARY);
        filterButton.setBackground(UITheme.BUTTON_BG);
        filterButton.setBorderPainted(false);
        filterButton.setFocusPainted(false);
        filterButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        filterButton.addActionListener(e -> {
            cycleFilter();
            refresh();
        });

        JButton back = new JButton("← BACK");
        back.setFont(UITheme.FONT_BUTTON);
        back.setForeground(UITheme.TEXT_SECONDARY);
        back.setBackground(UITheme.BUTTON_BG);
        back.setBorderPainted(false);
        back.setFocusPainted(false);
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.addActionListener(e -> onBack.run());

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightControls.setBackground(UITheme.BG_DARK);
        rightControls.add(filterButton);
        rightControls.add(back);

        panel.add(title,         BorderLayout.WEST);
        panel.add(rightControls, BorderLayout.EAST);
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
        if (!matchesFilter(house)) continue;
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
        BorderFactory.createLineBorder(NobleHouseColors.getPrimary(house.getId()), 2),
        new EmptyBorder(10, 12, 10, 12)
    ));

    JPanel west = new JPanel(new BorderLayout(6, 0));
    west.setBackground(UITheme.BG_PANEL);
    west.add(buildColorAccent(house), BorderLayout.WEST);
    west.add(buildPortrait(house),    BorderLayout.CENTER);

    card.add(west,               BorderLayout.WEST);
    card.add(buildInfo(house),   BorderLayout.CENTER);
    card.add(buildStats(house),  BorderLayout.EAST);

    card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    card.addMouseListener(new MouseAdapter() {
        @Override public void mouseClicked(MouseEvent e) {
            openHouseDetail(house);
        }
    });

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
                String init = houseInitial(house);
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
        JPanel panel = new JPanel(new GridLayout(6, 1, 0, 4));
        panel.setBackground(UITheme.BG_PANEL);
        panel.setPreferredSize(new Dimension(150, 0));

        panel.add(makeStatLabel("Opinion", house.getPlayerOpinion() + " / 100",
            opinionColor(house.getPlayerOpinion())));
        panel.add(makeStatLabel("Gold",      String.valueOf(house.getGold()),
            new Color(210, 170, 80)));
        panel.add(makeStatLabel("Food",      String.valueOf(house.getFood()),
            new Color(120, 200, 100)));
        panel.add(makeStatLabel("Manpower",  String.valueOf(house.getManpower()),
            UITheme.TEXT_PRIMARY));
        panel.add(makeStatLabel("Influence", String.valueOf(house.getInfluence()),
            UITheme.ACCENT_FROST));
        panel.add(makeStatLabel("MP/turn",
            "+" + house.getManpowerPerTurn(),
            UITheme.TEXT_SECONDARY));
        return panel;
    }

private JLabel makeStatLabel(String key, String value, Color valueColor) {
        String hexValue = String.format("#%02x%02x%02x",
            valueColor.getRed(), valueColor.getGreen(), valueColor.getBlue());
        Color keyColor = UITheme.TEXT_SECONDARY;
        String hexKey = String.format("#%02x%02x%02x",
            keyColor.getRed(), keyColor.getGreen(), keyColor.getBlue());

        JLabel label = new JLabel("<html><span style='color:" + hexKey + "'>" + key
            + ":</span> <b><span style='color:" + hexValue + "'>" + value + "</span></b></html>");
        label.setFont(UITheme.FONT_SMALL);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
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

private void cycleFilter() {
        if (currentFilter == HouseFilter.ALL) {
            currentFilter = HouseFilter.LANDED;
        } else if (currentFilter == HouseFilter.LANDED) {
            currentFilter = HouseFilter.LANDLESS;
        } else {
            currentFilter = HouseFilter.ALL;
        }
        filterButton.setText(filterLabel());
    }

    private String filterLabel() {
        if (currentFilter == HouseFilter.LANDED)   return "SHOWING: LANDED";
        if (currentFilter == HouseFilter.LANDLESS) return "SHOWING: LANDLESS";
        return "SHOWING: ALL";
    }

    private boolean matchesFilter(NobleHouse house) {
        if (currentFilter == HouseFilter.LANDED)   return !house.getZoneIds().isEmpty();
        if (currentFilter == HouseFilter.LANDLESS) return house.getZoneIds().isEmpty();
        return true;
    }

    private JPanel buildColorAccent(NobleHouse house) {
        JPanel accent = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int h = getHeight();
                g.setColor(NobleHouseColors.getPrimary(house.getId()));
                g.fillRect(0, 0, getWidth(), h * 2 / 3);
                g.setColor(NobleHouseColors.getSecondary(house.getId()));
                g.fillRect(0, h * 2 / 3, getWidth(), h - (h * 2 / 3));
            }
        };
        accent.setPreferredSize(new Dimension(6, 95));
        accent.setOpaque(false);
        return accent;
    }

    private void openHouseDetail(NobleHouse house) {
        NobleHouseManager manager = gameState.getNobleHouseManager();
        Window owner = SwingUtilities.getWindowAncestor(this);
        BankManager bankManager = manager.getBankManager();
        NobleHouseDetailDialog dialog = new NobleHouseDetailDialog(
            owner, house, manager.getClaimManager(), manager.getZoneManager(), bankManager);
        City.debug.Debug.log("ui", "noble-house-detail",
            "Opened detail view for " + house.getName());
        dialog.setVisible(true);
    }

private String houseInitial(NobleHouse house) {
        String name = house.getName();
        String[] words = name.split("\\s+");
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (word.equalsIgnoreCase("the")
                || word.equalsIgnoreCase("house")
                || word.equalsIgnoreCase("a")
                || word.equalsIgnoreCase("an")) {
                continue;
            }
            return word.substring(0, 1).toUpperCase();
        }
        return name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
    }

}