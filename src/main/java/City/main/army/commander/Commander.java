package City.main.army.commander;

import City.main.parameters.CommanderParams;
 
import City.main.politics.PoliticalParty;

/**
 * A named commander attached to a player army.
 * Skill 0–3. Gains XP from battles; levels up when threshold reached.
 * Affiliated with a PoliticalParty (not a raw view).
 */
public class Commander {

    private final String         name;
    private final String         race;
    private final PoliticalParty party;

    private int     commandingSkill;
    private int     xp;
    private boolean alive;

    // ─── Constructor ─────────────────────────────────────────────────────────

    public Commander(String name, String race, PoliticalParty party, int commandingSkill) {
        this.name            = name;
        this.race            = race;
        this.party           = party;
        this.commandingSkill = Math.max(0, Math.min(3, commandingSkill));
        this.xp              = 0;
        this.alive           = true;
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    public String         getName()            { return name; }
    public String         getRace()            { return race; }
    public PoliticalParty getParty()           { return party; }
    public int            getCommandingSkill() { return commandingSkill; }
    public int            getXp()              { return xp; }
    public boolean        isAlive()            { return alive; }

    /** Party name for display. Returns "None" if unaffiliated. */
    public String getPartyName() {
        return party != null ? party.getName() : "None";
    }

    public double getUpkeepCost() {
        return CommanderParams.COMMANDER_UPKEEP_BY_SKILL[commandingSkill];
    }

    // ─── XP & Levelling ──────────────────────────────────────────────────────

    /**
     * Adds XP and triggers level-ups.
     * @return true if the commander levelled up at least once.
     */
    public boolean addXp(int amount) {
        if (!alive) return false;
        xp += amount;
        return checkLevelUp();
    }

    private boolean checkLevelUp() {
        int[] thresholds = CommanderParams.COMMANDER_XP_THRESHOLDS;
        if (commandingSkill >= thresholds.length) return false;
        boolean levelled = false;
        while (commandingSkill < thresholds.length && xp >= thresholds[commandingSkill]) {
            xp -= thresholds[commandingSkill];
            commandingSkill++;
            levelled = true;
        }
        return levelled;
    }

    /**
     * XP needed to reach next skill level. Returns -1 if already at max.
     */
    public int xpToNextLevel() {
        int[] thresholds = CommanderParams.COMMANDER_XP_THRESHOLDS;
        if (commandingSkill >= thresholds.length) return -1;
        return thresholds[commandingSkill] - xp;
    }

    // ─── Death ───────────────────────────────────────────────────────────────

    public void kill() { this.alive = false; }

    // ─── Skill roll for recruitment pool ─────────────────────────────────────

    /**
     * Rolls a random skill level for a newly generated commander candidate.
     * Weights: skill 0 = 25%, skill 1 = 55%, skill 2 = 15%, skill 3 = 5%.
     */
    public static int rollSkill() {
        int roll = (int)(Math.random() * 100);
        if (roll < CommanderParams.COMMANDER_SKILL_WEIGHT_0) return 0;
        if (roll < CommanderParams.COMMANDER_SKILL_WEIGHT_1) return 1;
        if (roll < CommanderParams.COMMANDER_SKILL_WEIGHT_2) return 2;
        return 3;
    }
}