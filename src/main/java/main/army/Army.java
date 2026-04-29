// main/army/Army.java
package main.army;

import main.map.Zone;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Represents a player-controlled army on the map.
 * Movement is constrained by per-turn range and order delay.
 */
public class Army {

    private final String id;
    private String       zoneId;
    private int          movesRemaining;   // resets each turn

    /** Orders waiting to be delivered (countdown > 0 means not yet received). */
    private final Deque<PendingOrder> pendingOrders = new ArrayDeque<>();

    public Army(String id, String startZoneId) {
        this.id             = id;
        this.zoneId         = startZoneId;
        this.movesRemaining = 0;
    }

    // ─── Identity ─────────────────────────────────────────────────────────────

    public String getId()     { return id; }
    public String getZoneId() { return zoneId; }

    // ─── Movement state ───────────────────────────────────────────────────────

    public int  getMovesRemaining()       { return movesRemaining; }
    public void setMovesRemaining(int v)  { this.movesRemaining = Math.max(0, v); }

    public void consumeMove() {
        if (movesRemaining > 0) movesRemaining--;
    }

    /** Called at start of each turn: restores movement allowance. */
    public void resetMoves(int movesPerTurn) {
        this.movesRemaining = movesPerTurn;
    }

    // ─── Order queue ──────────────────────────────────────────────────────────

    public void enqueueOrder(PendingOrder order) {
        pendingOrders.addLast(order);
    }

    public Deque<PendingOrder> getPendingOrders() {
        return pendingOrders;
    }

    /** Advance all pending orders by one turn; returns any order whose delay hit zero. */
    public PendingOrder tickOrders() {
        if (pendingOrders.isEmpty()) return null;
        PendingOrder head = pendingOrders.peekFirst();
        if (head.isDelivered()) {
            pendingOrders.pollFirst();
            return head;
        }
        head.tick();
        return null;
    }

    // ─── Execution ────────────────────────────────────────────────────────────

    private String marchTarget = null;

    public String getMarchTarget()          { return marchTarget; }
    public void   setMarchTarget(String id) { this.marchTarget = id; }
    public void   clearMarchTarget()        { this.marchTarget = null; }

    /** Moves the army one zone, consuming one move. */
    public boolean moveTo(String targetZoneId) {
        if (movesRemaining <= 0) return false;
        zoneId = targetZoneId;
        consumeMove();
        return true;
    }

    public void teleportTo(String zoneId) {
        this.zoneId = zoneId;
    }
}