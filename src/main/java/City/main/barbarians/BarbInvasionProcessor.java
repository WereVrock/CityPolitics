
package City.main.barbarians;

import City.main.army.Army;
import City.main.army.ArmyManager;
import City.main.calendar.GameCalendar;
import City.main.map.Zone;
import City.main.map.ZoneManager;
import City.main.map.ZoneState;
import City.main.nobles.NobleArmy;
import City.main.nobles.NobleArmyManager;
import City.main.nobles.NobleHouse;
import City.main.nobles.NobleHouseManager;
 
import City.main.resources.ResourcePool;
import City.debug.Debug;
import City.main.parameters.BarbarianParams;
import City.main.parameters.DiplomacyParams;
import City.main.parameters.NobleAIParams;

import java.util.*;

/**
 * Drives all barbarian invasion logic each turn.
 * Called from TurnProcessor after noble processing.
 *
 * Responsibilities:
 * - Countdown management
 * - Warboss movement + raider splitting
 * - Raider / ravager movement
 * - Wave spawning (split across two turns)
 * - Combat resolution (barb attacks zones)
 * - Conquest (zone becomes ravaged + barbarian garrison)
 * - Pay-off flag clearing
 * - Game-over check
 */
public class BarbInvasionProcessor {
    private final BarbInvasionState state;
    private final BarbArmyManager armyManager;
    private final RavagedZoneManager ravagedZones;
    private final ZoneManager zoneManager;
    private final NobleHouseManager nobleHouseManager;
    private final ArmyManager playerArmyManager;

    /**
     * Callback fired when the player must be asked to pay off a barbarian army.
     */
    public interface PayOffCallback {
        /**
         * @return true if player chose to pay
         */
        boolean askPlayerPayOff(BarbArmy army, ResourcePool resources, String zoneId, NobleHouse owner,
                                java.util.List<Army> playerArmies, java.util.List<NobleArmy> nobleArmies, int nobleGarrison);
    }

    /**
     * Callback fired when the game should end.
     */
    public interface GameOverCallback {
        void triggerGameOver(String reason);
    }

    private PayOffCallback payOffCallback;
    private GameOverCallback gameOverCallback;
    private final Random rng = new Random();

    public BarbInvasionProcessor(BarbInvasionState state, BarbArmyManager armyManager, RavagedZoneManager ravagedZones,
                                 ZoneManager zoneManager, NobleHouseManager nobleHouseManager, ArmyManager playerArmyManager) {
        this.state = state;
        this.armyManager = armyManager;
        this.ravagedZones = ravagedZones;
        this.zoneManager = zoneManager;
        this.nobleHouseManager = nobleHouseManager;
        this.playerArmyManager = playerArmyManager;
    }

    public void setPayOffCallback(PayOffCallback cb) {
        this.payOffCallback = cb;
    }

    public void setGameOverCallback(GameOverCallback cb) {
        this.gameOverCallback = cb;
    }

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

    boolean earlyInvasion = state.isEarlyInvasion();

    // Warboss size scaled down for early invasions
    int warbossSize = BarbarianParams.BARB_WARBOSS_BASE_SIZE
             + calendar.getTotalTurnsElapsed() * BarbarianParams.BARB_WARBOSS_SIZE_PER_TURN;
    if (earlyInvasion) {
        warbossSize = (int)(warbossSize * BarbarianParams.BARB_EARLY_WAVE_SIZE_FRACTION);
        warbossSize = Math.max(30, warbossSize);
    }

    BarbArmy warboss = armyManager.spawnWarbossWithSize(spawnZone, warbossSize);
    log("invasion-start", "Warboss spawned at " + spawnZone + ", size=" + warboss.getSize()
            + ", name=" + warboss.getDisplayName()
            + ", earlyInvasion=" + earlyInvasion);
    state.startInvasion(absoluteTurn);

    if (earlyInvasion) {
        log.add("☠ FLEEING TRIBES POUR FROM THE NORTHERN WASTES!");
        log.add("  " + warboss.getDisplayName() + " (" + warboss.getSize()
                + " warriors) — DRIVEN SOUTH BY THE FROST GIANTS, MOVING FAST!");
        log.add("  These are survivors, not conquerors. Desperate and swift.");
    } else {
        log.add("☠ THE BARBARIAN HORDE DESCENDS FROM THE NORTH!");
        log.add("  " + warboss.getDisplayName() + " (" + warboss.getSize()
                + " warriors) — DRIVEN SOUTH BY THE FROST GIANTS!");
        log.add("  The barbarians do not come to conquer. They come because something worse follows.");
    }

    // First wave
    log.addAll(spawnWaveHalf(true));
    state.markFirstHalfSpawned();
    log("invasion-start", "First wave half spawned");

    return log;
}

private void log(String key, String msg) {
        Debug.log("barbarians", key, msg);
    }

    // ─── Wave spawning ───────────────────────────────────────────────────────

private List<String> spawnWaveHalf(boolean firstHalf) {
    List<String> log = new ArrayList<>();
    int raidersToSpawn  = BarbarianParams.BARB_WAVE_RAIDER_COUNT;
    int ravagersToSpawn = BarbarianParams.BARB_WAVE_RAVAGER_COUNT;

    int rCount = firstHalf ? (raidersToSpawn + 1) / 2 : raidersToSpawn / 2;
    int vCount = firstHalf ? (ravagersToSpawn + 1) / 2 : ravagersToSpawn / 2;

    // Early invasion = all armies are small/fast; early wave = same within normal invasion
    boolean earlyInvasion = state.isEarlyInvasion();
    boolean earlyWave     = earlyInvasion || state.isEarlyWave();

    double sizeFraction = earlyWave
            ? BarbarianParams.BARB_EARLY_WAVE_SIZE_FRACTION : 1.0;

    List<String> desolateZones = getDesolateZoneIds();
    if (desolateZones.isEmpty()) return log;

    String waveType = earlyInvasion ? "Fleeing tribe"
            : earlyWave ? "Fleeing warband" : "Warband";

    for (int i = 0; i < rCount; i++) {
        String z    = desolateZones.get(rng.nextInt(desolateZones.size()));
        int    size = (int)(BarbarianParams.BARB_WAVE_RAIDER_SIZE * sizeFraction);
        size = Math.max(5, size);
        BarbArmy raider = armyManager.spawnRaider(z, size, earlyWave);
        log.add("☠ " + waveType + " — " + raider.getDisplayName()
                + " (" + raider.getSize() + " warriors) emerge at " + z + ".");
        if (earlyInvasion) {
            log.add("  (Driven south by the Frost Giants — fast, light, desperate survivors.)");
        } else if (earlyWave) {
            log.add("  (Driven south by the Frost Giants, these survivors travel fast and light.)");
        }
    }

    for (int i = 0; i < vCount; i++) {
        String z    = desolateZones.get(rng.nextInt(desolateZones.size()));
        int    size = (int)(BarbarianParams.BARB_WAVE_RAVAGER_SIZE * sizeFraction);
        size = Math.max(10, size);
        BarbArmy ravager = armyManager.spawnRavager(z, size, earlyWave);
        log.add("☠ " + waveType + " — " + ravager.getDisplayName()
                + " (" + ravager.getSize() + " warriors) emerge at " + z + ".");
    }

    return log;
}

// ─── Raider splitting ────────────────────────────────────────────────────

private List<String> splitRaiderFromWarboss(int absoluteTurn) {
        List<String> log = new ArrayList<>();
        BarbArmy wb = armyManager.getWarboss();
        if (wb == null) return log;

        int splitSize = Math.max(BarbarianParams.BARB_WARBOSS_RAIDER_MIN,
                (int) (wb.getSize() * BarbarianParams.BARB_WARBOSS_RAIDER_FRACTION));
        if (splitSize >= wb.getSize()) return log;

        wb.setSize(wb.getSize() - splitSize);
        armyManager.spawnRaider(wb.getZoneId(), splitSize);

        // Log warboss size after split (before any movement merges)
        log("raider-split", "Split " + splitSize + " from warboss at " + wb.getZoneId()
                + " — warboss now " + wb.getSize()
                + " (note: may increase if warboss merges with raiders on move)");
        log.add("☠ " + splitSize + " barbarian raiders break off from the Warboss.");

        return log;
    }

// ─── Movement ────────────────────────────────────────────────────────────

private List<String> moveArmies() {
    List<String> log = new ArrayList<>();

    BarbArmy wb = armyManager.getWarboss();
    if (wb != null) {
        // Warboss moves EVERY turn (no cooldown — driven by terror)
        String next = wb.getNextZoneId();
        if (next != null) {
            String oldZone = wb.getZoneId();
            armyManager.moveArmy(wb, next);
            if (!next.equals(oldZone)) {
                log.add("☠ " + wb.getDisplayName() + " advances to " + next + ".");
                log("warboss-move", "Warboss moved from " + oldZone + " to " + next);
            }
        }
        // Pre-calculate next move
        String upcoming = pickWarbossNextZone(wb);
        wb.setNextZoneId(upcoming);
        log("warboss-next", "Next destination set to " + upcoming);
    }

    // Early-wave raiders move faster (every turn)
    // Standard raiders/ravagers move normally
    for (BarbArmy army : new ArrayList<>(armyManager.getMobileArmies())) {
        if (army.isWarboss() || army.isPaidOff()) continue;

        List<String> candidates = armyManager.getAdjacentMoveable(army.getZoneId());
        if (candidates.isEmpty()) continue;

        String dest = armyManager.pickPreferredZone(army, candidates);
        if (dest != null) {
            String oldZone = army.getZoneId();
            armyManager.moveArmy(army, dest);
            if (!dest.equals(oldZone)) {
                log("barb-move", army.getDisplayName() + " moved from " + oldZone + " to " + dest);
            }
        }
    }

    return log;
}

private String pickWarbossNextZone(BarbArmy wb) {
        List<String> candidates = armyManager.getAdjacentMoveable(wb.getZoneId());
        log("warboss-next-choices", "Candidates from " + wb.getZoneId() + ": " + candidates);
        if (candidates.isEmpty()) return null;

        // 50% chance to pick unvisited detour
        if (rng.nextDouble() < BarbarianParams.BARB_WARBOSS_DETOUR_CHANCE) {
            List<String> unvisited = new ArrayList<>();
            for (String z : candidates) {
                if (!armyManager.getInvasionVisited().contains(z)) unvisited.add(z);
            }
            if (!unvisited.isEmpty()) {
                String pick = unvisited.get(rng.nextInt(unvisited.size()));
                log("warboss-next-choice", "Detour to unvisited " + pick);
                return pick;
            }
        }

        // Pathfind toward heartland: pick adjacent zone closest to heartland
        String chosen = pickClosestToHeartland(candidates, wb.getZoneId());
        log("warboss-next-choice", "Pathfinding chose " + chosen);
        return chosen;
    }

    private String pickClosestToHeartland(List<String> candidates, String currentZone) {
        String best = null;
        int bestDepth = Integer.MAX_VALUE;

        for (String candidate : candidates) {
            int depth = bfsDepthToHeartland(candidate, 12);
            if (depth < bestDepth) {
                bestDepth = depth;
                best = candidate;
            }
        }

        return best != null ? best : candidates.get(rng.nextInt(candidates.size()));
    }

    private int bfsDepthToHeartland(String start, int maxDepth) {
        if (Army.HEARTLAND_ID.equals(start)) return 0;

        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);

        int depth = 0;
        while (!queue.isEmpty() && depth < maxDepth) {
            int size = queue.size();
            depth++;
            for (int i = 0; i < size; i++) {
                String curr = queue.poll();
                Zone zone = zoneManager.getZone(curr);
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

        return maxDepth;
    }

    // ─── Combat ──────────────────────────────────────────────────────────────
    private List<String> resolveCombat(ResourcePool playerResources) {
        List<String> log = new ArrayList<>();

        // Snapshot zone→armies map at start of phase to avoid concurrent modification
        Map<String, List<BarbArmy>> byZone = new LinkedHashMap<>();
        for (BarbArmy army : new ArrayList<>(armyManager.getMobileArmies())) {
            if (army.isPaidOff()) continue;
            byZone.computeIfAbsent(army.getZoneId(), k -> new ArrayList<>()).add(army);
        }

        for (Map.Entry<String, List<BarbArmy>> entry : byZone.entrySet()) {
            String zoneId = entry.getKey();
            List<BarbArmy> armiesInZone = new ArrayList<>(entry.getValue());

            if (Army.HEARTLAND_ID.equals(zoneId)) {
                BarbArmy wb = null;
                for (BarbArmy a : armiesInZone) {
                    if (a.isWarboss() && a.isAlive()) {
                        wb = a;
                        break;
                    }
                }
                if (wb != null) {
                    log.addAll(resolveHeartlandAssault(wb, playerResources));
                    if (!wb.isAlive()) armyManager.remove(wb);
                }
            } else {
                for (BarbArmy barb : armiesInZone) {
                    if (!barb.isAlive()) continue;
                    log.addAll(resolveCombatInZone(barb, zoneId, playerResources));
                    log("combat-done", "Resolved combat for " + barb.getType() + " in " + zoneId +
                            " (alive=" + barb.isAlive() + ")");
                    // Remove dead armies immediately after each fight
                    if (!barb.isAlive()) {
                        armyManager.remove(barb);
                        log("combat-remove", barb.getType() + " removed immediately after death at " + zoneId);
                    }
                }
            }
        }

        // Final sweep for any stragglers (e.g. killed during garrison placement)
        armyManager.removeDeadArmies();
        return log;
    }

    private List<String> resolveHeartlandAssault(BarbArmy warboss, ResourcePool playerResources) {
        List<String> log = new ArrayList<>();
        log.add("☠ THE WARBOSS MARCHES ON THE HEARTLAND!");

        List<Army> defenders = new ArrayList<>();
        for (Army a : playerArmyManager.getArmies()) {
            if (Army.HEARTLAND_ID.equals(a.getZoneId()) && a.isAlive()) defenders.add(a);
        }

        if (defenders.isEmpty()) {
            log.add("☠ No defenders stand against the Warboss. The realm falls!");
            if (gameOverCallback != null) gameOverCallback.triggerGameOver("The Warboss sacked the undefended heartland.");
            return log;
        }

        BarbCombatHandler.BarbCombatResult result =
                BarbCombatHandler.barbAttacksPlayerZone(warboss, defenders, Army.HEARTLAND_ID, log);
        if (result.attackerWon) {
            log.add("☠ THE HEARTLAND HAS FALLEN. ALL IS LOST.");
            if (gameOverCallback != null) gameOverCallback.triggerGameOver("The Warboss defeated the heartland defenders.");
        } else {
            log.add("The Warboss is repelled from the heartland!");
        }

        return log;
    }

private List<String> resolveCombatInZone(BarbArmy barb, String zoneId, ResourcePool playerResources) {
        List<String> log = new ArrayList<>();
        NobleHouse owner = nobleHouseManager.getOwnerOfZone(zoneId);

        // Noble AI pay-off: only ravagers can be paid off
        if (owner != null && barb.isRavager()) {
            if (shouldNoblePay(owner, barb)) {
                int goldCost = barb.getSize() * BarbarianParams.BARB_PAYOFF_GOLD_PER_MAN;
                owner.addGold(-Math.min(goldCost, owner.getGold()));
                barb.setPaidOff(true);
                log.add(owner.getName() + " pays off ravagers at " + zoneId + " (" + goldCost + " gold). They stand down for one turn.");
                return log;
            }
        }

        List<Army> playerArmies = new ArrayList<>();
        for (Army a : playerArmyManager.getDeployedArmies()) {
            if (zoneId.equals(a.getZoneId()) && a.isAlive()) playerArmies.add(a);
        }

        boolean hasNobleDefender  = owner != null;
        boolean hasPlayerDefender = !playerArmies.isEmpty();
        boolean hasDefenders      = hasNobleDefender || hasPlayerDefender;

        // Desolate zones have nothing to conquer or raid — skip entirely
        Zone zone = zoneManager.getZone(zoneId);
        boolean isDesolate = zone != null && zone.isDesolate();
        if (isDesolate && !hasDefenders) {
            log("combat-skip", barb.getType() + " [" + barb.getId() + "] in desolate " + zoneId + " — no action");
            return log;
        }

        log("combat-zone-check", barb.getType() + " [" + barb.getId() + "] at " + zoneId
                + " — noble=" + hasNobleDefender
                + " player=" + hasPlayerDefender
                + " size=" + barb.getSize());

        if (!hasDefenders) {
            if (barb.isRaider()) {
                log.addAll(raidZone(barb, zoneId, owner));
                return log;
            }
            log.addAll(conquerZone(barb, zoneId, null));
            return log;
        }

        // Player pay-off dialog: only for ravagers
        if (barb.isRavager() && payOffCallback != null) {
            log("payoff-check", "Asking player about ravagers at " + zoneId + " size=" + barb.getSize());
            List<NobleArmy> nobleArmiesDisplay = new ArrayList<>();
            int nobleGarrisonDisplay = 0;
            if (hasNobleDefender) {
                nobleGarrisonDisplay = owner.getGarrisonFor(zoneId);
                nobleArmiesDisplay = nobleHouseManager.getArmyManager()
                        .getArmiesInZone(zoneId, owner.getId());
            }
            boolean playerPays = payOffCallback.askPlayerPayOff(barb, playerResources, zoneId, owner,
                    playerArmies, nobleArmiesDisplay, nobleGarrisonDisplay);
            if (playerPays) {
                barb.setPaidOff(true);
                log.add("Player pays off ravagers at " + zoneId + ". They stand down.");
                log("payoff-accepted", "Player paid off ravagers at " + zoneId);
                return log;
            } else {
                log("payoff-declined", "Player declined to pay off ravagers at " + zoneId);
            }
        }

        // Collect ALL noble defenders in this zone (owner garrison + any noble armies)
        int nobleGarrison = 0;
        int nobleFort     = 0;
        int nobleMilitary = 0;
        List<NobleArmy> nobleArmies = new ArrayList<>();
        if (hasNobleDefender) {
            nobleGarrison = owner.getGarrisonFor(zoneId);
            nobleFort     = owner.getFortificationFor(zoneId);
            nobleMilitary = owner.getActiveCharacter() != null ? owner.getActiveCharacter().getMilitary() : 0;
            nobleArmies.addAll(nobleHouseManager.getArmyManager()
                    .getArmiesInZone(zoneId, owner.getId()));
        }

        // Non-owner noble armies in zone also defend (idle armies only)
        for (NobleHouse allied : new ArrayList<>(nobleHouseManager.getHouses())) {
            if (allied == owner || allied.isEliminated()) continue;
            for (NobleArmy na : nobleHouseManager.getArmyManager()
                    .getArmiesInZone(zoneId, allied.getId())) {
                if (na.isAlive() && !na.hasPendingOrder()) {
                    nobleArmies.add(na);
                    log.add(allied.getName() + " armies join the defense of " + zoneId + ".");
                    Debug.log("barbarians", "noble-join-defense",
                            allied.getName() + " idle army joins defense at " + zoneId);
                }
            }
        }

        // Total defender pool
        int totalDefenderSize = nobleGarrison;
        for (NobleArmy na : nobleArmies) totalDefenderSize += na.getSize();
        int playerContribution = 0;
        for (Army a : playerArmies) playerContribution += a.getSize();
        totalDefenderSize += playerContribution;

        // Barbarians are always the attacker — apply defender bonus
        double defBonus       = 1.0 + BarbarianParams.BARB_DEFENDER_BONUS;
        int    boostedDefSize = (int) (totalDefenderSize * defBonus);

        String defenderLabel = hasNobleDefender ? owner.getId() : "player";
        log.add("☠ " + barb.getType().name() + " attacks " + zoneId
                + " (" + barb.getSize() + " vs " + totalDefenderSize + " defenders"
                + (nobleGarrison     > 0 ? ", garrison: " + nobleGarrison     : "")
                + (playerContribution > 0 ? ", player: "  + playerContribution : "")
                + ")");

        log("combat-resolve", barb.getType() + " " + barb.getId()
                + " fighting at " + zoneId + " vs " + defenderLabel
                + " boostedDef=" + boostedDefSize);

        City.main.combat.ArmyForce atkForce = new City.main.combat.ArmyForce(
                "barbarians", barb.getSize(), 0, 0);
        City.main.combat.ArmyForce defForce = new City.main.combat.ArmyForce(
                defenderLabel, boostedDefSize, nobleFort, nobleMilitary);

        City.main.combat.CombatResult result = City.main.combat.CombatResolver.resolve(atkForce, defForce);
        log.addAll(result.getLog());

        barb.applyLosses(result.getAttackerLosses());
        int rawDefLoss = (int) (result.getDefenderLosses() / defBonus);
        boolean barbWon = "barbarians".equals(result.getWinnerId());

        log("combat-outcome", barb.getType() + " at " + zoneId
                + " — barbWon=" + barbWon
                + " barbRemaining=" + barb.getSize()
                + " rawDefLoss=" + rawDefLoss);

        distributeDefenderLosses(rawDefLoss, nobleGarrison, nobleArmies, playerArmies, owner, zoneId, log);

        if (barbWon) {
            // Recall surviving player armies to heartland
            for (Army a : playerArmies) {
                if (a.isAlive()) {
                    a.recallToCity();
                    log.add("☠ " + a.getDisplayName() + " is driven back to the Heartland!");
                    log("player-recall", a.getDisplayName() + " recalled after barb win at " + zoneId);
                }
            }
            if (barb.isRaider()) {
                log.addAll(raidZone(barb, zoneId, owner));
            } else {
                log.addAll(conquerZone(barb, zoneId, owner));
            }
        } else {
            log.add("Defenders repel the barbarian assault on " + zoneId + ".");
            log("combat-defended", "Barbarians defeated at " + zoneId);
            if (!barb.isAlive()) {
                armyManager.remove(barb);
                log("combat-remove", "Dead barbarian removed immediately at " + zoneId);
            }
        }

        return log;
    }

private void distributeDefenderLosses(int rawLoss, int nobleGarrison, List<NobleArmy> nobleArmies,
                                          List<Army> playerArmies, NobleHouse owner, String zoneId, List<String> log) {
        int remaining = rawLoss;

        if (owner != null && remaining > 0 && nobleGarrison > 0) {
            int lost = Math.min(remaining, nobleGarrison);
            owner.damageGarrison(zoneId, lost);
            remaining -= lost;
            Debug.log("barbarians", "losses", "Garrison at " + zoneId + " lost " + lost);
        }

        int totalNobleArmy = 0;
        for (NobleArmy na : nobleArmies) totalNobleArmy += na.getSize();

        if (totalNobleArmy > 0 && remaining > 0) {
            int nobleShare = Math.min(remaining, totalNobleArmy);
            int nobleRemaining = nobleShare;
            for (NobleArmy na : new ArrayList<>(nobleArmies)) {
                if (nobleRemaining <= 0) break;
                int proportion = (int) Math.ceil((double) na.getSize() / totalNobleArmy * nobleShare);
                int lost = Math.min(proportion, Math.min(nobleRemaining, na.getSize()));
                na.setSize(na.getSize() - lost);
                nobleRemaining -= lost;
                Debug.log("barbarians", "losses", "Noble army " + na.getId() + " lost " + lost);
            }
            remaining -= (nobleShare - nobleRemaining);
        }

        for (Army a : new ArrayList<>(playerArmies)) {
            if (remaining <= 0) break;
            int lost = Math.min(remaining, a.getSize());
            a.applyLosses(lost);
            remaining -= lost;
        }
    }

    private int playerArmyTotal(List<Army> armies) {
        int total = 0;
        for (Army a : armies) total += a.getSize();
        return total;
    }

    private List<String> raidZone(BarbArmy barb, String zoneId, NobleHouse owner) {
        List<String> log = new ArrayList<>();
        log("raid-start", barb.getType() + " raiding " + zoneId);

        ZoneState state = zoneManager.getState(zoneId);
        if (state != null && state.isRecentlyRaided()) {
            log.add("☠ Raiders find " + zoneId + " already raided. They move on.");
            log("raid-skip", zoneId + " already recently raided");
            return log;
        }

        int zoneGold = 0;
        Zone zone = zoneManager.getZone(zoneId);
        if (zone != null) zoneGold = zone.getGoldProduction();

        int maxByZone = (int) (zoneGold * DiplomacyParams.RAID_GOLD_ZONE_MULTIPLIER);
        int maxByArmy = (int) (barb.getSize() * DiplomacyParams.RAID_GOLD_PER_SOLDIER);
        int maxSteal = Math.min(maxByZone, maxByArmy);

        if (owner != null) {
            int steal = Math.min(maxSteal,
                    (int) (owner.getGold() * NobleAIParams.AI_RAID_GOLD_FRACTION));
            steal = Math.max(0, steal);
            owner.addGold(-steal);
            log.add("☠ Barbarian raiders steal " + steal + " gold from "
                    + owner.getName() + " at " + zoneId + ".");
        } else {
            log.add("☠ Barbarian raiders plunder " + zoneId + " but find no gold.");
        }

        if (state != null) state.markRaided();
        log("raid-finish", "Raided " + zoneId);

        return log;
    }

    // ─── Conquest ────────────────────────────────────────────────────────────

private List<String> conquerZone(BarbArmy barb, String zoneId, NobleHouse previousOwner) {
    List<String> log = new ArrayList<>();

    if (previousOwner != null) {
        previousOwner.removeZone(zoneId);
        // Give the displaced house a claim so the player can return it after liberation
        nobleHouseManager.getClaimManager().addClaim(previousOwner.getId(), zoneId);
        log.add(previousOwner.getName() + " loses " + zoneId
                + " to the barbarians and gains a claim on it!");
        Debug.log("barbarians", "conquest-claim",
                previousOwner.getId() + " given claim on " + zoneId + " after barb conquest");
    }

    if (barb.isWarboss()) {
        ravagedZones.markHeavilyRavaged(zoneId);
        log.add("☠ " + zoneId + " is heavily ravaged by the Warboss's horde!");
    } else {
        ravagedZones.markRavaged(zoneId);
        log.add("☠ " + zoneId + " is ravaged by the barbarians!");
    }

    // Do not leave a garrison if the zone is already occupied by barbarians
    if (!armyManager.getGarrisonsInZone(zoneId).isEmpty()) {
        log.add("☠ " + zoneId + " is already under barbarian control – no new garrison left.");
        return log;
    }

    int garrisonSize = barb.isWarboss()
            ? BarbarianParams.BARB_WARBOSS_GARRISON_SIZE
            : BarbarianParams.BARB_RAVAGER_GARRISON_SIZE;
    garrisonSize = Math.min(garrisonSize, barb.getSize());

    if (garrisonSize > 0) {
        barb.setSize(barb.getSize() - garrisonSize);
        BarbArmy garrison = new BarbArmy(barb.getType(), garrisonSize, zoneId);
        garrison.makeGarrison();
        armyManager.addGarrison(garrison);
        log.add(garrisonSize + " barbarians remain as garrison in " + zoneId + ".");
        Debug.log("barbarians", "garrison-placed",
                "Placed " + garrisonSize + " garrison in " + zoneId
                + " from " + barb.getType());
    } else {
        Debug.log("barbarians", "garrison-skip",
                "No garrison placed in " + zoneId + " (size would be 0 or none)");
    }

    return log;
}

// ─── Noble AI pay-off decision ────────────────────────────────────────────
    private boolean shouldNoblePay(NobleHouse noble, BarbArmy barb) {
        int garrisonSize = noble.getGarrisonFor(barb.getZoneId());
        int armySize = 0;
        for (NobleArmy a : nobleHouseManager.getArmyManager()
                .getArmiesInZone(barb.getZoneId(), noble.getId())) {
            armySize += a.getSize();
        }

        int defTotal = (int) ((garrisonSize + armySize) * (1.0 + BarbarianParams.BARB_DEFENDER_BONUS));
        int goldCost = barb.getSize() * BarbarianParams.BARB_PAYOFF_GOLD_PER_MAN;
        boolean canAfford = noble.getGold() >= goldCost;
        boolean canWin = defTotal > barb.getSize();

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
        log("recapture-start", "Starting noble recapture attempts");

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

                BarbCombatHandler.BarbCombatResult result = BarbCombatHandler.nobleAttacksBarbGarrison(
                        noble, garrison, zoneId, nobleHouseManager.getArmyManager(), log);

                if (result.attackerWon) {
                    nobleHouseManager.awardRecapturedZone(noble, zoneId);
                    armyManager.remove(garrison);
                    ravagedZones.markRavaged(zoneId);
                    log("recapture-win", noble.getName() + " recaptured " + zoneId);
                } else {
                    log("recapture-fail", noble.getName() + " failed to recapture " + zoneId);
                }
                break; // Only one noble per zone per turn
            }
        }

        return log;
    }

    public BarbInvasionState getState() {
        return state;
    }

    public BarbArmyManager getArmyManager() {
        return armyManager;
    }

    public RavagedZoneManager getRavagedZones() {
        return ravagedZones;
    }
}
