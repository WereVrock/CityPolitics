package main.barbarians;

import main.army.Army;
import main.army.ArmyManager;
import main.calendar.GameCalendar;
import main.map.Zone;
import main.map.ZoneManager;
import main.map.ZoneState;
import main.nobles.NobleArmy;
import main.nobles.NobleArmyManager;
import main.nobles.NobleHouse;
import main.nobles.NobleHouseManager;
import main.parameters.GameParameters;
import main.resources.ResourcePool;
import debug.Debug;

import java.util.*;

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
        boolean askPlayerPayOff(BarbArmy army, ResourcePool resources,
                                String zoneId, NobleHouse owner,
                                java.util.List<Army> playerArmies,
                                java.util.List<NobleArmy> nobleArmies,
                                int nobleGarrison);
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
        log("invasion-start", "Warboss spawned at " + spawnZone + ", size=" + warboss.getSize());
        state.startInvasion(absoluteTurn);
        log.add("☠ THE BARBARIAN HORDE DESCENDS! The Warboss (" + warboss.getSize()
                + " warriors) emerges from " + spawnZone + "!");

        // First wave: raiders + warboss (warboss IS the first wave's ravager slot)
        log.addAll(spawnWaveHalf(true));
        state.markFirstHalfSpawned();
        log("invasion-start", "First wave spawned");

        return log;
    }

    private void log(String key, String msg) {
        Debug.log("barbarians", key, msg);
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
        log("raider-split", "Split " + splitSize + " raiders from warboss at " + wb.getZoneId() + ", warboss remaining " + wb.getSize());
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
                String oldZone = wb.getZoneId();
                armyManager.moveArmy(wb, next);
                if (!next.equals(oldZone)) {
                    log.add("☠ The Warboss advances to " + next + ".");
                    log("warboss-move", "Warboss moved from " + oldZone + " to " + next);
                }
            } else {
                log("warboss-move", "Warboss has no next zone, staying in " + wb.getZoneId());
            }
            // Pre-calculate next move for display
            String upcoming = pickWarbossNextZone(wb);
            wb.setNextZoneId(upcoming);
            log("warboss-next", "Next destination set to " + upcoming);
        }

        // Raiders and ravagers move randomly, prefer unvisited
        for (BarbArmy army : new ArrayList<>(armyManager.getMobileArmies())) {
            if (army.isWarboss() || army.isPaidOff()) continue;
            List<String> candidates = armyManager.getAdjacentMoveable(army.getZoneId());
            if (candidates.isEmpty()) continue;
            String dest = armyManager.pickPreferredZone(army, candidates);
            if (dest != null) {
                String oldZone = army.getZoneId();
                armyManager.moveArmy(army, dest);
                if (!dest.equals(oldZone)) {
                    log("barb-move", army.getType() + " " + army.getId() + " moved from " + oldZone + " to " + dest);
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
        if (rng.nextDouble() < GameParameters.BARB_WARBOSS_DETOUR_CHANCE) {
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
        if (Army.HEARTLAND_ID.equals(start)) return 0;
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
        return maxDepth;
    }

    // ─── Combat ──────────────────────────────────────────────────────────────

    private List<String> resolveCombat(ResourcePool playerResources) {
        List<String> log = new ArrayList<>();

        Map<String, List<BarbArmy>> byZone = new LinkedHashMap<>();
        for (BarbArmy army : armyManager.getMobileArmies()) {
            if (army.isPaidOff()) continue;
            byZone.computeIfAbsent(army.getZoneId(), k -> new ArrayList<>()).add(army);
        }

        for (Map.Entry<String, List<BarbArmy>> entry : byZone.entrySet()) {
            String zoneId = entry.getKey();
            List<BarbArmy> armiesInZone = entry.getValue();

            // Check if player armies defend this non-heartland zone
        // (heartland handled separately below)
        if (Army.HEARTLAND_ID.equals(zoneId)) {
                // Only the Warboss can threaten the heartland
                BarbArmy wb = null;
                for (BarbArmy a : armiesInZone) {
                    if (a.isWarboss()) {
                        wb = a;
                        break;
                    }
                }
                if (wb != null) {
                    log.addAll(resolveHeartlandAssault(wb, playerResources));
                }
            } else {
                // Each barb army in the zone fights independently
                for (BarbArmy barb : armiesInZone) {
                    log.addAll(resolveCombatInZone(barb, zoneId, playerResources));
                    log("combat-done", "Resolved combat for " + barb.getType() + " in " + zoneId + " (alive=" + barb.isAlive() + ")");
                }
            }
        }

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
            if (gameOverCallback != null)
                gameOverCallback.triggerGameOver("The Warboss sacked the undefended heartland.");
            return log;
        }

        BarbCombatHandler.BarbCombatResult result =
                BarbCombatHandler.barbAttacksPlayerZone(warboss, defenders,
                        Army.HEARTLAND_ID, log);
        if (result.attackerWon) {
            log.add("☠ THE HEARTLAND HAS FALLEN. ALL IS LOST.");
            if (gameOverCallback != null)
                gameOverCallback.triggerGameOver("The Warboss defeated the heartland defenders.");
        } else {
            log.add("The Warboss is repelled from the heartland!");
        }
        return log;
    }

    private List<String> resolveCombatInZone(BarbArmy barb, String zoneId,
                                              ResourcePool playerResources) {
        List<String> log = new ArrayList<>();

        NobleHouse owner = nobleHouseManager.getOwnerOfZone(zoneId);

        // Noble AI pay-off: only ravagers can be paid off
        if (owner != null && barb.isRavager()) {
            if (shouldNoblePay(owner, barb)) {
                int goldCost = barb.getSize() * GameParameters.BARB_PAYOFF_GOLD_PER_MAN;
                owner.addGold(-Math.min(goldCost, owner.getGold()));
                barb.setPaidOff(true);
                log.add(owner.getName() + " pays off ravagers at " + zoneId
                        + " (" + goldCost + " gold). They stand down for one turn.");
                return log;
            }
        }

        // Player armies already engaged barbarians this turn in the player fight phase.
        // Here we only care about barbs that moved INTO a player zone AFTER the player
        // phase, so we still resolve: player defends, gets bonuses, garrison helps.
        List<Army> playerArmies = new ArrayList<>();
        for (Army a : playerArmyManager.getDeployedArmies()) {
            if (zoneId.equals(a.getZoneId()) && a.isAlive()) playerArmies.add(a);
        }

        boolean hasNobleDefender  = owner != null;
        boolean hasPlayerDefender = !playerArmies.isEmpty();
        boolean hasDefenders      = hasNobleDefender || hasPlayerDefender;

        if (!hasDefenders) {
            if (barb.isRaider()) {
                log.addAll(raidZone(barb, zoneId, owner, log));
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
            boolean playerPays = payOffCallback.askPlayerPayOff(barb, playerResources,
                    zoneId, owner, playerArmies, nobleArmiesDisplay, nobleGarrisonDisplay);
            if (playerPays) {
                barb.setPaidOff(true);
                log.add("Player pays off ravagers at " + zoneId + ". They stand down.");
                log("payoff-accepted", "Player paid off ravagers at " + zoneId);
                return log;
            } else {
                log("payoff-declined", "Player declined to pay off ravagers at " + zoneId);
            }
        }

        // Build combined defender pool
        int totalDefenderSize = 0;
        int nobleGarrison = 0;
        List<NobleArmy> nobleArmies = new ArrayList<>();
        int nobleFort = 0;
        int nobleMilitary = 0;

        if (hasNobleDefender) {
            nobleGarrison  = owner.getGarrisonFor(zoneId);
            nobleFort      = owner.getFortificationFor(zoneId);
            nobleMilitary  = owner.getActiveCharacter() != null
                    ? owner.getActiveCharacter().getMilitary() : 0;
            nobleArmies    = nobleHouseManager.getArmyManager()
                    .getArmiesInZone(zoneId, owner.getId());
            for (NobleArmy a : nobleArmies) totalDefenderSize += a.getSize();
            totalDefenderSize += nobleGarrison;
        }

        int playerContribution = 0;
        for (Army a : playerArmies) playerContribution += a.getSize();
        totalDefenderSize += playerContribution;

        // Apply defender bonus (barbarians are always attacker here)
        double defBonus        = 1.0 + GameParameters.BARB_DEFENDER_BONUS;
        int    boostedDefSize  = (int)(totalDefenderSize * defBonus);

        log.add("☠ " + barb.getType().name() + " attacks " + zoneId
                + " (" + barb.getSize() + " vs " + totalDefenderSize + " defenders)");

        main.nobles.combat.ArmyForce atkForce = new main.nobles.combat.ArmyForce(
                "barbarians", barb.getSize(), 0, 0);
        main.nobles.combat.ArmyForce defForce = new main.nobles.combat.ArmyForce(
                hasNobleDefender ? owner.getId() : "player",
                boostedDefSize, nobleFort, nobleMilitary);

        main.nobles.combat.CombatResult result =
                main.nobles.combat.CombatResolver.resolve(atkForce, defForce);
        log.addAll(result.getLog());

        barb.applyLosses(result.getAttackerLosses());

        // Scale losses back from boosted pool and distribute
        int rawDefLoss = (int)(result.getDefenderLosses() / defBonus);
        int remaining  = rawDefLoss;

        // Noble garrison absorbs first
        if (hasNobleDefender && remaining > 0) {
            int garrisonLost = Math.min(remaining, nobleGarrison);
            owner.damageGarrison(zoneId, garrisonLost);
            remaining -= garrisonLost;
        }
        // Noble armies next
        for (NobleArmy a : new ArrayList<>(nobleArmies)) {
            if (remaining <= 0) break;
            int lost = Math.min(remaining, a.getSize());
            a.setSize(a.getSize() - lost);
            remaining -= lost;
        }
        // Player armies last
        for (Army a : playerArmies) {
            if (remaining <= 0) break;
            int lost = Math.min(remaining, a.getSize());
            a.applyLosses(lost);
            remaining -= lost;
        }

        boolean barbWon = "barbarians".equals(result.getWinnerId());
        if (barbWon) {
            if (barb.isRaider()) {
                log.add("☠ Raiders defeat the defenders at " + zoneId + "!");
                log("raider-win", "Raiders won at " + zoneId + ", proceeding to raid");
                log.addAll(raidZone(barb, zoneId, owner, log));
                return log;
            }
            log.add("☠ Barbarians overrun " + zoneId + "!");
            log.addAll(conquerZone(barb, zoneId, owner));
            log("conquest", "Zone " + zoneId + " conquered by " + barb.getType());
        } else {
            log.add("Defenders repel the barbarian assault on " + zoneId + ".");
            log("combat-defended", "Barbarians defeated at " + zoneId);
        }

        return log;
    }

    private List<String> raidZone(BarbArmy barb, String zoneId, NobleHouse owner,
                                   List<String> log) {
        log("raid-start", barb.getType() + " raiding " + zoneId);
        // Already raided? Abort.
        ZoneState state = zoneManager.getState(zoneId);
        if (state != null && state.isRecentlyRaided()) {
            log.add("☠ Raiders find " + zoneId + " already raided. They move on.");
            log("raid-skip", zoneId + " already recently raided");
            return log;
        }
        // Steal gold
        int zoneGold = 0;
        Zone zone = zoneManager.getZone(zoneId);
        if (zone != null) zoneGold = zone.getGoldProduction();

        int maxByZone = (int)(zoneGold * GameParameters.RAID_GOLD_ZONE_MULTIPLIER);
        int maxByArmy = (int)(barb.getSize() * GameParameters.RAID_GOLD_PER_SOLDIER);
        int maxSteal  = Math.min(maxByZone, maxByArmy);

        if (owner != null) {
            int steal = Math.min(maxSteal,
                    (int)(owner.getGold() * GameParameters.AI_RAID_GOLD_FRACTION));
            steal = Math.max(0, steal);
            owner.addGold(-steal);
            log.add("☠ Barbarian raiders steal " + steal + " gold from "
                    + owner.getName() + " at " + zoneId + ".");
        } else {
            // No owner — nothing to steal, just mark raided
            log.add("☠ Barbarian raiders plunder " + zoneId + " but find no gold.");
        }

        // Mark raided (cooldown & production malus)
        if (state != null) state.markRaided();
        log("raid-finish", "Raided " + zoneId);

        return log;
    }

    // ─── Conquest ────────────────────────────────────────────────────────────

    private List<String> conquerZone(BarbArmy barb, String zoneId, NobleHouse previousOwner) {
        List<String> log = new ArrayList<>();

        if (previousOwner != null) {
            previousOwner.removeZone(zoneId);
            log.add(previousOwner.getName() + " loses " + zoneId + " to the barbarians!");
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
                ? GameParameters.BARB_WARBOSS_GARRISON_SIZE
                : GameParameters.BARB_RAVAGER_GARRISON_SIZE;
        garrisonSize = Math.min(garrisonSize, barb.getSize());

        if (garrisonSize > 0) {
            barb.setSize(barb.getSize() - garrisonSize);
            BarbArmy garrison = new BarbArmy(barb.getType(), garrisonSize, zoneId);
            garrison.makeGarrison();
            armyManager.addGarrison(garrison);
            log.add(garrisonSize + " barbarians remain as garrison in " + zoneId + ".");
            log("garrison-placed", "Placed " + garrisonSize + " garrison in " + zoneId + " from " + barb.getType());
        } else {
            log("garrison-skip", "No garrison placed in " + zoneId + " (size would be 0 or none)");
        }

        return log;
    }

    // ─── Noble AI pay-off decision ────────────────────────────────────────────

    private boolean shouldNoblePay(NobleHouse noble, BarbArmy barb) {
        int garrisonSize = noble.getGarrisonFor(barb.getZoneId());
        int armySize     = 0;
        for (NobleArmy a : nobleHouseManager.getArmyManager()
                .getArmiesInZone(barb.getZoneId(), noble.getId())) {
            armySize += a.getSize();
        }
        int defTotal   = (int)((garrisonSize + armySize)
                       * (1.0 + GameParameters.BARB_DEFENDER_BONUS));
        int goldCost   = barb.getSize() * GameParameters.BARB_PAYOFF_GOLD_PER_MAN;

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
                BarbCombatHandler.BarbCombatResult result =
                        BarbCombatHandler.nobleAttacksBarbGarrison(
                                noble, garrison, zoneId,
                                nobleHouseManager.getArmyManager(), log);
                if (result.attackerWon) {
                    nobleHouseManager.awardRecapturedZone(noble, zoneId);
                    armyManager.remove(garrison);
                    ravagedZones.markRavaged(zoneId);
                    log("recapture-win", noble.getName() + " recaptured " + zoneId);
                } else {
                    log("recapture-fail", noble.getName() + " failed to recapture " + zoneId);
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