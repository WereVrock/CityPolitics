package ui.map;

import main.map.Zone;
import main.map.ZoneManager;
import main.map.ZoneState;

import java.awt.*;

public class MapRenderer {

public static final Color COLOR_BG = new Color(188, 158, 110);

private static final Color COLOR_CAPITAL = new Color(110, 72, 35);
private static final Color COLOR_TOWN = new Color(52, 88, 55);
private static final Color COLOR_VILLAGE = new Color(148, 118, 76);

private static final Color COLOR_BORDER = new Color(32, 20, 8);
private static final Color COLOR_BORDER_SEL = new Color(235, 205, 85);

private static final Color COLOR_HOVER = new Color(255, 240, 180, 35);
private static final Color COLOR_LABEL = new Color(245, 235, 205);
private static final Color COLOR_LABEL_SHADOW = new Color(10, 5, 0, 180);
private static final Color COLOR_GOLD_TEXT = new Color(215, 175, 85);
private static final Color COLOR_FOOD_TEXT = new Color(110, 185, 95);

private static final Font FONT_ZONE_NAME = new Font("Serif", Font.BOLD, 13);
private static final Font FONT_ZONE_STATS = new Font("Serif", Font.ITALIC, 10);

private static final int ICON_LABEL_OFFSET = 18;

private final ZoneManager zoneManager;

public MapRenderer(ZoneManager zoneManager) {
this.zoneManager = zoneManager;
}

public void render(Graphics2D g2, Zone selected, Zone hovered) {
drawBackground(g2);

for (Zone zone : zoneManager.getZones()) {
drawZone(g2, zone, selected, hovered);
}
}

public Zone hitTest(Point world) {
for (Zone zone : zoneManager.getZones()) {
Polygon p = new Polygon(zone.getPolyX(), zone.getPolyY(), zone.getPolyX().length);
if (p.contains(world)) return zone;
}
return null;
}

private void drawBackground(Graphics2D g2) {
g2.setColor(COLOR_BG);
g2.fillRect(-200, -200, 1400, 1000);
}

private void drawZone(Graphics2D g2, Zone zone, Zone selected, Zone hovered) {
Polygon poly = new Polygon(zone.getPolyX(), zone.getPolyY(), zone.getPolyX().length);
ZoneState state = zoneManager.getState(zone.getId());

Color base = switch (zone.getSettlement()) {
case CAPITAL -> COLOR_CAPITAL;
case TOWN -> COLOR_TOWN;
case VILLAGE -> COLOR_VILLAGE;
};

if (zone == selected) base = base.brighter();

g2.setColor(base);
g2.fillPolygon(poly);

if (zone == hovered && zone != selected) {
g2.setColor(COLOR_HOVER);
g2.fillPolygon(poly);
}

if (state.getDamage() > 0) {
g2.setColor(new Color(180, 30, 30, 100));
g2.fillPolygon(poly);
}

g2.setColor(zone == selected ? COLOR_BORDER_SEL : COLOR_BORDER);
g2.setStroke(new BasicStroke(zone == selected ? 3f : 2f));
g2.drawPolygon(poly);

SettlementIconRenderer.drawSettlementIcon(g2, zone, COLOR_LABEL_SHADOW, COLOR_GOLD_TEXT);
drawZoneLabels(g2, zone);
}

private void drawZoneLabels(Graphics2D g2, Zone zone) {
int lx = zone.getLabelX();
int ly = zone.getLabelY() + ICON_LABEL_OFFSET;

g2.setFont(FONT_ZONE_NAME);

g2.setColor(COLOR_LABEL_SHADOW);
g2.drawString(zone.getDisplayName(), lx - 1, ly + 1);

g2.setColor(COLOR_LABEL);
g2.drawString(zone.getDisplayName(), lx, ly);

g2.setFont(FONT_ZONE_STATS);

String gold = "\u2666 " + zone.getGoldProduction();
String food = "\u2663 " + zone.getFoodProduction();

g2.setColor(COLOR_GOLD_TEXT);
g2.drawString(gold, lx - 20, ly + 14);

g2.setColor(COLOR_FOOD_TEXT);
g2.drawString(food, lx + 10, ly + 14);
}



}
