// ===== Commander.java (full replacement) =====
package main.army;

import main.parameters.GameParameters;
import main.politics.PolitcalView;

/**
 * A named commander attached to a player army.
 * Skill 0–3. Gains XP from battles; levels up when threshold reached.
 * Has a gold upkeep cost derived from current skill level.
 */
public class Commander {

    private final String       name;
    private final String       race;
    private final PolitcalView affiliation;

    private int     commandingSkill;
    private int     xp;
    private boolean alive;

    public Commander(String name, String race, PolitcalView affiliation, int commandingSkill) {
        this.name            = name;
        this.race            = race;
        this.affiliation     = affiliation;
        this.commandingSkill = Math.max(0, Math.min(3, commandingSkill));
        this.xp              = 0;
        this.alive           = true;
    }

    // --- Accessors ---
    public String       getName()            { return name; }
    public String       getRace()            { return race; }
    public PolitcalView getAffiliation()     { return affiliation; }
    public int          getCommandingSkill() { return commandingSkill; }
    public int          getXp()              { return xp; }
    public boolean      isAlive()            { return alive; }

    public double getUpkeepCost() {
        return GameParameters.COMMANDER_UPKEEP_BY_SKILL[commandingSkill];
    }

    // --- XP & Levelling ---

    /**
     * Adds XP and triggers level-ups.
     * @return true if the commander levelled up at least once
     */
    public boolean addXp(int amount) {
        if (!alive) return false;
        xp += amount;
        return checkLevelUp();
    }

    private boolean checkLevelUp() {
        int[] thresholds = GameParameters.COMMANDER_XP_THRESHOLDS;
        if (commandingSkill >= thresholds.length) return false; // already max
        boolean levelled = false;
        while (commandingSkill < thresholds.length && xp >= thresholds[commandingSkill]) {
            xp -= thresholds[commandingSkill];
            commandingSkill++;
            levelled = true;
        }
        return levelled;
    }

    /**
     * XP needed to reach next level. Returns -1 if at max level.
     */
    public int xpToNextLevel() {
        int[] thresholds = GameParameters.COMMANDER_XP_THRESHOLDS;
        if (commandingSkill >= thresholds.length) return -1;
        return thresholds[commandingSkill] - xp;
    }

    // --- Death ---
    public void kill() { this.alive = false; }

    // --- Skill roll for recruitment pool ---
    public static int rollSkill() {
        int roll = (int)(Math.random() * 100);
        if (roll < GameParameters.COMMANDER_SKILL_WEIGHT_0)  return 0;
        if (roll < GameParameters.COMMANDER_SKILL_WEIGHT_1)  return 1;
        if (roll < GameParameters.COMMANDER_SKILL_WEIGHT_2)  return 2;
        return 3;
    }
}