package City.main.nobles;

/**
 * Tracks player prestige for the noble council system.
 */
public class PlayerPrestige {

    private int prestige = 0;

    public int  getPrestige()       { return prestige; }
    public void addPrestige(int v)  { prestige = Math.max(0, prestige + v); }
    public void setPrestige(int v)  { prestige = Math.max(0, v); }
}