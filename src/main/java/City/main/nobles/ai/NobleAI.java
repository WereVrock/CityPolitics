package City.main.nobles.ai;

import City.debug.Debug;
import City.main.map.ZoneManager;
import City.main.nobles.ClaimManager;
import City.main.nobles.Motivation;
import City.main.nobles.NobleArmyManager;
import City.main.nobles.NobleHouse;
import City.main.nobles.RelationshipManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Facade — all external callers hit this class unchanged.
 * Real logic lives in NobleAIActions, NobleAIBattleJoiner, NobleAIMotivation,
 * NobleAIPower, NobleAIRelations, and NobleAITargeting.
 */
public class NobleAI {

    public static List<String> tick(NobleHouse actor,
                                    List<NobleHouse> allHouses,
                                    RelationshipManager relationships,
                                    ClaimManager claimManager,
                                    ZoneManager zoneManager,
                                    NobleArmyManager armyManager) {
        List<String> log = new ArrayList<>();
        if (actor.isEliminated()) return log;

        relationships.tickDecay(NobleAIUtils.allHouseIds(allHouses));
        NobleAIRelations.considerBreakingAlliances(actor, allHouses, relationships, armyManager, log);

        int warChestTarget = NobleAIRelations.getWarChestTarget(actor, allHouses, relationships, armyManager);
        Debug.log("noble", "warchest", actor.getName() + " target=" + warChestTarget);

        NobleAction opportunismAction = OpportunismEvaluator.evaluate(
                actor, allHouses, relationships, claimManager, armyManager, zoneManager, log);
        if (opportunismAction != null) {
            log.addAll(NobleAIActions.execute(actor, opportunismAction, Motivation.EXPANSION,
                    allHouses, relationships, claimManager, zoneManager, armyManager));
            NobleAIBattleJoiner.issueJoinBattleOrders(actor, allHouses, relationships, armyManager, log);
            return log;
        }

        Motivation motivation = NobleAIMotivation.pickMotivation(actor.getActiveCharacter());
        NobleAction action    = NobleAIMotivation.pickAction(actor, motivation, allHouses,
                relationships, claimManager, armyManager);
        Debug.log("noble", "tick", actor.getName() + " motivation=" + motivation + " action=" + action);
        if (action != null) {
            log.addAll(NobleAIActions.execute(actor, action, motivation,
                    allHouses, relationships, claimManager, zoneManager, armyManager));
        }

        NobleAIBattleJoiner.issueJoinBattleOrders(actor, allHouses, relationships, armyManager, log);
        return log;
    }

    // -------------------------------------------------------------------------
    // Power estimation — delegates to NobleAIPower
    // -------------------------------------------------------------------------

    public static int exactPotentialFieldArmy(NobleHouse house, NobleArmyManager armyManager) {
        return NobleAIPower.exactPotentialFieldArmy(house, armyManager);
    }

    public static int estimatedPower(NobleHouse observer, NobleHouse target,
                                     NobleArmyManager armyManager) {
        return NobleAIPower.estimatedPower(observer, target, armyManager);
    }

    public static int estimateAttackPower(NobleHouse house, NobleArmyManager armyManager) {
        return NobleAIPower.estimateAttackPower(house, armyManager);
    }

    public static int estimateMemberPower(NobleHouse member, NobleArmyManager armyManager) {
        return NobleAIPower.estimateMemberPower(member, armyManager);
    }

    public static int estimateDefenderCombatPower(NobleHouse attacker, NobleHouse defender,
                                                   String zoneId,
                                                   List<NobleHouse> allHouses,
                                                   NobleArmyManager armyManager,
                                                   RelationshipManager relationships) {
        return NobleAIPower.estimateDefenderCombatPower(
                attacker, defender, zoneId, allHouses, armyManager, relationships);
    }

    // -------------------------------------------------------------------------
    // Threat / alliance / claim — delegates to NobleAIRelations
    // -------------------------------------------------------------------------

    public static void updateThreatenedStatus(NobleHouse attacker,
                                               List<NobleHouse> allHouses,
                                               RelationshipManager relationships) {
        NobleAIRelations.updateThreatenedStatus(attacker, allHouses, relationships);
    }

    public static void updateThreatenedStatus(NobleHouse attacker,
                                               List<NobleHouse> allHouses,
                                               RelationshipManager relationships,
                                               double multiplier) {
        NobleAIRelations.updateThreatenedStatus(attacker, allHouses, relationships, multiplier);
    }

    public static void tickThreatenedDecay(List<NobleHouse> allHouses) {
        NobleAIRelations.tickThreatenedDecay(allHouses);
    }

    public static void tickClaimDecay(List<NobleHouse> allHouses,
                                       RelationshipManager relationships,
                                       ClaimManager claimManager,
                                       List<String> log) {
        NobleAIRelations.tickClaimDecay(allHouses, relationships, claimManager, log);
    }

    // -------------------------------------------------------------------------
    // Demand evaluation — delegates to NobleAIActions
    // -------------------------------------------------------------------------

    public static boolean evaluateDemand(NobleHouse requester, NobleHouse target,
                                          RelationshipManager relationships,
                                          List<String> allIds,
                                          int requesterDiplomacy,
                                          NobleArmyManager armyManager) {
        return NobleAIActions.evaluateDemand(
                requester, target, relationships, allIds, requesterDiplomacy, armyManager);
    }

public static int getWarChestTarget(NobleHouse actor, List<NobleHouse> allHouses,
                                         RelationshipManager relationships,
                                         NobleArmyManager armyManager) {
        return NobleAIRelations.getWarChestTarget(actor, allHouses, relationships, armyManager);
    }

}