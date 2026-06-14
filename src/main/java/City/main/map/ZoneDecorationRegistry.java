// ZoneDecorationRegistry.java
package City.main.map;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds and owns all ZoneDecoration instances keyed by zone ID.
 * This is the single source of truth for zone visual data.
 */
public class ZoneDecorationRegistry {

    private final Map<String, ZoneDecoration> decorations = new LinkedHashMap<>();

    public ZoneDecorationRegistry() {
        build();
    }

    public ZoneDecoration get(String zoneId) {
        return decorations.getOrDefault(zoneId,
            new ZoneDecoration(ZoneDecoration.TerrainSymbol.NONE));
    }

    private void put(String id, ZoneDecoration.TerrainSymbol symbol, List<int[]> edges) {
        decorations.put(id, new ZoneDecoration(symbol, edges));
    }

    private void put(String id, ZoneDecoration.TerrainSymbol symbol) {
        decorations.put(id, new ZoneDecoration(symbol));
    }

private void build() {
    put("heartland",      ZoneDecoration.TerrainSymbol.NONE);
    put("snowmarch",      ZoneDecoration.TerrainSymbol.ICE,
        List.of(new int[]{2, 3}));
    put("northern_vale",  ZoneDecoration.TerrainSymbol.FOREST);
    put("far_north",      ZoneDecoration.TerrainSymbol.ICE);
    put("iceveil_tundra", ZoneDecoration.TerrainSymbol.ICE);
    put("westgate",       ZoneDecoration.TerrainSymbol.COASTAL);
    put("frostpeak_pass", ZoneDecoration.TerrainSymbol.MOUNTAIN,
        List.of(new int[]{0, 1}, new int[]{1, 2}, new int[]{3, 0}));
    put("eastern_plains", ZoneDecoration.TerrainSymbol.FARMLAND);
    put("ashfield",       ZoneDecoration.TerrainSymbol.FARMLAND);
    put("trade_coast",    ZoneDecoration.TerrainSymbol.COASTAL);
    put("stonepass",      ZoneDecoration.TerrainSymbol.MOUNTAIN,
        List.of(new int[]{0, 1}, new int[]{1, 2}, new int[]{2, 3}));
    put("highland_gap",   ZoneDecoration.TerrainSymbol.MOUNTAIN,
        List.of(new int[]{5, 0}, new int[]{0, 1}));
    put("far_east",       ZoneDecoration.TerrainSymbol.COASTAL);
    put("greenvale",      ZoneDecoration.TerrainSymbol.FARMLAND);
    put("river_bend",     ZoneDecoration.TerrainSymbol.MARSH);
    put("southern_march", ZoneDecoration.TerrainSymbol.FARMLAND);
    put("duskfall",       ZoneDecoration.TerrainSymbol.FOREST);
    put("redcliff",       ZoneDecoration.TerrainSymbol.MOUNTAIN,
        List.of(new int[]{0, 1}, new int[]{1, 2}));
    put("thornwood",      ZoneDecoration.TerrainSymbol.FOREST,
        List.of(new int[]{0, 1}));
    put("bramblewood",    ZoneDecoration.TerrainSymbol.FOREST);
    put("ironhaven",      ZoneDecoration.TerrainSymbol.NONE);
    put("wetmarsh",       ZoneDecoration.TerrainSymbol.MARSH);
    put("ashenveil",      ZoneDecoration.TerrainSymbol.VOLCANO);
    put("port_reach",     ZoneDecoration.TerrainSymbol.COASTAL);
    put("saltmere",       ZoneDecoration.TerrainSymbol.COASTAL);

    // Desolate zones — no terrain symbol
    put("waste_northeast", ZoneDecoration.TerrainSymbol.NONE);
    put("waste_east",      ZoneDecoration.TerrainSymbol.NONE);
    put("waste_se_upper",  ZoneDecoration.TerrainSymbol.NONE);
    put("waste_se_lower",  ZoneDecoration.TerrainSymbol.NONE);
    put("waste_southwest", ZoneDecoration.TerrainSymbol.NONE);
    put("waste_farSW",     ZoneDecoration.TerrainSymbol.NONE);
}

}