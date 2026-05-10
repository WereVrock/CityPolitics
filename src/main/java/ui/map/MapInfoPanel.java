// MapInfoPanel.java
package ui.map;

import main.army.Army;
import main.map.Zone;
import main.map.ZoneManager;
import main.map.ZoneState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import ui.UITheme;

/**
 * Right-side info panel. Uses CardLayout to switch between zone and army views.
 */
public class MapInfoPanel extends JPanel {

    private static final String CARD_ZONE  = "zone";
    private static final String CARD_ARMY  = "army";
    private static final String CARD_EMPTY = "empty";

    private final ZoneManager zoneManager;
    private final CardLayout  cardLayout;
    private final JPanel      cards;

    // Zone
    private final JLabel    zoneTitleLabel;
    private final JLabel    zoneTypeLabel;
    private final JLabel    ownerLabel;
    private final JLabel    goldLabel;
    private final JLabel    foodLabel;
    private final JLabel    popsLabel;
    private final JLabel    supplyLabel;
    private final JLabel    damageLabel;
    private final JTextArea adjacentArea;

    // Army
    private final JLabel armyTitleLabel;
    private final JLabel armyZoneLabel;
    private final JLabel armyStatusLabel;

    private final main.nobles.NobleHouseManager nobleHouseManager;

    public MapInfoPanel(ZoneManager zoneManager, main.nobles.NobleHouseManager nobleHouseManager) {
        this.zoneManager       = zoneManager;
        this.nobleHouseManager = nobleHouseManager;
        cardLayout = new CardLayout();
        cards      = new JPanel(cardLayout);
        cards.setBackground(UITheme.BG_PANEL);

        setBackground(UITheme.BG_PANEL);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 10, 12, 10));

        // ── Empty card ──
        JPanel emptyCard = new JPanel();
        emptyCard.setBackground(UITheme.BG_PANEL);
        JLabel emptyLabel = makeLabel("Select a zone\nor an army.", UITheme.TEXT_SECONDARY, UITheme.FONT_SMALL);
        emptyCard.add(emptyLabel);
        cards.add(emptyCard, CARD_EMPTY);

        // ── Zone card ──
        zoneTitleLabel = makeLabel("", UITheme.TEXT_GOLD,           UITheme.FONT_HEADER);
        zoneTypeLabel  = makeLabel("", UITheme.TEXT_SECONDARY,      UITheme.FONT_SMALL);
        ownerLabel     = makeLabel("", new Color(220, 190, 130),    UITheme.FONT_SMALL);
        goldLabel      = makeLabel("", new Color(210, 170, 80),     UITheme.FONT_BODY);
        foodLabel      = makeLabel("", new Color(120, 200, 100),    UITheme.FONT_BODY);
        popsLabel      = makeLabel("", UITheme.TEXT_PRIMARY,        UITheme.FONT_BODY);
        supplyLabel    = makeLabel("", UITheme.ACCENT_FROST,        UITheme.FONT_BODY);
        damageLabel    = makeLabel("", UITheme.TEXT_RED,            UITheme.FONT_BODY);

        adjacentArea = new JTextArea();
        adjacentArea.setEditable(false);
        adjacentArea.setBackground(UITheme.BG_PANEL);
        adjacentArea.setForeground(UITheme.TEXT_PRIMARY);
        adjacentArea.setFont(UITheme.FONT_SMALL);
        adjacentArea.setLineWrap(true);
        adjacentArea.setWrapStyleWord(true);

        JPanel zoneCard = new JPanel();
        zoneCard.setLayout(new BoxLayout(zoneCard, BoxLayout.Y_AXIS));
        zoneCard.setBackground(UITheme.BG_PANEL);
        zoneCard.add(zoneTitleLabel);
        zoneCard.add(Box.createVerticalStrut(4));
        zoneCard.add(zoneTypeLabel);
        zoneCard.add(Box.createVerticalStrut(2));
        zoneCard.add(ownerLabel);
        zoneCard.add(sep());
        zoneCard.add(goldLabel);
        zoneCard.add(foodLabel);
        zoneCard.add(popsLabel);
        zoneCard.add(sep());
        zoneCard.add(supplyLabel);
        zoneCard.add(damageLabel);
        zoneCard.add(sep());
        zoneCard.add(makeLabel("Adjacent:", UITheme.TEXT_SECONDARY, UITheme.FONT_SMALL));
        zoneCard.add(adjacentArea);
        zoneCard.add(Box.createVerticalGlue());
        cards.add(zoneCard, CARD_ZONE);

        // ── Army card ──
        armyTitleLabel  = makeLabel("", UITheme.ACCENT_FROST,   UITheme.FONT_HEADER);
        armyZoneLabel   = makeLabel("", UITheme.TEXT_PRIMARY,   UITheme.FONT_BODY);
        armyStatusLabel = makeLabel("", UITheme.TEXT_SECONDARY, UITheme.FONT_SMALL);

        JPanel armyCard = new JPanel();
        armyCard.setLayout(new BoxLayout(armyCard, BoxLayout.Y_AXIS));
        armyCard.setBackground(UITheme.BG_PANEL);
        armyCard.add(armyTitleLabel);
        armyCard.add(Box.createVerticalStrut(4));
        armyCard.add(armyZoneLabel);
        armyCard.add(Box.createVerticalStrut(4));
        armyCard.add(armyStatusLabel);
        armyCard.add(Box.createVerticalStrut(8));

        armyCard.add(Box.createVerticalGlue());
        cards.add(armyCard, CARD_ARMY);

        add(cards, BorderLayout.CENTER);
        cardLayout.show(cards, CARD_EMPTY);
    }

public void showZone(Zone zone) {
    if (zone == null) { clearZone(); return; }
    ZoneState state = zoneManager.getState(zone.getId());

    zoneTitleLabel.setText(zone.getDisplayName());
    zoneTypeLabel.setText(capitalize(zone.getSettlement().name()));

    main.nobles.NobleHouse owner = nobleHouseManager.getOwnerOfZone(zone.getId());
    ownerLabel.setText(owner != null ? owner.getName() : "Unowned");

    goldLabel.setText("Gold/turn:  " + zone.getGoldProduction());
    foodLabel.setText("Food/turn:  " + zone.getFoodProduction());
    popsLabel.setText("Pops:       " + zone.getZonePops());
    supplyLabel.setText("Supply:     " + state.getSupplyLevel() + "%");
    damageLabel.setText("Damage:     " + state.getDamage() + "%");

    StringBuilder sb = new StringBuilder();
    for (String adjId : zone.getAdjacentIds()) {
        Zone adj = zoneManager.getZone(adjId);
        if (adj != null) sb.append(adj.getDisplayName()).append("\n");
    }
    adjacentArea.setText(sb.toString().trim());
    cardLayout.show(cards, CARD_ZONE);
}

public void showArmy(Army army, ZoneManager zm) {
        if (army == null) { clearArmy(); return; }
        armyTitleLabel.setText("⚔ " + army.getDisplayName());
        if (army.isInCity()) {
            armyZoneLabel.setText("📍 Heartland (City)");
            armyStatusLabel.setText("Status: In city");
        } else {
            Zone zone = zm.getZone(army.getZoneId());
            armyZoneLabel.setText("📍 " + (zone != null ? zone.getDisplayName() : army.getZoneId()));
            armyStatusLabel.setText("Status: Deployed");
        }
        cardLayout.show(cards, CARD_ARMY);
    }

public void clearZone() { cardLayout.show(cards, CARD_EMPTY); }
    public void clearArmy() { cardLayout.show(cards, CARD_EMPTY); }

    private JLabel makeLabel(String text, Color color, Font font) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(font);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JSeparator sep() {
        JSeparator s = new JSeparator();
        s.setForeground(UITheme.BORDER_COLOR);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        s.setAlignmentX(LEFT_ALIGNMENT);
        return s;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}