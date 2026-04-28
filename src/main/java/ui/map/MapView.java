// MapView.java
package ui.map;

import main.map.ZoneManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import ui.UITheme;

/**
 * Full map screen: MapPanel (centre) + MapInfoPanel (right sidebar) + back button.
 */
public class MapView extends JPanel {

    private final MapPanel     mapPanel;
    private final MapInfoPanel infoPanel;

    public MapView(main.core.GameState gameState, Runnable onBack) {
        ZoneManager zoneManager = gameState.getZoneManager();
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_DARK);

        infoPanel = new MapInfoPanel(zoneManager);
        mapPanel  = new MapPanel(
            zoneManager,
            gameState.getArmyManager(),
            zone  -> infoPanel.showZone(zone),
            army  -> infoPanel.showArmy(army)
        );

        // Top bar
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

        topBar.add(title,   BorderLayout.WEST);
        topBar.add(backBtn, BorderLayout.EAST);

        // Scroll pane for map (allows panning beyond visible area if needed)
        JScrollPane scroll = new JScrollPane(mapPanel,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setBackground(new Color(30, 24, 18));

        add(topBar,    BorderLayout.NORTH);
        add(scroll,    BorderLayout.CENTER);
        add(infoPanel, BorderLayout.EAST);
    }

    public void refresh() {
        mapPanel.clearSelection();
        infoPanel.showZone(null);
        infoPanel.showArmy(null);
        mapPanel.repaint();
    }
}