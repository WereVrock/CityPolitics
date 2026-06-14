package City.main.barbarians;

import City.main.army.Army;
import City.main.nobles.NobleHouse;
import City.main.nobles.NobleArmy;
import City.main.nobles.NobleArmyManager;
import City.main.combat.ArmyForce;
import City.main.combat.CombatResolver;
import City.main.combat.CombatResult;
import City.main.parameters.BarbarianParams;
 

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves combat between barbarian armies and defenders.
 * Defenders get a +30% bonus when barbarians are the attacker.
 * When player initiates, no bonus applies.
 */
public class BarbCombatHandler {

    private BarbCombatHandler() {}

    // ─── Barbarian attacks defender (noble or player) ─────────────────────────

    /**
     * Barbarian army attacks a zone.
     * Gathers defending noble armies + garrison, applies defender bonus.
     * Returns result; callers apply zone ownership changes.
     */
    public static BarbCombatResult barbAttacksZone(
            BarbArmy attacker,
            NobleHouse defender,
            String zoneId,
            NobleArmyManager armyManager,
            List<String> log) {

        int garrisonSize = defender.getGarrisonFor(zoneId);
        int fort         = defender.getFortificationFor(zoneId);
        int defMilitary  = defender.getActiveCharacter() != null
                         ? defender.getActiveCharacter().getMilitary() : 0;

        List<NobleArmy> defArmies = armyManager.getArmiesInZone(zoneId, defender.getId());
        int defArmySize = 0;
        for (NobleArmy a : defArmies) defArmySize += a.getSize();

        int totalDefSize = garrisonSize + defArmySize;

        log.add("☠ Barbarians attack " + zoneId + " (" + attacker.getSize()
                + " vs " + totalDefSize + " defenders)");

        // Defender bonus: treat their effective power as 30% higher
        double defBonus = 1.0 + BarbarianParams.BARB_DEFENDER_BONUS;
        int boostedDefSize = (int)(totalDefSize * defBonus);

        ArmyForce atkForce = new ArmyForce("barbarians", attacker.getSize(), 0, 0);
        ArmyForce defForce = new ArmyForce(defender.getId(), boostedDefSize, fort, defMilitary);

        CombatResult result = CombatResolver.resolve(atkForce, defForce);
        log.addAll(result.getLog());

        // Scale losses back to real troop count
        int defLossRaw = (int)(result.getDefenderLosses() / defBonus);
        attacker.applyLosses(result.getAttackerLosses());

        // Distribute real losses across garrison then armies
        int remaining = defLossRaw;
        int garrisonLost = Math.min(remaining, garrisonSize);
        defender.damageGarrison(zoneId, garrisonLost);
        remaining -= garrisonLost;
        for (NobleArmy a : new ArrayList<>(defArmies)) {
            if (remaining <= 0) break;
            int lost = Math.min(remaining, a.getSize());
            a.setSize(a.getSize() - lost);
            remaining -= lost;
        }

        boolean barbWon = "barbarians".equals(result.getWinnerId());
        log.add(barbWon ? "☠ Barbarians overrun " + zoneId + "!"
                        : "Defenders repel the barbarian assault on " + zoneId + ".");

        return new BarbCombatResult(barbWon, result.getAttackerLosses(), defLossRaw);
    }

    /**
     * Barbarian army attacks a zone with only player armies defending.
     */
    public static BarbCombatResult barbAttacksPlayerZone(
            BarbArmy attacker,
            List<Army> playerArmies,
            String zoneId,
            List<String> log) {

        int totalPlayerSize = 0;
        for (Army a : playerArmies) totalPlayerSize += a.getSize();

        log.add("☠ Barbarians attack " + zoneId + " (" + attacker.getSize()
                + " vs " + totalPlayerSize + " player defenders)");

        double defBonus      = 1.0 + BarbarianParams.BARB_DEFENDER_BONUS;
        int    boostedSize   = (int)(totalPlayerSize * defBonus);

        ArmyForce atkForce = new ArmyForce("barbarians", attacker.getSize(), 0, 0);
        ArmyForce defForce = new ArmyForce("player",     boostedSize,        0, 0);

        CombatResult result = CombatResolver.resolve(atkForce, defForce);
        log.addAll(result.getLog());

        int defLossRaw = (int)(result.getDefenderLosses() / defBonus);
        attacker.applyLosses(result.getAttackerLosses());

        // Distribute losses across player armies proportionally
        int remaining = defLossRaw;
        for (Army a : playerArmies) {
            if (remaining <= 0) break;
            int lost = Math.min(remaining, a.getSize());
            a.applyLosses(lost);
            remaining -= lost;
        }

        boolean barbWon = "barbarians".equals(result.getWinnerId());
        log.add(barbWon ? "☠ Barbarians seize " + zoneId + "!"
                        : "Player armies repel barbarians at " + zoneId + ".");

        return new BarbCombatResult(barbWon, result.getAttackerLosses(), defLossRaw);
    }

    /**
     * Player initiates attack on a barbarian army (no defender bonus).
     */
    public static BarbCombatResult playerAttacksBarbarian(
            List<Army> playerArmies,
            BarbArmy target,
            int nobleGarrison,
            List<String> log) {

        int playerSize = 0;
        for (Army a : playerArmies) playerSize += a.getSize();
        int totalAtk = playerSize;

        log.add("⚔ Player attacks barbarians at " + target.getZoneId()
                + " (" + totalAtk + " vs " + target.getSize() + ")");

        ArmyForce atkForce = new ArmyForce("player",      totalAtk,       0, 0);
        ArmyForce defForce = new ArmyForce("barbarians",  target.getSize(), 0, 0);

        CombatResult result = CombatResolver.resolve(atkForce, defForce);
        log.addAll(result.getLog());

        target.applyLosses(result.getDefenderLosses());

        int remaining = result.getAttackerLosses();
        for (Army a : playerArmies) {
            if (remaining <= 0) break;
            int lost = Math.min(remaining, a.getSize());
            a.applyLosses(lost);
            remaining -= lost;
        }

        boolean playerWon = "player".equals(result.getWinnerId());
        log.add(playerWon ? "Player forces defeat the barbarians!"
                          : "Barbarians repel the player's assault.");

        return new BarbCombatResult(playerWon, result.getAttackerLosses(), result.getDefenderLosses());
    }

    /**
     * Noble initiates attack on a barbarian garrison (recapture).
     * No defender bonus since noble is the attacker.
     */
    public static BarbCombatResult nobleAttacksBarbGarrison(
            NobleHouse attacker,
            BarbArmy garrison,
            String zoneId,
            NobleArmyManager armyManager,
            List<String> log) {

        List<NobleArmy> atkArmies = armyManager.getArmiesInZone(zoneId, attacker.getId());
        int atkSize = 0;
        for (NobleArmy a : atkArmies) atkSize += a.getSize();
        int garSize = attacker.getGarrisonFor(zoneId);
        int total   = atkSize + garSize;

        log.add(attacker.getName() + " attempts to recapture " + zoneId
                + " from barbarian garrison (" + total + " vs " + garrison.getSize() + ")");

        ArmyForce atkForce = new ArmyForce(attacker.getId(), total,          0, 0);
        ArmyForce defForce = new ArmyForce("barbarians",     garrison.getSize(), 0, 0);

        CombatResult result = CombatResolver.resolve(atkForce, defForce);
        log.addAll(result.getLog());

        garrison.applyLosses(result.getDefenderLosses());

        boolean nobleWon = attacker.getId().equals(result.getWinnerId());
        log.add(nobleWon ? attacker.getName() + " recaptures " + zoneId + "."
                         : "Barbarian garrison holds " + zoneId + ".");

        // Apply losses to noble armies proportionally
        int remaining = result.getAttackerLosses();
        for (NobleArmy a : new ArrayList<>(atkArmies)) {
            if (remaining <= 0) break;
            int lost = Math.min(remaining, a.getSize());
            a.setSize(a.getSize() - lost);
            remaining -= lost;
        }

        return new BarbCombatResult(nobleWon, result.getAttackerLosses(), result.getDefenderLosses());
    }

    // ─── Simple result carrier ────────────────────────────────────────────────

    public static class BarbCombatResult {
        public final boolean attackerWon;
        public final int     attackerLosses;
        public final int     defenderLosses;

        public BarbCombatResult(boolean attackerWon, int attackerLosses, int defenderLosses) {
            this.attackerWon    = attackerWon;
            this.attackerLosses = attackerLosses;
            this.defenderLosses = defenderLosses;
        }
    }
}