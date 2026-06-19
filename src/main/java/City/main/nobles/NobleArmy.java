package City.main.nobles;

import java.util.HashSet;
import java.util.Set;

/**
 * A raised army belonging to a noble house.
 * Sits in a zone. Pending orders resolve next turn.
 */
public class NobleArmy {

    private boolean skipNextUpkeep = false;
    private boolean mercenary      = false;

    public boolean getSkipNextUpkeep() { return skipNextUpkeep; }
    public void setSkipNextUpkeep(boolean v) { skipNextUpkeep = v; }
    public boolean isMercenary()       { return mercenary; }
    public void    setMercenary(boolean v) { mercenary = v; }

    public enum OrderType { NONE, ATTACK, RAID, JOIN_BATTLE }

    private final String id;
    private final String houseId;
    private int          size;
    private String       zoneId;

    // Pending order — resolves next turn
    private OrderType   pendingOrder        = OrderType.NONE;
    private String      pendingTargetZoneId = null;
    private boolean     orderReadyToResolve = false;
    private String      previousZoneId      = null;
    private Set<String> coalitionMemberIds  = null; // non-null = coalition attack

    public NobleArmy(String id, String houseId, int size, String zoneId) {
        this.id      = id;
        this.houseId = houseId;
        this.size    = size;
        this.zoneId  = zoneId;
    }

    // ─── Orders ──────────────────────────────────────────────────────────────

    /** Issue a normal order. Resolves next turn. */
    public void issueOrder(OrderType type, String targetZoneId) {
        this.pendingOrder        = type;
        this.pendingTargetZoneId = targetZoneId;
        this.orderReadyToResolve = false;
        this.coalitionMemberIds  = null;
    }

    /** Issue a coalition attack order with participating member house IDs. */
    public void issueCoalitionOrder(String targetZoneId, Set<String> memberIds) {
        this.pendingOrder        = OrderType.ATTACK;
        this.pendingTargetZoneId = targetZoneId;
        this.orderReadyToResolve = false;
        this.coalitionMemberIds  = new HashSet<>(memberIds);
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
        pendingOrder        = OrderType.NONE;
        pendingTargetZoneId = null;
        orderReadyToResolve = false;
        previousZoneId      = null;
        coalitionMemberIds  = null;
    }

    public void   setPreviousZoneId(String zoneId) { this.previousZoneId = zoneId; }
    public String getPreviousZoneId()              { return previousZoneId; }

    // ─── Accessors ───────────────────────────────────────────────────────────

    public String      getId()                  { return id; }
    public String      getHouseId()             { return houseId; }
    public int         getSize()                { return size; }
    public void        setSize(int size)        { this.size = Math.max(0, size); }
    public String      getZoneId()              { return zoneId; }
    public void        setZoneId(String zoneId) { this.zoneId = zoneId; }
    public OrderType   getPendingOrder()        { return pendingOrder; }
    public String      getPendingTargetZoneId() { return pendingTargetZoneId; }
    public boolean     isOrderReadyToResolve()  { return orderReadyToResolve; }
    public boolean     hasPendingOrder()        { return pendingOrder != OrderType.NONE; }
    public boolean     isAlive()                { return size > 0; }
    public Set<String> getCoalitionMemberIds()  { return coalitionMemberIds; }
    public boolean     isCoalitionAttack()      { return coalitionMemberIds != null; }

    /** Disband some soldiers, returning count actually disbanded. */
    public int disband(int count) {
        int actual = Math.min(count, size);
        size -= actual;
        return actual;
    }
}