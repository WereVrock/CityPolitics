// MountainEdgeRenderer.java
package City.ui.map;

import City.main.map.Zone;
import City.main.map.ZoneDecoration;
import City.main.map.ZoneDecorationRegistry;

import java.awt.*;
import java.util.List;

/**
 * Draws jagged mountain peaks along specified zone polygon edges.
 * Mountain edges are defined as pairs of vertex indices in ZoneDecoration.
 */
public class MountainEdgeRenderer {

    private static final Color COLOR_MOUNTAIN_DARK  = new Color(80,  70,  60,  220);
    private static final Color COLOR_MOUNTAIN_MID   = new Color(140, 130, 118, 220);
    private static final Color COLOR_MOUNTAIN_SNOW  = new Color(230, 235, 245, 200);
    private static final int   PEAK_SPACING         = 12; // pixels between peaks along edge

    private final ZoneDecorationRegistry registry;

    public MountainEdgeRenderer(ZoneDecorationRegistry registry) {
        this.registry = registry;
    }

    public void render(Graphics2D g2, Zone zone) {
        ZoneDecoration dec = registry.get(zone.getId());
        if (!dec.hasMountainEdges()) return;

        int[] polyX = zone.getPolyX();
        int[] polyY = zone.getPolyY();
        List<int[]> edges = dec.getMountainEdges();

        for (int[] edge : edges) {
            int idxA = edge[0];
            int idxB = edge[1];
            if (idxA >= polyX.length || idxB >= polyX.length) continue;

            int ax = polyX[idxA], ay = polyY[idxA];
            int bx = polyX[idxB], by = polyY[idxB];
            drawMountainEdge(g2, ax, ay, bx, by);
        }
    }

private void drawMountainEdge(Graphics2D g2, int ax, int ay, int bx, int by) {
    double dx     = bx - ax;
    double dy     = by - ay;
    double length = Math.sqrt(dx * dx + dy * dy);
    if (length < 1) return;

    double ux = dx / length;
    double uy = dy / length;

    // Perpendicular — pick whichever direction has negative Y (upward on screen)
    double nx = -uy;
    double ny =  ux;
    if (ny > 0) { nx = -nx; ny = -ny; }

    int peakCount = Math.max(2, (int) (length / PEAK_SPACING));

    for (int i = 0; i < peakCount; i++) {
        double t  = (i + 0.5) / peakCount;
        double cx = ax + t * dx;
        double cy = ay + t * dy;

        double peakH = 14 + (i % 3) * 4;
        double baseW = 10;

        int[] mountX = {
            (int)(cx - ux * baseW / 2),
            (int)(cx + ux * baseW / 2),
            (int)(cx + nx * peakH)
        };
        int[] mountY = {
            (int)(cy - uy * baseW / 2),
            (int)(cy + uy * baseW / 2),
            (int)(cy + ny * peakH)
        };

        g2.setColor(COLOR_MOUNTAIN_MID);
        g2.fillPolygon(mountX, mountY, 3);

        int[] snowX = {
            (int)(cx - ux * baseW / 6 + nx * (peakH * 0.55)),
            (int)(cx + ux * baseW / 6 + nx * (peakH * 0.55)),
            (int)(cx + nx * peakH)
        };
        int[] snowY = {
            (int)(cy - uy * baseW / 6 + ny * (peakH * 0.55)),
            (int)(cy + uy * baseW / 6 + ny * (peakH * 0.55)),
            (int)(cy + ny * peakH)
        };

        g2.setColor(COLOR_MOUNTAIN_SNOW);
        g2.fillPolygon(snowX, snowY, 3);

        g2.setColor(COLOR_MOUNTAIN_DARK);
        g2.setStroke(new BasicStroke(0.8f));
        g2.drawPolygon(mountX, mountY, 3);
        g2.setStroke(new BasicStroke(1f));
    }
}

}