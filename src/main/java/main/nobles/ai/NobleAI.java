package main.nobles.ai;

import main.nobles.*;
import main.nobles.combat.ArmyForce;
import main.nobles.combat.CombatResolver;
import main.nobles.combat.CombatResult;
import main.map.ZoneState;
import main.parameters.GameParameters;
import main.rules.NobleRules;

import java.util.*;

/**
 * Per-house AI brain.
 * Handles motivation, action selection, and threatened state.
 * Coalition logic lives in CoalitionManager.
 */
public class NobleAI {

    private static final Random RNG = new Random();

    // ─── Entry point ─────────────────────────────────────────────────────────

    public static List<String> tick(NobleHouse actor,
                                    List<NobleHouse> allHouses,
                                    RelationshipManager relationships,
                                    ClaimManager claimManager,
                                    main.map.ZoneManager zoneManager,
                                    NobleArmyManager armyManager) {
        List<String> log = new ArrayList<>();
        if (actor.isEliminated()) return log;

        relationships.tickDecay(allHouseIds(allHouses));
        considerBreakingAlliances(actor, allHouses, relationships, log);

        List<NobleArmy> existingArmies = new ArrayList<>(armyManager.getArmiesForHouse(actor.getId()));
        NobleArmy idleArmy = existingArmies.stream().filter(a -> !a.hasPendingOrder()).findFirst().orElse(null);

        int manpower = actor.getNobleManpower();
        int gold = actor.getGold();
        int minSize = GameParameters.NOBLE_ARMY_MIN_RECRUIT_SIZE;
        int maxSustainableSize = (int)(gold / (GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER * 2.0));

        if (manpower >= minSize && gold >= GameParameters.NOBLE_ARMY_RECRUIT_GOLD_THRESHOLD) {
            if (idleArmy == null) {
                int rawSize = Math.max(minSize, (int)(manpower * GameParameters.NOBLE_ARMY_RECRUIT_FRACTION));
                int recruitSize = Math.min(rawSize, maxSustainableSize);
                recruitSize = Math.max(minSize, recruitSize);
                NobleArmy recruited = armyManager.recruit(actor, recruitSize);
                if (recruited != null) {
                    log.add(actor.getName() + " recruits an army of " + recruited.getSize() + ".");
                }
            } else {
                int currentSize = idleArmy.getSize();
                int desiredIncrease = (int)(manpower * GameParameters.NOBLE_ARMY_RECRUIT_FRACTION);
                int targetSize = Math.min(currentSize + desiredIncrease, maxSustainableSize);
                if (targetSize > currentSize) {
                    int reinforceAmount = targetSize - currentSize;
                    boolean success = armyManager.reinforceArmy(actor, idleArmy, reinforceAmount);
                    if (success) {
                        log.add(actor.getName() + " reinforces its army by " + reinforceAmount
                                + " (now " + targetSize + ").");
                    }
                }
            }
        }

        if (actor.getGold() < GameParameters.NOBLE_ARMY_DISBAND_GOLD_THRESHOLD) {
            for (NobleArmy army : new ArrayList<>(armyManager.getArmiesForHouse(actor.getId()))) {
                if (!army.hasPendingOrder()) {
                    armyManager.disbandPartial(actor, army, army.getSize());
                    log.add(actor.getName() + " disbands army to conserve gold.");
                }
            }
        }

        Motivation motivation = pickMotivation(actor.getActiveCharacter());
        NobleAction action    = pickAction(actor, motivation, allHouses, relationships, claimManager);
        if (action == null) return log;

        log.addAll(execute(actor, action, motivation, allHouses,
                           relationships, claimManager, zoneManager, armyManager));
        return log;
    }

    // ─── Threatened ──────────────────────────────────────────────────────────

    public static void updateThreatenedStatus(NobleHouse attacker,
                                               List<NobleHouse> allHouses,
                                               RelationshipManager relationships) {
        int attackerZones = attacker.getZoneIds().size();
        int totalZones    = 0;
        for (NobleHouse h : allHouses) totalZones += h.getZoneIds().size();

        for (NobleHouse observer : allHouses) {
            if (observer == attacker || observer.isEliminated()) continue;
            Relationship rel = relationships.get(attacker.getId(), observer.getId());
            if (rel != Relationship.NEUTRAL) continue;

            int observerZones = observer.getZoneIds().size();
            double chance = (double)(attackerZones - observerZones)
                / Math.max(1, totalZones)
                * GameParameters.THREATENED_BASE_CHANCE_MULTIPLIER;
            chance = Math.max(0, Math.min(1.0, chance));

            if (RNG.nextDouble() < chance) {
                observer.setThreatened(true);
            }
        }
    }

    public static void tickThreatenedDecay(List<NobleHouse> allHouses) {
        for (NobleHouse house : allHouses) {
            if (house.isThreatened()
                    && RNG.nextDouble() < GameParameters.THREATENED_DECAY_CHANCE) {
                house.setThreatened(false);
            }
        }
    }

    // ─── Alliance breaking ────────────────────────────────────────────────────

    private static void considerBreakingAlliances(NobleHouse actor,
                                                   List<NobleHouse> allHouses,
                                                   RelationshipManager relationships,
                                                   List<String> log) {
        List<String> allIds = allHouseIds(allHouses);
        List<String> allies = new ArrayList<>(
            relationships.getAll(actor.getId(), Relationship.ALLIED, allIds));

        for (String allyId : allies) {
            NobleHouse ally = findById(allyId, allHouses);
            if (ally == null || ally.isEliminated()) {
                relationships.set(actor.getId(), allyId, Relationship.NEUTRAL);
                continue;
            }
            boolean tooWeak = ally.getTotalArmySize()
                < actor.getTotalArmySize() * GameParameters.ALLIANCE_MIN_ARMY_FRACTION;
            if (tooWeak) {
                NobleCharacter c   = actor.getActiveCharacter();
                int            dip = c != null ? c.getDiplomacy() : 0;
                double cleanChance = GameParameters.ALLIANCE_BREAK_CLEAN_BASE
                    + dip * GameParameters.ALLIANCE_BREAK_CLEAN_PER_DIPLOMACY;
                Relationship result = RNG.nextDouble() < cleanChance
                    ? Relationship.NEUTRAL : Relationship.HOSTILE;
                relationships.set(actor.getId(), allyId, result);
                log.add(actor.getName() + " breaks alliance with "
                    + ally.getName() + ". New relation: " + result.name());
            }
        }
    }

    // ─── Motivation ──────────────────────────────────────────────────────────

    private static Motivation pickMotivation(NobleCharacter character) {
        if (character == null) return Motivation.SECURITY;
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

        if (shouldGift(actor, motivation, allHouses, relationships)) {
            return NobleAction.GIFT;
        }

        return switch (motivation) {
            case EXPANSION -> {
                List<Claim> claims = claimManager.getClaimsFor(actor.getId());
                if (!claims.isEmpty()) {
                    NobleHouse target = findAttackTarget(actor, allHouses, relationships, claimManager);
                    if (target != null) yield NobleAction.ATTACK;
                }
                yield NobleAction.FABRICATE_CLAIM;
            }
            case WEALTH -> {
                NobleHouse raidTarget = findRaidTarget(actor, allHouses, relationships, null);
                yield raidTarget != null ? NobleAction.RAID : NobleAction.DEMAND;
            }
            case SECURITY -> {
                List<String> rivals = relationships.getAll(actor.getId(), Relationship.RIVAL, allIds);
                if (!rivals.isEmpty() && actor.getDefense() < GameParameters.AI_FORTIFY_THRESHOLD) {
                    yield NobleAction.FORTIFY;
                }
                int allyCount = relationships.getAll(actor.getId(), Relationship.ALLIED, allIds).size();
                yield allyCount < GameParameters.ALLIANCE_MAX_PER_HOUSE
                    ? NobleAction.ALLY : NobleAction.FORTIFY;
            }
            case PRESTIGE -> {
                NobleHouse supTarget = findSuperiorityTarget(actor, allHouses, relationships);
                if (supTarget != null) yield NobleAction.DEMAND;
                List<String> rivals = relationships.getAll(actor.getId(), Relationship.RIVAL, allIds);
                yield !rivals.isEmpty() ? NobleAction.SCHEME : NobleAction.FORTIFY;
            }
        };
    }

    // ─── Execution ───────────────────────────────────────────────────────────

    private static List<String> execute(NobleHouse actor, NobleAction action,
                                        Motivation motivation,
                                        List<NobleHouse> allHouses,
                                        RelationshipManager relationships,
                                        ClaimManager claimManager,
                                        main.map.ZoneManager zoneManager,
                                        NobleArmyManager armyManager) {
        List<String> log    = new ArrayList<>();
        List<String> allIds = allHouseIds(allHouses);
        NobleCharacter character = actor.getActiveCharacter();
        int cunning   = character != null ? character.getCunning()   : 0;
        int military  = character != null ? character.getMilitary()  : 0;
        int diplomacy = character != null ? character.getDiplomacy() : 0;

        switch (action) {

            case FABRICATE_CLAIM -> {
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_FABRICATE, log)) break;
                String targetZone = findClaimTarget(actor, allHouses, claimManager);
                if (targetZone == null) break;
                boolean success = claimManager.fabricate(actor.getId(), targetZone, cunning, RNG);
                if (success) {
                    log.add(actor.getName() + " fabricates a claim on " + targetZone + ".");
                    for (NobleHouse other : allHouses) {
                        if (other.getZoneIds().contains(targetZone)) {
                            relationships.set(actor.getId(), other.getId(), Relationship.RIVAL);
                            log.add(other.getName() + " becomes rival with "
                                + actor.getName() + " over the claim.");
                            break;
                        }
                    }
                } else {
                    log.add(actor.getName() + " fails to fabricate a claim. Cunning insufficient.");
                }
            }

            case ATTACK -> {
                if (!NobleRules.WAR_DECLARATION_ALLOWED) break;
                NobleHouse target = findAttackTarget(actor, allHouses, relationships, claimManager);
                if (target == null) break;
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_ATTACK, log)) break;

                String attackTargetZone = null;
                for (Claim claim : claimManager.getClaimsFor(actor.getId())) {
                    if (target.getZoneIds().contains(claim.getZoneId())) {
                        attackTargetZone = claim.getZoneId();
                        break;
                    }
                }
                if (attackTargetZone == null) break;

                List<NobleArmy> actorArmies = armyManager.getArmiesForHouse(actor.getId());
                NobleArmy army = null;
                for (NobleArmy a : actorArmies) {
                    if (!a.hasPendingOrder()) { army = a; break; }
                }
                if (army == null
                        && actor.getNobleManpower() >= GameParameters.NOBLE_ARMY_MIN_RECRUIT_SIZE
                        && actor.getGold() >= GameParameters.NOBLE_ARMY_RECRUIT_GOLD_THRESHOLD) {
                    int recruitSize = Math.max(GameParameters.NOBLE_ARMY_MIN_RECRUIT_SIZE,
                        (int)(actor.getNobleManpower() * GameParameters.NOBLE_ARMY_RECRUIT_FRACTION));
                    army = armyManager.recruit(actor, recruitSize);
                    if (army != null) {
                        log.add(actor.getName() + " raises an army of " + army.getSize() + " for the attack.");
                    }
                }
                if (army != null && !army.hasPendingOrder() && army.getSize() > 0) {
                    armyManager.moveArmy(army, attackTargetZone);
                    army.issueOrder(NobleArmy.OrderType.ATTACK, attackTargetZone);
                    log.add(actor.getName() + " marches on " + attackTargetZone + ".");
                    updateThreatenedStatus(actor, allHouses, relationships);
                    triggerAllyDefense(target, actor, allHouses, relationships, log);
                }
            }

            case RAID -> {
                NobleHouse target = findRaidTarget(actor, allHouses, relationships, zoneManager);
                if (target == null) break;
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_RAID, log)) break;

                String raidedZone = pickRaidableZone(target, zoneManager);
                if (raidedZone == null) {
                    log.add(actor.getName() + " finds no raidable zone in " + target.getName() + ".");
                    break;
                }
                List<NobleArmy> actorArmies = armyManager.getArmiesForHouse(actor.getId());
                NobleArmy raidArmy = null;
                for (NobleArmy a : actorArmies) {
                    if (!a.hasPendingOrder()) { raidArmy = a; break; }
                }
                if (raidArmy == null
                        && actor.getNobleManpower() >= GameParameters.NOBLE_ARMY_MIN_RECRUIT_SIZE
                        && actor.getGold() >= GameParameters.NOBLE_ARMY_RECRUIT_GOLD_THRESHOLD) {
                    int recruitSize = Math.max(GameParameters.NOBLE_ARMY_MIN_RECRUIT_SIZE,
                        (int)(actor.getNobleManpower() * GameParameters.NOBLE_ARMY_RECRUIT_FRACTION));
                    raidArmy = armyManager.recruit(actor, recruitSize);
                    if (raidArmy != null) {
                        log.add(actor.getName() + " raises a raiding party of " + raidArmy.getSize() + ".");
                    }
                }
                if (raidArmy != null && !raidArmy.hasPendingOrder() && raidArmy.getSize() > 0) {
                    armyManager.moveArmy(raidArmy, raidedZone);
                    raidArmy.issueOrder(NobleArmy.OrderType.RAID, raidedZone);
                    log.add(actor.getName() + " sends raiders toward " + raidedZone + ".");
                }
            }

            case DEMAND -> {
                if (motivation == Motivation.PRESTIGE) {
                    NobleHouse target = findSuperiorityTarget(actor, allHouses, relationships);
                    if (target == null) break;
                    if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_DEMAND, log)) break;
                    boolean accepted = evaluateAcknowledgeSuperiority(actor, target);
                    if (accepted) {
                        target.addPrestige(-GameParameters.DEMAND_PRESTIGE_AMOUNT);
                        actor.addPrestige(GameParameters.DEMAND_PRESTIGE_AMOUNT);
                        log.add(target.getName() + " acknowledges the superiority of "
                            + actor.getName() + ". Prestige transferred.");
                    } else {
                        log.add(target.getName() + " refuses to acknowledge "
                            + actor.getName() + "'s superiority.");
                        relationships.worsen(actor.getId(), target.getId());
                    }
                    break;
                }
                NobleHouse target = findDemandTarget(actor, allHouses, relationships);
                if (target == null) break;
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_DEMAND, log)) break;
                DemandType type    = demandTypeForMotivation(motivation);
                boolean    accepted = evaluateDemand(actor, target, relationships, allIds, diplomacy);
                if (accepted) {
                    applyDemand(actor, target, type, log);
                } else {
                    log.add(target.getName() + " refuses " + actor.getName() + "'s demand.");
                    relationships.worsen(actor.getId(), target.getId());
                }
            }

            case SCHEME -> {
                List<String> rivals = relationships.getAll(actor.getId(), Relationship.RIVAL, allIds);
                if (rivals.isEmpty()) break;
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_SCHEME, log)) break;
                String     targetId = rivals.get(RNG.nextInt(rivals.size()));
                NobleHouse target   = findById(targetId, allHouses);
                if (target == null) break;
                double successChance = GameParameters.SCHEME_BASE_SUCCESS_CHANCE
                    + cunning * GameParameters.SCHEME_CUNNING_BONUS_PER_POINT;
                if (RNG.nextDouble() < successChance) {
                    target.addPrestige(-GameParameters.AI_SCHEME_PRESTIGE_LOSS);
                    actor.addPrestige(GameParameters.AI_SCHEME_PRESTIGE_GAIN);
                    log.add(actor.getName() + " schemes against " + target.getName()
                        + ". Their prestige suffers.");
                } else {
                    log.add(actor.getName() + "'s scheme against " + target.getName()
                        + " is discovered. Relation worsens.");
                    relationships.worsen(actor.getId(), target.getId());
                }
            }

            case FORTIFY -> {
                int cost = GameParameters.AI_FORTIFY_GOLD_COST;
                if (actor.getGold() < cost) break;
                actor.addGold(-cost);
                actor.addDefense(GameParameters.AI_FORTIFY_DEFENSE_GAIN);
                String fortZone = actor.getCapitalZoneId();
                if (fortZone != null) {
                    actor.addGarrison(fortZone, GameParameters.FORTIFY_GARRISON_GAIN);
                }
                log.add(actor.getName() + " fortifies. Defense +"
                    + GameParameters.AI_FORTIFY_DEFENSE_GAIN
                    + ", Garrison +" + GameParameters.FORTIFY_GARRISON_GAIN + ".");
            }

            case ALLY -> {
                List<String> currentAllies = relationships.getAll(actor.getId(), Relationship.ALLIED, allIds);
                if (currentAllies.size() >= GameParameters.ALLIANCE_MAX_PER_HOUSE) break;
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_ALLY, log)) break;
                NobleHouse target = findAllyTarget(actor, allHouses, relationships);
                if (target == null) break;
                Relationship currentRel = relationships.get(actor.getId(), target.getId());
                if (currentRel == Relationship.FRIENDLY || currentRel == Relationship.ALLIED) break;
                boolean tooWeak = target.getTotalArmySize()
                    < actor.getTotalArmySize() * GameParameters.ALLIANCE_MIN_ARMY_FRACTION;
                if (tooWeak) {
                    log.add(actor.getName() + " considers alliance with "
                        + target.getName() + " but deems them too weak.");
                    break;
                }
                double acceptChance = GameParameters.ALLY_BASE_ACCEPT_CHANCE
                    + diplomacy * GameParameters.ALLY_DIPLOMACY_BONUS_PER_POINT;
                if (RNG.nextDouble() < acceptChance) {
                    relationships.set(actor.getId(), target.getId(), Relationship.ALLIED);
                    log.add(actor.getName() + " and " + target.getName() + " form an alliance.");
                } else {
                    log.add(target.getName() + " declines alliance with " + actor.getName() + ".");
                }
            }

            case GIFT -> {
                NobleHouse target = findGiftTarget(actor, allHouses, relationships);
                if (target == null) break;
                List<Claim> actorClaims = claimManager.getClaimsFor(actor.getId());
                Claim claimOnTarget = actorClaims.stream()
                    .filter(c -> target.getZoneIds().contains(c.getZoneId()))
                    .findFirst().orElse(null);
                if (claimOnTarget != null) {
                    claimManager.removeClaim(actor.getId(), claimOnTarget.getZoneId());
                    relationships.improve(actor.getId(), target.getId());
                    log.add(actor.getName() + " forfeits claim on " + claimOnTarget.getZoneId()
                        + " as gift to " + target.getName() + ". Relations improve.");
                } else if (actor.getGold() >= GameParameters.GIFT_MONEY_AMOUNT) {
                    actor.addGold(-GameParameters.GIFT_MONEY_AMOUNT);
                    target.addGold(GameParameters.GIFT_MONEY_AMOUNT);
                    relationships.improve(actor.getId(), target.getId());
                    log.add(actor.getName() + " gifts " + GameParameters.GIFT_MONEY_AMOUNT
                        + " gold to " + target.getName() + ". Relations improve.");
                }
            }

            case SUPPORT_RIVAL -> {
                List<String> allies = relationships.getAll(actor.getId(), Relationship.ALLIED, allIds);
                if (allies.isEmpty()) break;
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_SUPPORT, log)) break;
                String     allyId = allies.get(RNG.nextInt(allies.size()));
                NobleHouse ally   = findById(allyId, allHouses);
                if (ally == null) break;
                int support = (int)(actor.getGold() * GameParameters.AI_SUPPORT_GOLD_FRACTION);
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
        List<String> allies = new ArrayList<>(
            relationships.getAll(attacked.getId(), Relationship.ALLIED, allIds));

        for (String allyId : allies) {
            NobleHouse ally = findById(allyId, allHouses);
            if (ally == null || ally.isEliminated()) continue;
            Relationship allyWithAttacker = relationships.get(ally.getId(), attacker.getId());
            if (allyWithAttacker == Relationship.ALLIED
                    || allyWithAttacker == Relationship.FRIENDLY) continue;
            boolean strongEnough = ally.getTotalArmySize()
                >= attacker.getTotalArmySize() * GameParameters.ALLY_DEFENSE_MIN_STRENGTH_FRACTION;
            if (strongEnough) {
                int allyMilitary = ally.getActiveCharacter() != null
                    ? ally.getActiveCharacter().getMilitary() : 0;
                ArmyForce allyForce = new ArmyForce(ally.getId(),
                    (int)(ally.getTotalArmySize() * militaryMultiplier(allyMilitary)),
                    attacked.getDefense());
                ArmyForce atkForce = new ArmyForce(attacker.getId(),
                    attacker.getTotalArmySize(), 0);
                CombatResult defResult = CombatResolver.resolve(atkForce, allyForce);
                log.add(ally.getName() + " joins defense of " + attacked.getName() + "!");
                log.addAll(defResult.getLog());
                attacker.setTotalArmySize(atkForce.getArmySize());
                ally.setTotalArmySize(allyForce.getArmySize());
            } else {
                relationships.worsen(ally.getId(), attacked.getId());
                log.add(ally.getName() + " fails to honor alliance with "
                    + attacked.getName() + ". Relations cool.");
            }
        }
    }

    // ─── Demand evaluation ───────────────────────────────────────────────────

    public static boolean evaluateDemand(NobleHouse requester, NobleHouse target,
                                          RelationshipManager relationships,
                                          List<String> allIds,
                                          int requesterDiplomacy) {
        double score = GameParameters.DEMAND_BASE_SCORE;
        score += (requester.getPrestige() - target.getPrestige()) * GameParameters.DEMAND_PRESTIGE_WEIGHT;
        score += (requester.getTotalArmySize() - target.getTotalArmySize()) * GameParameters.DEMAND_ARMY_WEIGHT;
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

    private static boolean evaluateAcknowledgeSuperiority(NobleHouse demander, NobleHouse target) {
        int demanderMilitary = demander.getActiveCharacter() != null
            ? demander.getActiveCharacter().getMilitary() : 0;
        int targetMilitary   = target.getActiveCharacter() != null
            ? target.getActiveCharacter().getMilitary() : 0;
        if (demanderMilitary <= targetMilitary) return false;
        double base  = GameParameters.SUPERIORITY_BASE_ACCEPT_CHANCE;
        double noise = (RNG.nextDouble() - 0.5) * GameParameters.SUPERIORITY_RANDOM_RANGE;
        return (base + noise) >= 0.5;
    }

    // ─── Target finders ──────────────────────────────────────────────────────

    private static NobleHouse findAttackTarget(NobleHouse actor,
                                                List<NobleHouse> allHouses,
                                                RelationshipManager relationships,
                                                ClaimManager claimManager) {
        List<Claim> claims   = claimManager.getClaimsFor(actor.getId());
        NobleHouse  best     = null;
        int         bestArmy = Integer.MAX_VALUE;
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
                                              RelationshipManager relationships,
                                              main.map.ZoneManager zoneManager) {
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

    private static NobleHouse findDemandTarget(NobleHouse actor,
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

    private static NobleHouse findSuperiorityTarget(NobleHouse actor,
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

    private static NobleHouse findAllyTarget(NobleHouse actor,
                                              List<NobleHouse> allHouses,
                                              RelationshipManager relationships) {
        for (NobleHouse other : allHouses) {
            if (other == actor || other.isEliminated()) continue;
            if (relationships.get(actor.getId(), other.getId()) == Relationship.NEUTRAL) return other;
        }
        return null;
    }

    private static NobleHouse findGiftTarget(NobleHouse actor,
                                              List<NobleHouse> allHouses,
                                              RelationshipManager relationships) {
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

    private static String pickRaidableZone(NobleHouse target,
                                            main.map.ZoneManager zoneManager) {
        for (String zoneId : target.getZoneIds()) {
            ZoneState state = zoneManager.getState(zoneId);
            if (state != null && !state.isRecentlyRaided()) return zoneId;
        }
        return null;
    }

    // ─── Demand application ──────────────────────────────────────────────────

    private static void applyDemand(NobleHouse requester, NobleHouse target,
                                     DemandType type, List<String> log) {
        switch (type) {
            case WEALTH -> {
                int amount = (int)(target.getGold() * GameParameters.DEMAND_WEALTH_FRACTION);
                target.addGold(-amount);
                requester.addGold(amount);
                log.add(target.getName() + " yields " + amount + " gold to " + requester.getName() + ".");
            }
            case ARMY -> {
                requester.addToRaisedArmy(GameParameters.DEMAND_ARMY_AMOUNT);
                log.add(target.getName() + " sends " + GameParameters.DEMAND_ARMY_AMOUNT
                    + " soldiers to " + requester.getName() + ".");
            }
            case ACKNOWLEDGE_SUPERIORITY -> {
                target.addPrestige(-GameParameters.DEMAND_PRESTIGE_AMOUNT);
                requester.addPrestige(GameParameters.DEMAND_PRESTIGE_AMOUNT);
                log.add(target.getName() + " acknowledges the superiority of " + requester.getName() + ".");
            }
        }
    }

    private static DemandType demandTypeForMotivation(Motivation m) {
        return switch (m) {
            case WEALTH              -> DemandType.WEALTH;
            case EXPANSION, SECURITY -> DemandType.ARMY;
            case PRESTIGE            -> DemandType.ACKNOWLEDGE_SUPERIORITY;
        };
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static double militaryMultiplier(int military) {
        return 1.0 + military * GameParameters.MILITARY_SKILL_BONUS_PER_POINT;
    }

    private static boolean canSpendInfluence(NobleHouse house, int cost, List<String> log) {
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
        for (NobleHouse h : houses) if (h.getId().equals(id)) return h;
        return null;
    }

    private static boolean shouldGift(NobleHouse actor, Motivation motivation,
                                       List<NobleHouse> allHouses,
                                       RelationshipManager relationships) {
        double weight = switch (motivation) {
            case SECURITY  -> GameParameters.GIFT_WEIGHT_SECURITY;
            case WEALTH    -> actor.getGold() > GameParameters.GIFT_WEALTH_GOLD_THRESHOLD
                              ? GameParameters.GIFT_WEIGHT_WEALTH : 0.0;
            case PRESTIGE  -> GameParameters.GIFT_WEIGHT_PRESTIGE;
            case EXPANSION -> GameParameters.GIFT_WEIGHT_EXPANSION;
        };
        if (RNG.nextDouble() > weight) return false;
        for (NobleHouse other : allHouses) {
            if (other == actor || other.isEliminated()) continue;
            Relationship rel = relationships.get(actor.getId(), other.getId());
            if ((rel == Relationship.HOSTILE || rel == Relationship.NEUTRAL)
                    && other.getTotalArmySize() > actor.getTotalArmySize()) return true;
        }
        return false;
    }
}