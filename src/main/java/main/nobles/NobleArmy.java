// NobleArmy.java
package main.nobles;

/**
 * A raised army belonging to a noble house.
 * Sits in a zone. Pending orders resolve next turn.
 */
public class NobleArmy {

    public enum OrderType { NONE, ATTACK, RAID }

    private final String id;
    private final String houseId;
    private int          size;
    private String       zoneId;

    // Pending order — resolves next turn
    private OrderType    pendingOrder    = OrderType.NONE;
    private String       pendingTargetZoneId = null; // zone to attack/raid (army is already there)
    private boolean      orderReadyToResolve = false; // true after 1 turn wait

    public NobleArmy(String id, String houseId, int size, String zoneId) {
        this.id     = id;
        this.houseId = houseId;
        this.size    = size;
        this.zoneId  = zoneId;
    }

    // ─── Orders ──────────────────────────────────────────────────────────────

    /** Issue an order. Army is already in the target zone. Resolves next turn. */
    public void issueOrder(OrderType type, String targetZoneId) {
        this.pendingOrder           = type;
        this.pendingTargetZoneId    = targetZoneId;
        this.orderReadyToResolve    = false;
    }

    /**
     * Called at start of turn processing. Marks pending orders as ready.
     * Returns true if an order just became ready.
     */
    public boolean tickOrder() {
        if (pendingOrder != OrderType.NONE && !orderReadyToResolve) {
            orderReadyToResolve = true;
            return true;
        }
        return false;
    }

    public void clearOrder() {
        pendingOrder         = OrderType.NONE;
        pendingTargetZoneId  = null;
        orderReadyToResolve  = false;
    }

    // ─── Accessors ───────────────────────────────────────────────────────────

    public String    getId()                   { return id; }
    public String    getHouseId()              { return houseId; }
    public int       getSize()                 { return size; }
    public void      setSize(int size)         { this.size = Math.max(0, size); }
    public String    getZoneId()               { return zoneId; }
    public void      setZoneId(String zoneId)  { this.zoneId = zoneId; }
    public OrderType getPendingOrder()         { return pendingOrder; }
    public String    getPendingTargetZoneId()  { return pendingTargetZoneId; }
    public boolean   isOrderReadyToResolve()   { return orderReadyToResolve; }
    public boolean   hasPendingOrder()         { return pendingOrder != OrderType.NONE; }
    public boolean   isAlive()                 { return size > 0; }

    /** Disband some soldiers, returning count actually disbanded. */
    public int disband(int count) {
        int actual = Math.min(count, size);
        size -= actual;
        return actual;
    }
}