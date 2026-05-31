package main.barbarians;

import main.army.Army;
import main.army.ArmyManager;
import main.calendar.GameCalendar;
import main.map.Zone;
import main.map.ZoneManager;
import main.nobles.NobleArmy;
import main.nobles.NobleArmyManager;
import main.nobles.NobleHouse;
import main.nobles.NobleHouseManager;
import main.parameters.GameParameters;
import main.resources.ResourcePool;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Drives all barbarian invasion logic each turn.
 * Called from TurnProcessor after noble processing.
 *
 * Responsibilities:
 *  - Countdown management
 *  - Warboss movement + raider splitting
 *  - Raider / ravager movement
 *  - Wave spawning (split across two turns)
 *  - Combat resolution (barb attacks zones)
 *  - Conquest (zone becomes ravaged + barbarian garrison)
 *  - Pay-off flag clearing
 *  - Game-over check
 */
public class BarbInvasionProcessor {

    private final BarbInvasionState   state;
    private final BarbArmyManager     armyManager;
    private final RavagedZoneManager  ravagedZones;
    private final ZoneManager         zoneManager;
    private final NobleHouseManager   nobleHouseManager;
    private final ArmyManager         playerArmyManager;

    /** Callback fired when the player must be asked to pay off a barbarian army. */
    public interface PayOffCallback {
        /** @return true if player chose to pay */
        boolean askPlayerPayOff(BarbArmy army, ResourcePool resources);
    }

    /** Callback fired when the game should end. */
    public interface GameOverCallback {
        void triggerGameOver(String reason);
    }

    private PayOffCallback   payOffCallback;
    private GameOverCallback gameOverCallback;

    private final Random rng = new Random();

    public BarbInvasionProcessor(BarbInvasionState state,
                                  BarbArmyManager armyManager,
                                  RavagedZoneManager ravagedZones,
                                  ZoneManager zoneManager,
                                  NobleHouseManager nobleHouseManager,
                                  ArmyManager playerArmyManager) {
        this.state             = state;
        this.armyManager       = armyManager;
        this.ravagedZones      = ravagedZones;
        this.zoneManager       = zoneManager;
        this.nobleHouseManager = nobleHouseManager;
        this.playerArmyManager = playerArmyManager;
    }

    public void setPayOffCallback(PayOffCallback cb)   { this.payOffCallback   = cb; }
    public void setGameOverCallback(GameOverCallback cb){ this.gameOverCallback = cb; }

    // ─── Main entry ──────────────────────────────────────────────────────────

    public List<String> processTurn(GameCalendar calendar, ResourcePool playerResources) {
        List<String> log = new ArrayList<>();

        ravagedZones.tick();
        clearPaidOffFlags();

        int absoluteTurn = calendar.getTotalTurnsElapsed();

        // Countdown phase
        if (state.isCountdown()) {
            if (state.tickCountdown()) {
                log.addAll(startInvasion(absoluteTurn, calendar));
            }
            return log;
        }

        if (!state.isActive()) return log;

        state.tickInvasion();

        // Second half of a wave spawns before anything moves
        if (state.isWaveSecondHalfDue()) {
            log.addAll(spawnWaveHalf(false));
            state.markSecondHalfSpawned();
            state.scheduleNextWave(absoluteTurn);
        }

        // Warboss splits raider each turn
        log.addAll(splitRaiderFromWarboss(absoluteTurn));

        // Move all mobile armies
        log.addAll(moveArmies());

        // Resolve combat in each zone
        log.addAll(resolveCombat(playerResources));

        // Check first-half wave spawn
        if (state.isWaveDue(absoluteTurn)) {
            log.addAll(spawnWaveHalf(true));
            state.markFirstHalfSpawned();
        }

        // Noble recapture attempts — nobles with armies in barb-garrisoned zones fight back
        log.addAll(resolveNobleRecaptures());

        // Check invasion destroyed
        if (armyManager.isInvasionDestroyed()) {
            state.markDestroyed();
            log.add("✦ The barbarian invasion has been defeated! Peace returns to the realm.");
            state.resetCountdown();
        }

        return log;
    }

    // ─── Invasion start ──────────────────────────────────────────────────────

    private List<String> startInvasion(int absoluteTurn, GameCalendar calendar) {
        List<String> log = new ArrayList<>();
        String spawnZone = pickSpawnZone();
        if (spawnZone == null) {
            log.add("⚠ Barbarian invasion delayed — no desolate spawn zone found.");
            return log;
        }
        BarbArmy warboss = armyManager.spawnWarboss(spawnZone, calendar.getTotalTurnsElapsed());
        state.startInvasion(absoluteTurn);
        log.add("☠ THE BARBARIAN HORDE DESCENDS! The Warboss (" + warboss.getSize()
                + " warriors) emerges from " + spawnZone + "!");

        // First wave: raiders + warboss (warboss IS the first wave's ravager slot)
        log.addAll(spawnWaveHalf(true));
        state.markFirstHalfSpawned();

        return log;
    }

    // ─── Wave spawning ───────────────────────────────────────────────────────

    private List<String> spawnWaveHalf(boolean firstHalf) {
        List<String> log  = new ArrayList<>();
        int raidersToSpawn  = GameParameters.BARB_WAVE_RAIDER_COUNT;
        int ravagersToSpawn = GameParameters.BARB_WAVE_RAVAGER_COUNT;

        // First half gets ceil, second gets floor
        int rCount = firstHalf ? (raidersToSpawn + 1) / 2 : raidersToSpawn / 2;
        int vCount = firstHalf ? (ravagersToSpawn + 1) / 2 : ravagersToSpawn / 2;

        List<String> desolateZones = getDesolateZoneIds();
        if (desolateZones.isEmpty()) return log;

        for (int i = 0; i < rCount; i++) {
            String z = desolateZones.get(rng.nextInt(desolateZones.size()));
            BarbArmy raider = armyManager.spawnRaider(z, GameParameters.BARB_WAVE_RAIDER_SIZE);
            log.add("☠ Barbarian raiders (" + raider.getSize() + ") emerge at " + z + ".");
        }
        for (int i = 0; i < vCount; i++) {
            String z = desolateZones.get(rng.nextInt(desolateZones.size()));
            BarbArmy ravager = armyManager.spawnRavager(z, GameParameters.BARB_WAVE_RAVAGER_SIZE);
            log.add("☠ Barbarian ravagers (" + ravager.getSize() + ") emerge at " + z + ".");
        }
        return log;
    }

    // ─── Raider splitting ────────────────────────────────────────────────────

    private List<String> splitRaiderFromWarboss(int absoluteTurn) {
        List<String> log = new ArrayList<>();
        BarbArmy wb = armyManager.getWarboss();
        if (wb == null) return log;

        int splitSize = Math.max(GameParameters.BARB_WARBOSS_RAIDER_MIN,
                (int)(wb.getSize() * GameParameters.BARB_WARBOSS_RAIDER_FRACTION));
        if (splitSize >= wb.getSize()) return log;

        wb.setSize(wb.getSize() - splitSize);
        BarbArmy raider = armyManager.spawnRaider(wb.getZoneId(), splitSize);
        log.add("☠ " + splitSize + " barbarian raiders break off from the Warboss.");
        return log;
    }

    // ─── Movement ────────────────────────────────────────────────────────────

    private List<String> moveArmies() {
        List<String> log = new ArrayList<>();

        BarbArmy wb = armyManager.getWarboss();
        if (wb != null && wb.canMoveThisTurn()) {
            String next = wb.getNextZoneId();
            if (next != null) {
                armyManager.moveArmy(wb, next);
                log.add("☠ The Warboss advances to " + next + ".");
            }
            // Pre-calculate next move for display
            String upcoming = pickWarbossNextZone(wb);
            wb.setNextZoneId(upcoming);
        }

        // Raiders and ravagers move randomly, prefer unvisited
        for (BarbArmy army : new ArrayList<>(armyManager.getMobileArmies())) {
            if (army.isWarboss() || army.isPaidOff()) continue;
            List<String> candidates = armyManager.getAdjacentMoveable(army.getZoneId());
            if (candidates.isEmpty()) continue;
            String dest = armyManager.pickPreferredZone(army, candidates);
            if (dest != null) {
                armyManager.moveArmy(army, dest);
            }
        }

        return log;
    }

    private String pickWarbossNextZone(BarbArmy wb) {
        List<String> candidates = armyManager.getAdjacentMoveable(wb.getZoneId());
        if (candidates.isEmpty()) return null;

        // 50% chance to pick unvisited detour
        if (rng.nextDouble() < GameParameters.BARB_WARBOSS_DETOUR_CHANCE) {
            List<String> unvisited = new ArrayList<>();
            for (String z : candidates) {
                if (!armyManager.getInvasionVisited().contains(z)) unvisited.add(z);
            }
            if (!unvisited.isEmpty()) return unvisited.get(rng.nextInt(unvisited.size()));
        }

        // Pathfind toward heartland: pick adjacent zone closest to heartland
        // Heartland has no real coordinates — use zone adjacency BFS depth as proxy
        return pickClosestToHeartland(candidates, wb.getZoneId());
    }

    /**
     * Picks the candidate zone that is fewest hops from heartland-adjacent zones.
     * Heartland-adjacent = zones that have "heartland" in adjacency list
     * or, as fallback, any non-desolate zone we haven't conquered yet.
     */
    private String pickClosestToHeartland(List<String> candidates, String currentZone) {
        // BFS from each candidate toward heartland
        String best      = null;
        int    bestDepth = Integer.MAX_VALUE;

        for (String candidate : candidates) {
            int depth = bfsDepthToHeartland(candidate, 12);
            if (depth < bestDepth) {
                bestDepth = depth;
                best      = candidate;
            }
        }
        return best != null ? best : candidates.get(rng.nextInt(candidates.size()));
    }

    private int bfsDepthToHeartland(String start, int maxDepth) {
        Queue<String> queue   = new LinkedList<>();
        Set<String>   visited = new HashSet<>();
        queue.add(start);
        visited.add(start);
        int depth = 0;
        while (!queue.isEmpty() && depth < maxDepth) {
            int size = queue.size();
            depth++;
            for (int i = 0; i < size; i++) {
                String curr = queue.poll();
                Zone   zone = zoneManager.getZone(curr);
                if (zone == null) continue;
                for (String adj : zone.getAdjacentIds()) {
                    if (adj.equals(Army.HEARTLAND_ID)) return depth;
                    if (!visited.contains(adj)) {
                        visited.add(adj);
                        queue.add(adj);
                    }
                }
            }
        }
        return maxDepth; // not found within limit
    }

    // ─── Combat ──────────────────────────────────────────────────────────────

    private List<String> resolveCombat(ResourcePool playerResources) {
        List<String> log = new ArrayList<>();

        // Group mobile armies by zone
        Map<String, List<BarbArmy>> byZone = new LinkedHashMap<>();
        for (BarbArmy army : armyManager.getMobileArmies()) {
            if (army.isPaidOff()) continue;
            byZone.computeIfAbsent(army.getZoneId(), k -> new ArrayList<>()).add(army);
        }

        for (Map.Entry<String, List<BarbArmy>> entry : byZone.entrySet()) {
            String zoneId = entry.getKey();

            // Check heartland
            if (Army.HEARTLAND_ID.equals(zoneId)) {
                log.add("☠ THE WARBOSS REACHES THE HEARTLAND! ALL IS LOST!");
                if (gameOverCallback != null) gameOverCallback.triggerGameOver("Barbarians reached the heartland.");
                return log;
            }

            Zone zone = zoneManager.getZone(zoneId);
            if (zone == null || zone.isDesolate()) continue;

            for (BarbArmy barb : new ArrayList<>(entry.getValue())) {
                if (!barb.isAlive()) continue;
                log.addAll(resolveCombatInZone(barb, zoneId, playerResources));
            }
        }

        armyManager.removeDeadArmies();
        return log;
    }

private List<String> resolveCombatInZone(BarbArmy barb, String zoneId,
                                          ResourcePool playerResources) {
    List<String> log = new ArrayList<>();

    NobleHouse owner = nobleHouseManager.getOwnerOfZone(zoneId);

    // Noble AI pay-off check
    if (owner != null) {
        if (shouldNoblePay(owner, barb)) {
            int goldCost = barb.getSize() * GameParameters.BARB_PAYOFF_GOLD_PER_MAN;
            owner.addGold(-Math.min(goldCost, owner.getGold()));
            barb.setPaidOff(true);
            log.add(owner.getName() + " pays off barbarians at " + zoneId
                    + " (" + goldCost + " gold). They stand down for one turn.");
            return log;
        }
    }

    // Collect player armies in this zone
    List<Army> playerArmies = new ArrayList<>();
    for (Army a : playerArmyManager.getDeployedArmies()) {
        if (zoneId.equals(a.getZoneId()) && a.isAlive()) playerArmies.add(a);
    }

    boolean hasNobleDefender  = owner != null;
    boolean hasPlayerDefender = !playerArmies.isEmpty();
    boolean hasDefenders      = hasNobleDefender || hasPlayerDefender;

    if (!hasDefenders) {
        log.addAll(conquerZone(barb, zoneId, null));
        return log;
    }

    // Ask player to pay off
    if (payOffCallback != null) {
        boolean playerPays = payOffCallback.askPlayerPayOff(barb, playerResources);
        if (playerPays) {
            barb.setPaidOff(true);
            log.add("Player pays off barbarians at " + zoneId + ". They stand down.");
            return log;
        }
    }

    // Combat
    if (hasNobleDefender) {
        BarbCombatHandler.BarbCombatResult result =
                BarbCombatHandler.barbAttacksZone(barb, owner, zoneId,
                        nobleHouseManager.getArmyManager(), log);
        if (result.attackerWon) {
            log.addAll(conquerZone(barb, zoneId, owner));
        }
    } else {
        BarbCombatHandler.BarbCombatResult result =
                BarbCombatHandler.barbAttacksPlayerZone(barb, playerArmies, zoneId, log);
        if (result.attackerWon) {
            log.addAll(conquerZone(barb, zoneId, null));
        }
    }

    return log;
}

// ─── Conquest ────────────────────────────────────────────────────────────

    private List<String> conquerZone(BarbArmy barb, String zoneId, NobleHouse previousOwner) {
        List<String> log = new ArrayList<>();

        // Strip ownership
        if (previousOwner != null) {
            previousOwner.removeZone(zoneId);
            log.add(previousOwner.getName() + " loses " + zoneId + " to the barbarians!");
        }

        // Mark ravaged
        if (barb.isWarboss()) {
            ravagedZones.markHeavilyRavaged(zoneId);
            log.add("☠ " + zoneId + " is heavily ravaged by the Warboss's horde!");
        } else {
            ravagedZones.markRavaged(zoneId);
            log.add("☠ " + zoneId + " is ravaged by the barbarians!");
        }

        // Leave garrison
        int garrisonSize = barb.isWarboss()
                ? GameParameters.BARB_WARBOSS_GARRISON_SIZE
                : GameParameters.BARB_RAVAGER_GARRISON_SIZE;
        garrisonSize = Math.min(garrisonSize, barb.getSize());

        if (garrisonSize > 0) {
            barb.setSize(barb.getSize() - garrisonSize);
            BarbArmy garrison = new BarbArmy(barb.getType(), garrisonSize, zoneId);
            garrison.makeGarrison();
            // Add garrison directly via reflection-free approach — expose package method
            armyManager.addGarrison(garrison);
            log.add(garrisonSize + " barbarians remain as garrison in " + zoneId + ".");
        }

        return log;
    }

    // ─── Noble AI pay-off decision ────────────────────────────────────────────

    private boolean shouldNoblePay(NobleHouse noble, BarbArmy barb) {
        // Estimate noble total strength in zone
        int garrisonSize = noble.getGarrisonFor(barb.getZoneId());
        int armySize     = 0;
        for (NobleArmy a : nobleHouseManager.getArmyManager()
                .getArmiesInZone(barb.getZoneId(), noble.getId())) {
            armySize += a.getSize();
        }
        int defTotal   = (int)((garrisonSize + armySize)
                       * (1.0 + GameParameters.BARB_DEFENDER_BONUS));
        int goldCost   = barb.getSize() * GameParameters.BARB_PAYOFF_GOLD_PER_MAN;

        // Pay off if: can't win AND can afford
        boolean canAfford = noble.getGold() >= goldCost;
        boolean canWin    = defTotal > barb.getSize();
        return canAfford && !canWin;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String pickSpawnZone() {
        List<String> desolate = getDesolateZoneIds();
        if (desolate.isEmpty()) return null;
        return desolate.get(rng.nextInt(desolate.size()));
    }

    private List<String> getDesolateZoneIds() {
        List<String> result = new ArrayList<>();
        for (Zone z : zoneManager.getZones()) {
            if (z.isDesolate()) result.add(z.getId());
        }
        return result;
    }

    private void clearPaidOffFlags() {
        for (BarbArmy a : armyManager.getMobileArmies()) {
            a.setPaidOff(false);
        }
    }

private List<String> resolveNobleRecaptures() {
    List<String> log = new ArrayList<>();
    List<BarbArmy> garrisons = new ArrayList<>();
    for (BarbArmy a : armyManager.getAllArmies()) {
        if (a.isAlive() && a.isGarrison()) garrisons.add(a);
    }
    for (BarbArmy garrison : new ArrayList<>(garrisons)) {
        String zoneId = garrison.getZoneId();
        for (NobleHouse noble : new ArrayList<>(nobleHouseManager.getHouses())) {
            if (noble.isEliminated()) continue;
            if (noble.getZoneIds().contains(zoneId)) continue;
            List<NobleArmy> atkArmies = nobleHouseManager.getArmyManager()
                    .getArmiesInZone(zoneId, noble.getId());
            if (atkArmies.isEmpty()) continue;
            BarbCombatHandler.BarbCombatResult result =
                    BarbCombatHandler.nobleAttacksBarbGarrison(
                            noble, garrison, zoneId,
                            nobleHouseManager.getArmyManager(), log);
            if (result.attackerWon) {
                nobleHouseManager.awardRecapturedZone(noble, zoneId);
                armyManager.remove(garrison);
                ravagedZones.markRavaged(zoneId);
            }
            break;
        }
    }
    return log;
}

public BarbInvasionState  getState()        { return state; }
    public BarbArmyManager    getArmyManager()  { return armyManager; }
    public RavagedZoneManager getRavagedZones() { return ravagedZones; }
}