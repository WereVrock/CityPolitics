package tools.mapeditor;

import main.map.WorldGeography;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class MapEditorCanvas extends JPanel {

    private static final int    VERTEX_RADIUS     = 6;
    private static final int    LABEL_RADIUS      = 5;
    private static final int    SNAP_RADIUS       = 8;
    private static final int    WORLD_SNAP_RADIUS = 10;
    private static final Color  COLOR_SEA         = new Color(80, 130, 200, 140);
    private static final Color  COLOR_SEA_BORDER  = new Color(60, 110, 180, 200);
    private static final Color  COLOR_NORMAL      = new Color(148, 118, 76, 180);
    private static final Color  COLOR_DESOLATE    = new Color(50, 48, 52, 200);
    private static final Color  COLOR_SELECTED    = new Color(220, 200, 100, 200);
    private static final Color  COLOR_CHANGED     = new Color(100, 180, 100, 60);
    private static final Color  COLOR_VERTEX      = new Color(255, 220, 80);
    private static final Color  COLOR_VERTEX_DEL  = new Color(220, 60, 60);
    private static final Color  COLOR_LABEL_DOT   = new Color(80, 200, 255);
    private static final Color  COLOR_BORDER      = new Color(30, 20, 8);
    private static final Color  COLOR_BORDER_SEL  = new Color(240, 210, 80);
    private static final Font   FONT_LABEL        = new Font("Serif", Font.BOLD, 12);
    private static final Font   FONT_LABEL_SMALL  = new Font("Serif", Font.ITALIC, 10);

    private final EditorState    state;
    private final WorldGeography geography;
    private final OutputPanel    outputPanel;

    // Camera
    private float panX = 0, panY = 0, zoom = 1.0f;
    private Point dragStart;
    private float panXAtDrag, panYAtDrag;

    // Interaction state
    private int   dragVertexIndex = -1;
    private boolean draggingLabel = false;
    private Point lastDragWorld;

    public MapEditorCanvas(EditorState state, WorldGeography geography,
                           OutputPanel outputPanel) {
        this.state       = state;
        this.geography   = geography;
        this.outputPanel = outputPanel;

        setBackground(new Color(188, 158, 110));
        setupMouseHandlers();
    }

    // ── Mouse ────────────────────────────────────────────────────────────────

    private void setupMouseHandlers() {
        MouseAdapter ma = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                dragStart   = e.getPoint();
                panXAtDrag  = panX;
                panYAtDrag  = panY;

                Point world = screenToWorld(e.getPoint());
                EditableZone sel = state.getSelectedZone();

                if (sel != null) {
                    // Check label
                    if (sel.isLabelHit(world.x, world.y, SNAP_RADIUS + 4)) {
                        draggingLabel  = true;
                        lastDragWorld  = world;
                        return;
                    }
                    // Check vertex
                    int vi = sel.hitTestVertex(world.x, world.y, SNAP_RADIUS);
                    if (vi >= 0) {
                        dragVertexIndex = vi;
                        lastDragWorld   = world;
                        return;
                    }
                }
                // Select zone
                EditableZone hit = state.getZoneAt(world.x, world.y);
                state.setSelectedZone(hit);
                dragVertexIndex = -1;
                draggingLabel   = false;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragVertexIndex = -1;
                draggingLabel   = false;
                lastDragWorld   = null;

                int dx = e.getX() - dragStart.x;
                int dy = e.getY() - dragStart.y;
                // If barely moved treat as click (pan handled in drag)
                if (Math.abs(dx) < 3 && Math.abs(dy) < 3 &&
                    SwingUtilities.isMiddleMouseButton(e)) {
                    // middle click reserved
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                EditableZone sel = state.getSelectedZone();
                if (sel == null) return;
                Point world = screenToWorld(e.getPoint());

                if (SwingUtilities.isRightMouseButton(e)) {
                    // Right-click vertex → delete
                    int vi = sel.hitTestVertex(world.x, world.y, SNAP_RADIUS);
                    if (vi >= 0) {
                        sel.removeVertex(vi);
                        state.markChanged(sel.getId());
                        repaint();
                    }
                } else if (e.getClickCount() == 2) {
                    // Double-click edge → insert vertex
                    int ei = sel.hitTestEdge(world.x, world.y, SNAP_RADIUS + 4);
                    if (ei >= 0) {
                        int[] mid = sel.edgeMidpoint(ei);
                        sel.insertVertex(ei, mid[0], mid[1]);
                        state.markChanged(sel.getId());
                        repaint();
                    }
                }
            }
        };

        MouseMotionAdapter mma = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragVertexIndex >= 0 && state.getSelectedZone() != null) {
                    Point world = screenToWorld(e.getPoint());
                    int wx = world.x, wy = world.y;
                    if (state.isSnappingEnabled()) {
                        int[] snap = state.findSnapTarget(wx, wy,
                            state.getSelectedZone(), WORLD_SNAP_RADIUS);
                        if (snap != null) { wx = snap[0]; wy = snap[1]; }
                    }
                    state.getSelectedZone().getVertices()
                         .get(dragVertexIndex)[0] = wx;
                    state.getSelectedZone().getVertices()
                         .get(dragVertexIndex)[1] = wy;
                    state.markChanged(state.getSelectedZone().getId());
                    lastDragWorld = world;
                    repaint();
                } else if (draggingLabel && state.getSelectedZone() != null) {
                    Point world = screenToWorld(e.getPoint());
                    state.getSelectedZone().setLabelX(world.x);
                    state.getSelectedZone().setLabelY(world.y);
                    state.markChanged(state.getSelectedZone().getId());
                    lastDragWorld = world;
                    repaint();
                } else if (dragStart != null) {
                    // Pan
                    panX = panXAtDrag + (e.getX() - dragStart.x);
                    panY = panYAtDrag + (e.getY() - dragStart.y);
                    repaint();
                }
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(mma);
        addMouseWheelListener(e -> {
            Point screen = e.getPoint();
            float oldZoom = zoom;
            zoom -= (float) e.getPreciseWheelRotation() * 0.1f;
            zoom = Math.max(0.3f, Math.min(4.0f, zoom));
            panX = (float)(screen.x - (screen.x - panX) * (zoom / oldZoom));
            panY = (float)(screen.y - (screen.y - panY) * (zoom / oldZoom));
            repaint();
        });
    }

    // ── Coordinate helpers ────────────────────────────────────────────────────

    private Point screenToWorld(Point screen) {
        int wx = Math.round((screen.x - panX) / zoom);
        int wy = Math.round((screen.y - panY) / zoom);
        return new Point(wx, wy);
    }

    private Point worldToScreen(int wx, int wy) {
        return new Point(Math.round(wx * zoom + panX),
                         Math.round(wy * zoom + panY));
    }

    // ── Paint ─────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawSeas(g2);
        drawZones(g2);
        drawVerticesAndLabel(g2);

        g2.dispose();
    }

    private void drawSeas(Graphics2D g2) {
        for (WorldGeography.SeaRegion sea : geography.getSeaRegions()) {
            int n = sea.getPolyX().length;
            int[] sx = new int[n], sy = new int[n];
            for (int i = 0; i < n; i++) {
                Point s = worldToScreen(sea.getPolyX()[i], sea.getPolyY()[i]);
                sx[i] = s.x; sy[i] = s.y;
            }
            g2.setColor(COLOR_SEA);
            g2.fillPolygon(sx, sy, n);
            g2.setColor(COLOR_SEA_BORDER);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawPolygon(sx, sy, n);
            g2.setStroke(new BasicStroke(1f));
        }
    }

    private void drawZones(Graphics2D g2) {
        EditableZone sel = state.getSelectedZone();
        for (EditableZone ez : state.getZones()) {
            List<int[]> verts = ez.getVertices();
            int n = verts.size();
            int[] sx = new int[n], sy = new int[n];
            for (int i = 0; i < n; i++) {
                Point s = worldToScreen(verts.get(i)[0], verts.get(i)[1]);
                sx[i] = s.x; sy[i] = s.y;
            }
            Polygon poly = new Polygon(sx, sy, n);

            // Fill
            if (ez == sel) {
                g2.setColor(COLOR_SELECTED);
            } else if (ez.isDesolate()) {
                g2.setColor(COLOR_DESOLATE);
            } else {
                g2.setColor(COLOR_NORMAL);
            }
            g2.fillPolygon(poly);

            // Changed tint
            if (state.isChanged(ez.getId())) {
                g2.setColor(COLOR_CHANGED);
                g2.fillPolygon(poly);
            }

            // Border
            g2.setColor(ez == sel ? COLOR_BORDER_SEL : COLOR_BORDER);
            g2.setStroke(new BasicStroke(ez == sel ? 2.5f : 1.5f));
            g2.drawPolygon(poly);
            g2.setStroke(new BasicStroke(1f));

            // Label
            drawZoneLabel(g2, ez);
        }
    }

    private void drawZoneLabel(Graphics2D g2, EditableZone ez) {
        Point lp = worldToScreen(ez.getLabelX(), ez.getLabelY());
        Font font = ez.isDesolate() ? FONT_LABEL_SMALL : FONT_LABEL;
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(ez.getDisplayName());

        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(ez.getDisplayName(), lp.x - tw / 2 + 1, lp.y + 1);
        g2.setColor(ez.isDesolate() ? new Color(150, 140, 130) : new Color(245, 235, 205));
        g2.drawString(ez.getDisplayName(), lp.x - tw / 2, lp.y);
    }

    private void drawVerticesAndLabel(Graphics2D g2) {
        EditableZone sel = state.getSelectedZone();
        if (sel == null) return;

        List<int[]> verts = sel.getVertices();
        int n = verts.size();

        // Edge midpoints (insert hint)
        g2.setColor(new Color(255, 255, 255, 80));
        for (int i = 0; i < n; i++) {
            int[] mid = sel.edgeMidpoint(i);
            Point s = worldToScreen(mid[0], mid[1]);
            g2.fillOval(s.x - 3, s.y - 3, 6, 6);
        }

        // Vertices
        for (int i = 0; i < n; i++) {
            Point s = worldToScreen(verts.get(i)[0], verts.get(i)[1]);
            boolean isDrag = (i == dragVertexIndex);
            g2.setColor(isDrag ? Color.WHITE : COLOR_VERTEX);
            g2.fillOval(s.x - VERTEX_RADIUS, s.y - VERTEX_RADIUS,
                        VERTEX_RADIUS * 2, VERTEX_RADIUS * 2);
            g2.setColor(COLOR_VERTEX_DEL);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(s.x - VERTEX_RADIUS, s.y - VERTEX_RADIUS,
                        VERTEX_RADIUS * 2, VERTEX_RADIUS * 2);
            g2.setStroke(new BasicStroke(1f));
        }

        // Label anchor dot
        Point lp = worldToScreen(sel.getLabelX(), sel.getLabelY());
        g2.setColor(draggingLabel ? Color.WHITE : COLOR_LABEL_DOT);
        g2.fillOval(lp.x - LABEL_RADIUS, lp.y - LABEL_RADIUS,
                    LABEL_RADIUS * 2, LABEL_RADIUS * 2);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawOval(lp.x - LABEL_RADIUS, lp.y - LABEL_RADIUS,
                    LABEL_RADIUS * 2, LABEL_RADIUS * 2);
        g2.setStroke(new BasicStroke(1f));
    }

    public void resetView() {
        panX = 0; panY = 0; zoom = 1.0f;
        repaint();
    }
}