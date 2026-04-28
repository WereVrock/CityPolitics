// main/army/ArmyManager.java
package main.army;

import main.map.ZoneManager;
import main.parameters.GameParameters;

import java.util.*;

/**
 * Owns all armies. Processes per-turn movement ticks.
 * Issues orders with a delay based on messenger travel time from the capital.
 */
public class ArmyManager {

    private static final String CAPITAL_ID = "heartland";

    private final ZoneManager       zoneManager;
    private final List<Army>        armies     = new ArrayList<>();
    private final Map<String, Army> armyById   = new LinkedHashMap<>();

    public ArmyManager(ZoneManager zoneManager) {
        this.zoneManager = zoneManager;
        spawnStartingArmies();
    }

    // ─── Setup ────────────────────────────────────────────────────────────────

    private void spawnStartingArmies() {
        addArmy(new Army("army_1", CAPITAL_ID));
    }

    private void addArmy(Army army) {
        armies.add(army);
        armyById.put(army.getId(), army);
    }

    // ─── Access ───────────────────────────────────────────────────────────────

    public List<Army> getArmies()            { return Collections.unmodifiableList(armies); }
    public Army       getArmy(String id)     { return armyById.get(id); }

    public Army armyInZone(String zoneId) {
        for (Army a : armies) {
            if (a.getZoneId().equals(zoneId)) return a;
        }
        return null;
    }

    // ─── Order issuing ────────────────────────────────────────────────────────

    /**
     * Issues a move order to the army.
     * Delay = ceil(distance from capital to army / MESSAGE_SPEED).
     * Returns a human-readable result string.
     */
    public String issueMoveOrder(Army army, String targetZoneId) {
        if (!zoneManager.hasZone(targetZoneId)) return "Unknown target zone.";

        // Check adjacency chain feasibility — simplified: just check target is a real zone
        int distFromCapital = zoneManager.distance(CAPITAL_ID, army.getZoneId());
        int messageDelay    = (int) Math.ceil((double) distFromCapital / GameParameters.ARMY_MESSAGE_SPEED);

        PendingOrder order = new PendingOrder(PendingOrder.OrderType.MOVE_TO, targetZoneId, messageDelay);
        army.enqueueOrder(order);

        String armyZoneName  = zoneName(army.getZoneId());
        String targetZoneName = zoneName(targetZoneId);

        if (messageDelay == 0) {
            return army.getId() + " ordered to march to " + targetZoneName + " (immediate).";
        }
        return army.getId() + " ordered to march to " + targetZoneName
             + " — messenger will arrive in " + messageDelay + " turn(s) (army at " + armyZoneName + ").";
    }

    // ─── Turn processing ──────────────────────────────────────────────────────

    /**
     * Called once per turn. Resets movement, delivers orders, executes moves.
     * Returns log lines.
     */
    public List<String> processTurn() {
        List<String> log = new ArrayList<>();

        for (Army army : armies) {
            army.resetMoves(GameParameters.ARMY_MOVES_PER_TURN);

            PendingOrder delivered = army.tickOrders();
            if (delivered != null) {
                log.addAll(executeOrder(army, delivered));
            }
        }
        return log;
    }

    private List<String> executeOrder(Army army, PendingOrder order) {
        List<String> log = new ArrayList<>();
        if (order.getType() == PendingOrder.OrderType.MOVE_TO) {
            String target = order.getTargetZoneId();
            // Move step-by-step along BFS path, consuming moves
            List<String> path = bfsPath(army.getZoneId(), target);
            if (path == null || path.size() <= 1) {
                log.add("⚔ " + army.getId() + " order received but already at destination.");
                return log;
            }
            int steps = 0;
            for (int i = 1; i < path.size() && army.getMovesRemaining() > 0; i++) {
                army.moveTo(path.get(i));
                steps++;
            }
            String loc = zoneName(army.getZoneId());
            if (army.getZoneId().equals(target)) {
                log.add("⚔ " + army.getId() + " has reached " + loc + ".");
            } else {
                log.add("⚔ " + army.getId() + " marching — now at " + loc
                        + " (" + steps + " zone(s) moved). Order continues next turn.");
                // Re-enqueue remainder
                army.enqueueOrder(new PendingOrder(PendingOrder.OrderType.MOVE_TO, target, 0));
            }
        }
        return log;
    }

    // ─── BFS path ─────────────────────────────────────────────────────────────

    /** Returns ordered list of zone ids from start to end inclusive, or null if unreachable. */
    public List<String> bfsPath(String from, String to) {
        if (from.equals(to)) return List.of(from);
        Map<String, String> parent = new LinkedHashMap<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(from);
        parent.put(from, null);

        while (!queue.isEmpty()) {
            String cur = queue.poll();
            var zone = zoneManager.getZone(cur);
            if (zone == null) continue;
            for (String nb : zone.getAdjacentIds()) {
                if (!parent.containsKey(nb)) {
                    parent.put(nb, cur);
                    if (nb.equals(to)) {
                        return buildPath(parent, from, to);
                    }
                    queue.add(nb);
                }
            }
        }
        return null;
    }

    private List<String> buildPath(Map<String, String> parent, String from, String to) {
        LinkedList<String> path = new LinkedList<>();
        String cur = to;
        while (cur != null) {
            path.addFirst(cur);
            cur = parent.get(cur);
        }
        return path;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String zoneName(String id) {
        var z = zoneManager.getZone(id);
        return z != null ? z.getDisplayName() : id;
    }

    // ─── Save/load support ────────────────────────────────────────────────────

    public void reset() {
        armies.clear();
        armyById.clear();
        spawnStartingArmies();
    }
}