// ui/map/ArmyRenderer.java
package ui.map;

import main.army.Army;
import main.army.ArmyManager;
import main.army.PendingOrder;
import main.map.Zone;
import main.map.ZoneManager;

import java.awt.*;
import java.util.Deque;

/**
 * Renders armies on the map canvas.
 * Each army is drawn as a shield icon with a banner.
 * Armies with pending orders show a small hourglass indicator.
 */
public class ArmyRenderer {

    private static final Color COLOR_ARMY_BODY    = new Color(60,  80,  160);
    private static final Color COLOR_ARMY_OUTLINE  = new Color(200, 210, 255);
    private static final Color COLOR_ARMY_BANNER   = new Color(220, 50,  50);
    private static final Color COLOR_PENDING_DOT   = new Color(240, 190, 40);
    private static final Color COLOR_LABEL         = new Color(240, 235, 255);
    private static final Color COLOR_LABEL_SHADOW  = new Color(10,  5,   30, 180);
    private static final Color COLOR_SELECTED_RING = new Color(100, 160, 255);

    private static final Font FONT_LABEL = new Font("Serif", Font.BOLD, 10);

    private final ArmyManager armyManager;
    private final ZoneManager zoneManager;

    public ArmyRenderer(ArmyManager armyManager, ZoneManager zoneManager) {
        this.armyManager = armyManager;
        this.zoneManager = zoneManager;
    }

    public void render(Graphics2D g2, Army selectedArmy) {
        for (Army army : armyManager.getArmies()) {
            Zone zone = zoneManager.getZone(army.getZoneId());
            if (zone == null) continue;
            int cx = zone.getLabelX() + 30;
            int cy = zone.getLabelY() - 20;
            drawArmy(g2, army, cx, cy, army == selectedArmy);
        }
    }

    private void drawArmy(Graphics2D g2, Army army, int cx, int cy, boolean selected) {
        if (selected) {
            g2.setColor(COLOR_SELECTED_RING);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(cx - 13, cy - 16, 26, 28);
        }

        // Shield body
        int[] sx = { cx - 8, cx + 8, cx + 8, cx,     cx - 8 };
        int[] sy = { cy - 10, cy - 10, cy - 2, cy + 6, cy - 2  };
        g2.setColor(COLOR_ARMY_BODY);
        g2.fillPolygon(sx, sy, 5);
        g2.setColor(COLOR_ARMY_OUTLINE);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawPolygon(sx, sy, 5);

        // Banner pole
        g2.setColor(COLOR_ARMY_OUTLINE);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(cx, cy - 10, cx, cy - 22);

        // Banner flag
        int[] bx = { cx, cx + 8, cx };
        int[] by = { cy - 22, cy - 19, cy - 16 };
        g2.setColor(COLOR_ARMY_BANNER);
        g2.fillPolygon(bx, by, 3);

        // Pending order indicator (yellow dot + turns)
        Deque<PendingOrder> orders = army.getPendingOrders();
        if (!orders.isEmpty()) {
            PendingOrder head = orders.peekFirst();
            g2.setColor(COLOR_PENDING_DOT);
            g2.fillOval(cx + 5, cy - 14, 7, 7);
            if (head.getTurnsRemaining() > 0) {
                g2.setFont(new Font("SansSerif", Font.BOLD, 7));
                g2.setColor(Color.BLACK);
                g2.drawString(String.valueOf(head.getTurnsRemaining()), cx + 7, cy - 8);
            }
        }

        // Label
        g2.setFont(FONT_LABEL);
        String label = "Army";
        g2.setColor(COLOR_LABEL_SHADOW);
        g2.drawString(label, cx - 9, cy + 16);
        g2.setColor(COLOR_LABEL);
        g2.drawString(label, cx - 10, cy + 15);
    }

    /** Returns the army at a given world point, or null. */
    public Army hitTest(Point world, ZoneManager zm) {
        for (Army army : armyManager.getArmies()) {
            Zone zone = zm.getZone(army.getZoneId());
            if (zone == null) continue;
            int cx = zone.getLabelX() + 30;
            int cy = zone.getLabelY() - 20;
            int dx = world.x - cx;
            int dy = world.y - cy;
            if (dx * dx + dy * dy <= 13 * 13) return army;
        }
        return null;
    }
}