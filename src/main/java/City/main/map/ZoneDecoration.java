// ZoneDecoration.java
package City.main.map;

import java.util.Collections;
import java.util.List;

/**
 * Visual-only decoration data for a single zone.
 * No game logic here — purely for rendering.
 */
public class ZoneDecoration {

    public enum TerrainSymbol {
        NONE, FOREST, VOLCANO, ICE, FARMLAND, MARSH, COASTAL, MOUNTAIN
    }

    private final TerrainSymbol symbol;
    private final List<int[]>   mountainEdges; // each int[] is {vertexIndexA, vertexIndexB}

    public ZoneDecoration(TerrainSymbol symbol, List<int[]> mountainEdges) {
        this.symbol        = symbol;
        this.mountainEdges = mountainEdges;
    }

    public ZoneDecoration(TerrainSymbol symbol) {
        this(symbol, Collections.emptyList());
    }

    public TerrainSymbol  getSymbol()        { return symbol; }
    public List<int[]>    getMountainEdges() { return mountainEdges; }
    public boolean        hasMountainEdges() { return !mountainEdges.isEmpty(); }
}