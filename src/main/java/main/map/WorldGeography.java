// WorldGeography.java
package main.map;

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

        public River(String name, int[][] waypoints) {
            this.name      = name;
            this.waypoints = waypoints;
        }

        public String  getName()      { return name; }
        public int[][] getWaypoints() { return waypoints; }
    }

    public static class SeaRegion {
        private final String name;
        private final int[]  polyX;
        private final int[]  polyY;

        public SeaRegion(String name, int[] polyX, int[] polyY) {
            this.name  = name;
            this.polyX = polyX;
            this.polyY = polyY;
        }

        public String getName()  { return name; }
        public int[]  getPolyX() { return polyX; }
        public int[]  getPolyY() { return polyY; }
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
            {340, 0}, {330, 80}, {310, 160}, {290, 240},
            {270, 320}, {260, 400}, {255, 480}, {250, 560}
        }),
        new River("Eastmark River", new int[][] {
            {620, 220}, {680, 280}, {730, 340}, {760, 420},
            {780, 500}, {800, 570}
        }),
        new River("Marsh Creek", new int[][] {
            {500, 520}, {520, 580}, {530, 640}, {520, 700}
        })
    );
}

// ─── Sea region definitions ───────────────────────────────────────────────

private static List<SeaRegion> buildSeaRegions() {
    return List.of(
        new SeaRegion("Western Sea",
            new int[] {4,   94,  94,  80,  60,  0  },
            new int[] {103, 101, 166, 280, 420, 500 }
        ),
        new SeaRegion("Northern Ice Sea",
            new int[] {0,   200, 340, 500, 600, 700, 707, 0  },
            new int[] {0,   0,   0,   0,   0,   0,   90,  100}
        ),
        new SeaRegion("Eastern Sea",
            new int[] {1100, 1200, 1372, 787, 767},
            new int[] {100,  80,   497,  345, 216}
        ),
        new SeaRegion("Southern Sea",
            new int[] {0,   199, 398, 562, 800, 1000, 1200, 1200, 0  },
            new int[] {700, 680, 683, 667, 640, 654,  680,  700,  700}
        )
    );
}

}