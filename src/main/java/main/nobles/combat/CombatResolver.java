package main.nobles.combat;

import main.parameters.GameParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Placeholder combat resolver.
 * Armies deal random casualties scaled by army power.
 * Defense reduces attacker effectiveness.
 * Replace internals later without touching callers.
 */
public class CombatResolver {

    private static final Random RNG = new Random();

    /**
     * Resolve a single engagement between attacker and defender.
     * Mutates armySize on both forces.
     * @param attacker
     * @param defender
     * @return 
     */
    public static CombatResult resolve(ArmyForce attacker, ArmyForce defender) {
        List<String> log = new ArrayList<>();

        // Defense reduces attacker effectiveness: 0 defense = full power, 100 defense = half power
        double defenseMultiplier = 1.0 - (defender.getDefense() / 100.0)
                                       * GameParameters.COMBAT_DEFENSE_REDUCTION;

        // Each side deals casualties proportional to army size + randomness
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

        String winnerId;
        String loserId;

        if (attacker.getArmySize() > defender.getArmySize()) {
            winnerId = attacker.getHouseId();
            loserId  = defender.getHouseId();
            log.add(winnerId + " wins the engagement.");
        } else if (defender.getArmySize() > attacker.getArmySize()) {
            winnerId = defender.getHouseId();
            loserId  = attacker.getHouseId();
            log.add(winnerId + " repels the attack.");
        } else {
            winnerId = null;
            loserId  = null;
            log.add("The engagement ends in a draw.");
        }

        return new CombatResult(winnerId, loserId, attackerLosses, defenderLosses, log);
    }

    /**
     * How many casualties does a force of [power] inflict on a force of [targetSize]?
     * Placeholder: random 10–30% of target size, scaled by power ratio.
     */
    private static int resolveCasualties(int power, int targetSize) {
        if (power <= 0 || targetSize <= 0) return 0;
        double ratio    = Math.min(2.0, (double) power / Math.max(1, targetSize));
        double baseRate = GameParameters.COMBAT_BASE_CASUALTY_RATE;
        double variance = (RNG.nextDouble() - 0.5) * GameParameters.COMBAT_CASUALTY_VARIANCE;
        double rate     = Math.max(0.05, Math.min(0.6, baseRate * ratio + variance));
        return (int) Math.ceil(targetSize * rate);
    }
}