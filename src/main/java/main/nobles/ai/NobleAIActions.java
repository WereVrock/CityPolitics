package main.nobles.ai;

import debug.Debug;
import main.map.ZoneManager;
import main.nobles.Claim;
import main.nobles.ClaimManager;
import main.nobles.NobleArmy;
import main.nobles.NobleArmyManager;
import main.nobles.NobleCharacter;
import main.nobles.NobleHouse;
import main.nobles.Relationship;
import main.nobles.RelationshipManager;
import main.nobles.combat.ArmyForce;
import main.nobles.combat.CombatResolver;
import main.nobles.combat.CombatResult;
import main.parameters.GameParameters;

import java.util.ArrayList;
import java.util.List;
import main.nobles.Motivation;
import static main.nobles.Motivation.EXPANSION;
import static main.nobles.Motivation.PRESTIGE;
import static main.nobles.Motivation.SECURITY;
import static main.nobles.Motivation.WEALTH;

/** Executes the action chosen by {@link NobleAIMotivation} for a given noble house. */
final class NobleAIActions {

    private NobleAIActions() {}

    // -------------------------------------------------------------------------
    // Demand evaluation (public — also called from NobleAI)
    // -------------------------------------------------------------------------

    public static boolean evaluateDemand(NobleHouse requester, NobleHouse target,
                                          RelationshipManager relationships,
                                          List<String> allIds,
                                          int requesterDiplomacy,
                                          NobleArmyManager armyManager) {
        double score = GameParameters.DEMAND_BASE_SCORE;
        score += (requester.getPrestige() - target.getPrestige())
                * GameParameters.DEMAND_PRESTIGE_WEIGHT;

        int requesterPower = NobleAIPower.exactPotentialFieldArmy(requester, armyManager);
        int targetPower    = NobleAIPower.estimatedPower(requester, target, armyManager)
                + target.getTotalGarrisonSize();
        score += (requesterPower - targetPower) * GameParameters.DEMAND_ARMY_WEIGHT;

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
        score += (NobleAIUtils.RNG.nextDouble() - 0.5) * 2 * GameParameters.DEMAND_RANDOM_RANGE;
        return score >= GameParameters.DEMAND_ACCEPT_THRESHOLD;
    }

    // -------------------------------------------------------------------------
    // Main dispatch
    // -------------------------------------------------------------------------

    static List<String> execute(NobleHouse actor,
                                 NobleAction action,
                                 Motivation motivation,
                                 List<NobleHouse> allHouses,
                                 RelationshipManager relationships,
                                 ClaimManager claimManager,
                                 ZoneManager zoneManager,
                                 NobleArmyManager armyManager) {
        List<String> log    = new ArrayList<>();
        List<String> allIds = NobleAIUtils.allHouseIds(allHouses);

        NobleCharacter character = actor.getActiveCharacter();
        int cunning   = character != null ? character.getCunning()   : 0;
        int military  = character != null ? character.getMilitary()  : 0;
        int diplomacy = character != null ? character.getDiplomacy() : 0;

        switch (action) {

            case FABRICATE_CLAIM -> {
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_FABRICATE, log)) break;
                String targetZone = NobleAITargeting.findClaimTarget(actor, allHouses, claimManager);
                if (targetZone == null) break;

                List<String> myZones = new ArrayList<>(actor.getZoneIds());
                int ownerCunning = 0;
                for (NobleHouse other : allHouses) {
                    if (other.getZoneIds().contains(targetZone)) {
                        ownerCunning = other.getActiveCharacter() != null
                                ? other.getActiveCharacter().getCunning() : 0;
                        break;
                    }
                }
                boolean success = claimManager.fabricate(actor.getId(), targetZone,
                        cunning, ownerCunning, NobleAIUtils.RNG,
                        myZones, zoneManager.getZones());
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

            case ATTACK -> executeAttack(actor, motivation, character, cunning, military,
                    allHouses, relationships, claimManager, zoneManager, armyManager, log);

            case RAID -> {
                NobleHouse raidTarget = NobleAITargeting.findRaidTarget(
                        actor, allHouses, relationships, zoneManager);
                if (raidTarget == null) break;
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_RAID, log)) break;

                String raidedZone = NobleAITargeting.pickRaidableZone(raidTarget, zoneManager);
                if (raidedZone == null) {
                    log.add(actor.getName() + " finds no raidable zone in "
                            + raidTarget.getName() + ".");
                    break;
                }

                main.map.Zone targetZoneObj = zoneManager.getZone(raidedZone);
                int zoneGold = targetZoneObj != null
                        ? targetZoneObj.getGoldProduction() : GameParameters.ZONE_VILLAGE_GOLD;
                int maxStealFromZone = (int) (zoneGold * GameParameters.RAID_GOLD_ZONE_MULTIPLIER);
                int maxAffordable    = Math.min(actor.getNobleManpower(),
                        actor.getGold() / Math.max(1, GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER));
                int desiredSize = Math.min(maxStealFromZone, maxAffordable);

                NobleArmy raidArmy = firstIdleArmy(actor, armyManager);
                if (raidArmy == null && desiredSize >= GameParameters.NOBLE_ARMY_MIN_RECRUIT_SIZE) {
                    raidArmy = armyManager.recruit(actor, desiredSize);
                    if (raidArmy != null) {
                        log.add(actor.getName() + " raises a raiding party of "
                                + raidArmy.getSize() + ".");
                    }
                }
                if (raidArmy != null && !raidArmy.hasPendingOrder() && raidArmy.getSize() > 0) {
                    armyManager.moveArmy(raidArmy, raidedZone);
                    NobleArmy finalRaidArmy = armyManager.getFirstIdleArmyInZone(
                            actor.getId(), raidedZone);
                    if (finalRaidArmy == null) {
                        Debug.log("noble", "order-issued", actor.getName()
                                + " ERROR: no surviving army after move to "
                                + raidedZone + " for RAID");
                        break;
                    }
                    finalRaidArmy.issueOrder(NobleArmy.OrderType.RAID, raidedZone);
                    Debug.log("noble", "order-issued", actor.getName()
                            + " issued RAID order to army "
                            + finalRaidArmy.getId() + " at " + raidedZone);
                    log.add(actor.getName() + " sends raiders toward " + raidedZone + ".");
                }
            }

            case DEMAND -> {
                if (motivation == Motivation.PRESTIGE) {
                    NobleHouse supTarget = NobleAITargeting.findSuperiorityTarget(
                            actor, allHouses, relationships);
                    if (supTarget == null) break;
                    if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_DEMAND, log)) break;
                    boolean accepted = evaluateAcknowledgeSuperiority(actor, supTarget);
                    if (accepted) {
                        supTarget.addPrestige(-GameParameters.DEMAND_PRESTIGE_AMOUNT);
                        actor.addPrestige(GameParameters.DEMAND_PRESTIGE_AMOUNT);
                        log.add(supTarget.getName() + " acknowledges the superiority of "
                                + actor.getName() + ". Prestige transferred.");
                    } else {
                        log.add(supTarget.getName() + " refuses to acknowledge "
                                + actor.getName() + "'s superiority.");
                        relationships.worsen(actor.getId(), supTarget.getId());
                    }
                    break;
                }
                NobleHouse demTarget = NobleAITargeting.findDemandTarget(
                        actor, allHouses, relationships);
                if (demTarget == null) break;
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_DEMAND, log)) break;
                DemandType type     = demandTypeForMotivation(motivation);
                boolean accepted    = evaluateDemand(actor, demTarget, relationships,
                        allIds, diplomacy, armyManager);
                if (accepted) {
                    applyDemand(actor, demTarget, type, log);
                } else {
                    log.add(demTarget.getName() + " refuses " + actor.getName() + "'s demand.");
                    relationships.worsen(actor.getId(), demTarget.getId());
                }
            }

            case SCHEME -> {
                List<String> rivals = relationships.getAll(
                        actor.getId(), Relationship.RIVAL, allIds);
                if (rivals.isEmpty()) break;
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_SCHEME, log)) break;
                String targetId = rivals.get(NobleAIUtils.RNG.nextInt(rivals.size()));
                NobleHouse schemeTarget = NobleAIUtils.findById(targetId, allHouses);
                if (schemeTarget == null) break;

                double successChance = GameParameters.SCHEME_BASE_SUCCESS_CHANCE
                        + cunning * GameParameters.SCHEME_CUNNING_BONUS_PER_POINT;
                if (NobleAIUtils.RNG.nextDouble() < successChance) {
                    schemeTarget.addPrestige(-GameParameters.AI_SCHEME_PRESTIGE_LOSS);
                    actor.addPrestige(GameParameters.AI_SCHEME_PRESTIGE_GAIN);
                    log.add(actor.getName() + " schemes against " + schemeTarget.getName()
                            + ". Their prestige suffers.");
                } else {
                    log.add(actor.getName() + "'s scheme against " + schemeTarget.getName()
                            + " is discovered. Relation worsens.");
                    relationships.worsen(actor.getId(), schemeTarget.getId());
                }
            }

            case FORTIFY -> {
                int cost = GameParameters.AI_FORTIFY_GOLD_COST;
                if (actor.getGold() < cost) break;
                if (actor.getGold() - cost < NobleAIRelations.getWarChestTarget(
                        actor, allHouses, relationships, armyManager)) break;
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
                List<String> currentAllies = relationships.getAll(
                        actor.getId(), Relationship.ALLIED, allIds);
                if (currentAllies.size() >= GameParameters.ALLIANCE_MAX_PER_HOUSE) break;
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_ALLY, log)) break;

                NobleHouse allyTarget = NobleAITargeting.findAllyTarget(
                        actor, allHouses, relationships);
                if (allyTarget == null) break;
                Relationship currentRel = relationships.get(actor.getId(), allyTarget.getId());
                if (currentRel == Relationship.FRIENDLY || currentRel == Relationship.ALLIED) break;

                int candidateStrength = NobleAIPower.estimatedPower(actor, allyTarget, armyManager)
                        + allyTarget.getTotalGarrisonSize();
                int myStrength = NobleAIPower.exactPotentialFieldArmy(actor, armyManager)
                        + actor.getTotalGarrisonSize();
                if (candidateStrength < myStrength * GameParameters.ALLIANCE_MIN_ARMY_FRACTION) {
                    log.add(actor.getName() + " considers alliance with "
                            + allyTarget.getName() + " but deems them too weak.");
                    break;
                }

                double acceptChance = GameParameters.ALLY_BASE_ACCEPT_CHANCE
                        + diplomacy * GameParameters.ALLY_DIPLOMACY_BONUS_PER_POINT;
                if (NobleAIUtils.RNG.nextDouble() < acceptChance) {
                    relationships.set(actor.getId(), allyTarget.getId(), Relationship.ALLIED);
                    log.add(actor.getName() + " and " + allyTarget.getName()
                            + " form an alliance.");
                } else {
                    log.add(allyTarget.getName() + " declines alliance with "
                            + actor.getName() + ".");
                }
            }

            case GIFT -> {
                NobleHouse giftTarget = NobleAITargeting.findGiftTarget(
                        actor, allHouses, relationships, armyManager);
                if (giftTarget == null) break;

                List<Claim> actorClaims = claimManager.getClaimsFor(actor.getId());
                Claim claimOnTarget = actorClaims.stream()
                        .filter(c -> giftTarget.getZoneIds().contains(c.getZoneId()))
                        .findFirst().orElse(null);

                if (claimOnTarget != null) {
                    claimManager.removeClaim(actor.getId(), claimOnTarget.getZoneId());
                    relationships.improve(actor.getId(), giftTarget.getId());
                    log.add(actor.getName() + " forfeits claim on "
                            + claimOnTarget.getZoneId() + " as gift to "
                            + giftTarget.getName() + ". Relations improve.");
                } else if (actor.getGold() >= GameParameters.GIFT_MONEY_AMOUNT
                        && actor.getGold() - GameParameters.GIFT_MONEY_AMOUNT
                        >= NobleAIRelations.getWarChestTarget(
                                actor, allHouses, relationships, armyManager)) {
                    actor.addGold(-GameParameters.GIFT_MONEY_AMOUNT);
                    giftTarget.addGold(GameParameters.GIFT_MONEY_AMOUNT);
                    relationships.improve(actor.getId(), giftTarget.getId());
                    log.add(actor.getName() + " gifts " + GameParameters.GIFT_MONEY_AMOUNT
                            + " gold to " + giftTarget.getName() + ". Relations improve.");
                }
            }

            case SUPPORT_RIVAL -> {
                List<String> allies = relationships.getAll(
                        actor.getId(), Relationship.ALLIED, allIds);
                if (allies.isEmpty()) break;
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_SUPPORT, log)) break;
                String allyId = allies.get(NobleAIUtils.RNG.nextInt(allies.size()));
                NobleHouse ally = NobleAIUtils.findById(allyId, allHouses);
                if (ally == null) break;
                int support = (int) (actor.getGold() * GameParameters.AI_SUPPORT_GOLD_FRACTION);
                actor.addGold(-support);
                ally.addGold(support);
                log.add(actor.getName() + " sends " + support
                        + " gold to " + ally.getName() + " in support.");
            }

            case SABOTAGE -> {
                NobleHouse sabTarget = NobleAITargeting.findSabotageTarget(
                        actor, allHouses, relationships);
                if (sabTarget == null) break;
                if (actor.getGold() < GameParameters.AI_SABOTAGE_GOLD_COST) break;
                if (actor.getGold() - GameParameters.AI_SABOTAGE_GOLD_COST
                        < NobleAIRelations.getWarChestTarget(
                                actor, allHouses, relationships, armyManager)) break;
                if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_SABOTAGE, log)) break;
                actor.addGold(-GameParameters.AI_SABOTAGE_GOLD_COST);

                double successChance = GameParameters.SABOTAGE_BASE_SUCCESS_CHANCE
                        + cunning * GameParameters.SABOTAGE_CUNNING_BONUS_PER_POINT;
                if (NobleAIUtils.RNG.nextDouble() < successChance) {
                    List<String> validZones = new ArrayList<>();
                    for (String zid : sabTarget.getZoneIds()) {
                        if (sabTarget.getFortificationFor(zid) > 0) validZones.add(zid);
                    }
                    if (!validZones.isEmpty()) {
                        String sabZone = validZones.get(
                                NobleAIUtils.RNG.nextInt(validZones.size()));
                        sabTarget.addFortification(sabZone, -1);
                        log.add(actor.getName() + " sabotages " + sabTarget.getName()
                                + "'s fortifications at " + sabZone + ".");
                    }
                } else {
                    log.add(actor.getName() + "'s sabotage attempt against "
                            + sabTarget.getName() + " fails.");
                }
            }
        }

        return log;
    }

    // -------------------------------------------------------------------------
    // Attack — extracted into its own method for readability
    // -------------------------------------------------------------------------

    private static void executeAttack(NobleHouse actor,
                                       Motivation motivation,
                                       NobleCharacter character,
                                       int cunning, int military,
                                       List<NobleHouse> allHouses,
                                       RelationshipManager relationships,
                                       ClaimManager claimManager,
                                       ZoneManager zoneManager,
                                       NobleArmyManager armyManager,
                                       List<String> log) {
        int myPower = NobleAIPower.estimateAttackPower(actor, armyManager);
        Debug.log("noble", "attack", actor.getName() + " myPower=" + myPower);
        if (myPower <= 0) {
            Debug.log("noble", "attack", actor.getName() + " no power -> fallback");
            executeFallback(actor, motivation, allHouses, relationships,
                    claimManager, zoneManager, armyManager, log);
            return;
        }

        // Evaluate claimed targets
        NobleHouse bestClaimTarget  = null;
        String     bestClaimZone    = null;
        double     bestClaimValue   = 0;
        List<Claim> claims = claimManager.getClaimsFor(actor.getId());
        Debug.log("noble", "attack-claim", actor.getName()
                + " evaluating " + claims.size() + " claims");

        for (Claim c : claims) {
            for (NobleHouse other : allHouses) {
                if (other == actor || other.isEliminated()) continue;
                if (!other.getZoneIds().contains(c.getZoneId())) continue;
                Relationship rel = relationships.get(actor.getId(), other.getId());
                if (rel == Relationship.ALLIED || rel == Relationship.FRIENDLY) {
                    Debug.log("noble", "attack-claim", actor.getName()
                            + " claim on " + c.getZoneId() + " owned by " + other.getName()
                            + " SKIPPED (allied/friendly)");
                    continue;
                }
                int defPower = NobleAIPower.estimateDefenderCombatPower(
                        actor, other, c.getZoneId(), allHouses, armyManager, relationships);
                if (defPower <= 0) continue;
                main.map.Zone z = zoneManager.getZone(c.getZoneId());
                if (z == null) continue;
                double value = (double) z.getGoldProduction() / defPower;
                Debug.log("noble", "attack-claim", actor.getName()
                        + " claim on " + c.getZoneId() + " owner=" + other.getName()
                        + " defPower=" + defPower + " value=" + value);
                if (value > bestClaimValue) {
                    bestClaimValue  = value;
                    bestClaimTarget = other;
                    bestClaimZone   = c.getZoneId();
                }
                break;
            }
        }
        Debug.log("noble", "attack-claim", actor.getName()
                + " bestClaimTarget=" + (bestClaimTarget != null ? bestClaimTarget.getName() : "null")
                + " zone=" + bestClaimZone + " value=" + bestClaimValue);

        // Evaluate reckless (claimless) targets
        NobleHouse recklessTarget = null;
        String     recklessZone   = null;
        double     recklessValue  = 0;
        boolean tryReckless = character != null && cunning < 2 && military >= 2
                && motivation == Motivation.EXPANSION;
        Debug.log("noble", "attack-reckless", actor.getName() + " tryReckless=" + tryReckless);

        if (tryReckless) {
            Object[] reckless = NobleAITargeting.findRecklessClaimlessTarget(
                    actor, allHouses, relationships, claimManager, zoneManager, armyManager);
            if (reckless != null) {
                recklessTarget = (NobleHouse) reckless[0];
                recklessZone   = (String) reckless[1];
                int defPower   = NobleAIPower.estimateDefenderCombatPower(
                        actor, recklessTarget, recklessZone, allHouses, armyManager, relationships);
                main.map.Zone z = zoneManager.getZone(recklessZone);
                if (z != null && defPower > 0) {
                    recklessValue = (double) z.getGoldProduction() / defPower;
                }
                Debug.log("noble", "attack-reckless", actor.getName()
                        + " found target=" + recklessTarget.getName()
                        + " zone=" + recklessZone + " value=" + recklessValue);
            } else {
                Debug.log("noble", "attack-reckless", actor.getName() + " no reckless target found");
            }
        }

        // Choose best option
        NobleHouse target     = null;
        String     attackZone = null;
        boolean    isClaimless = false;

        if (recklessTarget != null
                && recklessValue >= bestClaimValue * GameParameters.RECKLESS_VALUE_MULTIPLIER) {
            target      = recklessTarget;
            attackZone  = recklessZone;
            isClaimless = true;
            Debug.log("noble", "attack-choose", actor.getName() + " -> RECKLESS " + attackZone);
        } else if (bestClaimTarget != null) {
            target      = bestClaimTarget;
            attackZone  = bestClaimZone;
            isClaimless = false;
            Debug.log("noble", "attack-choose", actor.getName() + " -> CLAIM " + attackZone);
        } else {
            Debug.log("noble", "attack-choose", actor.getName() + " -> no target yet");
        }

        // Feasibility gate
        if (target != null) {
            int defPower    = NobleAIPower.estimateDefenderCombatPower(
                    actor, target, attackZone, allHouses, armyManager, relationships);
            double threshold = defPower * GameParameters.NORMAL_ATTACK_STRENGTH_THRESHOLD;
            boolean feasible = myPower >= threshold;
            Debug.log("noble", "attack-feasibility", actor.getName()
                    + " target=" + target.getName() + " zone=" + attackZone
                    + " defPower=" + defPower + " needed=" + threshold
                    + " myPower=" + myPower + " feasible=" + feasible);
            if (!feasible) {
                target = null;
                Debug.log("noble", "attack-feasibility",
                        actor.getName() + " NOT FEASIBLE -> fallback");
            } else {
                Debug.log("noble", "attack-feasibility",
                        actor.getName() + " feasible and proceeding");
            }
        }

        if (target == null) {
            executeFallback(actor, motivation, allHouses, relationships,
                    claimManager, zoneManager, armyManager, log);
            return;
        }

        Debug.log("noble", "attack-exec", actor.getName()
                + " attacking " + attackZone + " isClaimless=" + isClaimless);
        if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_ATTACK, log)) return;

        // Raise or reuse army
        NobleArmy army = firstIdleArmy(actor, armyManager);
        if (army == null) {
            int recruitSize = NobleAIPower.maxRecruitableSize(actor);
            if (recruitSize >= GameParameters.NOBLE_ARMY_MIN_RECRUIT_SIZE) {
                army = armyManager.recruit(actor, recruitSize);
                if (army != null) {
                    log.add(actor.getName() + " raises an army of "
                            + army.getSize() + " for the attack.");
                }
            }
        }
        if (army != null && !army.hasPendingOrder() && army.getSize() > 0) {
            armyManager.moveArmy(army, attackZone);
            NobleArmy finalArmy = armyManager.getFirstIdleArmyInZone(actor.getId(), attackZone);
            if (finalArmy == null) {
                Debug.log("noble", "order-issued", actor.getName()
                        + " ERROR: no surviving army after move to " + attackZone + " for ATTACK");
                return;
            }
            finalArmy.issueOrder(NobleArmy.OrderType.ATTACK, attackZone);
            Debug.log("noble", "order-issued", actor.getName()
                    + " issued ATTACK order to army " + finalArmy.getId() + " at " + attackZone);
            log.add(isClaimless
                    ? actor.getName() + " marches on " + attackZone + " without a claim."
                    : actor.getName() + " marches on " + attackZone + ".");

            NobleAIRelations.updateThreatenedStatus(actor, allHouses, relationships,
                    isClaimless ? GameParameters.THREATENED_CLAIMLESS_MULTIPLIER : 1.0);
            triggerAllyDefense(target, actor, allHouses, relationships, armyManager, log);
        }
    }

    // -------------------------------------------------------------------------
    // Fallback when attack is not feasible
    // -------------------------------------------------------------------------

    static void executeFallback(NobleHouse actor,
                                 Motivation motivation,
                                 List<NobleHouse> allHouses,
                                 RelationshipManager relationships,
                                 ClaimManager claimManager,
                                 ZoneManager zoneManager,
                                 NobleArmyManager armyManager,
                                 List<String> log) {
        Debug.log("noble", "fallback", actor.getName()
                + " motivation=" + motivation + " trying alternatives");

        if (motivation == Motivation.EXPANSION) {
            // Try fabricate
            if (claimManager.getClaimsFor(actor.getId()).isEmpty()
                    && actor.getInfluence() >= GameParameters.AI_INFLUENCE_COST_FABRICATE) {
                String targetZone = NobleAITargeting.findClaimTarget(actor, allHouses, claimManager);
                if (targetZone != null) {
                    List<String> myZones = new ArrayList<>(actor.getZoneIds());
                    NobleCharacter ch = actor.getActiveCharacter();
                    int cunning = ch != null ? ch.getCunning() : 0;
                    int ownerCunning = 0;
                    for (NobleHouse other : allHouses) {
                        if (other.getZoneIds().contains(targetZone)) {
                            ownerCunning = other.getActiveCharacter() != null
                                    ? other.getActiveCharacter().getCunning() : 0;
                            break;
                        }
                    }
                    boolean success = claimManager.fabricate(actor.getId(), targetZone,
                            cunning, ownerCunning, NobleAIUtils.RNG,
                            myZones, zoneManager.getZones());
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
                        log.add(actor.getName()
                                + " fails to fabricate a claim. Cunning insufficient.");
                    }
                    return;
                }
            }
            // Try raid
            NobleHouse raidTarget = NobleAITargeting.findRaidTarget(
                    actor, allHouses, relationships, zoneManager);
            if (raidTarget != null
                    && actor.getInfluence() >= GameParameters.AI_INFLUENCE_COST_RAID) {
                String raidedZone = NobleAITargeting.pickRaidableZone(raidTarget, zoneManager);
                if (raidedZone != null) {
                    NobleArmy raidArmy = firstIdleArmy(actor, armyManager);
                    if (raidArmy == null) {
                        int recruitSize = NobleAIPower.maxRecruitableSize(actor);
                        if (recruitSize >= GameParameters.NOBLE_ARMY_MIN_RECRUIT_SIZE) {
                            raidArmy = armyManager.recruit(actor, recruitSize);
                            if (raidArmy != null) {
                                log.add(actor.getName() + " raises a raiding party of "
                                        + raidArmy.getSize() + ".");
                            }
                        }
                    }
                    if (raidArmy != null && !raidArmy.hasPendingOrder() && raidArmy.getSize() > 0) {
                        armyManager.moveArmy(raidArmy, raidedZone);
                        NobleArmy finalRaidArmy = armyManager.getFirstIdleArmyInZone(
                                actor.getId(), raidedZone);
                        if (finalRaidArmy == null) {
                            Debug.log("noble", "raid-fallback",
                                    actor.getName() + " no surviving army after move");
                            return;
                        }
                        finalRaidArmy.issueOrder(NobleArmy.OrderType.RAID, raidedZone);
                        log.add(actor.getName() + " sends raiders toward " + raidedZone + ".");
                        return;
                    }
                }
            }
        }
        // Default: fortify
        applyFortify(actor, allHouses, relationships, armyManager, log);
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    private static void applyFortify(NobleHouse actor,
                                      List<NobleHouse> allHouses,
                                      RelationshipManager relationships,
                                      NobleArmyManager armyManager,
                                      List<String> log) {
        if (actor.getGold() < GameParameters.AI_FORTIFY_GOLD_COST) return;
        actor.addGold(-GameParameters.AI_FORTIFY_GOLD_COST);
        actor.addDefense(GameParameters.AI_FORTIFY_DEFENSE_GAIN);
        String fortZone = actor.getCapitalZoneId();
        if (fortZone != null) {
            actor.addGarrison(fortZone, GameParameters.FORTIFY_GARRISON_GAIN);
        }
        log.add(actor.getName() + " fortifies. Defense +"
                + GameParameters.AI_FORTIFY_DEFENSE_GAIN
                + ", Garrison +" + GameParameters.FORTIFY_GARRISON_GAIN + ".");
    }

    private static void triggerAllyDefense(NobleHouse attacked, NobleHouse attacker,
                                            List<NobleHouse> allHouses,
                                            RelationshipManager relationships,
                                            NobleArmyManager armyManager,
                                            List<String> log) {
        List<String> allIds = NobleAIUtils.allHouseIds(allHouses);
        List<String> allies = new ArrayList<>(
                relationships.getAll(attacked.getId(), Relationship.ALLIED, allIds));
        int attackerFieldArmy = NobleAIPower.exactPotentialFieldArmy(attacker, armyManager);

        for (String allyId : allies) {
            NobleHouse ally = NobleAIUtils.findById(allyId, allHouses);
            if (ally == null || ally.isEliminated()) continue;
            Relationship allyWithAttacker = relationships.get(ally.getId(), attacker.getId());
            if (allyWithAttacker == Relationship.ALLIED
                    || allyWithAttacker == Relationship.FRIENDLY) continue;

            int allyStrength = NobleAIPower.exactPotentialFieldArmy(ally, armyManager)
                    + ally.getTotalGarrisonSize();
            boolean strongEnough = allyStrength
                    >= attackerFieldArmy * GameParameters.ALLY_DEFENSE_MIN_STRENGTH_FRACTION;

            if (strongEnough) {
                int allyMilitary = ally.getActiveCharacter() != null
                        ? ally.getActiveCharacter().getMilitary() : 0;
                ArmyForce allyForce = new ArmyForce(ally.getId(),
                        ally.getTotalArmySize(), attacked.getDefense(), allyMilitary);
                ArmyForce atkForce  = new ArmyForce(attacker.getId(),
                        attackerFieldArmy, 0, 0);
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

    private static void applyDemand(NobleHouse requester, NobleHouse target,
                                     DemandType type, List<String> log) {
        switch (type) {
            case WEALTH -> {
                int amount = (int) (target.getGold() * GameParameters.DEMAND_WEALTH_FRACTION);
                target.addGold(-amount);
                requester.addGold(amount);
                log.add(target.getName() + " yields " + amount + " gold to "
                        + requester.getName() + ".");
            }
            case ARMY -> {
                requester.addToRaisedArmy(GameParameters.DEMAND_ARMY_AMOUNT);
                log.add(target.getName() + " sends " + GameParameters.DEMAND_ARMY_AMOUNT
                        + " soldiers to " + requester.getName() + ".");
            }
            case ACKNOWLEDGE_SUPERIORITY -> {
                target.addPrestige(-GameParameters.DEMAND_PRESTIGE_AMOUNT);
                requester.addPrestige(GameParameters.DEMAND_PRESTIGE_AMOUNT);
                log.add(target.getName() + " acknowledges the superiority of "
                        + requester.getName() + ".");
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

    private static boolean evaluateAcknowledgeSuperiority(NobleHouse demander, NobleHouse target) {
        int demanderMilitary = demander.getActiveCharacter() != null
                ? demander.getActiveCharacter().getMilitary() : 0;
        int targetMilitary   = target.getActiveCharacter() != null
                ? target.getActiveCharacter().getMilitary() : 0;
        if (demanderMilitary <= targetMilitary) return false;
        double base  = GameParameters.SUPERIORITY_BASE_ACCEPT_CHANCE;
        double noise = (NobleAIUtils.RNG.nextDouble() - 0.5) * GameParameters.SUPERIORITY_RANDOM_RANGE;
        return (base + noise) >= 0.5;
    }

    static boolean canSpendInfluence(NobleHouse house, int cost, List<String> log) {
        if (house.getInfluence() < cost) return false;
        house.addInfluence(-cost);
        return true;
    }

    private static NobleArmy firstIdleArmy(NobleHouse actor, NobleArmyManager armyManager) {
        for (NobleArmy a : armyManager.getArmiesForHouse(actor.getId())) {
            if (!a.hasPendingOrder()) return a;
        }
        return null;
    }
}