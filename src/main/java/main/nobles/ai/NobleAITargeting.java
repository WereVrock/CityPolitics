package main.nobles.ai;

import debug.Debug;
import main.map.ZoneManager;
import main.map.ZoneState;
import main.nobles.Claim;
import main.nobles.ClaimManager;
import main.nobles.NobleArmyManager;
import main.nobles.NobleHouse;
import main.nobles.Relationship;
import main.nobles.RelationshipManager;
import main.parameters.GameParameters;

import java.util.ArrayList;
import java.util.List;

/** Finds the best targets for every offensive and diplomatic action. */
final class NobleAITargeting {

    private NobleAITargeting() {}

    static NobleHouse findRaidTarget(NobleHouse actor,
                                     List<NobleHouse> allHouses,
                                     RelationshipManager relationships,
                                     ZoneManager zoneManager) {
        NobleHouse best     = null;
        int        bestGold = 0;
        for (NobleHouse other : allHouses) {
            if (other == actor || other.isEliminated()) continue;
            Relationship rel = relationships.get(actor.getId(), other.getId());
            if (rel == Relationship.ALLIED || rel == Relationship.FRIENDLY) continue;
            if (zoneManager != null && pickRaidableZone(other, zoneManager) == null) continue;
            if (other.getGold() > bestGold) {
                best     = other;
                bestGold = other.getGold();
            }
        }
        return best;
    }

    static NobleHouse findDemandTarget(NobleHouse actor,
                                       List<NobleHouse> allHouses,
                                       RelationshipManager relationships) {
        NobleHouse best      = null;
        int        bestScore = Integer.MIN_VALUE;
        for (NobleHouse other : allHouses) {
            if (other == actor || other.isEliminated()) continue;
            if (relationships.get(actor.getId(), other.getId()) == Relationship.RIVAL) continue;
            int score = -other.getGold();
            if (score > bestScore) { best = other; bestScore = score; }
        }
        return best;
    }

    static NobleHouse findSuperiorityTarget(NobleHouse actor,
                                             List<NobleHouse> allHouses,
                                             RelationshipManager relationships) {
        int actorMilitary = actor.getActiveCharacter() != null
                ? actor.getActiveCharacter().getMilitary() : 0;
        for (NobleHouse other : allHouses) {
            if (other == actor || other.isEliminated()) continue;
            if (relationships.get(actor.getId(), other.getId()) == Relationship.RIVAL) continue;
            int otherMilitary = other.getActiveCharacter() != null
                    ? other.getActiveCharacter().getMilitary() : 0;
            if (actorMilitary > otherMilitary) return other;
        }
        return null;
    }

    static NobleHouse findAllyTarget(NobleHouse actor,
                                     List<NobleHouse> allHouses,
                                     RelationshipManager relationships) {
        for (NobleHouse other : allHouses) {
            if (other == actor || other.isEliminated()) continue;
            if (relationships.get(actor.getId(), other.getId()) == Relationship.NEUTRAL) return other;
        }
        return null;
    }

    static NobleHouse findGiftTarget(NobleHouse actor,
                                     List<NobleHouse> allHouses,
                                     RelationshipManager relationships,
                                     NobleArmyManager armyManager) {
        NobleHouse best      = null;
        int        bestPower = 0;
        for (NobleHouse other : allHouses) {
            if (other == actor || other.isEliminated()) continue;
            Relationship rel = relationships.get(actor.getId(), other.getId());
            if (rel == Relationship.RIVAL || rel == Relationship.ALLIED) continue;
            int otherPower = NobleAIPower.estimatedPower(actor, other, armyManager);
            if (otherPower > bestPower) {
                best      = other;
                bestPower = otherPower;
            }
        }
        return best;
    }

    static String findClaimTarget(NobleHouse actor,
                                   List<NobleHouse> allHouses,
                                   ClaimManager claimManager) {
        for (NobleHouse other : allHouses) {
            if (other == actor || other.isEliminated()) continue;
            for (String zoneId : other.getZoneIds()) {
                if (!claimManager.hasClaim(actor.getId(), zoneId)) return zoneId;
            }
        }
        return null;
    }

    static NobleHouse findSabotageTarget(NobleHouse actor,
                                          List<NobleHouse> allHouses,
                                          RelationshipManager relationships) {
        List<NobleHouse> candidates = new ArrayList<>();
        for (NobleHouse other : allHouses) {
            if (other == actor || other.isEliminated()) continue;
            Relationship rel = relationships.get(actor.getId(), other.getId());
            if (rel != Relationship.RIVAL && rel != Relationship.HOSTILE) continue;
            boolean hasFort = false;
            for (String zid : other.getZoneIds()) {
                if (other.getFortificationFor(zid) > 0) { hasFort = true; break; }
            }
            if (hasFort) candidates.add(other);
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(NobleAIUtils.RNG.nextInt(candidates.size()));
    }

    /**
     * Finds the best zone to attack without a claim (reckless expansion).
     * Returns {@code Object[]{NobleHouse target, String zoneId}} or {@code null}.
     */
    static Object[] findRecklessClaimlessTarget(NobleHouse actor,
                                                 List<NobleHouse> allHouses,
                                                 RelationshipManager relationships,
                                                 ClaimManager claimManager,
                                                 ZoneManager zoneManager,
                                                 NobleArmyManager armyManager) {
        int myPower = NobleAIPower.estimateAttackPower(actor, armyManager);
        Debug.log("noble", "reckless-scan", actor.getName() + " myPower=" + myPower);
        if (myPower <= 0) {
            Debug.log("noble", "reckless-scan", actor.getName() + " no power -> abort");
            return null;
        }

        // Find the best value among attackable claimed zones (for comparison threshold)
        double bestClaimedValue = 0;
        for (Claim c : claimManager.getClaimsFor(actor.getId())) {
            for (NobleHouse other : allHouses) {
                if (other == actor || other.isEliminated()) continue;
                if (!other.getZoneIds().contains(c.getZoneId())) continue;
                Relationship rel = relationships.get(actor.getId(), other.getId());
                if (rel == Relationship.ALLIED || rel == Relationship.FRIENDLY) continue;
                int defPower = NobleAIPower.estimateDefenderCombatPower(
                        actor, other, c.getZoneId(), allHouses, armyManager, relationships);
                if (defPower <= 0) continue;
                main.map.Zone z = zoneManager.getZone(c.getZoneId());
                if (z == null) continue;
                double value = (double) z.getGoldProduction() / defPower;
                if (value > bestClaimedValue) bestClaimedValue = value;
                break;
            }
        }
        Debug.log("noble", "reckless-scan",
                actor.getName() + " bestClaimedValue (attackable)=" + bestClaimedValue);

        NobleHouse bestTarget = null;
        String     bestZone   = null;
        double     bestValue  = 0;
        int scanned = 0, rejectedTooStrong = 0;

        for (NobleHouse other : allHouses) {
            if (other == actor || other.isEliminated()) continue;
            Relationship rel = relationships.get(actor.getId(), other.getId());
            if (rel != Relationship.RIVAL && rel != Relationship.HOSTILE) continue;
            for (String zid : other.getZoneIds()) {
                scanned++;
                int defPower = NobleAIPower.estimateDefenderCombatPower(
                        actor, other, zid, allHouses, armyManager, relationships);
                if (defPower <= 0) continue;
                double neededPower  = defPower * GameParameters.RECKLESS_MIN_STRENGTH;
                boolean strongEnough = myPower >= neededPower;
                main.map.Zone z = zoneManager.getZone(zid);
                if (z == null) continue;
                double value = (double) z.getGoldProduction() / defPower;
                Debug.log("noble", "reckless-scan", actor.getName()
                        + " zone=" + zid + " owner=" + other.getName()
                        + " defPower=" + defPower + " neededPower=" + neededPower
                        + " strongEnough=" + strongEnough + " value=" + value);
                if (!strongEnough) { rejectedTooStrong++; continue; }
                if (value > bestValue) {
                    bestValue  = value;
                    bestTarget = other;
                    bestZone   = zid;
                }
            }
        }

        Debug.log("noble", "reckless-scan", actor.getName()
                + " scanned=" + scanned + " rejectedTooStrong=" + rejectedTooStrong
                + " bestTarget=" + (bestTarget != null ? bestTarget.getName() : "null")
                + " bestZone=" + bestZone + " bestValue=" + bestValue);

        if (bestTarget == null) {
            Debug.log("noble", "reckless-scan", actor.getName() + " no valid target -> abort");
            return null;
        }

        double requiredValue = bestClaimedValue * GameParameters.RECKLESS_VALUE_MULTIPLIER;
        boolean passes = bestValue >= requiredValue;
        Debug.log("noble", "reckless-scan", actor.getName()
                + " bestValue=" + bestValue + " requiredValue=" + requiredValue
                + " passes=" + passes);
        if (!passes) {
            Debug.log("noble", "reckless-scan", actor.getName() + " not better enough -> abort");
            return null;
        }

        Debug.log("noble", "reckless-scan", actor.getName() + " -> FOUND " + bestZone);
        return new Object[]{bestTarget, bestZone};
    }

    /** Returns the first non-recently-raided zone owned by {@code target}, or null. */
    static String pickRaidableZone(NobleHouse target, ZoneManager zoneManager) {
        for (String zoneId : target.getZoneIds()) {
            ZoneState state = zoneManager.getState(zoneId);
            if (state != null && !state.isRecentlyRaided()) return zoneId;
        }
        return null;
    }
}