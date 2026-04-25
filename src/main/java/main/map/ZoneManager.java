// ZoneManager.java
package main.map;

import main.parameters.GameParameters;

import java.util.*;

/**
 * Owns all Zone definitions and their mutable ZoneState.
 * Provides BFS distance between zones.
 */
public class ZoneManager {

    private final List<Zone>              zones;
    private final Map<String, Zone>       zoneById;
    private final Map<String, ZoneState>  stateById;

    public ZoneManager() {
        this.zones     = buildZones();
        this.zoneById  = new LinkedHashMap<>();
        this.stateById = new LinkedHashMap<>();
        for (Zone z : zones) {
            zoneById.put(z.getId(), z);
            stateById.put(z.getId(), new ZoneState());
        }
    }

    public void reset() {
        for (ZoneState s : stateById.values()) s.reset();
    }

    // ─── Access ───────────────────────────────────────────────────────────────

    public List<Zone>  getZones()                  { return Collections.unmodifiableList(zones); }
    public Zone        getZone(String id)           { return zoneById.get(id); }
    public ZoneState   getState(String id)          { return stateById.get(id); }
    public boolean     hasZone(String id)           { return zoneById.containsKey(id); }

    // ─── BFS distance ────────────────────────────────────────────────────────

    /**
     * Returns the number of zone hops between two zone ids.
     * Returns Integer.MAX_VALUE if unreachable.
     */
    public int distance(String fromId, String toId) {
        if (fromId.equals(toId)) return 0;
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        Map<String, Integer> dist = new HashMap<>();
        queue.add(fromId);
        visited.add(fromId);
        dist.put(fromId, 0);
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            int d = dist.get(cur);
            Zone z = zoneById.get(cur);
            if (z == null) continue;
            for (String neighbour : z.getAdjacentIds()) {
                if (!visited.contains(neighbour)) {
                    visited.add(neighbour);
                    dist.put(neighbour, d + 1);
                    if (neighbour.equals(toId)) return d + 1;
                    queue.add(neighbour);
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    // ─── Zone definitions ─────────────────────────────────────────────────────

    private static List<Zone> buildZones() {
        List<Zone> list = new ArrayList<>();

        // Layout targets an 800×500 canvas.
        // Zones tile from west to east, north to south.
        // Capital (Heartland) sits in the centre-west.

        // ── HEARTLAND (capital) ───────────────────────────────────────────────
        list.add(new Zone(
            "heartland", "Heartland", Zone.SettlementType.CAPITAL,
            new int[]{160, 310, 330, 280, 200, 140},
            new int[]{180, 160, 280, 380, 390, 300},
            225, 275,
            GameParameters.ZONE_CAPITAL_GOLD, GameParameters.ZONE_CAPITAL_FOOD,
            GameParameters.ZONE_CAPITAL_POPS,
            List.of("northern_vale", "eastern_plains", "river_bend", "southern_march")
        ));

        // ── NORTHERN VALE (town) ──────────────────────────────────────────────
        list.add(new Zone(
            "northern_vale", "Northern Vale", Zone.SettlementType.TOWN,
            new int[]{100, 260, 310, 160},
            new int[]{60,  50,  160, 180},
            195, 105,
            GameParameters.ZONE_TOWN_GOLD, GameParameters.ZONE_TOWN_FOOD,
            GameParameters.ZONE_TOWN_POPS,
            List.of("heartland", "eastern_plains", "far_north")
        ));

        // ── FAR NORTH (village) ───────────────────────────────────────────────
        list.add(new Zone(
            "far_north", "Far North", Zone.SettlementType.VILLAGE,
            new int[]{260, 450, 440, 310},
            new int[]{50,  40,  150, 160},
            365, 95,
            GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD,
            GameParameters.ZONE_VILLAGE_POPS,
            List.of("northern_vale", "eastern_plains", "trade_coast")
        ));

        // ── EASTERN PLAINS (village) ──────────────────────────────────────────
        list.add(new Zone(
            "eastern_plains", "Eastern Plains", Zone.SettlementType.VILLAGE,
            new int[]{310, 440, 450, 420, 330, 280},
            new int[]{160, 150, 280, 330, 280, 380},
            375, 255,
            GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD,
            GameParameters.ZONE_VILLAGE_POPS,
            List.of("heartland", "northern_vale", "far_north", "trade_coast", "highland_gap", "river_bend")
        ));

        // ── TRADE COAST (town) ────────────────────────────────────────────────
        list.add(new Zone(
            "trade_coast", "Trade Coast", Zone.SettlementType.TOWN,
            new int[]{440, 600, 590, 500, 420},
            new int[]{150, 130, 270, 310, 280},
            510, 215,
            GameParameters.ZONE_TOWN_GOLD, GameParameters.ZONE_TOWN_FOOD,
            GameParameters.ZONE_TOWN_POPS,
            List.of("far_north", "eastern_plains", "highland_gap", "far_east")
        ));

        // ── HIGHLAND GAP (village) ────────────────────────────────────────────
        list.add(new Zone(
            "highland_gap", "Highland Gap", Zone.SettlementType.VILLAGE,
            new int[]{420, 500, 490, 430, 330},
            new int[]{330, 310, 430, 460, 380},
            430, 385,
            GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD,
            GameParameters.ZONE_VILLAGE_POPS,
            List.of("eastern_plains", "trade_coast", "far_east", "river_bend", "wetmarsh")
        ));

        // ── RIVER BEND (village) ──────────────────────────────────────────────
        list.add(new Zone(
            "river_bend", "River Bend", Zone.SettlementType.VILLAGE,
            new int[]{200, 280, 330, 430, 390, 260, 190},
            new int[]{390, 380, 380, 460, 490, 490, 460},
            310, 435,
            GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD,
            GameParameters.ZONE_VILLAGE_POPS,
            List.of("heartland", "eastern_plains", "highland_gap", "wetmarsh", "southern_march")
        ));

        // ── SOUTHERN MARCH (town) ─────────────────────────────────────────────
        list.add(new Zone(
            "southern_march", "Southern March", Zone.SettlementType.TOWN,
            new int[]{100, 200, 190, 140, 80},
            new int[]{300, 390, 460, 490, 440},
            145, 400,
            GameParameters.ZONE_TOWN_GOLD, GameParameters.ZONE_TOWN_FOOD,
            GameParameters.ZONE_TOWN_POPS,
            List.of("heartland", "river_bend")
        ));

        // ── WETMARSH (village) ────────────────────────────────────────────────
        list.add(new Zone(
            "wetmarsh", "Wetmarsh", Zone.SettlementType.VILLAGE,
            new int[]{260, 390, 430, 420, 330, 190},
            new int[]{490, 490, 460, 560, 560, 560},
            330, 520,
            GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD,
            GameParameters.ZONE_VILLAGE_POPS,
            List.of("river_bend", "highland_gap", "far_east", "port_reach")
        ));

        // ── FAR EAST (town) ───────────────────────────────────────────────────
        list.add(new Zone(
            "far_east", "Far East", Zone.SettlementType.TOWN,
            new int[]{500, 640, 650, 560, 490, 430},
            new int[]{310, 300, 450, 480, 430, 460},
            560, 385,
            GameParameters.ZONE_TOWN_GOLD, GameParameters.ZONE_TOWN_FOOD,
            GameParameters.ZONE_TOWN_POPS,
            List.of("trade_coast", "highland_gap", "wetmarsh", "port_reach")
        ));

        // ── PORT REACH (town) ─────────────────────────────────────────────────
        list.add(new Zone(
            "port_reach", "Port Reach", Zone.SettlementType.TOWN,
            new int[]{330, 420, 560, 650, 600, 420},
            new int[]{560, 560, 480, 450, 570, 580},
            490, 530,
            GameParameters.ZONE_TOWN_GOLD, GameParameters.ZONE_TOWN_FOOD,
            GameParameters.ZONE_TOWN_POPS,
            List.of("wetmarsh", "far_east")
        ));

        return list;
    }
}