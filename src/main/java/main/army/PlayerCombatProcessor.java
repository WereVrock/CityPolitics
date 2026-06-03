// PlayerCombatProcessor.java
package main.army;

import debug.Debug;
import main.barbarians.BarbArmy;
import main.barbarians.BarbArmyManager;
import main.barbarians.RavagedZoneManager;
import main.map.Zone;
import main.map.ZoneManager;
import main.nobles.ClaimManager;
import main.nobles.NobleArmy;
import main.nobles.NobleArmyManager;
import main.nobles.NobleHouse;
import main.nobles.NobleHouseManager;
import main.nobles.combat.ArmyForce;
import main.nobles.combat.CombatResolver;
import main.nobles.combat.CombatResult;
import main.parameters.GameParameters;

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

    private ZoneAwardCallback zoneAwardCallback;

    public void setZoneAwardCallback(ZoneAwardCallback cb) { this.zoneAwardCallback = cb; }

    // ─── Main entry ──────────────────────────────────────────────────────────

public List<String> processTurn(
            ArmyManager       playerArmyManager,
            BarbArmyManager   barbArmyManager,
            NobleHouseManager nobleHouseManager,
            ZoneManager       zoneManager,
            RavagedZoneManager ravagedZoneManager,
            ClaimManager      claimManager) {

        List<String> log = new ArrayList<>();

        // Group deployed player armies by zone so they fight together
        Map<String, List<Army>> armiesByZone = new LinkedHashMap<>();
        for (Army a : playerArmyManager.getDeployedArmies()) {
            if (a.isAlive()) {
                armiesByZone.computeIfAbsent(a.getZoneId(), k -> new ArrayList<>()).add(a);
            }
        }

        Debug.log("player-combat", "phase-start",
                "Player fight phase. Active zones: " + armiesByZone.size());

        for (Map.Entry<String, List<Army>> entry : armiesByZone.entrySet()) {
            String     zoneId      = entry.getKey();
            List<Army> zoneArmies  = entry.getValue();
            Zone       zone        = zoneManager.getZone(zoneId);

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

            log.add("⚔ Player armies at "
                    + (zone != null ? zone.getDisplayName() : zoneId)
                    + " engage enemies. (" + zoneArmies.size() + " armies combined)");

            for (BarbArmy barb : barbsHere) {
                boolean anyAlive = false;
                for (Army a : zoneArmies) { if (a.isAlive() && zoneId.equals(a.getZoneId())) { anyAlive = true; break; } }
                if (!anyAlive || !barb.isAlive()) continue;

                List<Army> activeArmies = new ArrayList<>();
                for (Army a : zoneArmies) { if (a.isAlive() && zoneId.equals(a.getZoneId())) activeArmies.add(a); }

                Debug.log("player-combat", "fight-mobile",
                        activeArmies.size() + " player armies vs "
                        + barb.getType() + " " + barb.getId() + " size=" + barb.getSize());

                log.addAll(resolvePlayerVsBarb(activeArmies, barb, zoneId, zone,
                        nobleHouseManager, barbArmyManager, zoneManager, claimManager,
                        ravagedZoneManager));

                if (!barb.isAlive()) {
                    barbArmyManager.remove(barb);
                } else {
                    retreatBarbToDesolate(barb, barbArmyManager, zoneManager, log);
                }
            }

            for (BarbArmy garrison : garrisonsHere) {
                boolean anyAlive = false;
                for (Army a : zoneArmies) { if (a.isAlive() && zoneId.equals(a.getZoneId())) { anyAlive = true; break; } }
                if (!anyAlive || !garrison.isAlive()) continue;

                List<Army> activeArmies = new ArrayList<>();
                for (Army a : zoneArmies) { if (a.isAlive() && zoneId.equals(a.getZoneId())) activeArmies.add(a); }

                log.addAll(resolvePlayerVsGarrison(activeArmies, garrison, zoneId, zone,
                        nobleHouseManager, barbArmyManager, zoneManager, claimManager,
                        ravagedZoneManager));
            }
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

        List<String> log = new ArrayList<>();

        // Collect idle noble armies AND noble garrison in this zone as allies
        List<NobleArmy> nobleAllies    = collectNobleAllies(zoneId, nobleHouseManager);
        int             nobleGarrison  = collectNobleGarrison(zoneId, nobleHouseManager);
        NobleHouse      garrisonOwner  = nobleHouseManager.getOwnerOfZone(zoneId);

        List<ArmyForce> attackerForces = new ArrayList<>();
        List<ArmyForce> playerForces   = new ArrayList<>();

        for (Army playerArmy : playerArmies) {
            int skill = playerArmy.getCommandingSkill();
            ArmyForce f = new ArmyForce("player_" + playerArmy.getId(), playerArmy.getSize(), 0, skill);
            attackerForces.add(f);
            playerForces.add(f);
            Debug.log("player-combat", "player-force",
                    playerArmy.getDisplayName() + " size=" + playerArmy.getSize() + " skill=" + skill);
        }

        // Noble garrison as combined force
        ArmyForce garrisonForce = null;
        if (nobleGarrison > 0 && garrisonOwner != null) {
            int mil = garrisonOwner.getActiveCharacter() != null
                    ? garrisonOwner.getActiveCharacter().getMilitary() : 0;
            garrisonForce = new ArmyForce("garrison_" + garrisonOwner.getId(),
                    nobleGarrison, 0, mil);
            attackerForces.add(garrisonForce);
            log.add(garrisonOwner.getName() + " garrison (" + nobleGarrison + ") joins the fight at " + zoneId + ".");
            Debug.log("player-combat", "garrison-ally",
                    garrisonOwner.getName() + " garrison " + nobleGarrison + " joins at " + zoneId);
        }

        List<ArmyForce> nobleAllyForces = new ArrayList<>();
        for (NobleArmy na : nobleAllies) {
            NobleHouse h = nobleHouseManager.getHouseById(na.getHouseId());
            int mil = h != null && h.getActiveCharacter() != null
                    ? h.getActiveCharacter().getMilitary() : 0;
            ArmyForce f = new ArmyForce(na.getHouseId(), na.getSize(), 0, mil);
            attackerForces.add(f);
            nobleAllyForces.add(f);
            log.add((h != null ? h.getName() : "Noble army") + " joins the player's attack at " + zoneId + ".");
        }

        boolean isDesolate       = zone != null && zone.isDesolate();
        int     barbEffectiveSize = isDesolate
                ? (int)(barb.getSize() * (1.0 + GameParameters.BARB_DEFENDER_BONUS))
                : barb.getSize();

        ArmyForce       barbForce      = new ArmyForce("barbarians", barbEffectiveSize, 0, 0);
        List<ArmyForce> defenderForces = new ArrayList<>();
        defenderForces.add(barbForce);

        int totalPlayerSize = 0;
        for (Army a : playerArmies) totalPlayerSize += a.getSize();
        int totalAllySize = nobleAllies.stream().mapToInt(NobleArmy::getSize).sum();

        log.add("Player attacks " + barb.getType().name()
                + " (player: " + totalPlayerSize
                + ", noble allies: " + totalAllySize
                + ", garrison: " + nobleGarrison
                + " vs barbs: " + barb.getSize() + ")"
                + (isDesolate ? " [desolate +30% barb def]" : ""));

        String leadId = "player_" + playerArmies.get(0).getId();
        CombatResult result = CombatResolver.resolveMultiSideBattle(
                attackerForces, defenderForces, leadId, "barbarians", 0);
        log.addAll(result.getLog());

        // Apply losses to player armies
        for (int i = 0; i < playerArmies.size(); i++) {
            playerArmies.get(i).setSize(playerForces.get(i).getRawSize());
        }

        // Apply losses to garrison
        if (garrisonForce != null && garrisonOwner != null) {
            int garrisonLost = nobleGarrison - garrisonForce.getRawSize();
            if (garrisonLost > 0) garrisonOwner.damageGarrison(zoneId, garrisonLost);
        }

        // Apply losses to noble ally armies
        for (int i = 0; i < nobleAllies.size(); i++) {
            nobleAllies.get(i).setSize(nobleAllyForces.get(i).getRawSize());
        }

        int barbRawLoss = isDesolate
                ? (int)(result.getDefenderLosses() / (1.0 + GameParameters.BARB_DEFENDER_BONUS))
                : result.getDefenderLosses();
        barb.applyLosses(barbRawLoss);

        boolean playerWon = result.getWinnerId() != null
                && result.getWinnerId().startsWith("player_");

        if (playerWon) {
            log.add("✓ Player forces defeat the " + barb.getType().name() + " at " + zoneId + ".");
            Debug.log("player-combat", "outcome", "PLAYER WIN at " + zoneId);
        } else {
            log.add("✗ Player forces are repelled at " + zoneId + ". Armies retreat to Heartland.");
            Debug.log("player-combat", "outcome", "PLAYER LOSS at " + zoneId + " — retreating");
            for (Army a : playerArmies) a.recallToCity();
        }

        return log;
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

        List<String> log = new ArrayList<>();

        List<NobleArmy> nobleAllies   = collectNobleAllies(zoneId, nobleHouseManager);
        int             nobleGarrison = collectNobleGarrison(zoneId, nobleHouseManager);
        NobleHouse      garrisonOwner = nobleHouseManager.getOwnerOfZone(zoneId);

        List<ArmyForce> attackerForces = new ArrayList<>();
        List<ArmyForce> playerForces   = new ArrayList<>();

        for (Army playerArmy : playerArmies) {
            int skill = playerArmy.getCommandingSkill();
            ArmyForce f = new ArmyForce("player_" + playerArmy.getId(), playerArmy.getSize(), 0, skill);
            attackerForces.add(f);
            playerForces.add(f);
        }

        ArmyForce garrisonForce = null;
        if (nobleGarrison > 0 && garrisonOwner != null) {
            int mil = garrisonOwner.getActiveCharacter() != null
                    ? garrisonOwner.getActiveCharacter().getMilitary() : 0;
            garrisonForce = new ArmyForce("garrison_" + garrisonOwner.getId(),
                    nobleGarrison, 0, mil);
            attackerForces.add(garrisonForce);
            log.add(garrisonOwner.getName() + " garrison (" + nobleGarrison + ") joins player assault at " + zoneId + ".");
        }

        List<ArmyForce> nobleAllyForces = new ArrayList<>();
        for (NobleArmy na : nobleAllies) {
            NobleHouse h = nobleHouseManager.getHouseById(na.getHouseId());
            int mil = h != null && h.getActiveCharacter() != null
                    ? h.getActiveCharacter().getMilitary() : 0;
            ArmyForce f = new ArmyForce(na.getHouseId(), na.getSize(), 0, mil);
            attackerForces.add(f);
            nobleAllyForces.add(f);
            log.add((h != null ? h.getName() : "Noble ally") + " joins player's garrison assault at " + zoneId + ".");
        }

        ArmyForce       barbGarrisonForce = new ArmyForce("barbarians", garrison.getSize(), 0, 0);
        List<ArmyForce> defenderForces    = new ArrayList<>();
        defenderForces.add(barbGarrisonForce);

        int totalPlayerSize = 0;
        for (Army a : playerArmies) totalPlayerSize += a.getSize();

        log.add("Player assaults barbarian garrison at " + zoneId
                + " (" + totalPlayerSize + " player"
                + (nobleGarrison > 0 ? ", " + nobleGarrison + " garrison" : "")
                + " vs " + garrison.getSize() + " barb garrison)");

        String leadId = "player_" + playerArmies.get(0).getId();
        CombatResult result = CombatResolver.resolveMultiSideBattle(
                attackerForces, defenderForces, leadId, "barbarians", 0);
        log.addAll(result.getLog());

        for (int i = 0; i < playerArmies.size(); i++) {
            playerArmies.get(i).setSize(playerForces.get(i).getRawSize());
        }

        if (garrisonForce != null && garrisonOwner != null) {
            int garrisonLost = nobleGarrison - garrisonForce.getRawSize();
            if (garrisonLost > 0) garrisonOwner.damageGarrison(zoneId, garrisonLost);
        }

        for (int i = 0; i < nobleAllies.size(); i++) {
            nobleAllies.get(i).setSize(nobleAllyForces.get(i).getRawSize());
        }

        garrison.applyLosses(result.getDefenderLosses());

        boolean playerWon = result.getWinnerId() != null
                && result.getWinnerId().startsWith("player_");

        if (playerWon) {
            log.add("✓ Player forces clear the barbarian garrison from " + zoneId + "!");
            Debug.log("player-combat", "garrison-outcome", "PLAYER WIN — garrison cleared at " + zoneId);
            barbArmyManager.remove(garrison);
            ravagedZoneManager.markRavaged(zoneId);
            log.addAll(awardZoneToClaimant(zoneId, nobleHouseManager, claimManager));
        } else {
            log.add("✗ Player forces fail to clear the garrison at " + zoneId + " and retreat to Heartland.");
            Debug.log("player-combat", "garrison-outcome", "PLAYER LOSS — retreating from " + zoneId);
            for (Army a : playerArmies) a.recallToCity();
        }

        return log;
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

        // Find all claimants that are not eliminated
        List<NobleHouse> claimants = new ArrayList<>();
        for (NobleHouse house : nobleHouseManager.getHouses()) {
            if (!house.isEliminated() && claimManager.hasClaim(house.getId(), zoneId)) {
                claimants.add(house);
            }
        }

        if (claimants.isEmpty()) {
            log.add("No noble house holds a claim on " + zoneId + ". Zone remains ungoverned.");
            return log;
        }

        NobleHouse chosen = null;
        if (zoneAwardCallback != null) {
            chosen = zoneAwardCallback.askPlayerChooseClaimant(zoneId, claimants);
        }

        if (chosen == null) {
            // Default: first claimant alphabetically
            chosen = claimants.get(0);
            log.add("No selection made. " + chosen.getName() + " assumes control of " + zoneId + ".");
        } else {
            log.add(chosen.getName() + " receives " + zoneId + " from the player's liberation.");
        }

        chosen.addZone(zoneId);
        chosen.resetGarrison(zoneId);
        claimManager.removeClaim(chosen.getId(), zoneId);
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