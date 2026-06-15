package City.main.nobles;

/**
 * Tracks player prestige for the noble council system.
 */
public class PlayerPrestige {

    private int prestige = 0;
    private int trust    = City.main.parameters.StartingParams.PLAYER_TRUST_MIN;

    public int  getPrestige()       { return prestige; }
    public void addPrestige(int v)  { prestige = Math.max(0, prestige + v); }
    public void setPrestige(int v)  { prestige = Math.max(0, v); }

    public int  getTrust()          { return trust; }
    public void addTrust(int v)     { trust = Math.max(City.main.parameters.StartingParams.PLAYER_TRUST_MIN,
                                                  Math.min(City.main.parameters.StartingParams.PLAYER_TRUST_MAX, trust + v)); }
    public void setTrust(int v)     { trust = Math.max(City.main.parameters.StartingParams.PLAYER_TRUST_MIN,
                                                  Math.min(City.main.parameters.StartingParams.PLAYER_TRUST_MAX, v)); }
    public int  getTrustCouncilBonus() { return (trust - 5) * 100; }
}