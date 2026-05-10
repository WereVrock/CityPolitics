// MapView.java
package ui.map;

import main.core.GameState;
import main.map.ZoneManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import main.army.Army;
import ui.UITheme;

/**
 * Full map screen: MapPanel (centre) + MapInfoPanel (right sidebar)
 * + ArmyListPanel (bottom-right) + back button.
 */
public class MapView extends JPanel {

    private final MapPanel     mapPanel;
    private final MapInfoPanel infoPanel;
    private final ArmyListPanel armyListPanel;

public MapView(GameState gameState, Runnable onBack) {
        ZoneManager zoneManager = gameState.getZoneManager();
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_DARK);

        infoPanel     = new MapInfoPanel(zoneManager, gameState.getNobleHouseManager());
        armyListPanel = new ArmyListPanel(gameState.getArmyManager());

        mapPanel = new MapPanel(
            gameState,
            zone -> { infoPanel.showZone(zone); },
            army -> { if (army != null) infoPanel.showArmy(army, zoneManager); else infoPanel.clearArmy(); },
            armyListPanel,
            gameState.getNobleHouseManager()
        );

        armyListPanel.setOnDragDropCallback(new ArmyListPanel.DragDropCallback() {
            @Override
            public void onDrop(Army army, String zoneId) {
                // army.moveTo() already called in MapPanel.drop()
                armyListPanel.refresh();
                mapPanel.repaint();
                infoPanel.showArmy(army, zoneManager);
            }

            @Override
            public void onDragCancelled(Army army) {
                // army.cancelDrag() already called in ArmyListPanel drag end
                armyListPanel.refresh();
                mapPanel.repaint();
            }
        });

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(UITheme.BG_PANEL);
        rightPanel.setPreferredSize(new Dimension(240, 0));
        rightPanel.add(infoPanel,     BorderLayout.CENTER);
        rightPanel.add(armyListPanel, BorderLayout.SOUTH);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UITheme.BG_PANEL);
        topBar.setBorder(new EmptyBorder(6, 12, 6, 12));

        JLabel title = new JLabel("REALM MAP");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_GOLD);

        JButton backBtn = new JButton("◀ BACK");
        backBtn.setFont(UITheme.FONT_BUTTON);
        backBtn.setForeground(UITheme.TEXT_SECONDARY);
        backBtn.setBackground(UITheme.BUTTON_BG);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> onBack.run());

        JButton politicalBtn = new JButton("⚑ POLITICAL VIEW");
        politicalBtn.setFont(UITheme.FONT_BUTTON);
        politicalBtn.setForeground(UITheme.TEXT_SECONDARY);
        politicalBtn.setBackground(UITheme.BUTTON_BG);
        politicalBtn.setBorderPainted(false);
        politicalBtn.setFocusPainted(false);
        politicalBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        politicalBtn.addActionListener(e -> {
            mapPanel.togglePoliticalView();
            boolean active = mapPanel.isPoliticalViewActive();
            politicalBtn.setForeground(active ? UITheme.TEXT_GOLD : UITheme.TEXT_SECONDARY);
            politicalBtn.setBackground(active ? new Color(60, 40, 20) : UITheme.BUTTON_BG);
        });

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        topRight.setBackground(UITheme.BG_PANEL);
        topRight.add(politicalBtn);
        topRight.add(backBtn);

        topBar.add(title,    BorderLayout.WEST);
        topBar.add(topRight, BorderLayout.EAST);

        add(topBar,     BorderLayout.NORTH);
        add(mapPanel,   BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }

public void refresh() {
        mapPanel.clearSelection();
        infoPanel.clearZone();
        infoPanel.clearArmy();
        armyListPanel.refresh();
        mapPanel.repaint();
    }
}