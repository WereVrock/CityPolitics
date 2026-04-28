package ui.map;

import main.map.Zone;
import main.map.ZoneManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

/**
 * Custom JPanel that renders the zone map.
 * Handles zoom, pan (drag), and zone selection via click.
 */
public class MapPanel extends JPanel {

    private final ZoneManager    zoneManager;
    private final Consumer<Zone> onZoneSelected;

    private final MapCamera   camera;
    private final MapRenderer renderer;

    private Zone  selectedZone = null;
    private Zone  hoveredZone  = null;

    private Point dragStart  = null;
    private int   panXAtDrag = 0;
    private int   panYAtDrag = 0;

    public MapPanel(ZoneManager zoneManager, Consumer<Zone> onZoneSelected) {
        this.zoneManager    = zoneManager;
        this.onZoneSelected = onZoneSelected;

        this.camera   = new MapCamera();
        this.renderer = new MapRenderer(zoneManager);

        setBackground(MapRenderer.COLOR_BG);
        setPreferredSize(new Dimension(800, 520));
        setupMouseHandlers();
    }

    // ─── Selection ────────────────────────────────────────────────────────────

    public void clearSelection() {
        selectedZone = null;
        repaint();
    }

    public Zone getSelectedZone() {
        return selectedZone;
    }

    // ─── Mouse ────────────────────────────────────────────────────────────────

    private void setupMouseHandlers() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStart  = e.getPoint();
                panXAtDrag = camera.getPanX();
                panYAtDrag = camera.getPanY();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Point rel = e.getPoint();
                if (dragStart != null) {
                    int dx = rel.x - dragStart.x;
                    int dy = rel.y - dragStart.y;
                    if (Math.abs(dx) < 4 && Math.abs(dy) < 4) {
                        handleClick(rel);
                    }
                }
                dragStart = null;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart != null) {
                    camera.setPan(
                        panXAtDrag + (e.getX() - dragStart.x),
                        panYAtDrag + (e.getY() - dragStart.y)
                    );
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Zone hit = zoneAtScreenPoint(e.getPoint());
                if (hit != hoveredZone) {
                    hoveredZone = hit;
                    repaint();
                }
            }
        });

        addMouseWheelListener(e -> {
            camera.zoomAt(e.getX(), e.getY(), (float) e.getPreciseWheelRotation());
            repaint();
        });
    }

    private void handleClick(Point screenPt) {
        Zone hit = zoneAtScreenPoint(screenPt);
        selectedZone = hit;
        repaint();
        onZoneSelected.accept(hit);
    }

    // ─── Hit testing ──────────────────────────────────────────────────────────

    private Zone zoneAtScreenPoint(Point screenPt) {
        Point world = camera.screenToWorld(screenPt);
        return renderer.hitTest(world);
    }

    // ─── Painting ─────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        camera.applyTransform(g2);
        renderer.render(g2, selectedZone, hoveredZone);

        g2.dispose();
    }
}