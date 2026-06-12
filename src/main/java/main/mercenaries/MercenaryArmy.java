package main.mercenaries;

/**
 * A single mercenary company.
 */
public class MercenaryArmy {

    private static int nextId = 1;

    public static void resetIdCounter() { nextId = 1; }

    private final String id;
    private final String displayName;
    private       int    size;
    private       String zoneId;

    public MercenaryArmy(String displayName, int size, String zoneId) {
        this.id          = "merc_" + (nextId++);
        this.displayName = displayName;
        this.size        = size;
        this.zoneId      = zoneId;
    }

    public String getId()          { return id; }
    public String getDisplayName() { return displayName; }
    public int    getSize()        { return size; }
    public void   setSize(int v)   { this.size = Math.max(0, v); }
    public void   applyLosses(int v){ this.size = Math.max(0, size - v); }
    public boolean isAlive()       { return size > 0; }
    public String getZoneId()      { return zoneId; }
    public void   setZoneId(String z) { this.zoneId = z; }

    @Override
    public String toString() {
        return "Merc[" + id + "] " + displayName + " size=" + size + " at=" + zoneId;
    }
}