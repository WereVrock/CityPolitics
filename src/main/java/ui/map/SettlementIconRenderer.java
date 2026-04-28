package ui.map;

import main.map.Zone;

import java.awt.*;

public final class SettlementIconRenderer {

    private SettlementIconRenderer() {}

    public static void drawSettlementIcon(Graphics2D g2, Zone zone, Color shadowColor, Color accentColor) {
        int cx = zone.getLabelX();
        int cy = zone.getLabelY() - 4;

        switch (zone.getSettlement()) {
            case CAPITAL -> drawCastleIcon(g2, cx, cy, shadowColor, accentColor);
            case TOWN    -> drawTowerIcon(g2, cx, cy, shadowColor);
            case VILLAGE -> drawHutIcon(g2, cx, cy, shadowColor);
        }
    }

    private static void drawCastleIcon(Graphics2D g2, int cx, int cy, Color shadowColor, Color accentColor) {
        g2.setColor(shadowColor);

        g2.fillRect(cx - 9, cy - 6, 18, 10);
        g2.fillRect(cx - 9, cy - 12, 4, 6);
        g2.fillRect(cx - 2, cy - 12, 4, 6);
        g2.fillRect(cx + 5, cy - 12, 4, 6);

        g2.setColor(new Color(30, 18, 6));
        g2.fillRoundRect(cx - 3, cy - 3, 6, 7, 3, 3);

        g2.setColor(accentColor);
        g2.setStroke(new BasicStroke(0.8f));
        g2.drawRect(cx - 9, cy - 6, 18, 10);
        g2.drawRect(cx - 9, cy - 12, 4, 6);
        g2.drawRect(cx - 2, cy - 12, 4, 6);
        g2.drawRect(cx + 5, cy - 12, 4, 6);
    }

    private static void drawTowerIcon(Graphics2D g2, int cx, int cy, Color shadowColor) {
        g2.setColor(shadowColor);
        g2.fillRect(cx - 5, cy - 10, 10, 14);

        int[] rx = {cx - 7, cx + 7, cx};
        int[] ry = {cy - 10, cy - 10, cy - 17};
        g2.fillPolygon(rx, ry, 3);

        g2.setColor(new Color(30, 18, 6));
        g2.fillRoundRect(cx - 2, cy - 6, 4, 5, 2, 2);

        g2.setColor(new Color(160, 200, 150));
        g2.setStroke(new BasicStroke(0.7f));
        g2.drawRect(cx - 5, cy - 10, 10, 14);
        g2.drawPolygon(rx, ry, 3);
    }

    private static void drawHutIcon(Graphics2D g2, int cx, int cy, Color shadowColor) {
        g2.setColor(shadowColor);
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
}