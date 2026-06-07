// ArmyRenderer.java
package ui.map;

import main.army.Army;
import main.army.ArmyManager;
import main.map.Zone;
import main.map.ZoneManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders deployed armies on the map canvas.
 * Multiple armies in the same zone are laid out side by side in slots.
 * Heartland armies that are in city (not dragging) are also rendered.
 */
public class ArmyRenderer {

    private static final Color COLOR_ARMY_BODY     = new Color(60,  80,  160);
    private static final Color COLOR_ARMY_OUTLINE  = new Color(200, 210, 255);
    private static final Color COLOR_ARMY_BANNER   = new Color(220, 50,  50);
    private static final Color COLOR_LABEL         = new Color(240, 235, 255);
    private static final Color COLOR_LABEL_SHADOW  = new Color(10,  5,   30, 180);
    private static final Color COLOR_SELECTED_RING = new Color(100, 160, 255);

    private static final int   SLOT_WIDTH  = 38; // horizontal spacing between armies
    private static final Font  FONT_LABEL  = new Font("Serif", Font.BOLD, 9);

    private final ArmyManager armyManager;
    private final ZoneManager zoneManager;

    public ArmyRenderer(ArmyManager armyManager, ZoneManager zoneManager) {
        this.armyManager = armyManager;
        this.zoneManager = zoneManager;
    }

    public void render(Graphics2D g2, Army selectedArmy) {
        // Group visible armies by zone
        Map<String, List<Army>> byZone = new LinkedHashMap<>();
        for (Army army : armyManager.getArmies()) {
            if (army.isDragging()) continue;
            String zid = army.getZoneId();
            byZone.computeIfAbsent(zid, k -> new ArrayList<>()).add(army);
        }

        for (Map.Entry<String, List<Army>> entry : byZone.entrySet()) {
            Zone zone = zoneManager.getZone(entry.getKey());
            if (zone == null) continue;
            List<Army> armies = entry.getValue();
            int count = armies.size();
            // Centre the row of armies around the zone label anchor
            int anchorX = zone.getLabelX() + 30;
            int anchorY = zone.getLabelY() - 20;
            int totalWidth = (count - 1) * SLOT_WIDTH;
            int startX = anchorX - totalWidth / 2;

            for (int i = 0; i < count; i++) {
                int cx = startX + i * SLOT_WIDTH;
                drawArmy(g2, armies.get(i), cx, anchorY, armies.get(i) == selectedArmy);
            }
        }
    }

private void drawArmy(Graphics2D g2, Army army, int cx, int cy, boolean selected) {
    if (selected) {
        g2.setColor(COLOR_SELECTED_RING);
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(cx - 11, cy - 14, 22, 24);
    }

    // Shield
    int[] sx = { cx - 7, cx + 7, cx + 7, cx,    cx - 7 };
    int[] sy = { cy - 9,  cy - 9,  cy - 2, cy + 5, cy - 2 };
    g2.setColor(COLOR_ARMY_BODY);
    g2.fillPolygon(sx, sy, 5);
    g2.setColor(COLOR_ARMY_OUTLINE);
    g2.setStroke(new BasicStroke(1f));
    g2.drawPolygon(sx, sy, 5);

    // Banner pole
    g2.drawLine(cx, cy - 9, cx, cy - 19);

    // Banner flag
    int[] bx = { cx, cx + 7, cx };
    int[] by = { cy - 19, cy - 16, cy - 13 };
    g2.setColor(COLOR_ARMY_BANNER);
    g2.fillPolygon(bx, by, 3);
}

/** Returns the army at a given world point, or null. Checks all visible armies. */
    public Army hitTest(Point world, ZoneManager zm) {
        // Build same slot layout as render to get exact positions
        Map<String, List<Army>> byZone = new LinkedHashMap<>();
        for (Army army : armyManager.getArmies()) {
            if (army.isDragging()) continue;
            byZone.computeIfAbsent(army.getZoneId(), k -> new ArrayList<>()).add(army);
        }

        for (Map.Entry<String, List<Army>> entry : byZone.entrySet()) {
            Zone zone = zm.getZone(entry.getKey());
            if (zone == null) continue;
            List<Army> armies = entry.getValue();
            int count      = armies.size();
            int anchorX    = zone.getLabelX() + 30;
            int anchorY    = zone.getLabelY() - 20;
            int totalWidth = (count - 1) * SLOT_WIDTH;
            int startX     = anchorX - totalWidth / 2;

            for (int i = 0; i < count; i++) {
                int cx = startX + i * SLOT_WIDTH;
                int dx = world.x - cx;
                int dy = world.y - anchorY;
                if (dx * dx + dy * dy <= 11 * 11) return armies.get(i);
            }
        }
        return null;
    }
}