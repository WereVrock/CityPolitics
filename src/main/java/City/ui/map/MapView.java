// MapView.java
package City.ui.map;

import City.main.core.GameState;
import City.main.map.ZoneManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import City.main.army.Army;
import City.main.map.Zone;
import City.ui.UITheme;

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
        infoPanel.setRavagedZoneManager(gameState.getRavagedZoneManager());
        infoPanel.setBarbArmyManager(gameState.getBarbArmyManager());
        armyListPanel = new ArmyListPanel(gameState.getArmyManager());
        armyListPanel.setMercenaryManager(gameState.getMercenaryManager());

        mapPanel = new MapPanel(
            gameState,
            zone -> { infoPanel.showZone(zone); },
            army -> { if (army != null) infoPanel.showArmy(army, zoneManager); else infoPanel.clearArmy(); },
            nobleArmy -> {
                if (nobleArmy != null) {
                    infoPanel.showNobleArmy(nobleArmy, zoneManager, gameState.getNobleHouseManager());
                } else {
                    infoPanel.clearArmy();
                }
            },
            barbArmy -> {
                if (barbArmy != null) {
                    infoPanel.showBarbArmy(barbArmy, gameState);
                } else {
                    infoPanel.clearArmy();
                }
            },
            mercArmy -> {
                if (mercArmy != null) {
                    infoPanel.showMercArmy(mercArmy, zoneManager);
                } else {
                    infoPanel.clearArmy();
                }
            },
            armyListPanel,
            gameState.getNobleHouseManager()
        );

        armyListPanel.setOnDragDropCallback(new ArmyListPanel.DragDropCallback() {
            @Override
            public void onDrop(Army army, String zoneId) {
                armyListPanel.refresh();
                mapPanel.repaint();
                infoPanel.showArmy(army, zoneManager);
            }

            @Override
            public void onDragCancelled(Army army) {
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

        JButton viewModeBtn = new JButton(MapViewMode.SETTLEMENT.label());
        viewModeBtn.setFont(UITheme.FONT_BUTTON);
        viewModeBtn.setForeground(UITheme.TEXT_GOLD);
        viewModeBtn.setBackground(new Color(60, 40, 20));
        viewModeBtn.setBorderPainted(false);
        viewModeBtn.setFocusPainted(false);
        viewModeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        viewModeBtn.addActionListener(e -> {
            mapPanel.cycleViewMode();
            MapViewMode mode = mapPanel.getViewMode();
            viewModeBtn.setText(mode.label());
            boolean active = mode != MapViewMode.SETTLEMENT;
            viewModeBtn.setForeground(active ? UITheme.TEXT_GOLD : UITheme.TEXT_SECONDARY);
            viewModeBtn.setBackground(active ? new Color(60, 40, 20) : UITheme.BUTTON_BG);
        });

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        topRight.setBackground(UITheme.BG_PANEL);
        topRight.add(viewModeBtn);
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

    /** Re-shows the currently selected zone (if any) after data changes like end turn. */
    public void refreshSelectedZone() {
        Zone zone = mapPanel.getSelectedZone();
        if (zone != null) infoPanel.showZone(zone);
    }

/**
     * Activates zone-picker mode across both the map canvas (grey-out) and the
     * info panel (eligibility text + select button).
     */
    public void enterZonePickerMode(java.util.Set<String> validIds,
                                     java.util.function.Consumer<String> onPick) {
        mapPanel.setPickerValidZoneIds(validIds);
        infoPanel.setUnlawfulPickerMode(validIds, onPick);
    }

    public void exitZonePickerMode() {
        mapPanel.clearPickerValidZoneIds();
        infoPanel.clearUnlawfulPickerMode();
    }

public void reinitialize(GameState gameState) {
    mapPanel.reinitialize(gameState);
    infoPanel.reinitialize(gameState);
    armyListPanel.reinitialize(gameState.getArmyManager());
    armyListPanel.setMercenaryManager(gameState.getMercenaryManager());
    refresh();
}

/** Re-applies map-panel fonts to the info and army-list panels. */
    public void applyMapPanelFonts() {
        infoPanel.applyMapPanelFonts();
        armyListPanel.applyMapPanelFonts();
    }

public MapInfoPanel getInfoPanel() { return infoPanel; }

}