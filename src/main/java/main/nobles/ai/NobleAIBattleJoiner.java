package main.nobles.ai;

import debug.Debug;
import main.nobles.NobleArmy;
import main.nobles.NobleArmyManager;
import main.nobles.NobleHouse;
import main.nobles.Relationship;
import main.nobles.RelationshipManager;
import main.parameters.GameParameters;

import java.util.ArrayList;
import java.util.List;

/** Scans pending battles and issues JOIN_BATTLE orders for eligible armies. */
final class NobleAIBattleJoiner {

    private NobleAIBattleJoiner() {}

    // -------------------------------------------------------------------------
    // Inner record
    // -------------------------------------------------------------------------

    private static final class PendingBattle {
        final NobleHouse attacker;
        final NobleHouse defender;
        final String     zoneId;

        PendingBattle(NobleHouse attacker, NobleHouse defender, String zoneId) {
            this.attacker = attacker;
            this.defender = defender;
            this.zoneId   = zoneId;
        }
    }

    // -------------------------------------------------------------------------
    // Public entry point
    // -------------------------------------------------------------------------

    static void issueJoinBattleOrders(NobleHouse actor,
                                       List<NobleHouse> allHouses,
                                       RelationshipManager relationships,
                                       NobleArmyManager armyManager,
                                       List<String> log) {
        List<PendingBattle> battles = collectPendingBattles(actor, allHouses, armyManager);
        Debug.log("noble", "join-battle",
                actor.getName() + " scanning for battles to join");

        if (battles.isEmpty()) return;

        List<PendingBattle> joinAsAttacker = new ArrayList<>();
        List<PendingBattle> joinAsDefender = new ArrayList<>();
        classifyBattles(actor, battles, relationships, joinAsAttacker, joinAsDefender);

        List<PendingBattle> toJoin = new ArrayList<>();
        toJoin.addAll(joinAsDefender);
        toJoin.addAll(joinAsAttacker);
        if (toJoin.isEmpty()) return;

        // Affordability check
        int costPerSoldier      = Math.max(1, GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER);
        int maxAffordableJoin   = Math.min(actor.getNobleManpower(),
                actor.getGold() / costPerSoldier);
        if (maxAffordableJoin < GameParameters.NOBLE_ARMY_MIN_RECRUIT_SIZE) {
            Debug.log("noble", "join-battle", actor.getName()
                    + " cannot afford even a minimal force (have "
                    + actor.getNobleManpower() + " manpower, " + actor.getGold() + " gold)");
            return;
        }

        // Recruit if needed
        if (armyManager.getArmiesForHouse(actor.getId()).isEmpty()) {
            NobleArmy recruited = armyManager.recruit(actor, maxAffordableJoin);
            if (recruited != null) {
                log.add(actor.getName() + " raises " + recruited.getSize()
                        + " soldiers to join battle.");
            }
        }

        // Collect idle armies and merge them into a single pool
        List<NobleArmy> available = new ArrayList<>();
        for (NobleArmy a : armyManager.getArmiesForHouse(actor.getId())) {
            if (!a.hasPendingOrder() && a.isAlive()) available.add(a);
        }
        if (available.isEmpty()) {
            Debug.log("noble", "join-battle",
                    actor.getName() + " has no available armies to join battles");
            return;
        }

        NobleArmy pool = available.get(0);
        for (int i = 1; i < available.size(); i++) {
            NobleArmy other = available.get(i);
            pool.setSize(pool.getSize() + other.getSize());
            armyManager.remove(other);
        }
        Debug.log("noble", "join-battle", actor.getName()
                + " merged " + available.size() + " armies into pool of size " + pool.getSize());

        // Dispatch — up to two battles
        int battleCount = Math.min(toJoin.size(), 2);
        Debug.log("noble", "join-battle", actor.getName()
                + " joining " + battleCount + " battle(s) out of " + toJoin.size());

        if (battleCount == 1) {
            dispatchSingle(actor, pool, toJoin.get(0), armyManager, log);
        } else {
            dispatchDouble(actor, pool, toJoin.get(0), toJoin.get(1), armyManager, log);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static List<PendingBattle> collectPendingBattles(NobleHouse actor,
                                                               List<NobleHouse> allHouses,
                                                               NobleArmyManager armyManager) {
        List<PendingBattle> battles = new ArrayList<>();
        for (NobleHouse other : allHouses) {
            if (other == actor || other.isEliminated()) continue;
            for (NobleArmy a : armyManager.getArmiesForHouse(other.getId())) {
                if (a.getPendingOrder() == NobleArmy.OrderType.ATTACK
                        && a.getPendingTargetZoneId() != null) {
                    NobleHouse defender = findZoneOwner(a.getPendingTargetZoneId(), allHouses);
                    if (defender != null) {
                        battles.add(new PendingBattle(other, defender,
                                a.getPendingTargetZoneId()));
                    }
                }
            }
        }
        return battles;
    }

    private static void classifyBattles(NobleHouse actor,
                                         List<PendingBattle> battles,
                                         RelationshipManager relationships,
                                         List<PendingBattle> joinAsAttacker,
                                         List<PendingBattle> joinAsDefender) {
        for (PendingBattle battle : battles) {
            Relationship relToAtk = relationships.get(actor.getId(), battle.attacker.getId());
            Relationship relToDef = relationships.get(actor.getId(), battle.defender.getId());

            // Always defend own zones
            if (battle.defender == actor) {
                joinAsDefender.add(battle);
                Debug.log("noble", "join-battle",
                        actor.getName() + " will defend own zone " + battle.zoneId);
                continue;
            }

            if (relToAtk == Relationship.HOSTILE || relToAtk == Relationship.RIVAL) {
                if (relToDef == Relationship.ALLIED
                        || actor.isThreatenedBy(battle.attacker.getId())) {
                    joinAsDefender.add(battle);
                    Debug.log("noble", "join-battle", actor.getName()
                            + " will defend " + battle.defender.getName()
                            + " at " + battle.zoneId + " (hostile to attacker)");
                }
                continue;
            }

            boolean wantAttack = relToAtk == Relationship.ALLIED
                    || relToDef == Relationship.HOSTILE
                    || relToDef == Relationship.RIVAL
                    || actor.isThreatenedBy(battle.defender.getId());
            boolean wantDefend = relToDef == Relationship.ALLIED;

            if (wantAttack) {
                joinAsAttacker.add(battle);
                Debug.log("noble", "join-battle", actor.getName()
                        + " will attack " + battle.defender.getName() + " at " + battle.zoneId);
            } else if (wantDefend) {
                joinAsDefender.add(battle);
                Debug.log("noble", "join-battle", actor.getName()
                        + " will defend " + battle.defender.getName() + " at " + battle.zoneId);
            }
        }
    }

    private static void dispatchSingle(NobleHouse actor, NobleArmy pool,
                                        PendingBattle b,
                                        NobleArmyManager armyManager,
                                        List<String> log) {
        armyManager.moveArmy(pool, b.zoneId);
        NobleArmy finalArmy = armyManager.getFirstIdleArmyInZone(actor.getId(), b.zoneId);
        if (finalArmy == null) {
            Debug.log("noble", "order-issued", actor.getName()
                    + " ERROR: no surviving army after move to " + b.zoneId + " for JOIN_BATTLE");
            return;
        }
        finalArmy.issueOrder(NobleArmy.OrderType.JOIN_BATTLE, b.zoneId);
        Debug.log("noble", "order-issued", actor.getName()
                + " issued JOIN_BATTLE order to army " + finalArmy.getId() + " at " + b.zoneId);
        log.add(actor.getName() + " marches to join battle at " + b.zoneId + ".");
    }

    private static void dispatchDouble(NobleHouse actor, NobleArmy pool,
                                        PendingBattle b0, PendingBattle b1,
                                        NobleArmyManager armyManager,
                                        List<String> log) {
        int half = pool.getSize() / 2;
        if (half < 1) {
            // Not enough to split — send whole pool to first battle
            dispatchSingle(actor, pool, b0, armyManager, log);
            return;
        }

        NobleArmy split = armyManager.splitArmy(pool, half);

        armyManager.moveArmy(pool, b0.zoneId);
        NobleArmy finalArmy0 = armyManager.getFirstIdleArmyInZone(actor.getId(), b0.zoneId);
        if (finalArmy0 == null) {
            Debug.log("noble", "order-issued", actor.getName()
                    + " ERROR: no surviving army after move to " + b0.zoneId + " for JOIN_BATTLE");
        } else {
            finalArmy0.issueOrder(NobleArmy.OrderType.JOIN_BATTLE, b0.zoneId);
            Debug.log("noble", "order-issued", actor.getName()
                    + " issued JOIN_BATTLE order to army "
                    + finalArmy0.getId() + " at " + b0.zoneId);
        }
        log.add(actor.getName() + " marches to join battle at " + b0.zoneId + ".");

        if (split != null) {
            armyManager.moveArmy(split, b1.zoneId);
            NobleArmy finalArmy1 = armyManager.getFirstIdleArmyInZone(actor.getId(), b1.zoneId);
            if (finalArmy1 == null) {
                Debug.log("noble", "order-issued", actor.getName()
                        + " ERROR: no surviving split army after move to "
                        + b1.zoneId + " for JOIN_BATTLE");
            } else {
                finalArmy1.issueOrder(NobleArmy.OrderType.JOIN_BATTLE, b1.zoneId);
                Debug.log("noble", "order-issued", actor.getName()
                        + " issued JOIN_BATTLE order to split army "
                        + finalArmy1.getId() + " at " + b1.zoneId);
            }
            log.add(actor.getName() + " sends split force to join battle at " + b1.zoneId + ".");
        }
    }

    private static NobleHouse findZoneOwner(String zoneId, List<NobleHouse> allHouses) {
        for (NobleHouse h : allHouses) {
            if (h.getZoneIds().contains(zoneId)) return h;
        }
        return null;
    }
}