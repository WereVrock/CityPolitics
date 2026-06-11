package main.map;

/**
 * Named anchor slots for army rendering within a zone.
 * Each slot defines an (offsetX, offsetY) relative to zone labelX/labelY.
 * Armies group by slot — all armies of the same faction share one slot anchor
 * and fan out horizontally from it.
 */
public class ZoneArmyAnchors {

    public enum Slot {
        PLAYER,
        NOBLE,
        BARBARIAN_MOBILE,
        BARBARIAN_GARRISON
    }

    private final int[] playerOffset;
    private final int[] nobleOffset;
    private final int[] barbMobileOffset;
    private final int[] barbGarrisonOffset;

    public ZoneArmyAnchors(int[] playerOffset, int[] nobleOffset,
                           int[] barbMobileOffset, int[] barbGarrisonOffset) {
        this.playerOffset       = playerOffset;
        this.nobleOffset        = nobleOffset;
        this.barbMobileOffset   = barbMobileOffset;
        this.barbGarrisonOffset = barbGarrisonOffset;
    }

    /** Returns the {offsetX, offsetY} for the given slot. */
    public int[] getOffset(Slot slot) {
        return switch (slot) {
            case PLAYER              -> playerOffset;
            case NOBLE               -> nobleOffset;
            case BARBARIAN_MOBILE    -> barbMobileOffset;
            case BARBARIAN_GARRISON  -> barbGarrisonOffset;
        };
    }

    /**
     * Returns absolute world coordinates for the anchor of the given slot,
     * given the zone's label position.
     */
    public int[] getWorldPosition(Slot slot, int zoneLabelX, int zoneLabelY) {
        int[] off = getOffset(slot);
        return new int[]{ zoneLabelX + off[0], zoneLabelY + off[1] };
    }
}