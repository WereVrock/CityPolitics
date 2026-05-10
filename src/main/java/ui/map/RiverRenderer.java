// RiverRenderer.java
package ui.map;

import main.map.WorldGeography;

import java.awt.*;

/**
 * Draws rivers as smooth curves on the map canvas.
 * Reads from WorldGeography — purely visual for now.
 */
public class RiverRenderer {

    private static final Color COLOR_RIVER      = new Color(80,  140, 200, 180);
    private static final Color COLOR_RIVER_EDGE = new Color(60,  110, 170, 120);
    private static final float RIVER_WIDTH      = 3.5f;

    private final WorldGeography geography;

    public RiverRenderer(WorldGeography geography) {
        this.geography = geography;
    }

    public void render(Graphics2D g2) {
        for (WorldGeography.River river : geography.getRivers()) {
            drawRiver(g2, river);
        }
    }

    private void drawRiver(Graphics2D g2, WorldGeography.River river) {
        int[][] pts = river.getWaypoints();
        if (pts.length < 2) return;

        // Draw slightly wider darker edge first, then bright river on top
        g2.setColor(COLOR_RIVER_EDGE);
        g2.setStroke(new BasicStroke(RIVER_WIDTH + 2f,
            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawPolyline(g2, pts);

        g2.setColor(COLOR_RIVER);
        g2.setStroke(new BasicStroke(RIVER_WIDTH,
            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawPolyline(g2, pts);

        g2.setStroke(new BasicStroke(1f));
    }

    private void drawPolyline(Graphics2D g2, int[][] pts) {
        for (int i = 0; i < pts.length - 1; i++) {
            g2.drawLine(pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1]);
        }
    }
}