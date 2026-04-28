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
// Parchment base — warm tan fill over the entire world canvas
g2.setColor(COLOR_BG);
g2.fillRect(-200, -200, 1400, 1000);

// Subtle hand-drawn grain lines (angled, irregular spacing)
g2.setColor(COLOR_PARCH_GRAIN);
g2.setStroke(new BasicStroke(0.4f));
int[] offsets = {0, 17, 31, 48, 62, 79, 95, 110, 128, 143,
160, 174, 191, 207, 220, 238, 253, 268, 284, 299,
315, 330, 347, 361, 378, 393, 410, 425, 440, 458,
472, 489, 503, 520, 535, 552, 566, 583, 598, 614,
630, 645, 660, 678, 693, 710, 725, 740, 758, 773};
for (int y : offsets) {
g2.drawLine(-200, y, 1400, y + 6);
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

// ── Inner texture: faint cross-hatch for depth ─────────────────────
g2.setColor(new Color(0, 0, 0, 18));
g2.setStroke(new BasicStroke(0.5f));
Rectangle bounds = poly.getBounds();
g2.setClip(poly);
for (int x = bounds.x; x < bounds.x + bounds.width; x += 9) {
g2.drawLine(x, bounds.y, x + bounds.height, bounds.y + bounds.height);
}
g2.setClip(null);

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

// ── Border — rough, double-stroked ────────────────────────────────────
// Outer dark stroke (rough feel)
g2.setColor(selected ? COLOR_BORDER_SEL : COLOR_BORDER);
g2.setStroke(new BasicStroke(selected ? 3f : 2f,
BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
g2.drawPolygon(poly);
// Inner lighter stroke for parchment edge highlight
if (!selected) {
g2.setColor(new Color(200, 170, 110, 60));
g2.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
g2.drawPolygon(poly);
}

// ── Settlement icon ───────────────────────────────────────────────────
drawSettlementIcon(g2, zone);

// ── Labels ────────────────────────────────────────────────────────────
drawZoneLabels(g2, zone);
}

private void drawZoneLabels(Graphics2D g2, Zone zone) {
int lx = zone.getLabelX();
int ly = zone.getLabelY() + ICON_LABEL_OFFSET;

// Name — drop shadow then text
g2.setFont(FONT_ZONE_NAME);
g2.setColor(COLOR_LABEL_SHADOW);
g2.drawString(zone.getDisplayName(), lx - 1 - centerOffset(g2, zone.getDisplayName()), ly + 1);
g2.setColor(COLOR_LABEL);
g2.drawString(zone.getDisplayName(), lx - centerOffset(g2, zone.getDisplayName()), ly);

// Gold / food — italic serif, no monospaced
g2.setFont(FONT_ZONE_STATS);
String goldStr = "\u2666 " + zone.getGoldProduction();  // ♦ gold
String foodStr = "\u2663 " + zone.getFoodProduction();  // ♣ food
int statsY  = ly + 14;
int gap     = 8;
int gw      = g2.getFontMetrics().stringWidth(goldStr);
int fw      = g2.getFontMetrics().stringWidth(foodStr);
int startX  = lx - (gw + gap + fw) / 2;
g2.setColor(COLOR_LABEL_SHADOW);
g2.drawString(goldStr, startX + 1, statsY + 1);
g2.drawString(foodStr, startX + gw + gap + 1, statsY + 1);
g2.setColor(COLOR_GOLD_TEXT);
g2.drawString(goldStr, startX, statsY);
g2.setColor(COLOR_FOOD_TEXT);
g2.drawString(foodStr, startX + gw + gap, statsY);
}

private static final int ICON_RADIUS      = 10;
private static final int ICON_LABEL_OFFSET = 18; // push labels below icon

private void drawSettlementIcon(Graphics2D g2, Zone zone) {
int cx = zone.getLabelX();
int cy = zone.getLabelY() - 4;

switch (zone.getSettlement()) {
case CAPITAL -> drawCastleIcon(g2, cx, cy);
case TOWN    -> drawTowerIcon(g2, cx, cy);
case VILLAGE -> drawHutIcon(g2, cx, cy);
}
}

/** Small castle silhouette for capitals. */
private void drawCastleIcon(Graphics2D g2, int cx, int cy) {
g2.setColor(COLOR_LABEL_SHADOW);
// Base wall
g2.fillRect(cx - 9, cy - 6, 18, 10);
// Three merlons
g2.fillRect(cx - 9, cy - 12, 4, 6);
g2.fillRect(cx - 2, cy - 12, 4, 6);
g2.fillRect(cx + 5, cy - 12, 4, 6);
// Gate
g2.setColor(new Color(30, 18, 6));
g2.fillRoundRect(cx - 3, cy - 3, 6, 7, 3, 3);
// Gold outline
g2.setColor(COLOR_GOLD_TEXT);
g2.setStroke(new BasicStroke(0.8f));
g2.drawRect(cx - 9, cy - 6, 18, 10);
g2.drawRect(cx - 9, cy - 12, 4, 6);
g2.drawRect(cx - 2, cy - 12, 4, 6);
g2.drawRect(cx + 5, cy - 12, 4, 6);
}

/** Simple tower for towns. */
private void drawTowerIcon(Graphics2D g2, int cx, int cy) {
g2.setColor(COLOR_LABEL_SHADOW);
g2.fillRect(cx - 5, cy - 10, 10, 14);
// Roof triangle
int[] rx = {cx - 7, cx + 7, cx};
int[] ry = {cy - 10, cy - 10, cy - 17};
g2.fillPolygon(rx, ry, 3);
// Window
g2.setColor(new Color(30, 18, 6));
g2.fillRoundRect(cx - 2, cy - 6, 4, 5, 2, 2);
// Outline
g2.setColor(new Color(160, 200, 150));
g2.setStroke(new BasicStroke(0.7f));
g2.drawRect(cx - 5, cy - 10, 10, 14);
g2.drawPolygon(rx, ry, 3);
}

/** Small hut/house for villages. */
private void drawHutIcon(Graphics2D g2, int cx, int cy) {
g2.setColor(COLOR_LABEL_SHADOW);
g2.fillRect(cx - 6, cy - 6, 12, 10);
int[] rx = {cx - 8, cx + 8, cx};
int[] ry = {cy - 6, cy - 6, cy - 14};
g2.fillPolygon(rx, ry, 3);
g2.setColor(new Color(30, 18, 6));
g2.fillRect(cx - 2, cy - 2, 4, 6);
g2.setColor(new Color(190, 155, 90));
g2.setStroke(new BasicStroke(0.7f));
g2.drawRect(cx - 6, cy - 6, 12, 10);
g2.drawPolygon(rx, ry, 3);
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
}

