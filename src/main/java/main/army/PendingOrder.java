// main/army/PendingOrder.java
package main.army;

/**
 * An order issued to an army that takes time to arrive.
 * Delay represents turns until the messenger reaches the army.
 */
public class PendingOrder {

    public enum OrderType { MOVE_TO }

    private final OrderType type;
    private final String    targetZoneId;
    private int             turnsUntilDelivery;

    public PendingOrder(OrderType type, String targetZoneId, int delay) {
        this.type                = type;
        this.targetZoneId        = targetZoneId;
        this.turnsUntilDelivery  = delay;
    }

    public void tick() {
        if (turnsUntilDelivery > 0) turnsUntilDelivery--;
    }

    public boolean    isDelivered()      { return turnsUntilDelivery <= 0; }
    public OrderType  getType()          { return type; }
    public String     getTargetZoneId()  { return targetZoneId; }
    public int        getTurnsRemaining(){ return turnsUntilDelivery; }
}