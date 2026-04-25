// MapInfoPanel.java
package ui;

import main.map.Zone;
import main.map.ZoneManager;
import main.map.ZoneState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

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
        add(Box.createVerticalGlue());

        showEmpty();
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