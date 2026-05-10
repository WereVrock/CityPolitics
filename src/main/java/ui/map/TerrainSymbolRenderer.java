// TerrainSymbolRenderer.java
package ui.map;

import main.map.Zone;
import main.map.ZoneDecoration;
import main.map.ZoneDecorationRegistry;

import java.awt.*;

/**
 * Draws small terrain symbols inside zone polygons.
 * All symbols are drawn with Graphics2D shapes — no unicode, scales with zoom.
 */
public class TerrainSymbolRenderer {

    private final ZoneDecorationRegistry registry;

    public TerrainSymbolRenderer(ZoneDecorationRegistry registry) {
        this.registry = registry;
    }

public void render(Graphics2D g2, Zone zone) {
    ZoneDecoration dec = registry.get(zone.getId());
    if (dec.getSymbol() == ZoneDecoration.TerrainSymbol.NONE) return;

    // Draw icon to the left of the zone name, vertically centred on it
    int cx = zone.getLabelX() - 22;
    int cy = zone.getLabelY() + 18; // matches ICON_LABEL_OFFSET in MapRenderer

    switch (dec.getSymbol()) {
        case FOREST   -> drawForest(g2, cx, cy);
        case VOLCANO  -> drawVolcano(g2, cx, cy);
        case ICE      -> drawIce(g2, cx, cy);
        case FARMLAND -> drawFarmland(g2, cx, cy);
        case MARSH    -> drawMarsh(g2, cx, cy);
        case COASTAL  -> drawWaves(g2, cx, cy);
        case MOUNTAIN -> drawMountainSymbol(g2, cx, cy);
        case NONE     -> {}
    }
}

// ─── Symbols ──────────────────────────────────────────────────────────────

    private void drawForest(Graphics2D g2, int cx, int cy) {
        g2.setColor(new Color(40, 120, 40, 180));
        for (int i = -1; i <= 1; i++) {
            int tx = cx + i * 14;
            int[] px = {tx - 7, tx + 7, tx};
            int[] py = {cy,     cy,     cy - 14};
            g2.fillPolygon(px, py, 3);
            g2.setColor(new Color(30, 90, 30, 180));
            g2.drawPolygon(px, py, 3);
            g2.setColor(new Color(40, 120, 40, 180));
            // second tier
            int[] px2 = {tx - 5, tx + 5, tx};
            int[] py2 = {cy - 8, cy - 8, cy - 20};
            g2.fillPolygon(px2, py2, 3);
        }
        // trunks
        g2.setColor(new Color(90, 55, 20, 180));
        for (int i = -1; i <= 1; i++) {
            g2.fillRect(cx + i * 14 - 2, cy, 4, 5);
        }
    }

    private void drawVolcano(Graphics2D g2, int cx, int cy) {
        // Volcano body
        int[] vx = {cx - 18, cx + 18, cx + 8, cx - 8};
        int[] vy = {cy + 4,  cy + 4,  cy - 18, cy - 18};
        g2.setColor(new Color(100, 60, 40, 200));
        g2.fillPolygon(vx, vy, 4);
        g2.setColor(new Color(60, 30, 20, 200));
        g2.drawPolygon(vx, vy, 4);
        // Lava glow at top
        g2.setColor(new Color(220, 80, 20, 180));
        g2.fillOval(cx - 8, cy - 22, 16, 10);
        // Lava drips
        g2.setColor(new Color(240, 120, 20, 160));
        g2.fillOval(cx - 4, cy - 16, 5, 8);
        g2.fillOval(cx + 2, cy - 14, 4, 7);
        // Smoke
        g2.setColor(new Color(180, 180, 180, 80));
        g2.fillOval(cx - 6, cy - 34, 10, 10);
        g2.fillOval(cx,     cy - 38, 8,  8);
    }

    private void drawIce(Graphics2D g2, int cx, int cy) {
        g2.setColor(new Color(180, 220, 255, 180));
        // Three ice spikes
        for (int i = -1; i <= 1; i++) {
            int tx = cx + i * 12;
            int h  = 14 - Math.abs(i) * 3;
            int[] px = {tx - 5, tx + 5, tx};
            int[] py = {cy,     cy,     cy - h};
            g2.fillPolygon(px, py, 3);
            g2.setColor(new Color(140, 190, 240, 180));
            g2.drawPolygon(px, py, 3);
            g2.setColor(new Color(180, 220, 255, 180));
        }
    }

    private void drawFarmland(Graphics2D g2, int cx, int cy) {
        g2.setColor(new Color(180, 160, 60, 160));
        // Grid of crop rows
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                int rx = cx - 18 + col * 10;
                int ry = cy - 10 + row * 8;
                g2.fillRoundRect(rx, ry, 7, 4, 2, 2);
            }
        }
        g2.setColor(new Color(140, 120, 40, 160));
        for (int row = 0; row < 3; row++) {
            g2.drawLine(cx - 18, cy - 8 + row * 8, cx + 20, cy - 8 + row * 8);
        }
    }

    private void drawMarsh(Graphics2D g2, int cx, int cy) {
        // Water lines
        g2.setColor(new Color(60, 120, 160, 150));
        for (int i = 0; i < 3; i++) {
            int wy = cy - 4 + i * 6;
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawArc(cx - 16 + i * 4, wy, 12, 5, 0, 180);
        }
        g2.setStroke(new BasicStroke(1f));
        // Reeds
        g2.setColor(new Color(60, 140, 60, 180));
        for (int i = -1; i <= 1; i++) {
            int rx = cx + i * 10;
            g2.drawLine(rx, cy + 8, rx, cy - 8);
            g2.fillOval(rx - 2, cy - 12, 5, 8);
        }
    }

    private void drawWaves(Graphics2D g2, int cx, int cy) {
        g2.setColor(new Color(60, 130, 200, 160));
        g2.setStroke(new BasicStroke(2f));
        for (int i = 0; i < 3; i++) {
            int wy = cy - 6 + i * 7;
            g2.drawArc(cx - 18, wy, 14, 6, 0, 180);
            g2.drawArc(cx - 4,  wy, 14, 6, 180, 180);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawMountainSymbol(Graphics2D g2, int cx, int cy) {
        // Two overlapping mountain peaks
        g2.setColor(new Color(130, 120, 110, 180));
        int[] px1 = {cx - 16, cx + 2,  cx - 7};
        int[] py1 = {cy + 4,  cy + 4,  cy - 14};
        g2.fillPolygon(px1, py1, 3);
        g2.setColor(new Color(150, 140, 130, 180));
        int[] px2 = {cx - 4,  cx + 18, cx + 7};
        int[] py2 = {cy + 4,  cy + 4,  cy - 18};
        g2.fillPolygon(px2, py2, 3);
        // Snow caps
        g2.setColor(new Color(240, 240, 255, 200));
        int[] sp1 = {cx - 10, cx - 4, cx - 7};
        int[] sy1 = {cy - 6,  cy - 6, cy - 14};
        g2.fillPolygon(sp1, sy1, 3);
        int[] sp2 = {cx + 3,  cx + 11, cx + 7};
        int[] sy2 = {cy - 8,  cy - 8,  cy - 18};
        g2.fillPolygon(sp2, sy2, 3);
        // Outlines
        g2.setColor(new Color(80, 70, 60, 180));
        g2.setStroke(new BasicStroke(0.8f));
        g2.drawPolygon(px1, py1, 3);
        g2.drawPolygon(px2, py2, 3);
        g2.setStroke(new BasicStroke(1f));
    }
}