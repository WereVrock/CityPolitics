package City.ui.map;

import City.main.barbarians.BarbArmy;
import City.main.barbarians.BarbArmyManager;
import City.main.map.Zone;
import City.main.map.ZoneManager;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Renders barbarian armies on the map canvas.
 * Warboss: large skull shield + next-zone arrow/target indicator.
 * Raiders/Ravagers: smaller variants with type colouring.
 */
public class BarbArmyRenderer {

    private static final Color COLOR_WARBOSS_BODY    = new Color(120,  20,  20);
    private static final Color COLOR_WARBOSS_OUTLINE = new Color(240, 100,  60);
    private static final Color COLOR_RAIDER_BODY     = new Color( 80,  30,  10);
    private static final Color COLOR_RAIDER_OUTLINE  = new Color(200, 130,  60);
    private static final Color COLOR_RAVAGER_BODY    = new Color( 60,  10,  60);
    private static final Color COLOR_RAVAGER_OUTLINE = new Color(180,  80, 200);
    private static final Color COLOR_LABEL           = new Color(255, 220, 180);
    private static final Color COLOR_LABEL_SHADOW    = new Color(10,   0,   0, 200);
    private static final Color COLOR_SELECTED_RING   = new Color(255, 200,  50);
    private static final Color COLOR_NEXT_ZONE_ARROW = new Color(255,  60,  60, 180);
    private static final Color COLOR_NEXT_ZONE_TARGET= new Color(255,  60,  60, 160);

    private static final int  SLOT_WIDTH  = 36;
    private static final Font FONT_LABEL  = new Font("Serif", Font.BOLD, 9);

    private final BarbArmyManager barbArmyManager;
    private final ZoneManager     zoneManager;

    private BarbArmy selectedArmy = null;

    public BarbArmyRenderer(BarbArmyManager barbArmyManager, ZoneManager zoneManager) {
        this.barbArmyManager = barbArmyManager;
        this.zoneManager     = zoneManager;
    }

    public void setSelectedArmy(BarbArmy army) { this.selectedArmy = army; }

    // ─── Render ──────────────────────────────────────────────────────────────

public void render(Graphics2D g2) {
        // Draw warboss next-zone indicator first (below armies)
        BarbArmy warboss = barbArmyManager.getWarboss();
        if (warboss != null && warboss.getNextZoneId() != null) {
            drawWarbossNextIndicator(g2, warboss);
        }

        // Group mobile armies by zone
        Map<String, List<BarbArmy>> byZone = new LinkedHashMap<>();
        for (BarbArmy army : barbArmyManager.getMobileArmies()) {
            byZone.computeIfAbsent(army.getZoneId(), k -> new ArrayList<>()).add(army);
        }

        for (Map.Entry<String, List<BarbArmy>> entry : byZone.entrySet()) {
            Zone zone = zoneManager.getZone(entry.getKey());
            if (zone == null) continue;
            List<BarbArmy> armies = entry.getValue();
            int count      = armies.size();
            int anchorX    = zone.getLabelX();
            int anchorY    = zone.getLabelY() - 44;
            int totalWidth = (count - 1) * SLOT_WIDTH;
            int startX     = anchorX - totalWidth / 2;
            for (int i = 0; i < count; i++) {
                drawBarbArmy(g2, armies.get(i), startX + i * SLOT_WIDTH, anchorY);
            }
        }

        // Draw garrison forces after mobile armies
        drawGarrisons(g2);
    }

private void drawGarrisons(Graphics2D g2) {
        Map<String, List<BarbArmy>> garrisonsByZone = new LinkedHashMap<>();
        for (BarbArmy army : barbArmyManager.getAllArmies()) {
            if (army.isGarrison() && army.isAlive()) {
                garrisonsByZone.computeIfAbsent(army.getZoneId(), k -> new ArrayList<>()).add(army);
            }
        }

        for (Map.Entry<String, List<BarbArmy>> entry : garrisonsByZone.entrySet()) {
            Zone zone = zoneManager.getZone(entry.getKey());
            if (zone == null) continue;
            List<BarbArmy> garrisonArmies = entry.getValue();
            int count = garrisonArmies.size();
            // place garrison shields to the right of the zone label, a bit lower than mobile armies
            int cx = zone.getLabelX() + 28; // similar to noble garrison placement
            int cy = zone.getLabelY() + 14;  // offset Y
            int spacing = 16;
            int startX = cx - ((count - 1) * spacing) / 2;
            for (int i = 0; i < count; i++) {
                drawBarbGarrison(g2, garrisonArmies.get(i), startX + i * spacing, cy);
            }
        }
    }

    private void drawBarbGarrison(Graphics2D g2, BarbArmy garrison, int cx, int cy) {
        // small skull shield
        Color body    = new Color(100, 20, 20);
        Color outline = new Color(200, 60, 40);
        int scale = 5;

        int[] sx = { cx - scale, cx + scale, cx + scale, cx,         cx - scale };
        int[] sy = { cy - scale, cy - scale, cy - scale/3, cy + scale, cy - scale/3 };
        g2.setColor(body);
        g2.fillPolygon(sx, sy, 5);
        g2.setColor(outline);
        g2.setStroke(new BasicStroke(1f));
        g2.drawPolygon(sx, sy, 5);

        // tiny skull
        drawSkull(g2, cx, cy - scale/2, 2, outline);

        // size label
        String lbl = String.valueOf(garrison.getSize());
        g2.setFont(FONT_LABEL);
        FontMetrics fm = g2.getFontMetrics();
        int lx = cx - fm.stringWidth(lbl) / 2;
        int ly = cy + scale + 10;
        g2.setColor(new Color(10, 0, 0, 200));
        g2.drawString(lbl, lx + 1, ly + 1);
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawString(lbl, lx, ly);
    }

// ─── Hit test ────────────────────────────────────────────────────────────

    public BarbArmy hitTest(Point world) {
        Map<String, List<BarbArmy>> byZone = new LinkedHashMap<>();
        for (BarbArmy army : barbArmyManager.getMobileArmies()) {
            byZone.computeIfAbsent(army.getZoneId(), k -> new ArrayList<>()).add(army);
        }
        for (Map.Entry<String, List<BarbArmy>> entry : byZone.entrySet()) {
            Zone zone = zoneManager.getZone(entry.getKey());
            if (zone == null) continue;
            List<BarbArmy> armies = entry.getValue();
            int count      = armies.size();
            int anchorX    = zone.getLabelX();
            int anchorY    = zone.getLabelY() - 44;
            int totalWidth = (count - 1) * SLOT_WIDTH;
            int startX     = anchorX - totalWidth / 2;
            for (int i = 0; i < count; i++) {
                int cx = startX + i * SLOT_WIDTH;
                int dx = world.x - cx;
                int dy = world.y - anchorY;
                if (dx * dx + dy * dy <= 13 * 13) return armies.get(i);
            }
        }
        return null;
    }

    // ─── Individual army drawing ──────────────────────────────────────────────

    private void drawBarbArmy(Graphics2D g2, BarbArmy army, int cx, int cy) {
        boolean selected = army == selectedArmy;
        if (selected) {
            g2.setColor(COLOR_SELECTED_RING);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawOval(cx - 14, cy - 17, 28, 30);
            g2.setStroke(new BasicStroke(1f));
        }

        Color body    = bodyColor(army);
        Color outline = outlineColor(army);
        int   scale   = army.isWarboss() ? 11 : 8;

        // Shield shape
        int[] sx = { cx - scale, cx + scale, cx + scale, cx,         cx - scale };
        int[] sy = { cy - scale, cy - scale, cy - scale/3, cy + scale, cy - scale/3 };
        g2.setColor(body);
        g2.fillPolygon(sx, sy, 5);
        g2.setColor(outline);
        g2.setStroke(new BasicStroke(army.isWarboss() ? 2f : 1.2f));
        g2.drawPolygon(sx, sy, 5);
        g2.setStroke(new BasicStroke(1f));

        // Skull detail on shield
        drawSkull(g2, cx, cy - scale / 2, army.isWarboss() ? 5 : 3, outline);

        // Warboss gets a banner
        if (army.isWarboss()) {
            g2.setColor(outline);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(cx, cy - scale, cx, cy - scale - 14);
            int[] bx = { cx, cx + 10, cx };
            int[] by = { cy - scale - 14, cy - scale - 10, cy - scale - 6 };
            g2.setColor(COLOR_WARBOSS_BODY);
            g2.fillPolygon(bx, by, 3);
            g2.setColor(outline);
            g2.drawPolygon(bx, by, 3);
            g2.setStroke(new BasicStroke(1f));
        }

        // Size label with tribe name
        String lbl = army.getDisplayName().length() > 12
                ? army.getSize() + ""
                : army.getDisplayName().split(" ")[0] + " " + army.getSize();
        g2.setFont(FONT_LABEL);
        FontMetrics fm = g2.getFontMetrics();
        int lx = cx - fm.stringWidth(lbl) / 2;
        int ly = cy + scale + 12;
        g2.setColor(COLOR_LABEL_SHADOW);
        g2.drawString(lbl, lx + 1, ly + 1);
        g2.setColor(COLOR_LABEL);
        g2.drawString(lbl, lx, ly);
    }

    private void drawSkull(Graphics2D g2, int cx, int cy, int r, Color color) {
        g2.setColor(color);
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        // Eye sockets
        g2.setColor(new Color(0, 0, 0, 180));
        int er = Math.max(1, r / 3);
        g2.fillOval(cx - r / 2 - er, cy - er, er * 2, er * 2);
        g2.fillOval(cx + r / 2 - er, cy - er, er * 2, er * 2);
    }

    // ─── Warboss next-zone indicator ─────────────────────────────────────────

    private void drawWarbossNextIndicator(Graphics2D g2, BarbArmy warboss) {
        Zone current = zoneManager.getZone(warboss.getZoneId());
        Zone next    = zoneManager.getZone(warboss.getNextZoneId());
        if (current == null || next == null) return;

        int x1 = current.getLabelX();
        int y1 = current.getLabelY() - 44;
        int x2 = next.getLabelX();
        int y2 = next.getLabelY() - 44;

        // Try to draw arrow; if zones are far enough apart draw line + arrowhead
        double dx    = x2 - x1;
        double dy    = y2 - y1;
        double dist  = Math.sqrt(dx * dx + dy * dy);

        if (dist < 20) {
            // Too close — just draw a red ring on the target zone
            drawTargetRing(g2, next);
            return;
        }

        // Draw dashed line
        g2.setColor(COLOR_NEXT_ZONE_ARROW);
        float[] dash = {6f, 4f};
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 1f, dash, 0f));
        g2.drawLine(x1, y1, x2, y2);
        g2.setStroke(new BasicStroke(1f));

        // Arrowhead at target
        double ux  = dx / dist;
        double uy  = dy / dist;
        int    tip = 12;
        int    ax  = x2 - (int)(ux * 6);
        int    ay  = y2 - (int)(uy * 6);
        int[]  arrowX = {
            ax,
            ax - (int)(ux * tip + uy * 6),
            ax - (int)(ux * tip - uy * 6)
        };
        int[] arrowY = {
            ay,
            ay - (int)(uy * tip - ux * 6),
            ay - (int)(uy * tip + ux * 6)
        };
        g2.setColor(COLOR_NEXT_ZONE_ARROW);
        g2.fillPolygon(arrowX, arrowY, 3);

        // Pulsing ring on target zone
        drawTargetRing(g2, next);
    }

    private void drawTargetRing(Graphics2D g2, Zone zone) {
        int cx = zone.getLabelX();
        int cy = zone.getLabelY();
        g2.setColor(COLOR_NEXT_ZONE_TARGET);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawOval(cx - 22, cy - 16, 44, 32);
        g2.setStroke(new BasicStroke(1f));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Color bodyColor(BarbArmy army) {
        return switch (army.getType()) {
            case WARBOSS -> COLOR_WARBOSS_BODY;
            case RAIDER  -> COLOR_RAIDER_BODY;
            case RAVAGER -> COLOR_RAVAGER_BODY;
        };
    }

    private Color outlineColor(BarbArmy army) {
        return switch (army.getType()) {
            case WARBOSS -> COLOR_WARBOSS_OUTLINE;
            case RAIDER  -> COLOR_RAIDER_OUTLINE;
            case RAVAGER -> COLOR_RAVAGER_OUTLINE;
        };
    }
}