// MapInfoPanel.java
package ui.map;

import main.map.Zone;
import main.map.ZoneManager;
import main.map.ZoneState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import ui.UITheme;

/**
 * Sidebar panel showing details about the currently selected zone.
 */
public class MapInfoPanel extends JPanel {

    private final ZoneManager zoneManager;
    private final JLabel      titleLabel;
    private final JLabel      typeLabel;
    private final JLabel      goldLabel;
    private final JLabel      foodLabel;
    private final JLabel      popsLabel;
    private final JLabel      supplyLabel;
    private final JLabel      damageLabel;
    private final JLabel      adjacentLabel;
    private final JTextArea   adjacentArea;

    // Army info
    private final JLabel      armyTitleLabel;
    private final JLabel      armyZoneLabel;
    private final JLabel      armyMovesLabel;
    private final JLabel      armyOrdersLabel;
    private final JTextArea   armyOrdersArea;

    public MapInfoPanel(ZoneManager zoneManager) {
        this.zoneManager = zoneManager;
        setBackground(UITheme.BG_PANEL);
        setPreferredSize(new Dimension(200, 0));
        setBorder(new EmptyBorder(12, 10, 12, 10));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        titleLabel    = makeLabel("Select a zone", UITheme.TEXT_GOLD,      UITheme.FONT_HEADER);
        typeLabel     = makeLabel("",              UITheme.TEXT_SECONDARY,  UITheme.FONT_SMALL);
        goldLabel     = makeLabel("",              new Color(210, 170, 80), UITheme.FONT_BODY);
        foodLabel     = makeLabel("",              new Color(120, 200, 100),UITheme.FONT_BODY);
        popsLabel     = makeLabel("",              UITheme.TEXT_PRIMARY,    UITheme.FONT_BODY);
        supplyLabel   = makeLabel("",              UITheme.ACCENT_FROST,    UITheme.FONT_BODY);
        damageLabel   = makeLabel("",              UITheme.TEXT_RED,        UITheme.FONT_BODY);
        adjacentLabel = makeLabel("Adjacent:",     UITheme.TEXT_SECONDARY,  UITheme.FONT_SMALL);

        adjacentArea  = new JTextArea();
        adjacentArea.setEditable(false);
        adjacentArea.setBackground(UITheme.BG_PANEL);
        adjacentArea.setForeground(UITheme.TEXT_PRIMARY);
        adjacentArea.setFont(UITheme.FONT_SMALL);
        adjacentArea.setLineWrap(true);
        adjacentArea.setWrapStyleWord(true);
        adjacentArea.setMaximumSize(new Dimension(180, 80));

        armyTitleLabel  = makeLabel("",  UITheme.ACCENT_FROST,   UITheme.FONT_HEADER);
        armyZoneLabel   = makeLabel("",  UITheme.TEXT_PRIMARY,   UITheme.FONT_BODY);
        armyMovesLabel  = makeLabel("",  UITheme.TEXT_SECONDARY, UITheme.FONT_BODY);
        armyOrdersLabel = makeLabel("Orders:", UITheme.TEXT_SECONDARY, UITheme.FONT_SMALL);

        armyOrdersArea  = new JTextArea();
        armyOrdersArea.setEditable(false);
        armyOrdersArea.setBackground(UITheme.BG_PANEL);
        armyOrdersArea.setForeground(UITheme.TEXT_PRIMARY);
        armyOrdersArea.setFont(UITheme.FONT_SMALL);
        armyOrdersArea.setLineWrap(true);
        armyOrdersArea.setWrapStyleWord(true);
        armyOrdersArea.setMaximumSize(new Dimension(180, 80));

        add(titleLabel);
        add(Box.createVerticalStrut(4));
        add(typeLabel);
        add(separator());
        add(goldLabel);
        add(foodLabel);
        add(popsLabel);
        add(separator());
        add(supplyLabel);
        add(damageLabel);
        add(separator());
        add(adjacentLabel);
        add(adjacentArea);
        add(separator());
        add(armyTitleLabel);
        add(Box.createVerticalStrut(2));
        add(armyZoneLabel);
        add(armyMovesLabel);
        add(armyOrdersLabel);
        add(armyOrdersArea);
        add(Box.createVerticalGlue());

        showEmpty();
    }

public void showArmy(main.army.Army army) {
        clearArmyInfo();
        if (army == null) return;
        Zone zone = zoneManager.getZone(army.getZoneId());
        armyTitleLabel.setText("⚔ Army — " + army.getId());
        armyZoneLabel.setText("📍 " + (zone != null ? zone.getDisplayName() : army.getZoneId()));
        
        int movesRemaining = army.getMovesRemaining();
        armyMovesLabel.setText("🕐 Moves left this turn: " + movesRemaining + "/" + main.parameters.GameParameters.ARMY_MOVES_PER_TURN);

        var orders = army.getPendingOrders();
        String marchTarget = army.getMarchTarget();
        
        StringBuilder sb = new StringBuilder();
        
        // Show currently executing march order
        if (marchTarget != null) {
            Zone tgt = zoneManager.getZone(marchTarget);
            String tgtName = tgt != null ? tgt.getDisplayName() : marchTarget;
            Zone current = zoneManager.getZone(army.getZoneId());
            String currentName = current != null ? current.getDisplayName() : army.getZoneId();
            sb.append("▶ MARCHING: ").append(currentName).append(" → ").append(tgtName).append("\n");
        }
        
        // Show pending (not yet delivered) orders
        if (!orders.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("⏳ PENDING ORDERS (messenger en route):\n");
            for (var order : orders) {
                Zone tgt = zoneManager.getZone(order.getTargetZoneId());
                String tgtName = tgt != null ? tgt.getDisplayName() : order.getTargetZoneId();
                if (order.getTurnsRemaining() > 0) {
                    sb.append("   → ").append(tgtName)
                      .append(" — arrives in ").append(order.getTurnsRemaining()).append(" turn");
                    if (order.getTurnsRemaining() > 1) sb.append("s");
                    sb.append("\n");
                } else {
                    sb.append("   → ").append(tgtName).append(" — ready to march\n");
                }
            }
        }
        
        if (sb.length() == 0) {
            sb.append("IDLE\nClick on any zone to issue a march order.\n");
            sb.append("Orders have delay = distance from capital ÷ messenger speed.");
        }
        
        armyOrdersArea.setText(sb.toString());
    }

private void clearArmyInfo() {
        armyTitleLabel.setText("");
        armyZoneLabel.setText("");
        armyMovesLabel.setText("");
        armyOrdersArea.setText("");
    }

public void showZone(Zone zone) {
        if (zone == null) { showEmpty(); return; }
        ZoneState state = zoneManager.getState(zone.getId());

        titleLabel.setText(zone.getDisplayName());
        typeLabel.setText(zone.getSettlement().name().charAt(0)
            + zone.getSettlement().name().substring(1).toLowerCase());
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
    }

    private void showEmpty() {
        titleLabel.setText("Select a zone");
        typeLabel.setText("");
        goldLabel.setText("");
        foodLabel.setText("");
        popsLabel.setText("");
        supplyLabel.setText("");
        damageLabel.setText("");
        adjacentArea.setText("");
        clearArmyInfo();
    }

    private JLabel makeLabel(String text, Color color, Font font) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(font);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(UITheme.BORDER_COLOR);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(LEFT_ALIGNMENT);
        return sep;
    }
}