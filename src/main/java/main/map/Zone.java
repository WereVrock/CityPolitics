// Zone.java
package main.map;

import java.util.List;

/**
 * Immutable data for a single map zone.
 * Mutable state (damage, supply) is kept separate in ZoneState.
 */
public class Zone {

    public enum SettlementType { CAPITAL, TOWN, VILLAGE }

    private final String         id;
    private final String         displayName;
    private final SettlementType settlement;
    private final int[]          polyX;
    private final int[]          polyY;
    private final int            labelX;
    private final int            labelY;
    private final int            goldProduction;
    private final int            foodProduction;
    private final int            zonePops;
    private final List<String>   adjacentIds;

    public Zone(String id, String displayName, SettlementType settlement,
                int[] polyX, int[] polyY, int labelX, int labelY,
                int goldProduction, int foodProduction, int zonePops,
                List<String> adjacentIds) {
        this.id             = id;
        this.displayName    = displayName;
        this.settlement     = settlement;
        this.polyX          = polyX;
        this.polyY          = polyY;
        this.labelX         = labelX;
        this.labelY         = labelY;
        this.goldProduction = goldProduction;
        this.foodProduction = foodProduction;
        this.zonePops       = zonePops;
        this.adjacentIds    = adjacentIds;
    }

    public String         getId()             { return id; }
    public String         getDisplayName()    { return displayName; }
    public SettlementType getSettlement()     { return settlement; }
    public int[]          getPolyX()          { return polyX; }
    public int[]          getPolyY()          { return polyY; }
    public int            getLabelX()         { return labelX; }
    public int            getLabelY()         { return labelY; }
    public int            getGoldProduction() { return goldProduction; }
    public int            getFoodProduction() { return foodProduction; }
    public int            getZonePops()       { return zonePops; }
    public List<String>   getAdjacentIds()    { return adjacentIds; }
}