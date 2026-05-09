// Army.java
package main.army;

/**
 * Represents a player-controlled army.
 * Armies in heartland are considered "in city" and appear in the city list.
 * zoneId is never null — heartland is the home zone.
 */
public class Army {

    public static final String HEARTLAND_ID = "heartland";

    private final String id;
    private String zoneId;
    private boolean dragging; // true while being dragged, hidden from map

    private final String displayName;

    public Army(String id, String displayName) {
        this.id          = id;
        this.displayName = displayName;
        this.zoneId      = HEARTLAND_ID;
        this.dragging    = false;
    }

    public String  getId()              { return id; }
    public String  getDisplayName()     { return displayName; }
    public String  getZoneId()          { return zoneId; }
    public boolean isInCity()           { return HEARTLAND_ID.equals(zoneId); }
    public boolean isDeployed()         { return !isInCity(); }
    public boolean isDragging()         { return dragging; }

    public void moveTo(String zoneId)   { this.zoneId = zoneId; this.dragging = false; }
    public void recallToCity()          { this.zoneId = HEARTLAND_ID; this.dragging = false; }
    public void startDrag()             { this.dragging = true; }
    public void cancelDrag()            { this.dragging = false; } // stays in heartland
}