package main.nobles.council;

import main.nobles.NobleHouse;

/**
 * A single voter in the noble council with their impression (voting weight) and current stance.
 */
public class CouncilVoter {

    public enum VoterType { PLAYER, ORACLE, PRESTIGIOUS_NOBLE, MINOR_NOBLE }
    public enum Stance     { YES, NO, UNDECIDED }

    private final String    id;
    private final String    displayName;
    private final VoterType type;
    private final NobleHouse house; // null for player/oracle
    private int    impression;
    private Stance stance;
    private boolean dealt;

    public CouncilVoter(String id, String displayName, VoterType type,
                        NobleHouse house, int impression) {
        this.id          = id;
        this.displayName = displayName;
        this.type        = type;
        this.house       = house;
        this.impression  = impression;
        this.stance      = Stance.UNDECIDED;
        this.dealt       = false;
    }

    public String      getId()          { return id; }
    public String      getDisplayName() { return displayName; }
    public VoterType   getType()        { return type; }
    public NobleHouse  getHouse()       { return house; }
    public int         getImpression()  { return impression; }
    public void        setImpression(int v) { this.impression = Math.max(0, v); }
    public Stance      getStance()      { return stance; }
    public void        setStance(Stance s) { this.stance = s; }
    public boolean     isDealt()        { return dealt; }
    public void        setDealt(boolean v) { this.dealt = v; }

    /** Impression this voter contributes toward YES. */
    public int getYesImpression() {
        return stance == Stance.YES ? impression : 0;
    }

    /** Impression this voter contributes toward NO. */
    public int getNoImpression() {
        return stance == Stance.NO ? impression : 0;
    }
}