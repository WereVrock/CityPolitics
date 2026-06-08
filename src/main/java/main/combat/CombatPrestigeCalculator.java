// ===== CombatPrestigeCalculator.java =====
package main.combat;

import main.army.commander.Commander;
import main.parameters.GameParameters;

import java.util.List;

/**
 * Calculates a single "battle score" from the engagement, then derives
 * prestige and XP from it.
 *
 * Battle Score formula
 * --------------------
 *   ratio        = attackerSize / max(1, defenderSize)   (capped 0.5 – 2.0)
 *   difficulty   = 1 / ratio   (fighting larger enemy = harder)
 *   bonusFactor  = 1 + (totalBonusPercent / 100) applied to enemy side
 *   rawScore     = sqrt(smallerSide) * difficulty * bonusFactor
 *
 * If won → rawScore *= WIN_MULTIPLIER (1.3)
 *
 * Prestige for party  = rawScore * PRESTIGE_COEFFICIENT
 * XP for a commander  = rawScore * XP_COEFFICIENT * (commander's force / total allied force)
 */
public class CombatPrestigeCalculator {

    private CombatPrestigeCalculator() {}

    /**
     * @param attackerSize      soldiers on attacker's side (player + allies)
     * @param defenderSize      soldiers on defender's side
     * @param enemyBonusPercent total percentage bonuses the enemy had (e.g. 30 for desolate)
     * @param playerWon         whether the player's side won
     * @return raw battle score (used to multiply prestige/XP coefficients)
     */
    public static double computeRawScore(int attackerSize,
                                         int defenderSize,
                                         int enemyBonusPercent,
                                         boolean playerWon) {
        double ratio       = (double) attackerSize / Math.max(1, defenderSize);
        ratio              = Math.max(0.5, Math.min(2.0, ratio));
        double difficulty  = 1.0 / ratio;
        double bonusFactor = 1.0 + (enemyBonusPercent / 100.0);
        int    smallerSide = Math.min(attackerSize, defenderSize);
        double rawScore    = Math.sqrt(smallerSide) * difficulty * bonusFactor;
        if (playerWon) rawScore *= GameParameters.COMBAT_WIN_PRESTIGE_MULTIPLIER;
        return rawScore;
    }

    /**
     * Prestige gained for the affiliated party.
     */
    public static int computePrestige(double rawScore) {
        return (int) Math.ceil(rawScore * GameParameters.PRESTIGE_COEFFICIENT);
    }

    /**
     * Distributes XP among commanders proportional to their force sizes.
     * Commanders with 0 force still get a minimum participation share.
     *
     * @param commanders      alive commanders that participated
     * @param forceSizes      parallel array of each commander's soldier count
     * @param rawScore        from computeRawScore
     */
    public static int[] distributeXp(List<Commander> commanders,
                                     int[] forceSizes,
                                     double rawScore) {
        int   n         = commanders.size();
        int[] xpGrants  = new int[n];
        if (n == 0) return xpGrants;

        double totalXpPool = rawScore * GameParameters.XP_COEFFICIENT;
        int    totalForce  = 0;
        for (int s : forceSizes) totalForce += s;

        if (totalForce == 0) {
            // Edge case: equal split
            int share = (int) Math.ceil(totalXpPool / n);
            for (int i = 0; i < n; i++) xpGrants[i] = share;
        } else {
            for (int i = 0; i < n; i++) {
                double fraction = (double) forceSizes[i] / totalForce;
                xpGrants[i] = (int) Math.ceil(totalXpPool * fraction);
            }
        }
        return xpGrants;
    }
}