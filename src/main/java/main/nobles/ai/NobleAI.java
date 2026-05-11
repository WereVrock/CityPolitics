package main.nobles.ai;

import main.nobles.*;
import main.nobles.combat.ArmyForce;
import main.nobles.combat.CombatResolver;
import main.nobles.combat.CombatResult;
import main.map.ZoneState;
import main.parameters.GameParameters;

import java.util.*;

/**
 * Per-house AI brain.
 * Handles motivation, action selection, coalition logic, and threatened state.
 */
public class NobleAI {

    private static final Random RNG = new Random();

    // ─── Entry point ─────────────────────────────────────────────────────────

    public static List<String> tick(NobleHouse actor,
                                    List<NobleHouse> allHouses,
                                    RelationshipManager relationships,
                                    ClaimManager claimManager,
                                    main.map.ZoneManager zoneManager) {
        List<String> log = new ArrayList<>();
        if (actor.isEliminated()) return log;

        relationships.tickDecay(allHouseIds(allHouses));
        considerBreakingAlliances(actor, allHouses, relationships, log);

        Motivation motivation = pickMotivation(actor.getActiveCharacter());
        NobleAction action    = pickAction(actor, motivation, allHouses,
                                           relationships, claimManager);
        if (action == null) return log;

        log.addAll(execute(actor, action, motivation, allHouses,
                           relationships, claimManager, zoneManager));
        return log;
    }

    // ─── Coalition check (called from NobleHouseManager) ─────────────────────

    /**
     * Check if a coalition should form against any house with >= COALITION_ZONE_THRESHOLD zones.
     * Returns log lines. Executes coalition attack if threshold met.
     */
    public static List<String> checkCoalition(List<NobleHouse> allHouses,
                                               RelationshipManager relationships,
                                               ClaimManager claimManager,
                                               main.map.ZoneManager zoneManager) {
        List<String> log = new ArrayList<>();

        for (NobleHouse threat : allHouses) {
            if (threat.isEliminated()) continue;
            if (threat.getZoneIds().size() < GameParameters.COALITION_ZONE_THRESHOLD) continue;

            log.addAll(tryFormCoalition(threat, allHouses, relationships,
                claimManager, zoneManager));
        }
        return log;
    }

    /**
     * Update threatened status for all neutral houses after an attack.
     */
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

    /**
     * Tick threatened decay for all houses. 5% chance per turn to clear.
     */
    public static void tickThreatenedDecay(List<NobleHouse> allHouses) {
        for (NobleHouse house : allHouses) {
            if (house.isThreatened()
                    && RNG.nextDouble() < GameParameters.THREATENED_DECAY_CHANCE) {
                house.setThreatened(false);
            }
        }
    }

    // ─── Coalition logic ─────────────────────────────────────────────────────

    private static List<String> tryFormCoalition(NobleHouse threat,
                                                   List<NobleHouse> allHouses,
                                                   RelationshipManager relationships,
                                                   ClaimManager claimManager,
                                                   main.map.ZoneManager zoneManager) {
        List<String> log = new ArrayList<>();

        // Gather eligible coalition members
        List<NobleHouse> members = new ArrayList<>();
        for (NobleHouse h : allHouses) {
            if (h == threat || h.isEliminated()) continue;
            if (!isCoalitionEligible(h, threat, relationships)) continue;
            members.add(h);
        }

        if (members.isEmpty()) return log;

        // Check combined army >= 90% of threat
        int combinedArmy = 0;
        for (NobleHouse m : members) combinedArmy += m.getTotalArmySize();

        double ratio = (double) combinedArmy / Math.max(1, threat.getTotalArmySize());
        if (ratio < GameParameters.COALITION_ARMY_THRESHOLD) return log;

        // Find coordinator — highest prestige among members
        NobleHouse coordinator = members.get(0);
        for (NobleHouse m : members) {
            if (m.getPrestige() > coordinator.getPrestige()) coordinator = m;
        }

        // Log coalition formation
        StringBuilder memberNames = new StringBuilder();
        for (int i = 0; i < members.size(); i++) {
            if (i > 0) memberNames.append(", ");
            memberNames.append(members.get(i).getName());
        }
        log.add("=== A coalition forms against " + threat.getName()
            + " ===  Coordinator: " + coordinator.getName()
            + "  Members: " + memberNames);

        // Build combined force
        List<ArmyForce> attackers = new ArrayList<>();
        for (NobleHouse m : members) {
            int military = m.getActiveCharacter() != null
                ? m.getActiveCharacter().getMilitary() : 0;
            attackers.add(new ArmyForce(m.getId(),
                (int)(m.getTotalArmySize() * militaryMultiplier(military)), 0));
        }

        int defMilitary = threat.getActiveCharacter() != null
            ? threat.getActiveCharacter().getMilitary() : 0;
        ArmyForce defender = new ArmyForce(threat.getId(),
            (int)(threat.getTotalArmySize() * militaryMultiplier(defMilitary)),
            threat.getDefense());

        CombatResult result = CombatResolver.resolveCoalition(
            attackers, defender, coordinator.getId());
        log.addAll(result.getLog());

        // Apply losses back to houses
        for (int i = 0; i < members.size(); i++) {
            members.get(i).setTotalArmySize(attackers.get(i).getArmySize());
        }
        threat.setTotalArmySize(defender.getArmySize());

        if (coordinator.getId().equals(result.getWinnerId())) {
            // Coordinator gets a claimed zone from threat
            List<Claim> coordClaims = claimManager.getClaimsFor(coordinator.getId());
            boolean transferred = false;
            for (Claim claim : coordClaims) {
                if (threat.getZoneIds().contains(claim.getZoneId())) {
                    String zone = claim.getZoneId();
                    applyConquestToZone(zone, coordinator, threat,
                        claimManager, zoneManager, log);
                    transferred = true;
                    break;
                }
            }
            if (!transferred && !threat.getZoneIds().isEmpty()) {
                // Take any zone if no claim
                String zone = threat.getZoneIds().get(0);
                applyConquestToZone(zone, coordinator, threat,
                    claimManager, zoneManager, log);
            }
            // Clear threatened status for all members
            for (NobleHouse m : members) m.setThreatened(false);
            log.add("Coalition dissolves after successful engagement.");
        } else {
            log.add("Coalition fails to break " + threat.getName()
                + ". Members regroup.");
        }

        return log;
    }

    private static boolean isCoalitionEligible(NobleHouse house, NobleHouse threat,
                                                RelationshipManager relationships) {
        Relationship rel = relationships.get(house.getId(), threat.getId());
        return rel == Relationship.RIVAL
            || rel == Relationship.HOSTILE
            || (rel == Relationship.NEUTRAL && house.isThreatened());
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
                    NobleHouse target = findAttackTarget(actor, allHouses,
                        relationships, claimManager);
                    if (target != null) yield NobleAction.ATTACK;
                }
                yield NobleAction.FABRICATE_CLAIM;
            }
            case WEALTH -> {
                NobleHouse raidTarget = findRaidTarget(actor, allHouses,
                    relationships, null);
                yield raidTarget != null ? NobleAction.RAID : NobleAction.DEMAND;
            }
            case SECURITY -> {
                List<String> rivals = relationships.getAll(actor.getId(),
                    Relationship.RIVAL, allIds);
                if (!rivals.isEmpty()
                        && actor.getDefense() < GameParameters.AI_FORTIFY_THRESHOLD) {
                    yield NobleAction.FORTIFY;
                }
                int allyCount = relationships.getAll(actor.getId(),
                    Relationship.ALLIED, allIds).size();
                yield allyCount < GameParameters.ALLIANCE_MAX_PER_HOUSE
                    ? NobleAction.ALLY : NobleAction.FORTIFY;
            }
            case PRESTIGE -> {
                // Only attempt acknowledge superiority if we have military advantage
                NobleHouse supTarget = findSuperiorityTarget(actor, allHouses,
                    relationships);
                if (supTarget != null) yield NobleAction.DEMAND;
                List<String> rivals = relationships.getAll(actor.getId(),
                    Relationship.RIVAL, allIds);
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
                                        main.map.ZoneManager zoneManager) {
        List<String> log    = new ArrayList<>();
        List<String> allIds = allHouseIds(allHouses);
        NobleCharacter character = actor.getActiveCharacter();
        int cunning   = character != null ? character.getCunning()   : 0;
        int military  = character != null ? character.getMilitary()  : 0;
        int diplomacy = character != null ? character.getDiplomacy() : 0;

        switch (action) {

            case FABRICATE_CLAIM -> {
                if (!canSpendInfluence(actor,
                        GameParameters.AI_INFLUENCE_COST_FABRICATE, log)) break;
                String targetZone = findClaimTarget(actor, allHouses, claimManager);
                if (targetZone == null) break;

                boolean success = claimManager.fabricate(actor.getId(),
                    targetZone, cunning, RNG);
                if (success) {
                    log.add(actor.getName() + " fabricates a claim on " + targetZone + ".");
                    for (NobleHouse other : allHouses) {
                        if (other.getZoneIds().contains(targetZone)) {
                            relationships.set(actor.getId(), other.getId(),
                                Relationship.RIVAL);
                            log.add(other.getName() + " becomes rival with "
                                + actor.getName() + " over the claim.");
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

                ArmyForce atk = new ArmyForce(actor.getId(),
                    (int)(actor.getTotalArmySize() * militaryMultiplier(military)), 0);
                int defMilitary = target.getActiveCharacter() != null
                    ? target.getActiveCharacter().getMilitary() : 0;
                ArmyForce def = new ArmyForce(target.getId(),
                    (int)(target.getTotalArmySize() * militaryMultiplier(defMilitary)),
                    target.getDefense());

                CombatResult result = CombatResolver.resolve(atk, def);
                log.addAll(result.getLog());

                actor.setTotalArmySize(atk.getArmySize());
                target.setTotalArmySize(def.getArmySize());

                // Update threatened status for neutral observers
                updateThreatenedStatus(actor, allHouses, relationships);

                if (actor.getId().equals(result.getWinnerId())) {
                    transferClaimedZone(actor, target, claimManager,
                        zoneManager, allHouses, log);
                    relationships.set(actor.getId(), target.getId(), Relationship.RIVAL);
                } else {
                    relationships.set(actor.getId(), target.getId(), Relationship.RIVAL);
                }

                triggerAllyDefense(target, actor, allHouses, relationships, log);
            }

            case RAID -> {
                NobleHouse target = findRaidTarget(actor, allHouses,
                    relationships, zoneManager);
                if (target == null) break;
                if (!canSpendInfluence(actor,
                        GameParameters.AI_INFLUENCE_COST_RAID, log)) break;

                // Pick a non-raided zone from target
                String raidedZone = pickRaidableZone(target, zoneManager);
                if (raidedZone == null) {
                    log.add(actor.getName() + " finds no raidable zone in "
                        + target.getName() + ".");
                    break;
                }

                int maxSteal = (int)(target.getZoneIds().size()
                    * GameParameters.NOBLE_ZONE_GOLD_PER_TURN
                    * GameParameters.RAID_MAX_GOLD_ZONE_MULTIPLIER);
                int stolen = Math.min(maxSteal,
                    (int)(target.getGold() * GameParameters.AI_RAID_GOLD_FRACTION));
                stolen = Math.max(0, stolen);

                target.addGold(-stolen);
                actor.addGold(stolen);

                // Mark zone as raided
                ZoneState state = zoneManager.getState(raidedZone);
                if (state != null) state.markRaided();

                log.add(actor.getName() + " raids " + target.getName()
                    + " (zone: " + raidedZone + ") and steals " + stolen + " gold.");

                int raidCount = relationships.recordRaid(actor.getId(), target.getId());
                log.add(target.getName() + " relation worsens (raid #" + raidCount + ").");
            }

            case DEMAND -> {
                // Check if this is acknowledge superiority
                if (motivation == Motivation.PRESTIGE) {
                    NobleHouse target = findSuperiorityTarget(actor, allHouses,
                        relationships);
                    if (target == null) break;
                    if (!canSpendInfluence(actor,
                            GameParameters.AI_INFLUENCE_COST_DEMAND, log)) break;

                    boolean accepted = evaluateAcknowledgeSuperiority(actor, target);
                    if (accepted) {
                        target.addPrestige(-GameParameters.DEMAND_PRESTIGE_AMOUNT);
                        actor.addPrestige(GameParameters.DEMAND_PRESTIGE_AMOUNT);
                        log.add(target.getName()
                            + " acknowledges the superiority of " + actor.getName()
                            + ". Prestige transferred.");
                    } else {
                        log.add(target.getName()
                            + " refuses to acknowledge " + actor.getName()
                            + "'s superiority.");
                        relationships.worsen(actor.getId(), target.getId());
                    }
                    break;
                }

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

                // Don't ally with FRIENDLY or ALLIED target (already better)
                Relationship currentRel = relationships.get(actor.getId(), target.getId());
                if (currentRel == Relationship.FRIENDLY
                        || currentRel == Relationship.ALLIED) break;

                boolean tooWeak = target.getTotalArmySize()
                    < actor.getTotalArmySize()
                        * GameParameters.ALLIANCE_MIN_ARMY_FRACTION;
                if (tooWeak) {
                    log.add(actor.getName() + " considers alliance with "
                        + target.getName() + " but deems them too weak.");
                    break;
                }

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
        List<String> allies = new ArrayList<>(
            relationships.getAll(attacked.getId(), Relationship.ALLIED, allIds));

        for (String allyId : allies) {
            NobleHouse ally = findById(allyId, allHouses);
            if (ally == null || ally.isEliminated()) continue;

            // Don't join if friendly/allied with attacker
            Relationship allyWithAttacker = relationships.get(ally.getId(),
                attacker.getId());
            if (allyWithAttacker == Relationship.ALLIED
                    || allyWithAttacker == Relationship.FRIENDLY) continue;

            boolean strongEnough = ally.getTotalArmySize()
                >= attacker.getTotalArmySize()
                    * GameParameters.ALLY_DEFENSE_MIN_STRENGTH_FRACTION;

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
        score += (requester.getPrestige() - target.getPrestige())
            * GameParameters.DEMAND_PRESTIGE_WEIGHT;
        score += (requester.getTotalArmySize() - target.getTotalArmySize())
            * GameParameters.DEMAND_ARMY_WEIGHT;
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

    /**
     * Acknowledge superiority: accepted only if demander has strictly higher
     * military score. Small randomness ±10%.
     */
    private static boolean evaluateAcknowledgeSuperiority(NobleHouse demander,
                                                            NobleHouse target) {
        int demanderMilitary = demander.getActiveCharacter() != null
            ? demander.getActiveCharacter().getMilitary() : 0;
        int targetMilitary   = target.getActiveCharacter() != null
            ? target.getActiveCharacter().getMilitary() : 0;

        if (demanderMilitary <= targetMilitary) return false;

        double base    = GameParameters.SUPERIORITY_BASE_ACCEPT_CHANCE;
        double noise   = (RNG.nextDouble() - 0.5) * GameParameters.SUPERIORITY_RANDOM_RANGE;
        return (base + noise) >= 0.5;
    }

    // ─── Target finders ──────────────────────────────────────────────────────

    private static NobleHouse findAttackTarget(NobleHouse actor,
                                                List<NobleHouse> allHouses,
                                                RelationshipManager relationships,
                                                ClaimManager claimManager) {
        List<Claim> claims = claimManager.getClaimsFor(actor.getId());
        NobleHouse  best   = null;
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
            // Must have at least one raidable zone
            if (zoneManager != null
                    && pickRaidableZone(other, zoneManager) == null) continue;
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
            if (relationships.get(actor.getId(), other.getId())
                    == Relationship.RIVAL) continue;
            int score = -other.getGold();
            if (score > bestScore) {
                best      = other;
                bestScore = score;
            }
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
            Relationship rel = relationships.get(actor.getId(), other.getId());
            if (rel == Relationship.RIVAL) continue;
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
            if (relationships.get(actor.getId(), other.getId())
                    == Relationship.NEUTRAL) return other;
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

    // ─── Zone transfer ───────────────────────────────────────────────────────

    private static void transferClaimedZone(NobleHouse winner, NobleHouse loser,
                                             ClaimManager claimManager,
                                             main.map.ZoneManager zoneManager,
                                             List<NobleHouse> allHouses,
                                             List<String> log) {
        List<Claim> claims = new ArrayList<>(claimManager.getClaimsFor(winner.getId()));
        for (Claim claim : claims) {
            if (loser.getZoneIds().contains(claim.getZoneId())) {
                String zone = claim.getZoneId();
                applyConquestToZone(zone, winner, loser, claimManager,
                    zoneManager, log);
                return;
            }
        }
    }

    private static void applyConquestToZone(String zoneId,
                                             NobleHouse winner, NobleHouse loser,
                                             ClaimManager claimManager,
                                             main.map.ZoneManager zoneManager,
                                             List<String> log) {
        loser.removeZone(zoneId);
        winner.addZone(zoneId);
        claimManager.removeAllClaimsOnZone(zoneId);

        ZoneState state = zoneManager.getState(zoneId);
        if (state != null) state.markConquered();

        log.add(winner.getName() + " captures " + zoneId
            + " from " + loser.getName() + ".");
        if (loser.isEliminated()) {
            log.add(loser.getName() + " has been eliminated.");
        }
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
                requester.addToRaisedArmy(GameParameters.DEMAND_ARMY_AMOUNT);
                log.add(target.getName() + " sends "
                    + GameParameters.DEMAND_ARMY_AMOUNT
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
            case WEALTH            -> DemandType.WEALTH;
            case EXPANSION, SECURITY -> DemandType.ARMY;
            case PRESTIGE          -> DemandType.ACKNOWLEDGE_SUPERIORITY;
        };
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