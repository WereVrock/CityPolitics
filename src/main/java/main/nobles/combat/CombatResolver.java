package main.nobles.combat;

import main.parameters.GameParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Placeholder combat resolver.
 * Supports both single attacker and combined coalition force.
 */
public class CombatResolver {

    private static final Random RNG = new Random();

    /**
     * Single attacker vs defender.
     */
    public static CombatResult resolve(ArmyForce attacker, ArmyForce defender) {
        List<String> log = new ArrayList<>();

        double defenseMultiplier = 1.0 - (defender.getDefense() / 100.0)
            * GameParameters.COMBAT_DEFENSE_REDUCTION;

        int attackerPower  = attacker.getArmySize();
        int defenderPower  = (int)(defender.getArmySize() * defenseMultiplier);

        int defenderLosses = resolveCasualties(attackerPower, defender.getArmySize());
        int attackerLosses = resolveCasualties(defenderPower, attacker.getArmySize());

        log.add(attacker.getHouseId() + " attacks " + defender.getHouseId()
            + " (" + attacker.getArmySize() + " vs " + defender.getArmySize() + ")");

        attacker.applyLosses(attackerLosses);
        defender.applyLosses(defenderLosses);

        log.add("Attacker losses: " + attackerLosses
            + "  Defender losses: " + defenderLosses);

        return resolveWinner(attacker.getHouseId(), defender.getHouseId(),
            attacker, defender, attackerLosses, defenderLosses, log);
    }

    /**
     * Coalition attack: multiple attackers combined vs one defender.
     * Returns a single CombatResult. Winner is "coalition" or defender id.
     * Losses are distributed proportionally across coalition members.
     */
    public static CombatResult resolveCoalition(List<ArmyForce> attackers,
                                                 ArmyForce defender,
                                                 String coordinatorId) {
        List<String> log = new ArrayList<>();

        int totalAttackerPower = 0;
        for (ArmyForce a : attackers) totalAttackerPower += a.getArmySize();

        double defenseMultiplier = 1.0 - (defender.getDefense() / 100.0)
            * GameParameters.COMBAT_DEFENSE_REDUCTION;
        int defenderPower = (int)(defender.getArmySize() * defenseMultiplier);

        int defenderLosses  = resolveCasualties(totalAttackerPower, defender.getArmySize());
        int totalAtkLosses  = resolveCasualties(defenderPower, totalAttackerPower);

        StringBuilder members = new StringBuilder();
        for (int i = 0; i < attackers.size(); i++) {
            if (i > 0) members.append(", ");
            members.append(attackers.get(i).getHouseId());
        }
        log.add("Coalition [" + members + "] attacks " + defender.getHouseId()
            + " (" + totalAttackerPower + " vs " + defender.getArmySize() + ")");

        // Distribute losses proportionally
        for (ArmyForce a : attackers) {
            double fraction = (double) a.getArmySize() / Math.max(1, totalAttackerPower);
            int losses = (int) Math.ceil(totalAtkLosses * fraction);
            a.applyLosses(losses);
        }
        defender.applyLosses(defenderLosses);

        log.add("Coalition losses: " + totalAtkLosses
            + "  Defender losses: " + defenderLosses);

        boolean coalitionWins = totalAttackerPower - totalAtkLosses
            > defender.getArmySize() - defenderLosses;

        if (coalitionWins) {
            log.add("Coalition wins the engagement.");
            return new CombatResult(coordinatorId, defender.getHouseId(),
                totalAtkLosses, defenderLosses, log);
        } else {
            log.add(defender.getHouseId() + " repels the coalition.");
            return new CombatResult(defender.getHouseId(), coordinatorId,
                totalAtkLosses, defenderLosses, log);
        }
    }

    private static CombatResult resolveWinner(String attackerId, String defenderId,
                                               ArmyForce attacker, ArmyForce defender,
                                               int attackerLosses, int defenderLosses,
                                               List<String> log) {
        String winnerId;
        String loserId;

        if (attacker.getArmySize() > defender.getArmySize()) {
            winnerId = attackerId;
            loserId  = defenderId;
            log.add(winnerId + " wins the engagement.");
        } else if (defender.getArmySize() > attacker.getArmySize()) {
            winnerId = defenderId;
            loserId  = attackerId;
            log.add(winnerId + " repels the attack.");
        } else {
            winnerId = null;
            loserId  = null;
            log.add("The engagement ends in a draw.");
        }

        return new CombatResult(winnerId, loserId,
            attackerLosses, defenderLosses, log);
    }

    private static int resolveCasualties(int power, int targetSize) {
        if (power <= 0 || targetSize <= 0) return 0;
        double ratio    = Math.min(2.0, (double) power / Math.max(1, targetSize));
        double baseRate = GameParameters.COMBAT_BASE_CASUALTY_RATE;
        double variance = (RNG.nextDouble() - 0.5) * GameParameters.COMBAT_CASUALTY_VARIANCE;
        double rate     = Math.max(0.05, Math.min(0.6, baseRate * ratio + variance));
        return (int) Math.ceil(targetSize * rate);
    }

    /**
     * Resolve a battle with multiple attacker armies and multiple defender armies.
     * Returns a CombatResult where winnerId is the side that won.
     */
    public static CombatResult resolveMultiSideBattle(
            List<ArmyForce> attackers,
            List<ArmyForce> defenders,
            String attackerCoordinatorId,
            String defenderHouseId,
            int defenderFortification
    ) {
        List<String> log = new ArrayList<>();

        int totalAttackerPower = 0;
        for (ArmyForce a : attackers) totalAttackerPower += a.getArmySize();

        double defenseMultiplier = 1.0 - (defenderFortification / 100.0)
                * GameParameters.COMBAT_DEFENSE_REDUCTION;
        int totalDefenderPower = 0;
        for (ArmyForce d : defenders) {
            int effective = (int)(d.getArmySize() * defenseMultiplier);
            totalDefenderPower += effective;
        }

        int defenderLossesTotal = resolveCasualties(totalAttackerPower, totalDefenderPower);
        int attackerLossesTotal = resolveCasualties(totalDefenderPower, totalAttackerPower);

        log.add("Multi‑side battle: Attackers power " + totalAttackerPower
                + " vs Defenders power " + totalDefenderPower);

        for (ArmyForce a : attackers) {
            double fraction = (double) a.getArmySize() / Math.max(1, totalAttackerPower);
            int losses = (int) Math.ceil(attackerLossesTotal * fraction);
            a.applyLosses(losses);
        }
        for (ArmyForce d : defenders) {
            double fraction = (double) d.getArmySize() / Math.max(1, totalDefenderPower);
            int losses = (int) Math.ceil(defenderLossesTotal * fraction);
            d.applyLosses(losses);
        }

        boolean attackersWin = (totalAttackerPower - attackerLossesTotal)
                > (totalDefenderPower - defenderLossesTotal);
        String winnerId = attackersWin ? attackerCoordinatorId : defenderHouseId;
        String loserId  = attackersWin ? defenderHouseId : attackerCoordinatorId;
        log.add(attackersWin ? "Attacker coalition wins." : "Defender coalition wins.");

        return new CombatResult(winnerId, loserId,
                attackerLossesTotal, defenderLossesTotal, log);
    }

   
}