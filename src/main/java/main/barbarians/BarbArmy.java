package main.barbarians;

/**
 * A single barbarian army on the map.
 * Can be a Warboss, Raider, or Ravager.
 */
public class BarbArmy {

    public enum Type { WARBOSS, RAIDER, RAVAGER }

    private static int nextId = 1;

    public static void resetIdCounter() { nextId = 1; }

  

    private final String id;
    private final Type   type;
    private       int    size;
    private       String zoneId;
    private       String displayName;

    // Movement
    private String nextZoneId;        // pre-calculated next move (visible to player for warboss)
    private int    moveCooldown;      // warboss skips every other turn
    private final java.util.Set<String> visitedZones = new java.util.LinkedHashSet<>();

    // Garrison remnant — left behind after conquest (not a mobile army)
    private boolean isGarrison = false;

    // Pay-off state
    private boolean paidOff    = false; // stood down this turn, no effect
    private boolean dismissed  = false; // fully disbanded

    public BarbArmy(Type type, int size, String zoneId) {
        this.id       = "barb_" + (nextId++);
        this.type     = type;
        this.size     = size;
        this.zoneId   = zoneId;
        this.visitedZones.add(zoneId);
        this.moveCooldown = (type == Type.WARBOSS) ? 1 : 0;
    }

    // ─── Identity ────────────────────────────────────────────────────────────

    public String getId()          { return id; }
    public Type   getType()        { return type; }
    public String getDisplayName() { return displayName != null ? displayName : type.name(); }
    public void   setDisplayName(String n) { this.displayName = n; }
    public boolean isWarboss() { return type == Type.WARBOSS; }
    public boolean isRaider()  { return type == Type.RAIDER; }
    public boolean isRavager() { return type == Type.RAVAGER; }

    // ─── Size ────────────────────────────────────────────────────────────────

    public int  getSize()              { return size; }
    public void setSize(int size)      { this.size = Math.max(0, size); }
    public void applyLosses(int lost)  { this.size = Math.max(0, size - lost); }
    public boolean isAlive()           { return size > 0 && !dismissed; }

    // ─── Zone ────────────────────────────────────────────────────────────────

    public String getZoneId()            { return zoneId; }
    public void   setZoneId(String z)    { this.zoneId = z; if (z != null) visitedZones.add(z); }
    public java.util.Set<String> getVisitedZones() { return java.util.Collections.unmodifiableSet(visitedZones); }
    public boolean hasVisited(String z)  { return visitedZones.contains(z); }

    // ─── Movement ────────────────────────────────────────────────────────────

    public String getNextZoneId()              { return nextZoneId; }
    public void   setNextZoneId(String z)      { this.nextZoneId = z; }

    /** Returns true if warboss should move this turn. */
    public boolean canMoveThisTurn() {
        if (type != Type.WARBOSS) return true;
        if (moveCooldown > 0) { moveCooldown--; return false; }
        moveCooldown = 1;
        return true;
    }

    // ─── Garrison ────────────────────────────────────────────────────────────

    public boolean isGarrison()        { return isGarrison; }
    public void    makeGarrison()      { this.isGarrison = true; }

    // ─── Pay-off ─────────────────────────────────────────────────────────────

    public boolean isPaidOff()         { return paidOff; }
    public void    setPaidOff(boolean v){ this.paidOff = v; }
    public boolean isDismissed()       { return dismissed; }
    public void    dismiss()           { this.dismissed = true; this.size = 0; }

    /** Gold cost for cheap pay-off (stand down one turn). */
    public int cheapPayOffGoldCost() {
        return size * main.parameters.GameParameters.BARB_PAYOFF_GOLD_PER_MAN;
    }

    /** Food cost for cheap pay-off. */
    public int cheapPayOffFoodCost() {
        return size * main.parameters.GameParameters.BARB_PAYOFF_FOOD_PER_MAN;
    }

    /** Gold cost for player full dismissal. Warboss has a multiplier. */
    public int fullDismissCost() {
        double mult = isWarboss()
                ? main.parameters.GameParameters.BARB_WARBOSS_DISMISS_MULTIPLIER
                : 1.0;
        return (int)(size * main.parameters.GameParameters.BARB_DISMISS_GOLD_PER_MAN * mult);
    }

    @Override
    public String toString() {
        return type.name() + "[" + id + "] size=" + size + " at=" + zoneId;
    }
}