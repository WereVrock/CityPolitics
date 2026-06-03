// MapRenderer.java
package ui.map;

import main.map.*;
import main.nobles.NobleHouse;
import main.nobles.NobleHouseColors;
import main.nobles.NobleHouseManager;

import java.awt.*;
import main.army.Army;
import main.nobles.NobleArmy;

/**
 * Orchestrates all map rendering in correct layer order.
 */
public class MapRenderer {

    public static final Color COLOR_BG = new Color(188, 158, 110);

    private static final Color COLOR_CAPITAL   = new Color(110, 72,  35);
    private static final Color COLOR_TOWN      = new Color(52,  88,  55);
    private static final Color COLOR_VILLAGE   = new Color(148, 118, 76);
    private static final Color COLOR_DESOLATE  = new Color(38,  38,  42);
    private static final Color COLOR_DESOLATE_BORDER = new Color(60, 60, 65);

    private static final Color TERRAIN_NONE     = new Color(148, 118, 76);
    private static final Color TERRAIN_FOREST   = new Color(55,  100, 50);
    private static final Color TERRAIN_VOLCANO  = new Color(110, 45,  30);
    private static final Color TERRAIN_ICE      = new Color(190, 215, 230);
    private static final Color TERRAIN_FARMLAND = new Color(160, 150, 70);
    private static final Color TERRAIN_MARSH    = new Color(70,  105, 85);
    private static final Color TERRAIN_COASTAL  = new Color(90,  130, 140);
    private static final Color TERRAIN_MOUNTAIN = new Color(120, 105, 90);
    private static final Color COLOR_BORDER  = new Color(32,  20,  8);
    private static final Color COLOR_BORDER_SEL  = new Color(235, 205, 85);
    private static final Color COLOR_HOVER   = new Color(255, 240, 180, 35);
    private static final Color COLOR_LABEL   = new Color(245, 235, 205);
    private static final Color COLOR_LABEL_SHADOW = new Color(10, 5, 0, 180);
    private static final Color COLOR_GOLD_TEXT    = new Color(215, 175, 85);
    private static final Color COLOR_FOOD_TEXT    = new Color(110, 185, 95);

    private static final Font FONT_ZONE_NAME  = new Font("Serif", Font.BOLD,   13);
    private static final Font FONT_ZONE_STATS = new Font("Serif", Font.ITALIC, 10);

    private static final int   ICON_LABEL_OFFSET      = 18;
    private static final float INNER_GLOW_MAX_WIDTH   = 16f;
    private static final int   INNER_GLOW_ALPHA_CAP   = 120;
    private static final int   INNER_GLOW_LAYERS      = 8;

    private final ZoneManager              zoneManager;
    private final ZoneDecorationRegistry   decorationRegistry;
    private final WorldGeography           worldGeography;
    private final NobleHouseManager        nobleHouseManager;
    private final SeaRenderer              seaRenderer;
    private final RiverRenderer            riverRenderer;
    private final MountainEdgeRenderer     mountainEdgeRenderer;
    private final TerrainSymbolRenderer    terrainSymbolRenderer;
    private       ArmyRenderer             armyRenderer;
    private       NobleArmyRenderer        nobleArmyRenderer;
    private       BarbArmyRenderer         barbArmyRenderer;
    private       main.barbarians.BarbArmyManager barbArmyManagerRef;
    private       NobleArmy                selectedNobleArmy = null;
    private MapViewMode viewMode = MapViewMode.SETTLEMENT;

    public MapRenderer(ZoneManager zoneManager,
                       ZoneDecorationRegistry decorationRegistry,
                       WorldGeography worldGeography,
                       NobleHouseManager nobleHouseManager) {
        this.zoneManager           = zoneManager;
        this.decorationRegistry    = decorationRegistry;
        this.worldGeography        = worldGeography;
        this.nobleHouseManager     = nobleHouseManager;
        this.seaRenderer           = new SeaRenderer(worldGeography);
        this.riverRenderer         = new RiverRenderer(worldGeography);
        this.mountainEdgeRenderer  = new MountainEdgeRenderer(decorationRegistry);
        this.terrainSymbolRenderer = new TerrainSymbolRenderer(decorationRegistry);
    }

    public void setArmyRenderer(ArmyRenderer armyRenderer) {
        this.armyRenderer = armyRenderer;
    }

    public void setNobleArmyRenderer(NobleArmyRenderer nobleArmyRenderer) {
        this.nobleArmyRenderer = nobleArmyRenderer;
    }

    public void setBarbArmyRenderer(BarbArmyRenderer barbArmyRenderer) {
        this.barbArmyRenderer = barbArmyRenderer;
    }

    public BarbArmyRenderer getBarbArmyRenderer() { return barbArmyRenderer; }

    public void setBarbArmyManager(main.barbarians.BarbArmyManager bam) {
        this.barbArmyManagerRef = bam;
    }

    private main.barbarians.BarbArmyManager barbArmyManager() { return barbArmyManagerRef; }

    public void setSelectedNobleArmy(NobleArmy army) {
        this.selectedNobleArmy = army;
    }

    public NobleArmyRenderer getNobleArmyRenderer() {
        return nobleArmyRenderer;
    }

    public void         setViewMode(MapViewMode mode) { this.viewMode = mode; }
    public MapViewMode  getViewMode()                 { return viewMode; }

    public void render(Graphics2D g2, Zone selected, Zone hovered, Army selectedArmy) {
        drawBackground(g2);

        // Layer 1 — sea
        seaRenderer.render(g2);

        // Layer 2 — zone fills
        for (Zone zone : zoneManager.getZones()) {
            drawZoneFill(g2, zone, selected, hovered);
        }

        // Layer 3 — mountain edges (on top of zone fills, before labels)
        for (Zone zone : zoneManager.getZones()) {
            mountainEdgeRenderer.render(g2, zone);
        }

        // Layer 4 — rivers
        riverRenderer.render(g2);

        // Layer 5 — terrain symbols
        for (Zone zone : zoneManager.getZones()) {
            terrainSymbolRenderer.render(g2, zone);
        }

        // Layer 6 — settlement icons and labels
        for (Zone zone : zoneManager.getZones()) {
            SettlementIconRenderer.drawSettlementIcon(g2, zone, COLOR_LABEL_SHADOW, COLOR_GOLD_TEXT);
            drawZoneLabels(g2, zone);
        }

        // Layer 7 — zone borders (drawn last so they're crisp on top)
        for (Zone zone : zoneManager.getZones()) {
            drawZoneBorder(g2, zone, selected);
        }

        // Layer 8 — player armies
        if (armyRenderer != null) {
            armyRenderer.render(g2, selectedArmy);
        }

        // Layer 9 — noble armies
        if (nobleArmyRenderer != null) {
            nobleArmyRenderer.render(g2, selectedNobleArmy);
        }

        // Layer 10 — barbarian armies
        if (barbArmyRenderer != null) {
            barbArmyRenderer.render(g2);
        }
    }

private void drawInnerGlow(Graphics2D g2, Polygon poly, Color color) {
    Shape old = g2.getClip();
    g2.setClip(poly);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    for (int i = 0; i < INNER_GLOW_LAYERS; i++) {
        float t     = (float) i / INNER_GLOW_LAYERS;
        int   alpha = (int) (INNER_GLOW_ALPHA_CAP * (1f - t));
        float width = INNER_GLOW_MAX_WIDTH * (1f - t);
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g2.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawPolygon(poly);
    }
    g2.setStroke(new BasicStroke(1f));
    g2.setClip(old);
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
        g2.fillRect(-200, -200, 2000, 1200);
    }

    // Injected by MapPanel so renderer can show ravaged overlay
    private main.barbarians.RavagedZoneManager ravagedZoneManager;

    public void setRavagedZoneManager(main.barbarians.RavagedZoneManager rzm) {
        this.ravagedZoneManager = rzm;
    }

public void setNobleHouseManager(main.nobles.NobleHouseManager nhm) {
        // NobleHouseManager is final in renderer — update the reference via field
        try {
            java.lang.reflect.Field f = MapRenderer.class.getDeclaredField("nobleHouseManager");
            f.setAccessible(true);
            f.set(this, nhm);
        } catch (Exception ignored) {}
    }

private static final Color COLOR_RAVAGED_OVERLAY         = new Color(160,  60,  10,  80);
    private static final Color COLOR_HEAVILY_RAVAGED_OVERLAY = new Color(120,  10,  10, 120);

    private void drawZoneFill(Graphics2D g2, Zone zone, Zone selected, Zone hovered) {
        Polygon   poly  = new Polygon(zone.getPolyX(), zone.getPolyY(), zone.getPolyX().length);
        ZoneState state = zoneManager.getState(zone.getId());

        if (zone.isDesolate()) {
            Color base = zone == selected ? COLOR_DESOLATE.brighter() : COLOR_DESOLATE;
            g2.setColor(base);
            g2.fillPolygon(poly);
            if (zone == hovered && zone != selected) {
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillPolygon(poly);
            }
            return;
        }
    switch (viewMode) {
        case POLITICAL -> {
            NobleHouse owner = nobleHouseManager.getOwnerOfZone(zone.getId());
            boolean isBarbarian = owner == null
                    && barbArmyManagerRef != null
                    && !barbArmyManagerRef.getGarrisonsInZone(zone.getId()).isEmpty();
            Color primary;
            Color secondary;
            if (isBarbarian) {
                primary   = zone == selected ? new Color(40, 30, 30).brighter() : new Color(20, 10, 10);
                secondary = new Color(80, 20, 20);
            } else if (owner != null) {
                primary   = NobleHouseColors.getPrimary(owner.getId());
                secondary = NobleHouseColors.getSecondary(owner.getId());
                if (zone == selected) { primary = primary.brighter(); secondary = secondary.brighter(); }
            } else {
                primary   = zone == selected ? new Color(60, 55, 70).brighter() : new Color(60, 55, 70);
                secondary = new Color(80, 75, 90);
            }
            g2.setColor(primary);
            g2.fillPolygon(poly);
            drawInnerGlow(g2, poly, secondary);
        }
        case PHYSICAL -> {
            ZoneDecoration dec = decorationRegistry.get(zone.getId());
            Color base = switch (dec.getSymbol()) {
                case FOREST   -> TERRAIN_FOREST;
                case VOLCANO  -> TERRAIN_VOLCANO;
                case ICE      -> TERRAIN_ICE;
                case FARMLAND -> TERRAIN_FARMLAND;
                case MARSH    -> TERRAIN_MARSH;
                case COASTAL  -> TERRAIN_COASTAL;
                case MOUNTAIN -> TERRAIN_MOUNTAIN;
                case NONE     -> TERRAIN_NONE;
            };
            if (zone == selected) base = base.brighter();
            g2.setColor(base);
            g2.fillPolygon(poly);
        }
        case SETTLEMENT -> {
            Color base = switch (zone.getSettlement()) {
                case CAPITAL  -> COLOR_CAPITAL;
                case TOWN     -> COLOR_TOWN;
                case VILLAGE  -> COLOR_VILLAGE;
                case DESOLATE -> COLOR_DESOLATE;
            };
            if (zone == selected) base = base.brighter();
            g2.setColor(base);
            g2.fillPolygon(poly);
        }
    }

    if (zone == hovered && zone != selected) {
        g2.setColor(COLOR_HOVER);
        g2.fillPolygon(poly);
    }

    if (state != null && state.getDamage() > 0) {
        g2.setColor(new Color(180, 30, 30, 100));
        g2.fillPolygon(poly);
    }

    // Ravaged overlay
    if (ravagedZoneManager != null) {
        main.barbarians.RavagedZoneManager.RavagedLevel lvl =
                ravagedZoneManager.getLevel(zone.getId());
        if (lvl == main.barbarians.RavagedZoneManager.RavagedLevel.HEAVILY_RAVAGED) {
            g2.setColor(COLOR_HEAVILY_RAVAGED_OVERLAY);
            g2.fillPolygon(poly);
        } else if (lvl == main.barbarians.RavagedZoneManager.RavagedLevel.RAVAGED) {
            g2.setColor(COLOR_RAVAGED_OVERLAY);
            g2.fillPolygon(poly);
        }
    }
}

private void drawZoneBorder(Graphics2D g2, Zone zone, Zone selected) {
    Polygon poly = new Polygon(zone.getPolyX(), zone.getPolyY(), zone.getPolyX().length);
    if (zone.isDesolate()) {
        g2.setColor(zone == selected ? COLOR_DESOLATE_BORDER.brighter() : COLOR_DESOLATE_BORDER);
        g2.setStroke(new BasicStroke(1.5f));
    } else {
        g2.setColor(zone == selected ? COLOR_BORDER_SEL : COLOR_BORDER);
        g2.setStroke(new BasicStroke(zone == selected ? 3f : 2f));
    }
    g2.drawPolygon(poly);
    g2.setStroke(new BasicStroke(1f));
}

private void drawZoneLabels(Graphics2D g2, Zone zone) {
    if (zone.isDesolate()) {
        // Minimal italic label, muted colour
        g2.setFont(new Font("Serif", Font.ITALIC, 11));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(zone.getDisplayName());
        int lx = zone.getLabelX();
        int ly = zone.getLabelY() + ICON_LABEL_OFFSET;
        g2.setColor(new Color(10, 8, 8, 160));
        g2.drawString(zone.getDisplayName(), lx - tw / 2 + 1, ly + 1);
        g2.setColor(new Color(110, 105, 100, 200));
        g2.drawString(zone.getDisplayName(), lx - tw / 2, ly);
        return;
    }

    ZoneDecoration dec     = decorationRegistry.get(zone.getId());
    boolean        hasIcon = dec.getSymbol() != ZoneDecoration.TerrainSymbol.NONE;

    int lx = zone.getLabelX();
    int ly = zone.getLabelY() + ICON_LABEL_OFFSET;
    int nameX = hasIcon ? lx + 6 : lx;

    g2.setFont(FONT_ZONE_NAME);
    FontMetrics fm = g2.getFontMetrics();
    int tw = fm.stringWidth(zone.getDisplayName());

    g2.setColor(COLOR_LABEL_SHADOW);
    g2.drawString(zone.getDisplayName(), nameX - tw / 2 - 1, ly + 1);
    g2.setColor(COLOR_LABEL);
    g2.drawString(zone.getDisplayName(), nameX - tw / 2, ly);

    g2.setFont(FONT_ZONE_STATS);
    fm = g2.getFontMetrics();
    String gold = "\u2666 " + zone.getGoldProduction();
    String food = "\u2663 " + zone.getFoodProduction();

    g2.setColor(COLOR_GOLD_TEXT);
    g2.drawString(gold, lx - 20, ly + 14);
    g2.setColor(COLOR_FOOD_TEXT);
    g2.drawString(food, lx + 10, ly + 14);
}

}