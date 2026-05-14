package ui.map;

import main.nobles.NobleArmy;
import main.nobles.NobleArmyManager;
import main.nobles.NobleHouseColors;
import main.map.Zone;
import main.map.ZoneManager;

import java.awt.*;
import java.util.*;
import java.util.List;

public class NobleArmyRenderer {

    private static final Color COLOR_OUTLINE       = new Color(20,  10,  5,  220);
    private static final Color COLOR_LABEL         = new Color(240, 235, 210);
    private static final Color COLOR_LABEL_SHADOW  = new Color(10,  5,   0,  180);
    private static final Color COLOR_SELECTED_RING = new Color(255, 230, 100);

    private static final int  SLOT_WIDTH  = 34;
    private static final int  Y_OFFSET    = 14;
    private static final Font FONT_LABEL  = new Font("Serif", Font.BOLD, 9);

    private final NobleArmyManager  armyManager;
    private final ZoneManager       zoneManager;
    private final main.nobles.NobleHouseManager nobleHouseManager;

    public NobleArmyRenderer(NobleArmyManager armyManager, ZoneManager zoneManager,
                              main.nobles.NobleHouseManager nobleHouseManager) {
        this.armyManager      = armyManager;
        this.zoneManager      = zoneManager;
        this.nobleHouseManager = nobleHouseManager;
    }

public void render(Graphics2D g2, NobleArmy selectedArmy) {
        // Layer A — garrisons (small shields, right of label)
        drawGarrisons(g2);

        // Layer B — raised armies (larger shields with size label, above zone label)
        Map<String, List<NobleArmy>> byZone = groupByZone();
        for (Map.Entry<String, List<NobleArmy>> entry : byZone.entrySet()) {
            Zone zone = zoneManager.getZone(entry.getKey());
            if (zone == null) continue;
            List<NobleArmy> armies = entry.getValue();
            int count      = armies.size();
            int anchorX    = zone.getLabelX();
            int anchorY    = zone.getLabelY() - 30; // above the zone name
            int totalWidth = (count - 1) * SLOT_WIDTH;
            int startX     = anchorX - totalWidth / 2;
            for (int i = 0; i < count; i++) {
                drawNobleArmy(g2, armies.get(i), startX + i * SLOT_WIDTH, anchorY,
                    armies.get(i) == selectedArmy);
            }
        }
    }

public NobleArmy hitTest(Point world) {
        Map<String, List<NobleArmy>> byZone = groupByZone();
        for (Map.Entry<String, List<NobleArmy>> entry : byZone.entrySet()) {
            Zone zone = zoneManager.getZone(entry.getKey());
            if (zone == null) continue;
            List<NobleArmy> armies = entry.getValue();
            int count      = armies.size();
            int anchorX    = zone.getLabelX();
            int anchorY    = zone.getLabelY() - 30;
            int totalWidth = (count - 1) * SLOT_WIDTH;
            int startX     = anchorX - totalWidth / 2;
            for (int i = 0; i < count; i++) {
                int cx = startX + i * SLOT_WIDTH;
                int dx = world.x - cx;
                int dy = world.y - anchorY;
                if (dx * dx + dy * dy <= 14 * 14) return armies.get(i);
            }
        }
        return null;
    }

private void drawNobleArmy(Graphics2D g2, NobleArmy army,
                                int cx, int cy, boolean selected) {
        Color primary = NobleHouseColors.getPrimary(army.getHouseId());
        Color banner  = NobleHouseColors.getSecondary(army.getHouseId());

        if (selected) {
            g2.setColor(COLOR_SELECTED_RING);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawOval(cx - 13, cy - 16, 26, 28);
            g2.setStroke(new BasicStroke(1f));
        }

        // Larger shield than garrison
        int[] sx = { cx - 9, cx + 9, cx + 9, cx,    cx - 9 };
        int[] sy = { cy - 11, cy - 11, cy - 3, cy + 7, cy - 3 };
        g2.setColor(primary);
        g2.fillPolygon(sx, sy, 5);
        g2.setColor(COLOR_OUTLINE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawPolygon(sx, sy, 5);
        g2.setStroke(new BasicStroke(1f));

        // Banner pole
        g2.setColor(COLOR_OUTLINE);
        g2.drawLine(cx, cy - 11, cx, cy - 23);

        // Banner flag
        int[] bx = { cx, cx + 9, cx };
        int[] by = { cy - 23, cy - 19, cy - 15 };
        g2.setColor(banner);
        g2.fillPolygon(bx, by, 3);
        g2.setColor(COLOR_OUTLINE);
        g2.drawPolygon(bx, by, 3);

        // Order indicator dot
        if (army.hasPendingOrder()) {
            Color orderColor = army.getPendingOrder() == NobleArmy.OrderType.ATTACK
                ? new Color(220, 50, 50) : new Color(220, 180, 50);
            g2.setColor(orderColor);
            g2.fillOval(cx + 6, cy - 26, 6, 6);
            g2.setColor(COLOR_OUTLINE);
            g2.drawOval(cx + 6, cy - 26, 6, 6);
        }

        // Size label — bold, below shield
        String label = String.valueOf(army.getSize());
        g2.setFont(FONT_LABEL);
        FontMetrics fm = g2.getFontMetrics();
        int labelX = cx - fm.stringWidth(label) / 2;
        int labelY = cy + 22;
        g2.setColor(new Color(0, 0, 0, 200));
        g2.drawString(label, labelX + 1, labelY + 1);
        g2.setColor(COLOR_LABEL);
        g2.drawString(label, labelX, labelY);
    }

private Map<String, List<NobleArmy>> groupByZone() {
        Map<String, List<NobleArmy>> byZone = new LinkedHashMap<>();
        for (NobleArmy army : armyManager.getAllArmies()) {
            if (!army.isAlive()) continue;
            String zid = army.getZoneId();
            if (zid == null || zid.isEmpty()) continue;
            // Verify zone exists in manager
            if (zoneManager.getZone(zid) == null) continue;
            byZone.computeIfAbsent(zid, k -> new ArrayList<>()).add(army);
        }
        return byZone;
    }

private void drawGarrisons(Graphics2D g2) {
        for (Zone zone : zoneManager.getZones()) {
            main.nobles.NobleHouse owner = nobleHouseManager.getOwnerOfZone(zone.getId());
            if (owner == null) continue;
            int garrison = owner.getGarrisonFor(zone.getId());
            if (garrison <= 0) continue;
            boolean isCapital = zone.getId().equals(owner.getCapitalZoneId());
            // Garrison anchors to the RIGHT of the label
            int cx = zone.getLabelX() + 28;
            int cy = zone.getLabelY() + Y_OFFSET;
            drawGarrison(g2, owner, garrison, cx, cy, isCapital);
        }
    }

private void drawGarrison(Graphics2D g2, main.nobles.NobleHouse house,
                               int size, int cx, int cy, boolean isCapital) {
        Color primary = fade(NobleHouseColors.getPrimary(house.getId()), 160);
        Color outline = fade(NobleHouseColors.getSecondary(house.getId()), 180);

        // Smaller shield for garrison
        int[] sx = { cx - 5, cx + 5, cx + 5, cx,    cx - 5 };
        int[] sy = { cy - 7,  cy - 7,  cy - 2, cy + 3, cy - 2 };
        g2.setColor(primary);
        g2.fillPolygon(sx, sy, 5);
        g2.setColor(outline);
        g2.setStroke(new BasicStroke(1f));
        g2.drawPolygon(sx, sy, 5);

        // Crown marker for capital garrison
        if (isCapital) {
            g2.setColor(new Color(255, 210, 80, 200));
            g2.fillRect(cx - 4, cy - 10, 2, 3);
            g2.fillRect(cx - 1, cy - 11, 2, 4);
            g2.fillRect(cx + 2, cy - 10, 2, 3);
        }

        // Size label
        String label = String.valueOf(size);
        g2.setFont(FONT_LABEL);
        FontMetrics fm = g2.getFontMetrics();
        int labelX = cx - fm.stringWidth(label) / 2;
        int labelY = cy + 14;
        g2.setColor(new Color(10, 5, 0, 160));
        g2.drawString(label, labelX + 1, labelY + 1);
        g2.setColor(COLOR_LABEL);
        g2.drawString(label, labelX, labelY);
    }

    private static Color fade(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

}