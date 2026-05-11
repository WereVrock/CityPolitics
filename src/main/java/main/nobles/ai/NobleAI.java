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
 * find best action, execute it.
 */
public class NobleAI {

    private static final Random RNG = new Random();

    // ─── Entry point ─────────────────────────────────────────────────────────

    public static List<String> tick(NobleHouse actor,
                                    List<NobleHouse> allHouses,
                                    RelationshipManager relationships,
                                    ClaimManager claimManager) {
        List<String> log = new ArrayList<>();
        if (actor.isEliminated()) return log;

        NobleCharacter character = actor.getActiveCharacter();
        if (character == null) return log;

        // Decay hostile relationships
        relationships.tickDecay(allHouseIds(allHouses));

        // Consider breaking bad alliances first (free action)
        considerBreakingAlliances(actor, allHouses, relationships, log);

        Motivation motivation = pickMotivation(character);
        NobleAction action    = pickAction(actor, motivation, allHouses,
                                           relationships, claimManager);
        if (action == null) return log;

        log.addAll(execute(actor, action, motivation, allHouses,
                           relationships, claimManager));
        return log;
    }

    // ─── Alliance breaking (free action) ─────────────────────────────────────

    private static void considerBreakingAlliances(NobleHouse actor,
                                                   List<NobleHouse> allHouses,
                                                   RelationshipManager relationships,
                                                   List<String> log) {
        List<String> allIds = allHouseIds(allHouses);
        List<String> allies = relationships.getAll(actor.getId(),
            Relationship.ALLIED, allIds);

        for (String allyId : allies) {
            NobleHouse ally = findById(allyId, allHouses);
            if (ally == null || ally.isEliminated()) {
                relationships.set(actor.getId(), allyId, Relationship.NEUTRAL);
                continue;
            }
            // Break if ally is too weak (< 50% of own army)
            boolean tooWeak = ally.getTotalArmySize()
                < actor.getTotalArmySize() * GameParameters.ALLIANCE_MIN_ARMY_FRACTION;
            if (tooWeak) {
                // Cleanness based on diplomacy: higher diplomacy = more likely NEUTRAL not HOSTILE
                NobleCharacter c  = actor.getActiveCharacter();
                int            dip = c != null ? c.getDiplomacy() : 0;
                double         cleanChance = GameParameters.ALLIANCE_BREAK_CLEAN_BASE
                    + dip * GameParameters.ALLIANCE_BREAK_CLEAN_PER_DIPLOMACY;
                Relationship result = RNG.nextDouble() < cleanChance
                    ? Relationship.NEUTRAL
                    : Relationship.HOSTILE;
                relationships.set(actor.getId(), allyId, result);
                log.add(actor.getName() + " breaks alliance with "
                    + ally.getName() + ". New relation: " + result.name());
            }
        }
    }

    // ─── Motivation selection ─────────────────────────────────────────────────

    private static Motivation pickMotivation(NobleCharacter character) {
        return RNG.nextDouble() < GameParameters.AI_DOMINANT_MOTIVATION_CHANCE
            ? character.getDominantMotivation()
            : character.getSecondaryMotivation();
    }

    // ─── Action selection ────────────────────────────────────────────────────

    private static NobleAction pickAction(NobleHouse actor, Motivation motivation,
                                          List<NobleHouse> allHouses,
                                          RelationshipManager relationships,
                                          ClaimManager claimManager) {
        List<String> allIds = allHouseIds(allHouses);

        // Gift if hostile with strong neighbor and security-minded
        if (shouldGift(actor, motivation, allHouses, relationships, allIds)) {
            return NobleAction.GIFT;
        }

        return switch (motivation) {
            case EXPANSION -> {
                // Need a claim to attack. Fabricate if none, attack if have one.
                List<Claim> claims = claimManager.getClaimsFor(actor.getId());
                if (!claims.isEmpty()) {
                    NobleHouse target = findAttackTarget(actor, allHouses,
                        relationships, claimManager);
                    if (target != null) yield NobleAction.ATTACK;
                }
                yield NobleAction.FABRICATE_CLAIM;
            }
            case WEALTH -> {
                NobleHouse raidTarget = findRaidTarget(actor, allHouses, relationships);
                yield raidTarget != null ? NobleAction.RAID : NobleAction.DEMAND;
            }
            case SECURITY -> {
                List<String> rivals = relationships.getAll(actor.getId(),
                    Relationship.RIVAL, allIds);
                if (!rivals.isEmpty() && actor.getDefense()
                        < GameParameters.AI_FORTIFY_THRESHOLD) {
                    yield NobleAction.FORTIFY;
                }
                int allyCount = relationships.getAll(actor.getId(),
                    Relationship.ALLIED, allIds).size();
                yield allyCount < GameParameters.ALLIANCE_MAX_PER_HOUSE
                    ? NobleAction.ALLY
                    : NobleAction.FORTIFY;
            }
            case PRESTIGE -> {
                List<String> rivals = relationships.getAll(actor.getId(),
                    Relationship.RIVAL, allIds);
                yield !rivals.isEmpty() ? NobleAction.SCHEME : NobleAction.DEMAND;
            }
        };
    }

    // ─── Gift decision ───────────────────────────────────────────────────────

    private static boolean shouldGift(NobleHouse actor, Motivation motivation,
                                       List<NobleHouse> allHouses,
                                       RelationshipManager relationships,
                                       List<String> allIds) {
        double weight = switch (motivation) {
            case SECURITY  -> GameParameters.GIFT_WEIGHT_SECURITY;
            case WEALTH    -> actor.getGold() > GameParameters.GIFT_WEALTH_GOLD_THRESHOLD
                              ? GameParameters.GIFT_WEIGHT_WEALTH : 0.0;
            case PRESTIGE  -> GameParameters.GIFT_WEIGHT_PRESTIGE;
            case EXPANSION -> GameParameters.GIFT_WEIGHT_EXPANSION;
        };

        if (RNG.nextDouble() > weight) return false;

        // Only gift if there's a hostile neighbor stronger than us
        for (NobleHouse other : allHouses) {
            if (other == actor || other.isEliminated()) continue;
            Relationship rel = relationships.get(actor.getId(), other.getId());
            if ((rel == Relationship.HOSTILE || rel == Relationship.NEUTRAL)
                    && other.getTotalArmySize() > actor.getTotalArmySize()) {
                return true;
            }
        }
        return false;
    }

    // ─── Execution ───────────────────────────────────────────────────────────

    private static List<String> execute(NobleHouse actor, NobleAction action,
                                        Motivation motivation,
                                        List<NobleHouse> allHouses,
                                        RelationshipManager relationships,
                                        ClaimManager claimManager) {
        List<String> log     = new ArrayList<>();
        List<String> allIds  = allHouseIds(allHouses);
        NobleCharacter character = actor.getActiveCharacter();
        int cunning   = character != null ? character.getCunning()   : 0;
        int military  = character != null ? character.getMilitary()  : 0;
        int diplomacy = character != null ? character.getDiplomacy() : 0;

        switch (action) {

            case FABRICATE_CLAIM -> {
                if (!canSpendInfluence(actor,
                        GameParameters.AI_INFLUENCE_COST_FABRICATE, log)) break;

                // Pick a zone not owned by actor, not already claimed
                String targetZone = findClaimTarget(actor, allHouses, claimManager);
                if (targetZone == null) break;

                boolean success = claimManager.fabricate(actor.getId(),
                    targetZone, cunning, RNG);

                if (success) {
                    log.add(actor.getName() + " fabricates a claim on "
                        + targetZone + ".");
                    // Owner becomes RIVAL with claimant
                    for (NobleHouse other : allHouses) {
                        if (other.getZoneIds().contains(targetZone)) {
                            relationships.set(actor.getId(), other.getId(),
                                Relationship.RIVAL);
                            log.add(other.getName()
                                + " becomes rival with " + actor.getName()
                                + " over the claim.");
                            break;
                        }
                    }
                } else {
                    log.add(actor.getName()
                        + " fails to fabricate a claim. Cunning insufficient.");
                }
            }

            case ATTACK -> {
                NobleHouse target = findAttackTarget(actor, allHouses,
                    relationships, claimManager);
                if (target == null) break;
                if (!canSpendInfluence(actor,
                        GameParameters.AI_INFLUENCE_COST_ATTACK, log)) break;

                double attackMult  = militaryMultiplier(military);
                double defendMult  = militaryMultiplier(
                    target.getActiveCharacter() != null
                        ? target.getActiveCharacter().getMilitary() : 0);

                ArmyForce atk = new ArmyForce(actor.getId(),
                    (int)(actor.getTotalArmySize() * attackMult), 0);
                ArmyForce def = new ArmyForce(target.getId(),
                    (int)(target.getTotalArmySize() * defendMult),
                    target.getDefense());

                CombatResult result = CombatResolver.resolve(atk, def);
                log.addAll(result.getLog());

                actor.setTotalArmySize(atk.getArmySize());
                target.setTotalArmySize(def.getArmySize());

                if (actor.getId().equals(result.getWinnerId())) {
                    // Transfer a claimed zone
                    transferClaimedZone(actor, target, claimManager, allHouses, log);
                    relationships.set(actor.getId(), target.getId(),
                        Relationship.RIVAL);
                } else if (target.getId().equals(result.getWinnerId())) {
                    relationships.set(actor.getId(), target.getId(),
                        Relationship.RIVAL);
                }

                // Trigger ally defense
                triggerAllyDefense(target, actor, allHouses, relationships, log);
            }

            case RAID -> {
                NobleHouse target = findRaidTarget(actor, allHouses, relationships);
                if (target == null) break;
                if (!canSpendInfluence(actor,
                        GameParameters.AI_INFLUENCE_COST_RAID, log)) break;

                int maxSteal = (int)(target.getZoneIds().size()
                    * GameParameters.NOBLE_ZONE_GOLD_PER_TURN
                    * GameParameters.RAID_MAX_GOLD_ZONE_MULTIPLIER);
                int stolen = Math.min(maxSteal,
                    (int)(target.getGold() * GameParameters.AI_RAID_GOLD_FRACTION));
                stolen = Math.max(0, stolen);

                target.addGold(-stolen);
                actor.addGold(stolen);
                log.add(actor.getName() + " raids " + target.getName()
                    + " and steals " + stolen + " gold.");

                int raidCount = relationships.recordRaid(actor.getId(), target.getId());
                log.add(target.getName() + " relation worsens (raid #" + raidCount + ").");
            }

            case DEMAND -> {
                NobleHouse target = findDemandTarget(actor, allHouses, relationships);
                if (target == null) break;
                if (!canSpendInfluence(actor,
                        GameParameters.AI_INFLUENCE_COST_DEMAND, log)) break;

                DemandType type     = demandTypeForMotivation(motivation);
                boolean    accepted = evaluateDemand(actor, target, relationships,
                    allIds, diplomacy);

                if (accepted) {
                    applyDemand(actor, target, type, log);
                } else {
                    log.add(target.getName() + " refuses "
                        + actor.getName() + "'s demand.");
                    relationships.worsen(actor.getId(), target.getId());
                }
            }

            case SCHEME -> {
                List<String> rivals = relationships.getAll(actor.getId(),
                    Relationship.RIVAL, allIds);
                if (rivals.isEmpty()) break;
                if (!canSpendInfluence(actor,
                        GameParameters.AI_INFLUENCE_COST_SCHEME, log)) break;

                String     targetId = rivals.get(RNG.nextInt(rivals.size()));
                NobleHouse target   = findById(targetId, allHouses);
                if (target == null) break;

                // Cunning affects scheme success
                double successChance = GameParameters.SCHEME_BASE_SUCCESS_CHANCE
                    + cunning * GameParameters.SCHEME_CUNNING_BONUS_PER_POINT;

                if (RNG.nextDouble() < successChance) {
                    target.addPrestige(-GameParameters.AI_SCHEME_PRESTIGE_LOSS);
                    actor.addPrestige(GameParameters.AI_SCHEME_PRESTIGE_GAIN);
                    log.add(actor.getName() + " schemes against "
                        + target.getName() + ". Their prestige suffers.");
                } else {
                    log.add(actor.getName() + "'s scheme against "
                        + target.getName() + " is discovered. Relation worsens.");
                    relationships.worsen(actor.getId(), target.getId());
                }
            }

            case FORTIFY -> {
                int cost = GameParameters.AI_FORTIFY_GOLD_COST;
                if (actor.getGold() < cost) break;
                actor.addGold(-cost);
                actor.addDefense(GameParameters.AI_FORTIFY_DEFENSE_GAIN);
                log.add(actor.getName() + " fortifies. Defense +"
                    + GameParameters.AI_FORTIFY_DEFENSE_GAIN + ".");
            }

            case ALLY -> {
                List<String> currentAllies = relationships.getAll(actor.getId(),
                    Relationship.ALLIED, allIds);
                if (currentAllies.size() >= GameParameters.ALLIANCE_MAX_PER_HOUSE) break;
                if (!canSpendInfluence(actor,
                        GameParameters.AI_INFLUENCE_COST_ALLY, log)) break;

                NobleHouse target = findAllyTarget(actor, allHouses, relationships);
                if (target == null) break;

                // Refuse if target too weak
                boolean tooWeak = target.getTotalArmySize()
                    < actor.getTotalArmySize()
                        * GameParameters.ALLIANCE_MIN_ARMY_FRACTION;
                if (tooWeak) {
                    log.add(actor.getName() + " considers alliance with "
                        + target.getName() + " but deems them too weak.");
                    break;
                }

                // Diplomacy bonus on acceptance
                double acceptChance = GameParameters.ALLY_BASE_ACCEPT_CHANCE
                    + diplomacy * GameParameters.ALLY_DIPLOMACY_BONUS_PER_POINT;
                if (RNG.nextDouble() < acceptChance) {
                    relationships.set(actor.getId(), target.getId(),
                        Relationship.ALLIED);
                    log.add(actor.getName() + " and " + target.getName()
                        + " form an alliance.");
                } else {
                    log.add(target.getName() + " declines alliance with "
                        + actor.getName() + ".");
                }
            }

            case GIFT -> {
                NobleHouse target = findGiftTarget(actor, allHouses, relationships);
                if (target == null) break;

                // Prefer forfeit claim if we have one, else money
                List<Claim> actorClaims = claimManager.getClaimsFor(actor.getId());
                Claim claimOnTarget = actorClaims.stream()
                    .filter(c -> target.getZoneIds().contains(c.getZoneId()))
                    .findFirst().orElse(null);

                if (claimOnTarget != null) {
                    claimManager.removeClaim(actor.getId(),
                        claimOnTarget.getZoneId());
                    relationships.improve(actor.getId(), target.getId());
                    log.add(actor.getName() + " forfeits claim on "
                        + claimOnTarget.getZoneId() + " as gift to "
                        + target.getName() + ". Relations improve.");
                } else if (actor.getGold() >= GameParameters.GIFT_MONEY_AMOUNT) {
                    actor.addGold(-GameParameters.GIFT_MONEY_AMOUNT);
                    target.addGold(GameParameters.GIFT_MONEY_AMOUNT);
                    relationships.improve(actor.getId(), target.getId());
                    log.add(actor.getName() + " gifts "
                        + GameParameters.GIFT_MONEY_AMOUNT + " gold to "
                        + target.getName() + ". Relations improve.");
                }
            }

            case SUPPORT_RIVAL -> {
                List<String> allies = relationships.getAll(actor.getId(),
                    Relationship.ALLIED, allIds);
                if (allies.isEmpty()) break;
                if (!canSpendInfluence(actor,
                        GameParameters.AI_INFLUENCE_COST_SUPPORT, log)) break;

                String     allyId = allies.get(RNG.nextInt(allies.size()));
                NobleHouse ally   = findById(allyId, allHouses);
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

    // ─── Ally defense ────────────────────────────────────────────────────────

    private static void triggerAllyDefense(NobleHouse attacked, NobleHouse attacker,
                                            List<NobleHouse> allHouses,
                                            RelationshipManager relationships,
                                            List<String> log) {
        List<String> allIds = allHouseIds(allHouses);
        List<String> allies = relationships.getAll(attacked.getId(),
            Relationship.ALLIED, allIds);

        for (String allyId : allies) {
            NobleHouse ally = findById(allyId, allHouses);
            if (ally == null || ally.isEliminated()) continue;

            boolean strongEnough = ally.getTotalArmySize()
                >= attacker.getTotalArmySize()
                    * GameParameters.ALLY_DEFENSE_MIN_STRENGTH_FRACTION;
            boolean notAtWar = relationships.get(ally.getId(), attacker.getId())
                != Relationship.RIVAL;
            boolean sharesRival = relationships.shareRival(ally.getId(),
                attacker.getId(), allIds);

            if (strongEnough && (notAtWar || sharesRival)) {
                // Join the defense
                int allyMilitary = ally.getActiveCharacter() != null
                    ? ally.getActiveCharacter().getMilitary() : 0;
                ArmyForce allyForce = new ArmyForce(ally.getId(),
                    (int)(ally.getTotalArmySize() * militaryMultiplier(allyMilitary)),
                    attacked.getDefense());
                ArmyForce atkForce  = new ArmyForce(attacker.getId(),
                    attacker.getTotalArmySize(), 0);

                CombatResult defResult = CombatResolver.resolve(atkForce, allyForce);
                log.add(ally.getName() + " joins defense of " + attacked.getName() + "!");
                log.addAll(defResult.getLog());

                attacker.setTotalArmySize(atkForce.getArmySize());
                ally.setTotalArmySize(allyForce.getArmySize());
            } else {
                // Fail to join — alliance degrades
                relationships.worsen(ally.getId(), attacked.getId());
                log.add(ally.getName() + " fails to honor alliance with "
                    + attacked.getName() + ". Relations cool.");
            }
        }
    }

    // ─── Demand evaluation ───────────────────────────────────────────────────

    public static boolean evaluateDemand(NobleHouse requester, NobleHouse target,
                                          RelationshipManager relationships,
                                          List<String> allIds, int requesterDiplomacy) {
        double score = GameParameters.DEMAND_BASE_SCORE;

        int prestigeDiff = requester.getPrestige() - target.getPrestige();
        score += prestigeDiff * GameParameters.DEMAND_PRESTIGE_WEIGHT;

        int armyDiff = requester.getTotalArmySize() - target.getTotalArmySize();
        score += armyDiff * GameParameters.DEMAND_ARMY_WEIGHT;

        score += switch (relationships.get(requester.getId(), target.getId())) {
            case ALLIED, FRIENDLY -> GameParameters.DEMAND_ALLIED_BONUS;
            case NEUTRAL          -> 0;
            case HOSTILE          -> GameParameters.DEMAND_RIVAL_PENALTY / 2.0;
            case RIVAL            -> GameParameters.DEMAND_RIVAL_PENALTY;
        };

        if (relationships.shareRival(requester.getId(), target.getId(), allIds)) {
            score += GameParameters.DEMAND_SHARED_RIVAL_BONUS;
        }

        score += requesterDiplomacy * GameParameters.DEMAND_DIPLOMACY_BONUS_PER_POINT;
        score += (RNG.nextDouble() - 0.5) * 2 * GameParameters.DEMAND_RANDOM_RANGE;

        return score >= GameParameters.DEMAND_ACCEPT_THRESHOLD;
    }

    // ─── Target finders ──────────────────────────────────────────────────────

    private static NobleHouse findAttackTarget(NobleHouse actor,
                                                List<NobleHouse> allHouses,
                                                RelationshipManager relationships,
                                                ClaimManager claimManager) {
        List<Claim> claims = claimManager.getClaimsFor(actor.getId());
        NobleHouse best    = null;
        int        bestArmy = Integer.MAX_VALUE;

        for (Claim claim : claims) {
            for (NobleHouse other : allHouses) {
                if (other == actor || other.isEliminated()) continue;
                if (!other.getZoneIds().contains(claim.getZoneId())) continue;
                Relationship rel = relationships.get(actor.getId(), other.getId());
                if (rel == Relationship.ALLIED || rel == Relationship.FRIENDLY) continue;
                if (other.getTotalArmySize() < actor.getTotalArmySize()
                        && other.getTotalArmySize() < bestArmy) {
                    best     = other;
                    bestArmy = other.getTotalArmySize();
                }
            }
        }
        return best;
    }

    private static NobleHouse findRaidTarget(NobleHouse actor,
                                              List<NobleHouse> allHouses,
                                              RelationshipManager relationships) {
        NobleHouse best     = null;
        int        bestGold = 0;
        for (NobleHouse other : allHouses) {
            if (other == actor || other.isEliminated()) continue;
            Relationship rel = relationships.get(actor.getId(), other.getId());
            if (rel == Relationship.ALLIED || rel == Relationship.FRIENDLY) continue;
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
        NobleHouse best      = null;
        int        bestScore = Integer.MIN_VALUE;
        for (NobleHouse other : allHouses) {
            if (other == actor || other.isEliminated()) continue;
            Relationship rel = relationships.get(actor.getId(), other.getId());
            if (rel == Relationship.RIVAL) continue;
            int score = -other.getGold();
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
            if (other == actor || other.isEliminated()) continue;
            if (relationships.get(actor.getId(), other.getId())
                    == Relationship.NEUTRAL) return other;
        }
        return null;
    }

    private static NobleHouse findGiftTarget(NobleHouse actor,
                                              List<NobleHouse> allHouses,
                                              RelationshipManager relationships) {
        // Find strongest hostile/neutral neighbor
        NobleHouse best     = null;
        int        bestArmy = 0;
        for (NobleHouse other : allHouses) {
            if (other == actor || other.isEliminated()) continue;
            Relationship rel = relationships.get(actor.getId(), other.getId());
            if (rel == Relationship.RIVAL || rel == Relationship.ALLIED) continue;
            if (other.getTotalArmySize() > bestArmy) {
                best     = other;
                bestArmy = other.getTotalArmySize();
            }
        }
        return best;
    }

    private static String findClaimTarget(NobleHouse actor,
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
            case WEALTH            -> DemandType.WEALTH;
            case EXPANSION, SECURITY -> DemandType.ARMY;
            case PRESTIGE          -> DemandType.PRESTIGE;
        };
    }

    // ─── Zone transfer ───────────────────────────────────────────────────────

    private static void transferClaimedZone(NobleHouse winner, NobleHouse loser,
                                             ClaimManager claimManager,
                                             List<NobleHouse> allHouses,
                                             List<String> log) {
        List<Claim> claims = claimManager.getClaimsFor(winner.getId());
        for (Claim claim : claims) {
            if (loser.getZoneIds().contains(claim.getZoneId())) {
                String zone = claim.getZoneId();
                loser.removeZone(zone);
                winner.addZone(zone);
                claimManager.removeAllClaimsOnZone(zone);
                log.add(winner.getName() + " captures " + zone
                    + " from " + loser.getName() + ".");
                if (loser.isEliminated()) {
                    log.add(loser.getName() + " has been eliminated.");
                }
                return;
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static double militaryMultiplier(int military) {
        return 1.0 + military * GameParameters.MILITARY_SKILL_BONUS_PER_POINT;
    }

    private static boolean canSpendInfluence(NobleHouse house, int cost,
                                              List<String> log) {
        if (house.getInfluence() < cost) return false;
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