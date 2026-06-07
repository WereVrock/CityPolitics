// ===== CommanderDeathResolver.java =====
package main.army;

import main.parameters.GameParameters;

import java.util.Random;

/**
 * Decides whether a commander dies after a battle based on casualty rate.
 *
 * Thresholds (from GameParameters):
 *   < 30% casualties     → no death roll
 *   30–79% casualties    → deathChance = (casualtyPct - 30)%
 *   >= 80% casualties    → deathChance = casualtyPct%
 *   If player won        → multiply final chance by WIN_MODIFIER (0.7)
 */
public class CommanderDeathResolver {

    private static final Random RNG = new Random();

    private CommanderDeathResolver() {}

    /**
     * @param commander      the commander to check
     * @param soldiersBefore soldiers before the battle
     * @param soldiersLost   soldiers lost during the battle
     * @param playerWon      whether the player's side won
     * @return true if the commander dies
     */
    public static boolean resolve(Commander commander,
                                  int soldiersBefore,
                                  int soldiersLost,
                                  boolean playerWon) {
        if (!commander.isAlive() || soldiersBefore <= 0) return false;

        double casualtyPct = (double) soldiersLost / soldiersBefore * 100.0;
        double lower = GameParameters.COMMANDER_DEATH_CASUALTY_LOWER * 100; // 30
        double upper = GameParameters.COMMANDER_DEATH_CASUALTY_UPPER * 100; // 80

        double deathChancePct;
        if      (casualtyPct < lower) return false;
        else if (casualtyPct < upper) deathChancePct = casualtyPct - lower;
        else                          deathChancePct = casualtyPct;

        if (playerWon) deathChancePct *= GameParameters.COMMANDER_DEATH_WIN_MODIFIER;

        boolean dies = RNG.nextDouble() * 100.0 < deathChancePct;
        if (dies) commander.kill();
        return dies;
    }
}