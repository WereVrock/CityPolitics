// WorldGeography.java
package City.main.map;

import java.util.List;

/**
 * Owns all world-level geographic features: rivers and sea regions.
 * Purely data — no rendering logic.
 * When rivers/sea become mechanical, add behavior here.
 */
public class WorldGeography {

    public static class River {
        private final String  name;
        private final int[][] waypoints; // {x, y} pairs
        private final int     labelX;
        private final int     labelY;

        public River(String name, int[][] waypoints, int labelX, int labelY) {
            this.name      = name;
            this.waypoints = waypoints;
            this.labelX    = labelX;
            this.labelY    = labelY;
        }

        public String  getName()      { return name; }
        public int[][] getWaypoints() { return waypoints; }
        public int     getLabelX()    { return labelX; }
        public int     getLabelY()    { return labelY; }
    }

    public static class SeaRegion {
        private final String name;
        private final int[]  polyX;
        private final int[]  polyY;
        private final int    labelX;
        private final int    labelY;

        public SeaRegion(String name, int[] polyX, int[] polyY, int labelX, int labelY) {
            this.name  = name;
            this.polyX = polyX;
            this.polyY = polyY;
            this.labelX = labelX;
            this.labelY = labelY;
        }

        public String getName()  { return name; }
        public int[]  getPolyX() { return polyX; }
        public int[]  getPolyY() { return polyY; }
        public int    getLabelX() { return labelX; }
        public int    getLabelY() { return labelY; }
    }

    private final List<River>     rivers;
    private final List<SeaRegion> seaRegions;

    public WorldGeography() {
        this.rivers     = buildRivers();
        this.seaRegions = buildSeaRegions();
    }

    public List<River>     getRivers()     { return rivers; }
    public List<SeaRegion> getSeaRegions() { return seaRegions; }

    // ─── River definitions ────────────────────────────────────────────────────

private static List<River> buildRivers() {
    return List.of(
        new River("Great North River", new int[][] {
            {320, 99}, {342, 192}, {316, 242}, {287, 272},
            {270, 320}, {243, 442}, {218, 529}, {307, 616}
        }, 304, 79),
        new River("Eastmark River", new int[][] {
            {614, 208}, {651, 216}, {680, 280}, {730, 340},
            {760, 420}, {780, 500}, {800, 570}
        }, 633, 214),
        new River("Marsh Creek", new int[][] {
            {500, 520}, {520, 580}, {530, 640}, {521, 672}
        }, 500, 520)
    );
}

// ─── Sea region definitions ───────────────────────────────────────────────

private static List<SeaRegion> buildSeaRegions() {
    return List.of(
        new SeaRegion("Western Sea",
            new int[] {4, 49, 94, 94, 80, 60, 0},
            new int[] {103, 102, 101, 166, 280, 420, 500},
            55, 261
        ),
        new SeaRegion("Northern Ice Sea",
            new int[] {0, 200, 340, 500, 600, 700, 707, 480, 320, 173, 96, 4},
            new int[] {0, 0, 0, 0, 0, 0, 90, 90, 99, 108, 100, 103},
            380, 23
        ),
        new SeaRegion("Eastern Sea",
            new int[] {1100, 1200, 1372, 787, 767},
            new int[] {100, 80, 497, 345, 216},
            0, 0
        ),
        new SeaRegion("Southern Sea",
            new int[] {0, 199, 398, 562, 800, 1000, 1200, 1200, 0},
            new int[] {700, 680, 683, 667, 640, 654, 680, 700, 700},
            0, 0
        )
    );
}

}