package City.main.nobles.ai;

import City.debug.Debug;
 
import City.main.map.Zone;
import City.main.nobles.Claim;
import City.main.nobles.ClaimManager;
import City.main.nobles.Motivation;
import City.main.nobles.NobleArmyManager;
import City.main.nobles.NobleCharacter;
import City.main.nobles.NobleHouse;
import City.main.nobles.Relationship;
import City.main.nobles.RelationshipManager;
import City.main.parameters.NobleAIParams;

import java.util.*;

/**
 * Runs before the normal motivation-based action.
 * Lets houses with expansionist or security leanings launch
 * opportunistic attacks when they have overwhelming superiority.
 */
public class OpportunismEvaluator {

    private static final Random RNG = new Random();
    private static final Set<String> cooldownHouses = new HashSet<>();

    public static void reset() {
        cooldownHouses.clear();
    }

    /**
     * Evaluate whether the actor should take an opportunistic action this turn.
     * Returns an action if one is taken, null otherwise.
     */
    public static NobleAction evaluate(NobleHouse actor,
                                       List<NobleHouse> allHouses,
                                       RelationshipManager relationships,
                                       ClaimManager claimManager,
                                       NobleArmyManager armyManager,
                                       City.main.map.ZoneManager zoneManager,
                                       List<String> log) {
        // Honour cooldown from a previous failed fabrication
        if (cooldownHouses.contains(actor.getId())) {
            cooldownHouses.remove(actor.getId());
            return null;
        }

        NobleCharacter character = actor.getActiveCharacter();
        if (character == null) return null;

        Motivation dom = character.getDominantMotivation();
        Motivation sec = character.getSecondaryMotivation();

        boolean expansionMinor = (sec == Motivation.EXPANSION);
        boolean securityMajor  = (dom == Motivation.SECURITY);
        boolean securityMinor  = (sec == Motivation.SECURITY && RNG.nextDouble() < 0.25);
        boolean prestigeMajor  = (dom == Motivation.PRESTIGE);
        boolean wealthMajor    = (dom == Motivation.WEALTH);

        if (!expansionMinor && !securityMajor && !securityMinor && !prestigeMajor && !wealthMajor) return null;

        Debug.log("noble", "opportunism", actor.getName() + " evaluating opportunism"
                + " (expansionMinor=" + expansionMinor
                + ", securityMajor=" + securityMajor
                + ", securityMinor=" + securityMinor + ")");

        // ── 1. Build list of potential targets per active trait ─────
        List<NobleHouse> eligible = new ArrayList<>();
        int myPower = NobleAI.exactPotentialFieldArmy(actor, armyManager);

        if (expansionMinor) {
            double requiredRatio = NobleAIParams.OPPORTUNISM_STRENGTH_RATIO_HOSTILE;
            for (NobleHouse other : allHouses) {
                if (other == actor || other.isEliminated()) continue;
                Relationship rel = relationships.get(actor.getId(), other.getId());
                if (rel == Relationship.HOSTILE || rel == Relationship.RIVAL || rel == Relationship.NEUTRAL) {
                    double ratio = (rel == Relationship.NEUTRAL)
                            ? NobleAIParams.OPPORTUNISM_STRENGTH_RATIO_NEUTRAL
                            : requiredRatio;
                    int targetPower = NobleAI.estimatedPower(actor, other, armyManager)
                                    + other.getTotalGarrisonSize();
                    if (targetPower > 0 && myPower >= targetPower * ratio) {
                        eligible.add(other);
                    }
                }
            }
        }

        if (prestigeMajor) {
            for (NobleHouse other : allHouses) {
                if (other == actor || other.isEliminated()) continue;
                Relationship rel = relationships.get(actor.getId(), other.getId());
                if (rel != Relationship.RIVAL) continue;
                int targetPower = NobleAI.estimatedPower(actor, other, armyManager)
                                + other.getTotalGarrisonSize();
                if (targetPower > 0 && myPower >= targetPower * NobleAIParams.OPPORTUNISM_PRESTIGE_STRENGTH_RATIO) {
                    eligible.add(other);
                }
            }
        }

        if (wealthMajor) {
            // Calculate average gold production across all zones
            double totalGold = 0;
            int totalZones = 0;
            for (City.main.map.Zone z : zoneManager.getZones()) {
                totalGold += z.getGoldProduction();
                totalZones++;
            }
            double avgGold = (totalZones > 0) ? (totalGold / totalZones) : 0;
            for (NobleHouse other : allHouses) {
                if (other == actor || other.isEliminated()) continue;
                Relationship rel = relationships.get(actor.getId(), other.getId());
                if (rel == Relationship.ALLIED || rel == Relationship.FRIENDLY) continue;
                // Check if any zone owned by other has above‑average gold
                boolean hasRichZone = false;
                for (String zid : other.getZoneIds()) {
                    City.main.map.Zone z = zoneManager.getZone(zid);
                    if (z != null && z.getGoldProduction() > avgGold) {
                        hasRichZone = true;
                        break;
                    }
                }
                if (!hasRichZone) continue;
                int targetPower = NobleAI.estimatedPower(actor, other, armyManager)
                                + other.getTotalGarrisonSize();
                if (targetPower > 0 && myPower >= targetPower * NobleAIParams.OPPORTUNISM_WEALTH_STRENGTH_RATIO) {
                    eligible.add(other);
                }
            }
        }

        if (securityMajor || securityMinor) {
            for (NobleHouse other : allHouses) {
                if (other == actor || other.isEliminated()) continue;
                Relationship rel = relationships.get(actor.getId(), other.getId());
                if (rel != Relationship.HOSTILE && rel != Relationship.RIVAL) continue;
                boolean hasClaimOnActor = false;
                for (Claim c : claimManager.getClaimsFor(other.getId())) {
                    if (actor.getZoneIds().contains(c.getZoneId())) {
                        hasClaimOnActor = true;
                        break;
                    }
                }
                if (!hasClaimOnActor) continue;
                int targetPower = NobleAI.estimatedPower(actor, other, armyManager)
                                + other.getTotalGarrisonSize();
                if (targetPower > 0 && myPower >= targetPower * NobleAIParams.OPPORTUNISM_SECURITY_STRENGTH_RATIO) {
                    eligible.add(other);
                }
            }
        }

        if (eligible.isEmpty()) {
            Debug.log("noble", "opportunism", actor.getName() + " no valid target found");
            return null;
        }

        // ── 3. Attack the weakest target with a claim ───────────────
        NobleHouse bestTarget   = null;
        String     bestClaimZone = null;
        int        bestDefPower  = Integer.MAX_VALUE;

        for (NobleHouse target : eligible) {
            for (Claim c : claimManager.getClaimsFor(actor.getId())) {
                if (target.getZoneIds().contains(c.getZoneId())) {
                    int defPower = NobleAI.estimateDefenderCombatPower(
                            actor, target, c.getZoneId(), allHouses, armyManager, relationships);
                    if (defPower < bestDefPower) {
                        bestDefPower  = defPower;
                        bestTarget    = target;
                        bestClaimZone = c.getZoneId();
                    }
                }
            }
        }

        if (bestTarget != null && bestClaimZone != null) {
            if (armyManager.hasPendingAttackOrder(actor.getId(), bestClaimZone)) {
                Debug.log("noble", "opportunism", actor.getName()
                        + " skipping opportunistic attack on " + bestTarget.getName()
                        + " zone " + bestClaimZone + " – already attacking");
                return null;
            }
            Debug.log("noble", "opportunism", actor.getName() + " opportunistic attack on "
                    + bestTarget.getName() + " zone " + bestClaimZone);
            return NobleAction.ATTACK;
        }

        // ── 4. No claims – fabricate on weakest eligible target ─────
        NobleHouse fabricateTarget = null;
        String     fabricateZone   = null;
        int        lowestPower     = Integer.MAX_VALUE;

        // 4a. Prefer adjacent
        for (NobleHouse target : eligible) {
            int targetPower = NobleAI.estimatedPower(actor, target, armyManager)
                            + target.getTotalGarrisonSize();
            for (String zoneId : target.getZoneIds()) {
                Zone zone = zoneManager.getZone(zoneId);
                if (zone != null) {
                    boolean adjacent = false;
                    for (String adjId : zone.getAdjacentIds()) {
                        if (actor.getZoneIds().contains(adjId)) {
                            adjacent = true;
                            break;
                        }
                    }
                    if (adjacent && targetPower < lowestPower
                            && !claimManager.hasClaim(actor.getId(), zoneId)) {
                        lowestPower     = targetPower;
                        fabricateTarget = target;
                        fabricateZone   = zoneId;
                    }
                }
            }
        }

        // 4b. Fallback – any eligible target
        if (fabricateTarget == null) {
            for (NobleHouse target : eligible) {
                int targetPower = NobleAI.estimatedPower(actor, target, armyManager)
                                + target.getTotalGarrisonSize();
                for (String zoneId : target.getZoneIds()) {
                    if (targetPower < lowestPower
                            && !claimManager.hasClaim(actor.getId(), zoneId)) {
                        lowestPower     = targetPower;
                        fabricateTarget = target;
                        fabricateZone   = zoneId;
                    }
                }
            }
        }

        if (fabricateTarget != null && fabricateZone != null) {
            int cunning = character.getCunning();
            List<String> myZones = new ArrayList<>(actor.getZoneIds());
            int ownerCunning = 0;
            for (NobleHouse other : allHouses) {
                if (other.getZoneIds().contains(fabricateZone)) {
                    ownerCunning = other.getActiveCharacter() != null ? other.getActiveCharacter().getCunning() : 0;
                    break;
                }
            }
            boolean success = claimManager.fabricate(
                    actor.getId(), fabricateZone, cunning, ownerCunning, RNG,
                    myZones, zoneManager.getZones());
            Debug.log("noble", "opportunism", actor.getName() + " opportunism fabrication on "
                    + fabricateTarget.getName() + " zone " + fabricateZone
                    + " success=" + success);
            if (success) {
                log.add(actor.getName() + " fabricates a claim on "
                        + fabricateZone + " (opportunistic).");
                for (NobleHouse other : allHouses) {
                    if (other.getZoneIds().contains(fabricateZone)) {
                        relationships.set(actor.getId(), other.getId(), Relationship.RIVAL);
                        log.add(other.getName() + " becomes rival with "
                                + actor.getName() + " over the claim.");
                        break;
                    }
                }
            } else {
                log.add(actor.getName() + " fails to fabricate a claim on "
                        + fabricateZone + " (opportunistic).");
                cooldownHouses.add(actor.getId());
                Debug.log("noble", "opportunism", actor.getName() + " fabrication failed, cooldown applied");
            }
            // The action for this turn was the fabrication attempt itself
            return null;
        }

        return null;
    }
}