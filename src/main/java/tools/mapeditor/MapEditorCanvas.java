package tools.mapeditor;

import main.map.WorldGeography;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class MapEditorCanvas extends JPanel {

    private static final int    VERTEX_RADIUS        = 6;
    private static final int    LABEL_RADIUS         = 5;
    private static final int    SNAP_RADIUS          = 8;
    private static final int    WORLD_SNAP_RADIUS    = 10;
    private static final Color  COLOR_SEA            = new Color(80, 130, 200, 140);
    private static final Color  COLOR_SEA_BORDER     = new Color(60, 110, 180, 200);
    private static final Color  COLOR_NORMAL         = new Color(148, 118, 76, 180);
    private static final Color  COLOR_DESOLATE       = new Color(50, 48, 52, 200);
    private static final Color  COLOR_SELECTED       = new Color(220, 200, 100, 200);
    private static final Color  COLOR_CHANGED        = new Color(100, 180, 100, 60);
    private static final Color  COLOR_VERTEX         = new Color(255, 220, 80);
    private static final Color  COLOR_VERTEX_DEL     = new Color(220, 60, 60);
    private static final Color  COLOR_LABEL_DOT      = new Color(80, 200, 255);
    private static final Color  COLOR_BORDER         = new Color(30, 20, 8);
    private static final Color  COLOR_BORDER_SEL     = new Color(240, 210, 80);
    private static final Color  COLOR_RIVER          = new Color(40, 120, 200, 200);
    private static final Color  COLOR_RIVER_SELECTED = new Color(100, 200, 255, 220);
    private static final Color  COLOR_MOUNTAIN_EDGE  = new Color(100, 70, 40, 220);
    private static final Font   FONT_LABEL           = new Font("Serif", Font.BOLD, 12);
    private static final Font   FONT_LABEL_SMALL     = new Font("Serif", Font.ITALIC, 10);

    private final EditorState    state;
    private final WorldGeography geography;
    private final OutputPanel    outputPanel;

    // Camera
    private float panX = 0, panY = 0, zoom = 1.0f;
    private Point  dragStart;
    private float  panXAtDrag, panYAtDrag;

    // Zone interaction
    private int     dragZoneVertexIndex = -1;
    private boolean draggingZoneLabel   = false;

    // River interaction
    private int     dragRiverWaypointIndex = -1;
    private boolean draggingRiverLabel     = false;

    // Sea interaction
    private int     dragSeaVertexIndex = -1;
    private boolean draggingSeaLabel   = false;

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

                // Check drag targets on already selected features (priority to zone, then river, then sea)
                EditableZone selZone = state.getSelectedZone();
                if (selZone != null) {
                    if (selZone.isLabelHit(world.x, world.y, SNAP_RADIUS + 4)) {
                        draggingZoneLabel = true;
                        lastDragWorld = world;
                        return;
                    }
                    int vi = selZone.hitTestVertex(world.x, world.y, SNAP_RADIUS);
                    if (vi >= 0) {
                        dragZoneVertexIndex = vi;
                        lastDragWorld = world;
                        return;
                    }
                }

                EditableRiver selRiver = state.getSelectedRiver();
                if (selRiver != null) {
                    if (selRiver.isLabelHit(world.x, world.y, SNAP_RADIUS + 4)) {
                        draggingRiverLabel = true;
                        lastDragWorld = world;
                        return;
                    }
                    int wi = selRiver.hitTestWaypointIndex(world.x, world.y, SNAP_RADIUS);
                    if (wi >= 0) {
                        dragRiverWaypointIndex = wi;
                        lastDragWorld = world;
                        return;
                    }
                }

                EditableSea selSea = state.getSelectedSea();
                if (selSea != null) {
                    if (selSea.isLabelHit(world.x, world.y, SNAP_RADIUS + 4)) {
                        draggingSeaLabel = true;
                        lastDragWorld = world;
                        return;
                    }
                    int vi = selSea.hitTestVertex(world.x, world.y, SNAP_RADIUS);
                    if (vi >= 0) {
                        dragSeaVertexIndex = vi;
                        lastDragWorld = world;
                        return;
                    }
                }

                // Select a new feature
                EditableZone hitZone = state.getZoneAt(world.x, world.y);
                if (hitZone != null) {
                    state.setSelectedZone(hitZone);
                    dragZoneVertexIndex = -1; draggingZoneLabel = false;
                    dragRiverWaypointIndex = -1; draggingRiverLabel = false;
                    dragSeaVertexIndex = -1; draggingSeaLabel = false;
                    repaint();
                    return;
                }

                EditableRiver hitRiver = state.getRiverAt(world.x, world.y, SNAP_RADIUS);
                if (hitRiver != null) {
                    state.setSelectedRiver(hitRiver);
                    dragZoneVertexIndex = -1; draggingZoneLabel = false;
                    dragRiverWaypointIndex = -1; draggingRiverLabel = false;
                    dragSeaVertexIndex = -1; draggingSeaLabel = false;
                    repaint();
                    return;
                }

                EditableSea hitSea = state.getSeaAt(world.x, world.y);
                if (hitSea != null) {
                    state.setSelectedSea(hitSea);
                    dragZoneVertexIndex = -1; draggingZoneLabel = false;
                    dragRiverWaypointIndex = -1; draggingRiverLabel = false;
                    dragSeaVertexIndex = -1; draggingSeaLabel = false;
                    repaint();
                    return;
                }

                // Nothing hit → clear selection
                state.clearSelection();
                dragZoneVertexIndex = -1; draggingZoneLabel = false;
                dragRiverWaypointIndex = -1; draggingRiverLabel = false;
                dragSeaVertexIndex = -1; draggingSeaLabel = false;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragZoneVertexIndex = -1; draggingZoneLabel = false;
                dragRiverWaypointIndex = -1; draggingRiverLabel = false;
                dragSeaVertexIndex = -1; draggingSeaLabel = false;
                lastDragWorld = null;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                Point world = screenToWorld(e.getPoint());

                if (SwingUtilities.isRightMouseButton(e)) {
                    // Right-click vertex/waypoint → delete
                    EditableZone zone = state.getSelectedZone();
                    if (zone != null) {
                        int vi = zone.hitTestVertex(world.x, world.y, SNAP_RADIUS);
                        if (vi >= 0) {
                            zone.removeVertex(vi);
                            state.markChanged(zone.getId());
                            repaint();
                            return;
                        }
                    }
                    EditableRiver river = state.getSelectedRiver();
                    if (river != null) {
                        int wi = river.hitTestWaypointIndex(world.x, world.y, SNAP_RADIUS);
                        if (wi >= 0 && river.getWaypoints().size() > 2) {
                            river.removeWaypoint(wi);
                            state.markRiverChanged(river.getName());
                            repaint();
                            return;
                        }
                    }
                    EditableSea sea = state.getSelectedSea();
                    if (sea != null) {
                        int vi = sea.hitTestVertex(world.x, world.y, SNAP_RADIUS);
                        if (vi >= 0 && sea.getVertices().size() > 3) {
                            sea.removeVertex(vi);
                            state.markSeaChanged(sea.getName());
                            repaint();
                            return;
                        }
                    }
                } else if (e.getClickCount() == 2) {
                    // Double-click edge → insert vertex/waypoint
                    EditableZone zone = state.getSelectedZone();
                    if (zone != null) {
                        int ei = zone.hitTestEdge(world.x, world.y, SNAP_RADIUS + 4);
                        if (ei >= 0) {
                            int[] mid = zone.edgeMidpoint(ei);
                            zone.insertVertex(ei, mid[0], mid[1]);
                            state.markChanged(zone.getId());
                            repaint();
                            return;
                        }
                    }
                    EditableRiver river = state.getSelectedRiver();
                    if (river != null) {
                        int si = river.hitTestSegment(world.x, world.y, SNAP_RADIUS + 4);
                        if (si >= 0) {
                            int[] mid = river.edgeMidpoint(si);
                            river.insertWaypoint(si, mid[0], mid[1]);
                            state.markRiverChanged(river.getName());
                            repaint();
                            return;
                        }
                    }
                    EditableSea sea = state.getSelectedSea();
                    if (sea != null) {
                        int ei = sea.hitTestEdge(world.x, world.y, SNAP_RADIUS + 4);
                        if (ei >= 0) {
                            int[] mid = sea.edgeMidpoint(ei);
                            sea.insertVertex(ei, mid[0], mid[1]);
                            state.markSeaChanged(sea.getName());
                            repaint();
                            return;
                        }
                    }
                }
            }
        };

        MouseMotionAdapter mma = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragZoneVertexIndex >= 0 && state.getSelectedZone() != null) {
                    Point world = screenToWorld(e.getPoint());
                    int wx = world.x, wy = world.y;
                    if (state.isSnappingEnabled()) {
                        int[] snap = state.findSnapTarget(wx, wy,
                                state.getSelectedZone(), WORLD_SNAP_RADIUS);
                        if (snap != null) { wx = snap[0]; wy = snap[1]; }
                    }
                    state.getSelectedZone().getVertices().get(dragZoneVertexIndex)[0] = wx;
                    state.getSelectedZone().getVertices().get(dragZoneVertexIndex)[1] = wy;
                    state.markChanged(state.getSelectedZone().getId());
                    lastDragWorld = world;
                    repaint();
                } else if (draggingZoneLabel && state.getSelectedZone() != null) {
                    Point world = screenToWorld(e.getPoint());
                    state.getSelectedZone().setLabelX(world.x);
                    state.getSelectedZone().setLabelY(world.y);
                    state.markChanged(state.getSelectedZone().getId());
                    lastDragWorld = world;
                    repaint();
                } else if (dragRiverWaypointIndex >= 0 && state.getSelectedRiver() != null) {
                    Point world = screenToWorld(e.getPoint());
                    state.getSelectedRiver().getWaypoints().get(dragRiverWaypointIndex)[0] = world.x;
                    state.getSelectedRiver().getWaypoints().get(dragRiverWaypointIndex)[1] = world.y;
                    state.markRiverChanged(state.getSelectedRiver().getName());
                    repaint();
                } else if (draggingRiverLabel && state.getSelectedRiver() != null) {
                    Point world = screenToWorld(e.getPoint());
                    state.getSelectedRiver().setLabelX(world.x);
                    state.getSelectedRiver().setLabelY(world.y);
                    state.markRiverChanged(state.getSelectedRiver().getName());
                    repaint();
                } else if (dragSeaVertexIndex >= 0 && state.getSelectedSea() != null) {
                    Point world = screenToWorld(e.getPoint());
                    int wx = world.x, wy = world.y;
                    if (state.isSnappingEnabled()) {
                        int[] snap = state.findSnapTarget(wx, wy,
                                state.getSelectedSea(), WORLD_SNAP_RADIUS);
                        if (snap != null) { wx = snap[0]; wy = snap[1]; }
                    }
                    state.getSelectedSea().getVertices().get(dragSeaVertexIndex)[0] = wx;
                    state.getSelectedSea().getVertices().get(dragSeaVertexIndex)[1] = wy;
                    state.markSeaChanged(state.getSelectedSea().getName());
                    repaint();
                } else if (draggingSeaLabel && state.getSelectedSea() != null) {
                    Point world = screenToWorld(e.getPoint());
                    state.getSelectedSea().setLabelX(world.x);
                    state.getSelectedSea().setLabelY(world.y);
                    state.markSeaChanged(state.getSelectedSea().getName());
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
            panX = (float) (screen.x - (screen.x - panX) * (zoom / oldZoom));
            panY = (float) (screen.y - (screen.y - panY) * (zoom / oldZoom));
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
        return new Point(Math.round(wx * zoom + panX), Math.round(wy * zoom + panY));
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
        drawMountainEdges(g2);
        drawRivers(g2);
        drawVerticesAndLabels(g2);

        g2.dispose();
    }

    private void drawSeas(Graphics2D g2) {
        EditableSea selected = state.getSelectedSea();
        for (EditableSea sea : state.getSeas()) {
            List<int[]> verts = sea.getVertices();
            int n = verts.size();
            int[] sx = new int[n], sy = new int[n];
            for (int i = 0; i < n; i++) {
                Point s = worldToScreen(verts.get(i)[0], verts.get(i)[1]);
                sx[i] = s.x; sy[i] = s.y;
            }
            Polygon poly = new Polygon(sx, sy, n);
            g2.setColor(COLOR_SEA);
            g2.fillPolygon(poly);
            g2.setColor(COLOR_SEA_BORDER);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawPolygon(poly);
            g2.setStroke(new BasicStroke(1f));

            // Changed tint
            if (state.isSeaChanged(sea.getName())) {
                g2.setColor(COLOR_CHANGED);
                g2.fillPolygon(poly);
            }

            // Highlight selected sea
            if (sea == selected) {
                g2.setColor(new Color(255, 255, 100, 80));
                g2.fillPolygon(poly);
                g2.setColor(new Color(255, 200, 0));
                g2.setStroke(new BasicStroke(2f));
                g2.drawPolygon(poly);
                g2.setStroke(new BasicStroke(1f));
            }
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
            drawFeatureLabel(g2, ez.getDisplayName(), ez.getLabelX(), ez.getLabelY(),
                             ez.isDesolate() ? FONT_LABEL_SMALL : FONT_LABEL,
                             ez.isDesolate() ? new Color(150, 140, 130) : new Color(245, 235, 205));
        }
    }

    private void drawMountainEdges(Graphics2D g2) {
        for (EditableZone ez : state.getZones()) {
            List<int[]> verts = ez.getVertices();
            int n = verts.size();
            for (int i = 0; i < n; i++) {
                if (ez.isMountainEdge(i)) {
                    int[] a = verts.get(i);
                    int[] b = verts.get((i + 1) % n);
                    Point sa = worldToScreen(a[0], a[1]);
                    Point sb = worldToScreen(b[0], b[1]);
                    drawMountainSymbol(g2, sa.x, sa.y, sb.x, sb.y);
                }
            }
        }
    }

    private void drawMountainSymbol(Graphics2D g2, int x1, int y1, int x2, int y2) {
        g2.setColor(COLOR_MOUNTAIN_EDGE);
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.hypot(dx, dy);
        if (len == 0) return;
        double ux = dx / len, uy = dy / len;
        double nx = -uy, ny = ux; // perpendicular
        double step = 12;
        for (double t = step / 2; t < len - step / 2; t += step) {
            double cx = x1 + ux * t;
            double cy = y1 + uy * t;
            double offset = (t % (step * 2) < step) ? 6 : -6; // zigzag
            int px = (int) Math.round(cx + nx * offset);
            int py = (int) Math.round(cy + ny * offset);
            g2.drawLine((int) Math.round(cx), (int) Math.round(cy), px, py);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawRivers(Graphics2D g2) {
        EditableRiver selected = state.getSelectedRiver();
        for (EditableRiver river : state.getRivers()) {
            List<int[]> pts = river.getWaypoints();
            int n = pts.size();
            if (n < 2) continue;
            int[] sx = new int[n], sy = new int[n];
            for (int i = 0; i < n; i++) {
                Point s = worldToScreen(pts.get(i)[0], pts.get(i)[1]);
                sx[i] = s.x; sy[i] = s.y;
            }
            g2.setColor(river == selected ? COLOR_RIVER_SELECTED : COLOR_RIVER);
            g2.setStroke(new BasicStroke(river == selected ? 2.5f : 2f));
            g2.drawPolyline(sx, sy, n);
            g2.setStroke(new BasicStroke(1f));

            // Changed tint (small marker)
            if (state.isRiverChanged(river.getName())) {
                Point first = worldToScreen(pts.get(0)[0], pts.get(0)[1]);
                g2.setColor(Color.GREEN);
                g2.fillOval(first.x - 5, first.y - 5, 10, 10);
            }

            // Label
            drawFeatureLabel(g2, river.getName(), river.getLabelX(), river.getLabelY(),
                             FONT_LABEL_SMALL, new Color(210, 230, 255));
        }
    }

    private void drawFeatureLabel(Graphics2D g2, String text, int lx, int ly,
                                  Font font, Color color) {
        Point lp = worldToScreen(lx, ly);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text);
        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(text, lp.x - tw / 2 + 1, lp.y + 1);
        g2.setColor(color);
        g2.drawString(text, lp.x - tw / 2, lp.y);
    }

    private void drawVerticesAndLabels(Graphics2D g2) {
        // Zone vertices + label anchor
        EditableZone selZone = state.getSelectedZone();
        if (selZone != null) {
            List<int[]> verts = selZone.getVertices();
            int n = verts.size();

            // Edge midpoints (insert hint)
            g2.setColor(new Color(255, 255, 255, 80));
            for (int i = 0; i < n; i++) {
                int[] mid = selZone.edgeMidpoint(i);
                Point s = worldToScreen(mid[0], mid[1]);
                g2.fillOval(s.x - 3, s.y - 3, 6, 6);
            }

            // Vertices
            for (int i = 0; i < n; i++) {
                Point s = worldToScreen(verts.get(i)[0], verts.get(i)[1]);
                boolean isDrag = (i == dragZoneVertexIndex);
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
            drawAnchorDot(g2, selZone.getLabelX(), selZone.getLabelY(), draggingZoneLabel);
        }

        // River waypoints + label anchor
        EditableRiver selRiver = state.getSelectedRiver();
        if (selRiver != null) {
            List<int[]> pts = selRiver.getWaypoints();
            g2.setColor(new Color(255, 255, 255, 80));
            for (int i = 0; i < pts.size() - 1; i++) {
                int[] mid = selRiver.edgeMidpoint(i);
                Point s = worldToScreen(mid[0], mid[1]);
                g2.fillOval(s.x - 3, s.y - 3, 6, 6);
            }
            for (int i = 0; i < pts.size(); i++) {
                Point s = worldToScreen(pts.get(i)[0], pts.get(i)[1]);
                boolean isDrag = (i == dragRiverWaypointIndex);
                g2.setColor(isDrag ? Color.WHITE : new Color(100, 180, 255));
                g2.fillOval(s.x - VERTEX_RADIUS, s.y - VERTEX_RADIUS,
                            VERTEX_RADIUS * 2, VERTEX_RADIUS * 2);
                g2.setColor(Color.BLUE);
                g2.drawOval(s.x - VERTEX_RADIUS, s.y - VERTEX_RADIUS,
                            VERTEX_RADIUS * 2, VERTEX_RADIUS * 2);
            }
            drawAnchorDot(g2, selRiver.getLabelX(), selRiver.getLabelY(), draggingRiverLabel);
        }

        // Sea vertices + label anchor
        EditableSea selSea = state.getSelectedSea();
        if (selSea != null) {
            List<int[]> verts = selSea.getVertices();
            int n = verts.size();
            g2.setColor(new Color(255, 255, 255, 80));
            for (int i = 0; i < n; i++) {
                int[] mid = selSea.edgeMidpoint(i);
                Point s = worldToScreen(mid[0], mid[1]);
                g2.fillOval(s.x - 3, s.y - 3, 6, 6);
            }
            for (int i = 0; i < n; i++) {
                Point s = worldToScreen(verts.get(i)[0], verts.get(i)[1]);
                boolean isDrag = (i == dragSeaVertexIndex);
                g2.setColor(isDrag ? Color.WHITE : COLOR_VERTEX);
                g2.fillOval(s.x - VERTEX_RADIUS, s.y - VERTEX_RADIUS,
                            VERTEX_RADIUS * 2, VERTEX_RADIUS * 2);
                g2.setColor(COLOR_VERTEX_DEL);
                g2.drawOval(s.x - VERTEX_RADIUS, s.y - VERTEX_RADIUS,
                            VERTEX_RADIUS * 2, VERTEX_RADIUS * 2);
            }
            drawAnchorDot(g2, selSea.getLabelX(), selSea.getLabelY(), draggingSeaLabel);
        }
    }

    private void drawAnchorDot(Graphics2D g2, int lx, int ly, boolean dragging) {
        Point lp = worldToScreen(lx, ly);
        g2.setColor(dragging ? Color.WHITE : COLOR_LABEL_DOT);
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