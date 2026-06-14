package City.main.nobles.ai;

import City.debug.Debug;
import City.main.nobles.ClaimManager;
import City.main.nobles.NobleArmyManager;
import City.main.nobles.NobleCharacter;
import City.main.nobles.NobleHouse;
import City.main.nobles.Relationship;
import City.main.nobles.RelationshipManager;
 

import java.util.List;
import City.main.nobles.Motivation;
import static City.main.nobles.Motivation.EXPANSION;
import static City.main.nobles.Motivation.PRESTIGE;
import static City.main.nobles.Motivation.SECURITY;
import static City.main.nobles.Motivation.WEALTH;
import City.main.parameters.DiplomacyParams;
import City.main.parameters.NobleAIParams;

/** Decides what a house wants (motivation) and what it will do (action) each tick. */
final class NobleAIMotivation {

    private NobleAIMotivation() {}

    static Motivation pickMotivation(NobleCharacter character) {
        if (character == null) return Motivation.SECURITY;
        return NobleAIUtils.RNG.nextDouble() < NobleAIParams.AI_DOMINANT_MOTIVATION_CHANCE
                ? character.getDominantMotivation()
                : character.getSecondaryMotivation();
    }

    static NobleAction pickAction(NobleHouse actor,
                                   Motivation motivation,
                                   List<NobleHouse> allHouses,
                                   RelationshipManager relationships,
                                   ClaimManager claimManager,
                                   NobleArmyManager armyManager) {
        List<String> allIds = NobleAIUtils.allHouseIds(allHouses);

        if (shouldGift(actor, motivation, allHouses, relationships, armyManager)) {
            Debug.log("noble", "action-pick", actor.getName() + " -> GIFT (shouldGift)");
            return NobleAction.GIFT;
        }

        return switch (motivation) {
            case EXPANSION -> {
                boolean hasClaims = hasClaimOnNonAlliedZone(actor, allHouses, relationships, claimManager);
                NobleCharacter ch = actor.getActiveCharacter();
                boolean recklessEligible = ch != null && ch.getCunning() < 2 && ch.getMilitary() >= 2
                        && ch.getDominantMotivation() == Motivation.EXPANSION;
                Debug.log("noble", "action-pick", actor.getName()
                        + " EXPANSION hasClaims=" + hasClaims
                        + " recklessEligible=" + recklessEligible);
                if (hasClaims || recklessEligible) {
                    Debug.log("noble", "action-pick", actor.getName() + " -> ATTACK");
                    yield NobleAction.ATTACK;
                }
                Debug.log("noble", "action-pick", actor.getName() + " -> FABRICATE_CLAIM");
                yield NobleAction.FABRICATE_CLAIM;
            }
            case WEALTH -> {
                NobleHouse raidTarget = NobleAITargeting.findRaidTarget(
                        actor, allHouses, relationships, null);
                yield raidTarget != null ? NobleAction.RAID : NobleAction.DEMAND;
            }
            case SECURITY -> {
                List<String> rivals   = relationships.getAll(actor.getId(), Relationship.RIVAL,   allIds);
                List<String> hostiles = relationships.getAll(actor.getId(), Relationship.HOSTILE, allIds);
                if (!rivals.isEmpty() && actor.getDefense() < NobleAIParams.AI_FORTIFY_THRESHOLD) {
                    yield NobleAction.FORTIFY;
                }
                if (!rivals.isEmpty() || !hostiles.isEmpty()) {
                    NobleCharacter ch = actor.getActiveCharacter();
                    if (ch != null && ch.getCunning() >= 2
                            && NobleAIUtils.RNG.nextDouble() < 0.4) {
                        yield NobleAction.SABOTAGE;
                    }
                }
                int allyCount = relationships.getAll(actor.getId(), Relationship.ALLIED, allIds).size();
                yield allyCount < DiplomacyParams.ALLIANCE_MAX_PER_HOUSE
                        ? NobleAction.ALLY : NobleAction.FORTIFY;
            }
            case PRESTIGE -> {
                NobleHouse supTarget = NobleAITargeting.findSuperiorityTarget(
                        actor, allHouses, relationships);
                if (supTarget != null) yield NobleAction.DEMAND;
                List<String> rivals   = relationships.getAll(actor.getId(), Relationship.RIVAL,   allIds);
                List<String> hostiles = relationships.getAll(actor.getId(), Relationship.HOSTILE, allIds);
                if (!rivals.isEmpty() || !hostiles.isEmpty()) {
                    NobleCharacter ch = actor.getActiveCharacter();
                    if (ch != null && ch.getCunning() >= 1
                            && NobleAIUtils.RNG.nextDouble() < 0.5) {
                        yield NobleAction.SABOTAGE;
                    }
                }
                yield !rivals.isEmpty() ? NobleAction.SCHEME : NobleAction.FORTIFY;
            }
        };
    }

    // -------------------------------------------------------------------------
    // Gift guard
    // -------------------------------------------------------------------------

    static boolean shouldGift(NobleHouse actor,
                               Motivation motivation,
                               List<NobleHouse> allHouses,
                               RelationshipManager relationships,
                               NobleArmyManager armyManager) {
        double weight = switch (motivation) {
            case SECURITY  -> DiplomacyParams.GIFT_WEIGHT_SECURITY;
            case WEALTH    -> actor.getGold() > DiplomacyParams.GIFT_WEALTH_GOLD_THRESHOLD
                    ? DiplomacyParams.GIFT_WEIGHT_WEALTH : 0.0;
            case PRESTIGE  -> DiplomacyParams.GIFT_WEIGHT_PRESTIGE;
            case EXPANSION -> DiplomacyParams.GIFT_WEIGHT_EXPANSION;
        };
        if (NobleAIUtils.RNG.nextDouble() > weight) return false;

        int selfStrength = NobleAIPower.exactPotentialFieldArmy(actor, armyManager)
                + actor.getTotalGarrisonSize();
        for (NobleHouse other : allHouses) {
            if (other == actor || other.isEliminated()) continue;
            Relationship rel = relationships.get(actor.getId(), other.getId());
            if (rel == Relationship.HOSTILE || rel == Relationship.NEUTRAL) {
                int otherStrength = NobleAIPower.estimatedPower(actor, other, armyManager);
                if (otherStrength > selfStrength) return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Motivation priority weights (used by war-chest calculation)
    // -------------------------------------------------------------------------

    static double motivationPriority(Motivation m) {
        return switch (m) {
            case EXPANSION -> NobleAIParams.WAR_CHEST_PRIORITY_EXPANSION;
            case SECURITY  -> NobleAIParams.WAR_CHEST_PRIORITY_SECURITY;
            case WEALTH    -> NobleAIParams.WAR_CHEST_PRIORITY_WEALTH;
            case PRESTIGE  -> NobleAIParams.WAR_CHEST_PRIORITY_PRESTIGE;
        };
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static boolean hasClaimOnNonAlliedZone(NobleHouse actor,
                                                    List<NobleHouse> allHouses,
                                                    RelationshipManager relationships,
                                                    ClaimManager claimManager) {
        for (City.main.nobles.Claim c : claimManager.getClaimsFor(actor.getId())) {
            for (NobleHouse other : allHouses) {
                if (other == actor || other.isEliminated()) continue;
                if (other.getZoneIds().contains(c.getZoneId())) {
                    Relationship rel = relationships.get(actor.getId(), other.getId());
                    if (rel != Relationship.ALLIED && rel != Relationship.FRIENDLY) return true;
                }
            }
        }
        return false;
    }
}