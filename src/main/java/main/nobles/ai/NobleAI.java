package main.nobles.ai;

import main.nobles.*;
import main.nobles.combat.ArmyForce;
import main.nobles.combat.CombatResolver;
import main.nobles.combat.CombatResult;
import main.parameters.GameParameters;

import java.util.*;

/**
 * Per-house AI brain.
 * Each turn: pick motivation (75% dominant / 25% secondary),
 * find best action for that motivation, execute it.
 */
public class NobleAI {

    private static final Random RNG = new Random();

    // ─── Entry point ─────────────────────────────────────────────────────────

    /**
     * Run one AI turn for a single house.
     * Returns log lines describing what happened.
     */
    public static List<String> tick(NobleHouse actor,
                                    List<NobleHouse> allHouses,
                                    RelationshipManager relationships) {
        List<String> log = new ArrayList<>();

        NobleCharacter character = actor.getActiveCharacter();
        if (character == null) return log;

        Motivation motivation = pickMotivation(character);
        NobleAction action    = pickAction(actor, motivation, allHouses, relationships);
        if (action == null) return log;

        log.addAll(execute(actor, action, motivation, allHouses, relationships));
        return log;
    }

    // ─── Motivation selection ─────────────────────────────────────────────────

    private static Motivation pickMotivation(NobleCharacter character) {
        // State-based override: if wealth critically low, force WEALTH motivation
        return RNG.nextDouble() < GameParameters.AI_DOMINANT_MOTIVATION_CHANCE
            ? character.getDominantMotivation()
            : character.getSecondaryMotivation();
    }

    // ─── Action selection ────────────────────────────────────────────────────

    private static NobleAction pickAction(NobleHouse actor, Motivation motivation,
                                          List<NobleHouse> allHouses,
                                          RelationshipManager relationships) {
        List<String> allIds = allHouseIds(allHouses);

        return switch (motivation) {
            case EXPANSION -> {
                // Attack a neighboring rival or neutral with weaker army
                NobleHouse target = findAttackTarget(actor, allHouses, relationships);
                yield target != null ? NobleAction.ATTACK : NobleAction.FORTIFY;
            }
            case WEALTH -> {
                // Raid a neighbor for gold, or demand wealth from weaker house
                NobleHouse raidTarget = findRaidTarget(actor, allHouses, relationships);
                yield raidTarget != null ? NobleAction.RAID : NobleAction.DEMAND;
            }
            case SECURITY -> {
                // Fortify if defense is low, otherwise ally with neighbor
                yield actor.getDefense() < GameParameters.AI_FORTIFY_THRESHOLD
                    ? NobleAction.FORTIFY
                    : NobleAction.ALLY;
            }
            case PRESTIGE -> {
                // Scheme against a rival, or demand prestige from weaker house
                List<String> rivals = relationships.getAll(actor.getId(),
                    Relationship.RIVAL, allIds);
                yield !rivals.isEmpty() ? NobleAction.SCHEME : NobleAction.DEMAND;
            }
        };
    }

    // ─── Execution ───────────────────────────────────────────────────────────

    private static List<String> execute(NobleHouse actor, NobleAction action,
                                        Motivation motivation,
                                        List<NobleHouse> allHouses,
                                        RelationshipManager relationships) {
        List<String> log = new ArrayList<>();
        List<String> allIds = allHouseIds(allHouses);

        switch (action) {

            case ATTACK -> {
                NobleHouse target = findAttackTarget(actor, allHouses, relationships);
                if (target == null) break;
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_ATTACK, log)) break;

                ArmyForce atk = new ArmyForce(actor.getId(),
                    actor.getTotalArmySize(), 0);
                ArmyForce def = new ArmyForce(target.getId(),
                    target.getTotalArmySize(), target.getDefense());

                CombatResult result = CombatResolver.resolve(atk, def);
                log.addAll(result.getLog());

                actor.setTotalArmySize(atk.getArmySize());
                target.setTotalArmySize(def.getArmySize());

                if (actor.getId().equals(result.getWinnerId())) {
                    // Winner takes a zone from loser
                    transferZone(actor, target, allHouses, log);
                    relationships.set(actor.getId(), target.getId(), Relationship.RIVAL);
                } else if (target.getId().equals(result.getWinnerId())) {
                    relationships.set(actor.getId(), target.getId(), Relationship.RIVAL);
                }
            }

            case RAID -> {
                NobleHouse target = findRaidTarget(actor, allHouses, relationships);
                if (target == null) break;
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_RAID, log)) break;

                int stolen = (int)(target.getGold()
                    * GameParameters.AI_RAID_GOLD_FRACTION);
                target.addGold(-stolen);
                actor.addGold(stolen);
                log.add(actor.getName() + " raids " + target.getName()
                    + " and steals " + stolen + " gold.");
                relationships.set(actor.getId(), target.getId(), Relationship.RIVAL);
            }

            case DEMAND -> {
                NobleHouse target = findDemandTarget(actor, allHouses, relationships);
                if (target == null) break;
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_DEMAND, log)) break;

                DemandType type = demandTypeForMotivation(motivation);
                boolean accepted = evaluateDemand(actor, target, relationships, allIds);

                if (accepted) {
                    applyDemand(actor, target, type, log);
                } else {
                    log.add(target.getName() + " refuses " + actor.getName()
                        + "'s demand.");
                    // Refusal worsens relationship
                    if (relationships.get(actor.getId(), target.getId())
                            == Relationship.ALLIED) {
                        relationships.set(actor.getId(), target.getId(),
                            Relationship.NEUTRAL);
                    } else {
                        relationships.set(actor.getId(), target.getId(),
                            Relationship.RIVAL);
                    }
                }
            }

            case SCHEME -> {
                List<String> rivals = relationships.getAll(actor.getId(),
                    Relationship.RIVAL, allIds);
                if (rivals.isEmpty()) break;
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_SCHEME, log)) break;

                String targetId = rivals.get(RNG.nextInt(rivals.size()));
                NobleHouse target = findById(targetId, allHouses);
                if (target == null) break;

                int prestigeLoss = GameParameters.AI_SCHEME_PRESTIGE_LOSS;
                target.addPrestige(-prestigeLoss);
                actor.addPrestige(GameParameters.AI_SCHEME_PRESTIGE_GAIN);
                log.add(actor.getName() + " schemes against " + target.getName()
                    + ". Their prestige suffers.");
            }

            case FORTIFY -> {
                int cost = GameParameters.AI_FORTIFY_GOLD_COST;
                if (actor.getGold() < cost) break;
                actor.addGold(-cost);
                actor.addDefense(GameParameters.AI_FORTIFY_DEFENSE_GAIN);
                log.add(actor.getName() + " fortifies their territory. Defense +"
                    + GameParameters.AI_FORTIFY_DEFENSE_GAIN + ".");
            }

            case ALLY -> {
                NobleHouse target = findAllyTarget(actor, allHouses, relationships);
                if (target == null) break;
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_ALLY, log)) break;

                // Both must not be rivals — target accepts if not a rival
                if (relationships.get(actor.getId(), target.getId())
                        != Relationship.RIVAL) {
                    relationships.set(actor.getId(), target.getId(),
                        Relationship.ALLIED);
                    log.add(actor.getName() + " and " + target.getName()
                        + " form an alliance.");
                }
            }

            case SUPPORT_RIVAL -> {
                // Send army/gold to an ally against a shared rival
                List<String> allies = relationships.getAll(actor.getId(),
                    Relationship.ALLIED, allIds);
                if (allies.isEmpty()) break;
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_SUPPORT, log)) break;

                String allyId = allies.get(RNG.nextInt(allies.size()));
                NobleHouse ally = findById(allyId, allHouses);
                if (ally == null) break;

                int support = (int)(actor.getGold()
                    * GameParameters.AI_SUPPORT_GOLD_FRACTION);
                actor.addGold(-support);
                ally.addGold(support);
                log.add(actor.getName() + " sends " + support
                    + " gold to " + ally.getName() + " in support.");
            }
        }

        return log;
    }

    // ─── Demand evaluation ───────────────────────────────────────────────────

    /**
     * Mostly deterministic demand acceptance formula.
     * score >= 50 → accept.
     */
    public static boolean evaluateDemand(NobleHouse requester, NobleHouse target,
                                          RelationshipManager relationships,
                                          List<String> allIds) {
        double score = GameParameters.DEMAND_BASE_SCORE;

        // Prestige difference: requester high prestige = easier demand
        int prestigeDiff = requester.getPrestige() - target.getPrestige();
        score += prestigeDiff * GameParameters.DEMAND_PRESTIGE_WEIGHT;

        // Army difference: stronger requester is more intimidating
        int armyDiff = requester.getTotalArmySize() - target.getTotalArmySize();
        score += armyDiff * GameParameters.DEMAND_ARMY_WEIGHT;

        // Relationship modifier
        Relationship rel = relationships.get(requester.getId(), target.getId());
        score += switch (rel) {
            case ALLIED  -> GameParameters.DEMAND_ALLIED_BONUS;
            case NEUTRAL -> 0;
            case RIVAL   -> GameParameters.DEMAND_RIVAL_PENALTY;
        };

        // Shared rival bonus
        if (relationships.shareRival(requester.getId(), target.getId(), allIds)) {
            score += GameParameters.DEMAND_SHARED_RIVAL_BONUS;
        }

        // Randomness
        score += (RNG.nextDouble() - 0.5) * 2 * GameParameters.DEMAND_RANDOM_RANGE;

        return score >= GameParameters.DEMAND_ACCEPT_THRESHOLD;
    }

    // ─── Target finders ──────────────────────────────────────────────────────

    private static NobleHouse findAttackTarget(NobleHouse actor,
                                                List<NobleHouse> allHouses,
                                                RelationshipManager relationships) {
        NobleHouse best = null;
        int        bestArmy = Integer.MAX_VALUE;

        for (NobleHouse other : allHouses) {
            if (other == actor) continue;
            Relationship rel = relationships.get(actor.getId(), other.getId());
            if (rel == Relationship.ALLIED) continue;
            if (other.getTotalArmySize() < actor.getTotalArmySize()
                    && other.getTotalArmySize() < bestArmy) {
                best     = other;
                bestArmy = other.getTotalArmySize();
            }
        }
        return best;
    }

    private static NobleHouse findRaidTarget(NobleHouse actor,
                                              List<NobleHouse> allHouses,
                                              RelationshipManager relationships) {
        NobleHouse best = null;
        int        bestGold = 0;

        for (NobleHouse other : allHouses) {
            if (other == actor) continue;
            if (relationships.get(actor.getId(), other.getId())
                    == Relationship.ALLIED) continue;
            if (other.getGold() > bestGold) {
                best     = other;
                bestGold = other.getGold();
            }
        }
        return best;
    }

    private static NobleHouse findDemandTarget(NobleHouse actor,
                                                List<NobleHouse> allHouses,
                                                RelationshipManager relationships) {
        // Prefer allies or neutral houses with fewer resources
        NobleHouse best = null;
        int        bestScore = Integer.MIN_VALUE;

        for (NobleHouse other : allHouses) {
            if (other == actor) continue;
            if (relationships.get(actor.getId(), other.getId())
                    == Relationship.RIVAL) continue;
            int score = -other.getGold(); // prefer weaker targets
            if (score > bestScore) {
                best      = other;
                bestScore = score;
            }
        }
        return best;
    }

    private static NobleHouse findAllyTarget(NobleHouse actor,
                                              List<NobleHouse> allHouses,
                                              RelationshipManager relationships) {
        for (NobleHouse other : allHouses) {
            if (other == actor) continue;
            if (relationships.get(actor.getId(), other.getId())
                    == Relationship.NEUTRAL) return other;
        }
        return null;
    }

    // ─── Demand application ──────────────────────────────────────────────────

    private static void applyDemand(NobleHouse requester, NobleHouse target,
                                     DemandType type, List<String> log) {
        switch (type) {
            case WEALTH -> {
                int amount = (int)(target.getGold()
                    * GameParameters.DEMAND_WEALTH_FRACTION);
                target.addGold(-amount);
                requester.addGold(amount);
                log.add(target.getName() + " yields " + amount
                    + " gold to " + requester.getName() + ".");
            }
            case ARMY -> {
                int amount = GameParameters.DEMAND_ARMY_AMOUNT;
                requester.addToRaisedArmy(amount);
                log.add(target.getName() + " sends " + amount
                    + " soldiers to " + requester.getName() + ".");
            }
            case PRESTIGE -> {
                int amount = GameParameters.DEMAND_PRESTIGE_AMOUNT;
                target.addPrestige(-amount);
                requester.addPrestige(amount);
                log.add(target.getName() + " concedes prestige to "
                    + requester.getName() + ".");
            }
        }
    }

    private static DemandType demandTypeForMotivation(Motivation m) {
        return switch (m) {
            case WEALTH   -> DemandType.WEALTH;
            case EXPANSION, SECURITY -> DemandType.ARMY;
            case PRESTIGE -> DemandType.PRESTIGE;
        };
    }

    // ─── Zone transfer ───────────────────────────────────────────────────────

    private static void transferZone(NobleHouse winner, NobleHouse loser,
                                      List<NobleHouse> allHouses,
                                      List<String> log) {
        List<String> loserZones = new ArrayList<>(loser.getZoneIds());
        if (loserZones.isEmpty()) return;
        String zone = loserZones.get(RNG.nextInt(loserZones.size()));
        loser.removeZone(zone);
        winner.addZone(zone);
        log.add(winner.getName() + " captures " + zone + " from " + loser.getName() + ".");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static boolean canSpendInfluence(NobleHouse house, int cost,
                                              List<String> log) {
        if (house.getInfluence() < cost) {
            return false;
        }
        house.addInfluence(-cost);
        return true;
    }

    private static List<String> allHouseIds(List<NobleHouse> houses) {
        List<String> ids = new ArrayList<>();
        for (NobleHouse h : houses) ids.add(h.getId());
        return ids;
    }

    private static NobleHouse findById(String id, List<NobleHouse> houses) {
        for (NobleHouse h : houses) {
            if (h.getId().equals(id)) return h;
        }
        return null;
    }
}