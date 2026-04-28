package ui.map;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class MapCamera {

    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.5f;

    private float zoom = 1.0f;
    private int panX = 0;
    private int panY = 0;

    public void applyTransform(Graphics2D g2) {
        AffineTransform tx = new AffineTransform();
        tx.translate(panX, panY);
        tx.scale(zoom, zoom);
        g2.setTransform(tx);
    }

    public Point screenToWorld(Point p) {
        int wx = Math.round((p.x - panX) / zoom);
        int wy = Math.round((p.y - panY) / zoom);
        return new Point(wx, wy);
    }

    public void zoomAt(double mx, double my, float wheelDelta) {
        float oldZoom = zoom;
        zoom -= wheelDelta * 0.1f;
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));

        panX = (int) (mx - (mx - panX) * (zoom / oldZoom));
        panY = (int) (my - (my - panY) * (zoom / oldZoom));
    }

    public void setPan(int x, int y) {
        this.panX = x;
        this.panY = y;
    }

    public int getPanX() { return panX; }
    public int getPanY() { return panY; }
}