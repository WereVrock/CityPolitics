// SeaRenderer.java
package City.ui.map;

import City.main.map.WorldGeography;

import java.awt.*;

/**
 * Draws sea regions as filled polygons on the map canvas.
 * Reads from WorldGeography — purely visual for now.
 */
public class SeaRenderer {

    private static final Color COLOR_SEA_FILL   = new Color(60,  110, 180, 200);
    private static final Color COLOR_SEA_EDGE   = new Color(80,  140, 210, 255);
    private static final Color COLOR_SEA_FOAM   = new Color(180, 210, 240, 100);

    private final WorldGeography geography;

    public SeaRenderer(WorldGeography geography) {
        this.geography = geography;
    }

    public void render(Graphics2D g2) {
        for (WorldGeography.SeaRegion sea : geography.getSeaRegions()) {
            drawSea(g2, sea);
        }
    }

private void drawSea(Graphics2D g2, WorldGeography.SeaRegion sea) {
    Polygon poly = new Polygon(sea.getPolyX(), sea.getPolyY(), sea.getPolyX().length);

    g2.setColor(COLOR_SEA_FILL);
    g2.fillPolygon(poly);

    // Foam highlight along inner edge
    g2.setColor(COLOR_SEA_FOAM);
    g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.drawPolygon(poly);

    g2.setColor(COLOR_SEA_EDGE);
    g2.setStroke(new BasicStroke(1.5f));
    g2.drawPolygon(poly);
    g2.setStroke(new BasicStroke(1f));

    // Centroid label
    int[] px = sea.getPolyX(), py = sea.getPolyY();
    int n = px.length;
    int cx = 0, cy = 0;
    for (int i = 0; i < n; i++) { cx += px[i]; cy += py[i]; }
    cx /= n; cy /= n;

    Font seaFont = new Font("Serif", Font.ITALIC, 11);
    g2.setFont(seaFont);
    FontMetrics fm = g2.getFontMetrics();
    int tw = fm.stringWidth(sea.getName());

    g2.setColor(new Color(10, 20, 50, 160));
    g2.drawString(sea.getName(), cx - tw / 2 + 1, cy + 1);
    g2.setColor(new Color(200, 225, 255, 210));
    g2.drawString(sea.getName(), cx - tw / 2, cy);
}

}