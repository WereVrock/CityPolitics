package main.army;

import debug.Debug;

/**
 * Represents a player-controlled army.
 * Armies in heartland are considered "in city" and appear in the city list.
 * zoneId is never null — heartland is the home zone.
 *
 * Commander requirement:
 *   An army may only be deployed (moved outside heartland) if it has a living commander.
 *   If a commander dies mid-deployment, ArmyManager.getDeployedArmies() forces recall.
 */
public class Army {

    public static final String HEARTLAND_ID = "heartland";

    private final String id;
    private final String displayName;
    private String       zoneId;
    private boolean      dragging;
    private int          size;
    private Commander    commander;
    private int          soldierCount;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public Army(String id, String displayName) {
        this.id           = id;
        this.displayName  = displayName;
        this.zoneId       = HEARTLAND_ID;
        this.dragging     = false;
        this.size         = main.parameters.GameParameters.PLAYER_ARMY_STARTING_SIZE;
        this.soldierCount = main.parameters.GameParameters.PLAYER_ARMY_STARTING_SIZE;
    }

    public Army(String id, String displayName, Commander commander) {
        this(id, displayName);
        this.commander = commander;
    }

    // ─── Identity ─────────────────────────────────────────────────────────────

    public String getId()          { return id; }
    public String getDisplayName() { return displayName; }

    // ─── Location ─────────────────────────────────────────────────────────────

    public String  getZoneId()  { return zoneId; }
    public boolean isInCity()   { return HEARTLAND_ID.equals(zoneId); }
    public boolean isDeployed() { return !isInCity(); }
    public boolean isDragging() { return dragging; }

/**
     * Moves the army to the given zone.
     * Silently refuses and recalls to heartland if the army has no living commander.
     */

/**
     * Moves the army to the given zone.
     * Silently refuses and recalls to heartland if the army has no living commander.
     */
    public void moveTo(String zoneId) {
        if (!hasLivingCommander()) {
            Debug.log("army", "move-blocked",
                    id + " cannot deploy — no living commander. Recalled.");
            recallToCity();
            return;
        }
        this.zoneId   = zoneId;
        this.dragging = false;
        Debug.log("army", "move", id + " → " + zoneId);
    }

/**
     * Returns the army to heartland and cancels any drag state.
     * Called on player loss, commander death, or manual unassign.
     */
    public void recallToCity() {
        this.zoneId   = HEARTLAND_ID;
        this.dragging = false;
        Debug.log("army", "recalled", id + " — " + displayName);
    }

/**
     * Restores the army's zone during save/load without the commander check.
     * Must only be called from SaveManager during game restoration.
     */
    public void restoreZone(String zoneId) {
        this.zoneId   = zoneId;
        this.dragging = false;
    }

public void startDrag()  { this.dragging = true; }
    public void cancelDrag() { this.dragging = false; }

    // ─── Size / alive ─────────────────────────────────────────────────────────

    public int     getSize()              { return size; }
    public void    setSize(int size)      { this.size = Math.max(0, size); }
    public void    applyLosses(int v)     { this.size = Math.max(0, size - v); }
    public boolean isAlive()              { return size > 0; }

    /**
     * Soldier count as tracked by the recruitment/upkeep system.
     * Kept in sync with size.
     */
    public int  getSoldierCount()          { return soldierCount; }
    public void setSoldierCount(int count) { this.soldierCount = Math.max(0, count); }

    /**
     * Adds recruited soldiers, updating both size and soldierCount.
     */
    public void addSoldiers(int amount) {
        int added      = Math.max(0, amount);
        this.size         += added;
        this.soldierCount += added;
        Debug.log("army", "add-soldiers", id + " +" + added
                + " total=" + this.size);
    }

    /**
     * Removes soldiers (desertion or combat loss), updating both size and soldierCount.
     */
    public void removeSoldiers(int amount) {
        int removed       = Math.min(Math.max(0, amount), this.size);
        this.size         -= removed;
        this.soldierCount  = Math.max(0, this.soldierCount - removed);
        Debug.log("army", "remove-soldiers", id + " -" + removed
                + " remaining=" + this.size);
    }

    // ─── Commander ────────────────────────────────────────────────────────────

    public Commander getCommander()            { return commander; }

public void setCommander(Commander c) {
        this.commander = c;
        // If commander removed and army is deployed, force recall immediately
        if (c == null && !isInCity()) {
            recallToCity();
            Debug.log("army", "unassign-recall",
                    id + " recalled — commander unassigned while deployed");
        }
    }

/**
     * Commanding skill used in combat.
     * Defaults to 1 if no commander assigned.
     */
    public int getCommandingSkill() {
        return commander != null ? commander.getCommandingSkill() : 1;
    }

    /**
     * True only when this army has a commander who is still alive.
     * An army without a living commander cannot be deployed.
     */
    public boolean hasLivingCommander() {
        return commander != null && commander.isAlive();
    }
}