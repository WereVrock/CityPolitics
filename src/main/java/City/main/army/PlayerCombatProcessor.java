// PlayerCombatProcessor.java
package City.main.army;

import City.main.army.commander.CommanderDeathResolver;
import City.main.army.commander.Commander;
import City.debug.Debug;
import City.main.barbarians.BarbArmy;
import City.main.barbarians.BarbArmyManager;
import City.main.barbarians.RavagedZoneManager;
import City.main.map.Zone;
import City.main.map.ZoneManager;
import City.main.nobles.ClaimManager;
import City.main.nobles.NobleArmy;
import City.main.nobles.NobleArmyManager;
import City.main.nobles.NobleHouse;
import City.main.nobles.NobleHouseManager;
import City.main.combat.ArmyForce;
import City.main.combat.CombatResolver;
import City.main.combat.CombatResult;
import City.main.combat.CombatPrestigeCalculator;
import City.main.parameters.BarbarianParams;
 
import City.main.parameters.PrestigeXPParams;
import City.main.politics.PartyManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the player army fight phase that runs each turn after noble processing
 * but before barbarian processing.
 *
 * Rules:
 * - Player army in zone with barbarians → player attacks (no defender bonus for player,
 *   barbarians get 30% defender bonus if zone is desolate).
 * - Barbarian moves into player zone → handled in BarbInvasionProcessor (player defends,
 *   gets full bonuses + garrison help). This processor only handles the proactive case.
 * - Noble armies in same zone join player's side automatically.
 * - If player wins in a barbarian-garrisoned zone, prompt to assign a claimant noble house.
 * - If player army loses, it retreats to heartland.
 */
public class PlayerCombatProcessor {

    /**
     * Callback to ask the player which noble house should receive a liberated zone.
     * Returns the chosen NobleHouse, or null if player cancels / no claimants.
     */
    public interface ZoneAwardCallback {
        NobleHouse askPlayerChooseClaimant(String zoneId, List<NobleHouse> claimants);
    }

    private ZoneAwardCallback            zoneAwardCallback;
    private PartyManager                 partyManager;
    private City.main.nobles.PlayerPrestige   playerPrestige;
    private City.main.nobles.ProtectionManager protectionManager;
    private City.main.nobles.NobleHouseManager nobleHouseManager;

    public void setZoneAwardCallback(ZoneAwardCallback cb)     { this.zoneAwardCallback = cb; }
    public void setPartyManager(PartyManager pm)               { this.partyManager = pm; }
    public void setPlayerPrestige(City.main.nobles.PlayerPrestige pp)         { this.playerPrestige = pp; }
    public void setProtectionManager(City.main.nobles.ProtectionManager pm)   { this.protectionManager = pm; }
    public void setNobleHouseManagerRef(City.main.nobles.NobleHouseManager nhm){ this.nobleHouseManager = nhm; }

    // ─── Main entry ──────────────────────────────────────────────────────────

public List<String> processTurn(
            ArmyManager       playerArmyManager,
            BarbArmyManager   barbArmyManager,
            NobleHouseManager nobleHouseManager,
            ZoneManager       zoneManager,
            RavagedZoneManager ravagedZoneManager,
            ClaimManager      claimManager) {

        List<String> log = new ArrayList<>();

        Map<String, List<Army>> armiesByZone = new LinkedHashMap<>();
        for (Army a : playerArmyManager.getDeployedArmies()) {
            if (a.isAlive()) {
                armiesByZone.computeIfAbsent(a.getZoneId(), k -> new ArrayList<>()).add(a);
            }
        }

        Debug.log("player-combat", "phase-start",
                "Player fight phase. Active zones: " + armiesByZone.size());

        for (Map.Entry<String, List<Army>> entry : new ArrayList<>(armiesByZone.entrySet())) {
            String     zoneId     = entry.getKey();
            List<Army> zoneArmies = entry.getValue();
            Zone       zone       = zoneManager.getZone(zoneId);

            List<BarbArmy> barbsHere = new ArrayList<>();
            for (BarbArmy b : barbArmyManager.getMobileArmies()) {
                if (zoneId.equals(b.getZoneId()) && !b.isPaidOff() && b.isAlive())
                    barbsHere.add(b);
            }

            List<BarbArmy> garrisonsHere = new ArrayList<>();
            for (BarbArmy b : barbArmyManager.getGarrisonsInZone(zoneId)) {
                if (b.isAlive()) garrisonsHere.add(b);
            }

            Debug.log("player-combat", "zone-check",
                    zoneId + " — player armies: " + zoneArmies.size()
                    + ", mobile barbs: " + barbsHere.size()
                    + ", garrisons: " + garrisonsHere.size());

            if (barbsHere.isEmpty() && garrisonsHere.isEmpty()) continue;

            List<Army> activePlayerArmies = new ArrayList<>();
            for (Army a : zoneArmies) {
                if (a.isAlive() && zoneId.equals(a.getZoneId())) activePlayerArmies.add(a);
            }
            if (activePlayerArmies.isEmpty()) continue;

            // All barbs (mobile + garrison) fight together against player
            List<BarbArmy> allBarbs = new ArrayList<>();
            allBarbs.addAll(barbsHere);
            allBarbs.addAll(garrisonsHere);

            log.add("⚔ Player ATTACKS at " + (zone != null ? zone.getDisplayName() : zoneId)
                    + " — player: " + activePlayerArmies.size() + " armies"
                    + ", mobile barbs: " + barbsHere.size()
                    + ", barb garrisons: " + garrisonsHere.size());

            log.addAll(resolvePlayerAttack(activePlayerArmies, allBarbs,
                    zoneId, zone, nobleHouseManager, barbArmyManager, zoneManager,
                    claimManager, ravagedZoneManager));
        }

        barbArmyManager.removeDeadArmies();
        Debug.log("player-combat", "phase-end", "Player fight phase complete.");
        return log;
    }

// ─── Player attacks mobile barbarian ─────────────────────────────────────

private List<String> resolvePlayerVsBarb(
            List<Army> playerArmies, BarbArmy barb,
            String zoneId, Zone zone,
            NobleHouseManager nobleHouseManager,
            BarbArmyManager barbArmyManager,
            ZoneManager zoneManager,
            ClaimManager claimManager,
            RavagedZoneManager ravagedZoneManager) {
        return new ArrayList<>();
    }

// ─── Player attacks barbarian garrison ────────────────────────────────────

private List<String> resolvePlayerVsGarrison(
            List<Army> playerArmies, BarbArmy garrison,
            String zoneId, Zone zone,
            NobleHouseManager nobleHouseManager,
            BarbArmyManager barbArmyManager,
            ZoneManager zoneManager,
            ClaimManager claimManager,
            RavagedZoneManager ravagedZoneManager) {
        return new ArrayList<>();
    }

// ─── Zone award after liberation ─────────────────────────────────────────

    /**
     * After player clears a barbarian garrison, offer the zone to a noble with a claim.
     * Uses zoneAwardCallback to ask the player which house should receive it.
     */

public List<String> awardZoneToClaimant(
            String zoneId,
            NobleHouseManager nobleHouseManager,
            ClaimManager claimManager) {

        List<String> log = new ArrayList<>();

        List<NobleHouse> claimants = new ArrayList<>();
        for (NobleHouse house : nobleHouseManager.getHouses()) {
            if (claimManager.hasClaim(house.getId(), zoneId)) {
                claimants.add(house);
            }
        }

        if (claimants.isEmpty()) {
            Debug.log("player-combat", "zone-award",
                    "No claimants for " + zoneId + " — zone remains unowned.");
            log.add("No noble house holds a claim on " + zoneId + ". Zone remains ungoverned.");
            return log;
        }

        NobleHouse chosen = null;
        if (zoneAwardCallback != null) {
            chosen = zoneAwardCallback.askPlayerChooseClaimant(zoneId, claimants);
        }

        if (chosen == null) {
            chosen = claimants.get(0);
            log.add("No selection made. " + chosen.getName()
                    + " assumes control of " + zoneId + ".");
        } else {
            log.add(chosen.getName() + " receives " + zoneId
                    + " from the player's liberation.");
        }

        chosen.addZone(zoneId);
        chosen.resetGarrison(zoneId);
        claimManager.removeClaim(chosen.getId(), zoneId);
        chosen.adjustPlayerOpinion(PrestigeXPParams.LIBERATED_ZONE_OPINION_BONUS);
        log.add(chosen.getName() + " is grateful for the liberation. (+"
                + PrestigeXPParams.LIBERATED_ZONE_OPINION_BONUS + " opinion)");
        Debug.log("player-combat", "zone-award",
                chosen.getId() + " awarded " + zoneId + " after liberation, opinion +"
                + PrestigeXPParams.LIBERATED_ZONE_OPINION_BONUS);
        return log;
    }

// ─── Helpers ─────────────────────────────────────────────────────────────

private List<NobleArmy> collectNobleAllies(String zoneId, NobleHouseManager nhm) {
        List<NobleArmy> allies = new ArrayList<>();
        NobleArmyManager am = nhm.getArmyManager();
        for (NobleHouse house : nhm.getHouses()) {
            if (house.isEliminated()) continue;
            for (NobleArmy na : am.getArmiesInZone(zoneId, house.getId())) {
                if (na.isAlive() && !na.hasPendingOrder()) {
                    allies.add(na);
                    Debug.log("player-combat", "ally-found",
                            house.getName() + " army " + na.getId()
                            + " size=" + na.getSize() + " at " + zoneId);
                }
            }
        }
        return allies;
    }

/**
     * Returns the total garrison size of the noble house that owns this zone.
     * Only the owning house's garrison fights — non-owner houses cannot have
     * garrisoned soldiers in someone else's zone.
     */
    private int collectNobleGarrison(String zoneId, NobleHouseManager nhm) {
        NobleHouse owner = nhm.getOwnerOfZone(zoneId);
        if (owner == null) return 0;
        int garrison = owner.getGarrisonFor(zoneId);
        if (garrison > 0) {
            Debug.log("player-combat", "garrison-found",
                    owner.getName() + " garrison=" + garrison + " at " + zoneId);
        }
        return garrison;
    }

/**
     * Finds which player army owns the given commander.
     * Returns null if not found (shouldn't happen in normal flow).
     */
    private Army findArmyForCommander(List<Army> armies, Commander commander) {
        for (Army a : armies) {
            if (a.getCommander() == commander) return a;
        }
        return null;
    }

/**
     * Player is ATTACKER.
     * - Player + noble mobile armies attack. NO garrison help for player.
     * - ALL barbs in zone (mobile + garrison) defend together.
     * - Barbs in desolate zone get +30% defender bonus.
     * - Player loses → alive armies retreat to heartland.
     * - Barbs lose → alive barbs retreat to desolate zone; dead barbs removed.
     * - If barb garrison was cleared → offer zone to claimant noble.
     */

private List<String> resolvePlayerAttack(
            List<Army> playerArmies,
            List<BarbArmy> allBarbs,
            String zoneId, Zone zone,
            NobleHouseManager nobleHouseManager,
            BarbArmyManager barbArmyManager,
            ZoneManager zoneManager,
            ClaimManager claimManager,
            RavagedZoneManager ravagedZoneManager) {

        List<String> log = new ArrayList<>();
        if (playerArmies.isEmpty() || allBarbs.isEmpty()) return log;

        // ── Attacker side ──────────────────────────────────────────────────────
        List<NobleArmy>  nobleAllies     = collectNobleAllies(zoneId, nobleHouseManager);
        List<ArmyForce>  attackerForces  = new ArrayList<>();
        List<ArmyForce>  playerForces    = new ArrayList<>();
        List<ArmyForce>  nobleAllyForces = new ArrayList<>();

        // Track sizes before battle for death/prestige resolution
        int[] playerSizesBefore = new int[playerArmies.size()];

        for (int i = 0; i < playerArmies.size(); i++) {
            Army a = playerArmies.get(i);
            playerSizesBefore[i] = a.getSize();
            int skill = a.getCommandingSkill();
            ArmyForce f = new ArmyForce("player_" + a.getId(), a.getSize(), 0, skill);
            attackerForces.add(f);
            playerForces.add(f);
            Debug.log("player-combat", "player-force",
                    a.getDisplayName() + " size=" + a.getSize() + " skill=" + skill);
        }

        for (NobleArmy na : nobleAllies) {
            NobleHouse h = nobleHouseManager.getHouseById(na.getHouseId());
            int mil = h != null && h.getActiveCharacter() != null
                    ? h.getActiveCharacter().getMilitary() : 0;
            ArmyForce f = new ArmyForce(na.getHouseId(), na.getSize(), 0, mil);
            attackerForces.add(f);
            nobleAllyForces.add(f);
            log.add((h != null ? h.getName() : "Noble army")
                    + " mobile forces join the player's attack at " + zoneId + ".");
        }

        // ── Defender side ──────────────────────────────────────────────────────
        boolean isDesolate   = zone != null && zone.isDesolate();
        int     totalBarbRaw = 0;
        for (BarbArmy b : allBarbs) totalBarbRaw += b.getSize();

        int barbEffective = isDesolate
                ? (int)(totalBarbRaw * (1.0 + BarbarianParams.BARB_DEFENDER_BONUS))
                : totalBarbRaw;

        ArmyForce       barbForce      = new ArmyForce("barbarians", barbEffective, 0, 0);
        List<ArmyForce> defenderForces = new ArrayList<>();
        defenderForces.add(barbForce);

        int totalPlayerSize = 0;
        for (Army a : playerArmies) totalPlayerSize += a.getSize();
        int totalAllySize = nobleAllies.stream().mapToInt(NobleArmy::getSize).sum();
        int totalAttackerSize = totalPlayerSize + totalAllySize;

        log.add("Player ATTACKS — player: " + totalPlayerSize
                + ", noble allies: " + totalAllySize
                + " vs barbs: " + totalBarbRaw
                + (isDesolate ? " [desolate +30% barb def]" : ""));

        String leadId = "player_" + playerArmies.get(0).getId();
        CombatResult result = CombatResolver.resolveMultiSideBattle(
                attackerForces, defenderForces, leadId, "barbarians", 0);
        log.addAll(result.getLog());

        // ── Apply losses to player armies ──────────────────────────────────────
        for (int i = 0; i < playerArmies.size(); i++) {
            playerArmies.get(i).setSize(playerForces.get(i).getRawSize());
        }
        // Apply losses to noble allies
        for (int i = 0; i < nobleAllies.size(); i++) {
            nobleAllies.get(i).setSize(nobleAllyForces.get(i).getRawSize());
        }

        // Apply barb losses proportionally
        int barbRawLoss = isDesolate
                ? (int)(result.getDefenderLosses() / (1.0 + BarbarianParams.BARB_DEFENDER_BONUS))
                : result.getDefenderLosses();
        distributeBarbLosses(allBarbs, barbRawLoss);

        boolean playerWon = result.getWinnerId() != null
                && result.getWinnerId().startsWith("player_");

        // ── Prestige & XP calculation ──────────────────────────────────────────
        int enemyBonusPct = isDesolate ? (int)(BarbarianParams.BARB_DEFENDER_BONUS * 100) : 0;
        double rawScore = CombatPrestigeCalculator.computeRawScore(
                totalAttackerSize, totalBarbRaw, enemyBonusPct, playerWon);

        // Collect participating commanders and their force sizes
        List<Commander> participatingCommanders = new ArrayList<>();
        int[]           commanderForceSizes     = new int[playerArmies.size()];
        int             cmdIdx                  = 0;
        for (Army a : playerArmies) {
            if (a.hasLivingCommander()) {
                participatingCommanders.add(a.getCommander());
                commanderForceSizes[cmdIdx++] = playerSizesBefore[playerArmies.indexOf(a)];
            }
        }
        // Trim array to actual count
        int[] trimmedForceSizes = new int[participatingCommanders.size()];
        System.arraycopy(commanderForceSizes, 0, trimmedForceSizes, 0, participatingCommanders.size());

        int[] xpGrants = CombatPrestigeCalculator.distributeXp(
                participatingCommanders, trimmedForceSizes, rawScore);

        // ── Per-commander: XP, prestige, death ────────────────────────────────
        for (int i = 0; i < participatingCommanders.size(); i++) {
            Commander c = participatingCommanders.get(i);
            if (!c.isAlive()) continue;

            // XP
            boolean levelledUp = c.addXp(xpGrants[i]);
            if (levelledUp) {
                log.add("★ " + c.getName() + " has reached skill level " + c.getCommandingSkill() + "!");
                Debug.log("player-combat", "commander-levelup",
                        c.getName() + " new skill=" + c.getCommandingSkill());
            }

            // Prestige for affiliated party
            if (playerWon && c.getParty() != null) {
                int prestige = CombatPrestigeCalculator.computePrestige(rawScore);
                c.getParty().addPrestige(prestige);
                Debug.log("player-combat", "prestige",
                        c.getName() + " party=" + c.getPartyName() + " +" + prestige);
            }

            // Death roll — use the army's size before and after to get losses
            Army ownerArmy = findArmyForCommander(playerArmies, c);
            if (ownerArmy != null) {
                int sizeBefore = playerSizesBefore[playerArmies.indexOf(ownerArmy)];
                int lost       = sizeBefore - ownerArmy.getSize();
                boolean died   = CommanderDeathResolver.resolve(c, sizeBefore, lost, playerWon);
                if (died) {
                    log.add("✝ " + c.getName() + " has fallen in battle.");
                    Debug.log("player-combat", "commander-death", c.getName() + " died at " + zoneId);
                    // Army with dead commander recalls to heartland
                    ownerArmy.recallToCity();
                    log.add(ownerArmy.getDisplayName() + " retreats to Heartland — their commander has fallen.");
                }
            }
        }

        // ── Outcome ───────────────────────────────────────────────────────────
        if (playerWon) {
            log.add("✓ Player forces victorious at " + zoneId + ".");
            Debug.log("player-combat", "outcome", "PLAYER WIN at " + zoneId);

            boolean hadGarrison = false;
            for (BarbArmy b : new ArrayList<>(allBarbs)) {
                if (b.isGarrison()) { hadGarrison = true; }
                if (!b.isAlive()) {
                    barbArmyManager.remove(b);
                } else {
                    retreatBarbToDesolate(b, barbArmyManager, zoneManager, log);
                }
            }

            if (hadGarrison) {
                ravagedZoneManager.markRavaged(zoneId);
                log.addAll(awardZoneToClaimant(zoneId, nobleHouseManager, claimManager));
            }

        } else {
            log.add("✗ Player forces repelled at " + zoneId + ". Armies retreat to Heartland.");
            Debug.log("player-combat", "outcome", "PLAYER LOSS at " + zoneId);
            for (Army a : playerArmies) {
                if (a.isAlive()) a.recallToCity();
            }
        }

        return log;
    }

/**
     * Distribute barbarian losses proportionally across all participating barb armies.
     * Last army absorbs rounding remainder.
     */
    private void distributeBarbLosses(List<BarbArmy> barbs, int totalLoss) {
        int totalSize = 0;
        for (BarbArmy b : barbs) totalSize += b.getSize();
        if (totalSize <= 0 || totalLoss <= 0) return;

        int remaining = totalLoss;
        for (int i = 0; i < barbs.size(); i++) {
            BarbArmy b = barbs.get(i);
            int share = (i == barbs.size() - 1)
                    ? remaining
                    : (int)((double) b.getSize() / totalSize * totalLoss);
            share = Math.min(share, b.getSize());
            b.applyLosses(share);
            remaining -= share;
            Debug.log("player-combat", "barb-loss",
                    b.getId() + " type=" + b.getType()
                    + " lost=" + share + " remaining=" + b.getSize());
        }
    }

/**
     * Retreats a surviving barbarian army to an adjacent desolate zone.
     * If none is reachable from current position, searches one step further.
     * If still none found, the army is removed (nowhere to flee).
     */
    private void retreatBarbToDesolate(BarbArmy barb, BarbArmyManager barbArmyManager,
                                        ZoneManager zoneManager, List<String> log) {
        String currentZone = barb.getZoneId();

        // First: look for adjacent desolate zones
        List<String> adjacent = barbArmyManager.getAdjacentMoveable(currentZone);
        String retreatZone = null;
        for (String z : adjacent) {
            Zone zone = zoneManager.getZone(z);
            if (zone != null && zone.isDesolate()) {
                retreatZone = z;
                break;
            }
        }

        // Second pass: look one zone further if none adjacent
        if (retreatZone == null) {
            for (String z : adjacent) {
                List<String> secondRing = barbArmyManager.getAdjacentMoveable(z);
                for (String z2 : secondRing) {
                    Zone zone = zoneManager.getZone(z2);
                    if (zone != null && zone.isDesolate()) {
                        retreatZone = z2;
                        break;
                    }
                }
                if (retreatZone != null) break;
            }
        }

        if (retreatZone != null) {
            barbArmyManager.moveArmy(barb, retreatZone);
            log.add("☠ Surviving " + barb.getType().name() + " (" + barb.getSize()
                    + ") retreats to " + retreatZone + ".");
            Debug.log("player-combat", "barb-retreat",
                    barb.getId() + " retreated from " + currentZone + " to " + retreatZone);
        } else {
            barbArmyManager.remove(barb);
            log.add("☠ Surviving " + barb.getType().name()
                    + " find no escape and are destroyed.");
            Debug.log("player-combat", "barb-retreat",
                    barb.getId() + " had no desolate retreat — removed");
        }
    }

}