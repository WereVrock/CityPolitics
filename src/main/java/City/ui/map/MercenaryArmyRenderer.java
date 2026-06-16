// ===== MercenaryArmyRenderer.java (NEW) =====
package City.ui.map;

import City.main.mercenaries.MercenaryArmy;
import City.main.mercenaries.MercenaryManager;
import City.main.map.Zone;
import City.main.map.ZoneManager;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Renders mercenary armies on the map canvas as gold diamond shields.
 */
public class MercenaryArmyRenderer {

    private static final Color COLOR_BODY     = new Color(130, 100,  30);
    private static final Color COLOR_OUTLINE  = new Color(220, 180,  60);
    private static final Color COLOR_LABEL    = new Color(255, 230, 150);
    private static final Color COLOR_SHADOW   = new Color(10,   5,   0, 180);
    private static final Color COLOR_SELECTED = new Color(255, 240, 100);

    private static final int  SLOT_WIDTH = 32;
    private static final Font FONT_LABEL = new Font("Serif", Font.BOLD, 9);

    private final MercenaryManager mercenaryManager;
    private final ZoneManager       zoneManager;
    private City.main.mercenaries.MercenaryArmy selectedArmy = null;

    public void setSelectedArmy(City.main.mercenaries.MercenaryArmy army) { this.selectedArmy = army; }

    public MercenaryArmyRenderer(MercenaryManager mercenaryManager, ZoneManager zoneManager) {
        this.mercenaryManager = mercenaryManager;
        this.zoneManager      = zoneManager;
    }

    public void render(Graphics2D g2) {
        Map<String, List<MercenaryArmy>> byZone = new LinkedHashMap<>();
        for (MercenaryArmy army : mercenaryManager.getArmies()) {
            if (!army.isAlive()) continue;
            byZone.computeIfAbsent(army.getZoneId(), k -> new ArrayList<>()).add(army);
        }

        for (Map.Entry<String, List<MercenaryArmy>> entry : byZone.entrySet()) {
            Zone zone = zoneManager.getZone(entry.getKey());
            if (zone == null) continue;
            List<MercenaryArmy> armies = entry.getValue();
            int count      = armies.size();
            // Offset left of player armies (+30 offset) so they don't overlap
            int anchorX    = zone.getLabelX() - 32;
            int anchorY    = zone.getLabelY() - 20;
            int totalWidth = (count - 1) * SLOT_WIDTH;
            int startX     = anchorX - totalWidth / 2;
            for (int i = 0; i < count; i++) {
                drawMercArmy(g2, armies.get(i), startX + i * SLOT_WIDTH, anchorY);
            }
        }
    }

private void drawMercArmy(Graphics2D g2, MercenaryArmy army, int cx, int cy) {
        boolean selected = army == selectedArmy;
        if (selected) {
            g2.setColor(COLOR_SELECTED);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawOval(cx - 12, cy - 14, 24, 26);
            g2.setStroke(new BasicStroke(1f));
        }
        // Diamond / coin shape
        int[] sx = { cx, cx + 8, cx, cx - 8 };
        int[] sy = { cy - 10, cy, cy + 10, cy };
        g2.setColor(COLOR_BODY);
        g2.fillPolygon(sx, sy, 4);
        g2.setColor(COLOR_OUTLINE);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawPolygon(sx, sy, 4);
        g2.setStroke(new BasicStroke(1f));

        // $ symbol inside
        g2.setFont(new Font("SansSerif", Font.BOLD, 7));
        g2.setColor(COLOR_OUTLINE);
        g2.drawString("$", cx - 3, cy + 3);

        // Size label
        String lbl = String.valueOf(army.getSize());
        g2.setFont(FONT_LABEL);
        FontMetrics fm = g2.getFontMetrics();
        int lx = cx - fm.stringWidth(lbl) / 2;
        int ly = cy + 20;
        g2.setColor(COLOR_SHADOW);
        g2.drawString(lbl, lx + 1, ly + 1);
        g2.setColor(COLOR_LABEL);
        g2.drawString(lbl, lx, ly);
    }

public MercenaryArmy hitTest(Point world) {
        Map<String, List<MercenaryArmy>> byZone = new LinkedHashMap<>();
        for (MercenaryArmy army : mercenaryManager.getArmies()) {
            if (!army.isAlive()) continue;
            byZone.computeIfAbsent(army.getZoneId(), k -> new ArrayList<>()).add(army);
        }
        for (Map.Entry<String, List<MercenaryArmy>> entry : byZone.entrySet()) {
            Zone zone = zoneManager.getZone(entry.getKey());
            if (zone == null) continue;
            List<MercenaryArmy> armies = entry.getValue();
            int count      = armies.size();
            int anchorX    = zone.getLabelX() - 32;
            int anchorY    = zone.getLabelY() - 20;
            int totalWidth = (count - 1) * SLOT_WIDTH;
            int startX     = anchorX - totalWidth / 2;
            for (int i = 0; i < count; i++) {
                int cx = startX + i * SLOT_WIDTH;
                int dx = world.x - cx;
                int dy = world.y - anchorY;
                if (dx * dx + dy * dy <= 12 * 12) return armies.get(i);
            }
        }
        return null;
    }

}