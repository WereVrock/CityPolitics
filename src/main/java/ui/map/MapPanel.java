// MapPanel.java
package ui.map;

import main.map.Zone;
import main.map.ZoneManager;
import main.map.ZoneState;
import main.parameters.GameParameters;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.function.Consumer;

/**
 * Custom JPanel that renders the zone map.
 * Handles zoom, pan (drag), and zone selection via click.
 */
public class MapPanel extends JPanel {

    // ─── Visual constants ─────────────────────────────────────────────────────
    private static final Color COLOR_CAPITAL        = new Color(120, 80,  30);
    private static final Color COLOR_TOWN           = new Color(55,  85,  60);
    private static final Color COLOR_VILLAGE        = new Color(50,  70,  50);
    private static final Color COLOR_SELECTED       = new Color(200, 170, 60);
    private static final Color COLOR_BORDER         = new Color(20,  15,  10);
    private static final Color COLOR_BORDER_SEL     = new Color(230, 200, 80);
    private static final Color COLOR_HOVER          = new Color(255, 255, 255, 40);
    private static final Color COLOR_LABEL          = new Color(240, 230, 200);
    private static final Color COLOR_LABEL_SHADOW   = new Color(0, 0, 0, 160);
    private static final Color COLOR_DAMAGE_OVERLAY = new Color(180, 30, 30, 100);
    private static final Color COLOR_GOLD_TEXT      = new Color(210, 170, 80);
    private static final Color COLOR_FOOD_TEXT      = new Color(120, 200, 100);
    private static final Color COLOR_BG             = new Color(30, 24, 18);
    private static final Color COLOR_GRID           = new Color(50, 40, 30, 60);

    private static final Font FONT_ZONE_NAME  = new Font("Serif", Font.BOLD,  13);
    private static final Font FONT_ZONE_STATS = new Font("Monospaced", Font.PLAIN, 10);
    private static final Font FONT_SETTLEMENT = new Font("Serif", Font.PLAIN, 11);

    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.5f;

    // ─── State ────────────────────────────────────────────────────────────────
    private final ZoneManager            zoneManager;
    private final Consumer<Zone>         onZoneSelected;

    private Zone    selectedZone = null;
    private Zone    hoveredZone  = null;

    private float   zoom         = 1.0f;
    private int     panX         = 0;
    private int     panY         = 0;
    private Point   dragStart    = null;
    private int     panXAtDrag   = 0;
    private int     panYAtDrag   = 0;

    public MapPanel(ZoneManager zoneManager, Consumer<Zone> onZoneSelected) {
        this.zoneManager    = zoneManager;
        this.onZoneSelected = onZoneSelected;

        setBackground(COLOR_BG);
        setPreferredSize(new Dimension(800, 520));
        setupMouseHandlers();
    }

    // ─── Selection ────────────────────────────────────────────────────────────

    public void clearSelection() {
        selectedZone = null;
        repaint();
    }

    public Zone getSelectedZone() { return selectedZone; }

    // ─── Mouse ────────────────────────────────────────────────────────────────

    private void setupMouseHandlers() {
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                dragStart    = e.getPoint();
                panXAtDrag   = panX;
                panYAtDrag   = panY;
            }
            @Override public void mouseReleased(MouseEvent e) {
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
            @Override public void mouseDragged(MouseEvent e) {
                if (dragStart != null) {
                    panX = panXAtDrag + (e.getX() - dragStart.x);
                    panY = panYAtDrag + (e.getY() - dragStart.y);
                    repaint();
                }
            }
            @Override public void mouseMoved(MouseEvent e) {
                Zone hit = zoneAtScreenPoint(e.getPoint());
                if (hit != hoveredZone) {
                    hoveredZone = hit;
                    repaint();
                }
            }
        });

        addMouseWheelListener(e -> {
            float oldZoom = zoom;
            zoom -= (float) e.getPreciseWheelRotation() * 0.1f;
            zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
            // Zoom towards cursor
            double mx = e.getX();
            double my = e.getY();
            panX = (int) (mx - (mx - panX) * (zoom / oldZoom));
            panY = (int) (my - (my - panY) * (zoom / oldZoom));
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
        // Convert from panel-local coords to world coords accounting for pan and zoom
        Point world = screenToWorld(screenPt);
        for (Zone zone : zoneManager.getZones()) {
            Polygon poly = buildPolygon(zone);
            if (poly.contains(world)) return zone;
        }
        return null;
    }

    private Point screenToWorld(Point panelLocal) {
        // panelLocal is already relative to this panel's origin (MouseEvent gives panel-local coords)
        int wx = Math.round((panelLocal.x - panX) / zoom);
        int wy = Math.round((panelLocal.y - panY) / zoom);
        return new Point(wx, wy);
    }

    private Polygon buildPolygon(Zone zone) {
        return new Polygon(zone.getPolyX(), zone.getPolyY(), zone.getPolyX().length);
    }

    // ─── Painting ─────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,     RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Apply pan + zoom transform
        AffineTransform transform = new AffineTransform();
        transform.translate(panX, panY);
        transform.scale(zoom, zoom);
        g2.setTransform(transform);

        drawBackground(g2);

        for (Zone zone : zoneManager.getZones()) {
            drawZone(g2, zone);
        }

        g2.dispose();
    }

    private void drawBackground(Graphics2D g2) {
        // Subtle grid for parchment feel
        g2.setColor(COLOR_GRID);
        g2.setStroke(new BasicStroke(0.5f));
        for (int x = 0; x < 850; x += 50) {
            g2.drawLine(x, 0, x, 600);
        }
        for (int y = 0; y < 600; y += 50) {
            g2.drawLine(0, y, 850, y);
        }
    }

    private void drawZone(Graphics2D g2, Zone zone) {
        Polygon poly      = buildPolygon(zone);
        ZoneState state   = zoneManager.getState(zone.getId());
        boolean selected  = zone == selectedZone;
        boolean hovered   = zone == hoveredZone;

        // ── Fill ──────────────────────────────────────────────────────────────
        Color base = baseColor(zone);
        if (selected) base = base.brighter();
        g2.setColor(base);
        g2.fillPolygon(poly);

        // ── Hover overlay ─────────────────────────────────────────────────────
        if (hovered && !selected) {
            g2.setColor(COLOR_HOVER);
            g2.fillPolygon(poly);
        }

        // ── Damage overlay ────────────────────────────────────────────────────
        if (state.getDamage() > 0) {
            int alpha = (int) (state.getDamage() / 100.0 * 140);
            g2.setColor(new Color(180, 30, 30, Math.min(alpha, 140)));
            g2.fillPolygon(poly);
        }

        // ── Border ────────────────────────────────────────────────────────────
        g2.setColor(selected ? COLOR_BORDER_SEL : COLOR_BORDER);
        g2.setStroke(new BasicStroke(selected ? 2.5f : 1.5f));
        g2.drawPolygon(poly);

        // ── Labels ────────────────────────────────────────────────────────────
        drawZoneLabels(g2, zone);
    }

    private void drawZoneLabels(Graphics2D g2, Zone zone) {
        int lx = zone.getLabelX();
        int ly = zone.getLabelY();

        // Shadow then name
        g2.setFont(FONT_ZONE_NAME);
        g2.setColor(COLOR_LABEL_SHADOW);
        g2.drawString(zone.getDisplayName(), lx - 1 - centerOffset(g2, zone.getDisplayName()), ly + 1);
        g2.setColor(COLOR_LABEL);
        g2.drawString(zone.getDisplayName(), lx - centerOffset(g2, zone.getDisplayName()), ly);

        // Settlement type
        String settlement = settlementLabel(zone);
        g2.setFont(FONT_SETTLEMENT);
        g2.setColor(COLOR_LABEL_SHADOW);
        g2.drawString(settlement, lx - 1 - centerOffset(g2, settlement), ly + 14);
        g2.setColor(new Color(200, 190, 160));
        g2.drawString(settlement, lx - centerOffset(g2, settlement), ly + 14);

        // Gold / food
        g2.setFont(FONT_ZONE_STATS);
        String goldStr = "G:" + zone.getGoldProduction();
        String foodStr = "F:" + zone.getFoodProduction();
        int statsY = ly + 27;
        int totalW = g2.getFontMetrics().stringWidth(goldStr + "  " + foodStr);
        int startX = lx - totalW / 2;
        g2.setColor(COLOR_GOLD_TEXT);
        g2.drawString(goldStr, startX, statsY);
        g2.setColor(COLOR_FOOD_TEXT);
        g2.drawString(foodStr, startX + g2.getFontMetrics().stringWidth(goldStr + "  "), statsY);
    }

    private int centerOffset(Graphics2D g2, String text) {
        return g2.getFontMetrics().stringWidth(text) / 2;
    }

    private Color baseColor(Zone zone) {
        return switch (zone.getSettlement()) {
            case CAPITAL -> COLOR_CAPITAL;
            case TOWN    -> COLOR_TOWN;
            case VILLAGE -> COLOR_VILLAGE;
        };
    }

    private String settlementLabel(Zone zone) {
        return switch (zone.getSettlement()) {
            case CAPITAL -> "[Capital]";
            case TOWN    -> "[Town]";
            case VILLAGE -> "[Village]";
        };
    }
}