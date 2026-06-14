package City.main.nobles.ai;

import City.main.nobles.Claim;
import City.main.nobles.ClaimManager;
import City.main.nobles.NobleArmyManager;
import City.main.nobles.NobleCharacter;
import City.main.nobles.NobleHouse;
import City.main.nobles.Relationship;
import City.main.nobles.RelationshipManager;
 

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import City.main.nobles.Motivation;
import City.main.parameters.DiplomacyParams;
import City.main.parameters.NobleAIParams;
import City.main.parameters.NobleHouseParams;

/** Manages alliance health, threat status, claim decay, and the war-chest target. */
public final class NobleAIRelations {

    private NobleAIRelations() {}

    // -------------------------------------------------------------------------
    // Alliance health
    // -------------------------------------------------------------------------

    static void considerBreakingAlliances(NobleHouse actor,
                                           List<NobleHouse> allHouses,
                                           RelationshipManager relationships,
                                           NobleArmyManager armyManager,
                                           List<String> log) {
        List<String> allIds = NobleAIUtils.allHouseIds(allHouses);
        List<String> allies = new ArrayList<>(
                relationships.getAll(actor.getId(), Relationship.ALLIED, allIds));

        for (String allyId : allies) {
            NobleHouse ally = NobleAIUtils.findById(allyId, allHouses);
            if (ally == null || ally.isEliminated()) {
                relationships.set(actor.getId(), allyId, Relationship.NEUTRAL);
                continue;
            }
            int allyPower = NobleAIPower.estimatedPower(actor, ally, armyManager)
                    + (int) (0.7 * ally.getTotalGarrisonSize());
            boolean tooWeak = allyPower < NobleAIPower.exactPotentialFieldArmy(actor, armyManager)
                    * DiplomacyParams.ALLIANCE_MIN_ARMY_FRACTION;
            if (tooWeak) {
                NobleCharacter c   = actor.getActiveCharacter();
                int            dip = c != null ? c.getDiplomacy() : 0;
                double cleanChance = DiplomacyParams.ALLIANCE_BREAK_CLEAN_BASE
                        + dip * DiplomacyParams.ALLIANCE_BREAK_CLEAN_PER_DIPLOMACY;
                Relationship result = NobleAIUtils.RNG.nextDouble() < cleanChance
                        ? Relationship.NEUTRAL : Relationship.HOSTILE;
                relationships.set(actor.getId(), allyId, result);
                log.add(actor.getName() + " breaks alliance with "
                        + ally.getName() + ". New relation: " + result.name());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Threat tracking
    // -------------------------------------------------------------------------

    public static void updateThreatenedStatus(NobleHouse attacker,
                                               List<NobleHouse> allHouses,
                                               RelationshipManager relationships) {
        updateThreatenedStatus(attacker, allHouses, relationships, 1.0);
    }

    public static void updateThreatenedStatus(NobleHouse attacker,
                                               List<NobleHouse> allHouses,
                                               RelationshipManager relationships,
                                               double multiplier) {
        int attackerZones = attacker.getZoneIds().size();
        int totalZones    = 0;
        for (NobleHouse h : allHouses) totalZones += h.getZoneIds().size();

        for (NobleHouse observer : allHouses) {
            if (observer == attacker || observer.isEliminated()) continue;
            Relationship rel = relationships.get(attacker.getId(), observer.getId());
            if (rel != Relationship.NEUTRAL) continue;

            int observerZones = observer.getZoneIds().size();
            double chance = (double) (attackerZones - observerZones)
                    / Math.max(1, totalZones)
                    * DiplomacyParams.THREATENED_BASE_CHANCE_MULTIPLIER
                    * multiplier;
            chance = Math.max(0, Math.min(1.0, chance));

            if (NobleAIUtils.RNG.nextDouble() < chance) {
                observer.addThreat(attacker.getId());
            }
        }
    }

    public static void tickThreatenedDecay(List<NobleHouse> allHouses) {
        for (NobleHouse house : allHouses) {
            Set<String> threats = new HashSet<>(house.getThreatenedBy());
            for (String threatId : threats) {
                if (NobleAIUtils.RNG.nextDouble() < DiplomacyParams.THREATENED_DECAY_CHANCE) {
                    house.removeThreat(threatId);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Claim decay
    // -------------------------------------------------------------------------

    public static void tickClaimDecay(List<NobleHouse> allHouses,
                                       RelationshipManager relationships,
                                       ClaimManager claimManager,
                                       List<String> log) {
        for (NobleHouse house : allHouses) {
            if (house.isEliminated()) continue;

            Claim decayed = claimManager.rollClaimDecay(house.getId(), NobleAIUtils.RNG);
            if (decayed == null) continue;

            boolean keep = false;
            for (NobleHouse other : allHouses) {
                if (other.getZoneIds().contains(decayed.getZoneId())) {
                    Relationship rel = relationships.get(house.getId(), other.getId());
                    if (rel == Relationship.RIVAL || rel == Relationship.HOSTILE) {
                        keep = true;
                        break;
                    }
                    if (rel == Relationship.ALLIED || rel == Relationship.FRIENDLY) {
                        if (house.getInfluence() >= DiplomacyParams.CLAIM_DECAY_INFLUENCE_COST * 10) {
                            keep = true;
                        }
                    }
                }
            }
            if (!keep && house.getActiveCharacter() != null
                    && house.getActiveCharacter().getDominantMotivation() == Motivation.EXPANSION) {
                keep = true;
            }

            if (keep && house.getInfluence() >= DiplomacyParams.CLAIM_DECAY_INFLUENCE_COST) {
                house.addInfluence(-DiplomacyParams.CLAIM_DECAY_INFLUENCE_COST);
            } else {
                claimManager.removeClaim(house.getId(), decayed.getZoneId());
            }
        }
    }

    // -------------------------------------------------------------------------
    // War-chest target
    // -------------------------------------------------------------------------

    static int getWarChestTarget(NobleHouse actor,
                                  List<NobleHouse> allHouses,
                                  RelationshipManager relationships,
                                  NobleArmyManager armyManager) {
        int maxEnemyPower = 0;
        for (NobleHouse other : allHouses) {
            if (other == actor || other.isEliminated()) continue;
            Relationship rel = relationships.get(actor.getId(), other.getId());
            if (rel != Relationship.RIVAL && rel != Relationship.HOSTILE) continue;
            int enemyPower = NobleAIPower.estimatedPower(actor, other, armyManager);
            if (enemyPower > maxEnemyPower) maxEnemyPower = enemyPower;
        }
        if (maxEnemyPower < 5) maxEnemyPower = 5;

        int myMil = actor.getActiveCharacter() != null
                ? actor.getActiveCharacter().getMilitary() : 0;
        double myMult = 1.0 + myMil * DiplomacyParams.MILITARY_SKILL_BONUS_PER_POINT;
        int neededSoldiers = (int) Math.ceil(maxEnemyPower / myMult);

        int recruitCost = neededSoldiers * NobleHouseParams.NOBLE_UPKEEP_COST_PER_SOLDIER;
        int upkeepCost  = neededSoldiers * NobleHouseParams.NOBLE_UPKEEP_COST_PER_SOLDIER
                * NobleAIParams.WAR_CHEST_UPKEEP_TURNS;
        int baseGold = recruitCost + upkeepCost;

        NobleCharacter ch  = actor.getActiveCharacter();
        Motivation dom = ch != null ? ch.getDominantMotivation()  : Motivation.SECURITY;
        Motivation sec = ch != null ? ch.getSecondaryMotivation() : Motivation.SECURITY;
        double priority = 0.75 * NobleAIMotivation.motivationPriority(dom)
                + 0.25 * NobleAIMotivation.motivationPriority(sec);

        int cunning = ch != null ? ch.getCunning() : 0;
        double fuzzRange = NobleAIParams.WAR_CHEST_FUZZ_BASE
                + (4 - cunning) * NobleAIParams.WAR_CHEST_FUZZ_PER_MISSING;
        double fuzz = 1.0 + (NobleAIUtils.RNG.nextDouble() * 2 - 1) * fuzzRange;

        int target = (int) (baseGold * priority * fuzz);
        if (target < NobleHouseParams.NOBLE_ARMY_RECRUIT_GOLD_THRESHOLD) {
            target = NobleHouseParams.NOBLE_ARMY_RECRUIT_GOLD_THRESHOLD;
        }
        return Math.max(0, target);
    }
}