@@PATCH

@@FILE: MapPanel.java
@@FIND:
public class MapPanel extends JPanel {

// ─── Visual constants ─────────────────────────────────────────────────────
// ── Parchment palette ─────────────────────────────────────────────────────
private static final Color COLOR_CAPITAL        = new Color(110, 72,  35);
private static final Color COLOR_TOWN           = new Color(52,  88,  55);
private static final Color COLOR_VILLAGE        = new Color(148, 118, 76);
private static final Color COLOR_SELECTED       = new Color(210, 175, 65);
private static final Color COLOR_BORDER         = new Color(32,  20,  8);
private static final Color COLOR_BORDER_SEL     = new Color(235, 205, 85);
private static final Color COLOR_HOVER          = new Color(255, 240, 180, 35);
private static final Color COLOR_LABEL          = new Color(245, 235, 205);
private static final Color COLOR_LABEL_SHADOW   = new Color(10,  5,   0,  180);
private static final Color COLOR_DAMAGE_OVERLAY = new Color(160, 25,  25,  110);
private static final Color COLOR_GOLD_TEXT      = new Color(215, 175, 85);
private static final Color COLOR_FOOD_TEXT      = new Color(110, 185, 95);
private static final Color COLOR_BG             = new Color(188, 158, 110);   // parchment
private static final Color COLOR_PARCH_GRAIN    = new Color(100, 72,  35,  22); // subtle grain lines

// ── Fonts ─────────────────────────────────────────────────────────────────
private static final Font FONT_ZONE_NAME  = new Font("Serif", Font.BOLD,        13);
private static final Font FONT_ZONE_STATS = new Font("Serif", Font.ITALIC,      10);
private static final Font FONT_SETTLEMENT = new Font("Serif", Font.PLAIN,       11);

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
@@REPLACE:
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

@@FIND:
private void handleClick(Point screenPt) {
Zone hit = zoneAtScreenPoint(screenPt);
selectedZone = hit;
repaint();
onZoneSelected.accept(hit);
}
@@REPLACE:
private void handleClick(Point screenPt) {
    Zone hit = zoneAtScreenPoint(screenPt);
    selectedZone = hit;
    repaint();
    onZoneSelected.accept(hit);
}

@@FIND:
private Zone zoneAtScreenPoint(Point screenPt) {
// Convert from panel-local coords to world coords accounting for pan and zoom
Point world = screenToWorld(screenPt);
for (Zone zone : zoneManager.getZones()) {
Polygon poly = buildPolygon(zone);
if (poly.contains(world)) return zone;
}
return null;
}
@@REPLACE:
private Zone zoneAtScreenPoint(Point screenPt) {
    Point world = camera.screenToWorld(screenPt);
    return renderer.hitTest(world);
}

@@FIND:
private Point screenToWorld(Point panelLocal) {
// panelLocal is already relative to this panel's origin (MouseEvent gives panel-local coords)
int wx = Math.round((panelLocal.x - panX) / zoom);
int wy = Math.round((panelLocal.y - panY) / zoom);
return new Point(wx, wy);
}
@@REPLACE:
// removed (handled by MapCamera)

@@FIND:
private Polygon buildPolygon(Zone zone) {
return new Polygon(zone.getPolyX(), zone.getPolyY(), zone.getPolyX().length);
}
@@REPLACE:
// removed (handled by MapRenderer)

@@FIND:
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
@@REPLACE:
@Override
protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g.create();

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,     RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    camera.applyTransform(g2);
    renderer.render(g2, selectedZone, hoveredZone);

    g2.dispose();
}

@@FIND:
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
@@REPLACE:
addMouseMotionListener(new MouseMotionAdapter() {
@Override public void mouseDragged(MouseEvent e) {
    if (dragStart != null) {
        camera.setPan(
            panXAtDrag + (e.getX() - dragStart.x),
            panYAtDrag + (e.getY() - dragStart.y)
        );
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

@@FIND:
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
@@REPLACE:
addMouseWheelListener(e -> {
    camera.zoomAt(e.getX(), e.getY(), (float) e.getPreciseWheelRotation());
    repaint();
});

@@FIND:
@Override public void mousePressed(MouseEvent e) {
dragStart    = e.getPoint();
panXAtDrag   = panX;
panYAtDrag   = panY;
}
@@REPLACE:
@Override public void mousePressed(MouseEvent e) {
    dragStart  = e.getPoint();
    panXAtDrag = camera.getPanX();
    panYAtDrag = camera.getPanY();
}

@@END