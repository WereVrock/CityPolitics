package City.main.map;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Owns ZoneArmyAnchors for every zone.
 * Zones without explicit overrides get computed defaults that match
 * the original renderer hard-coded positions.
 *
 * Default offsets (relative to zone labelX / labelY):
 *   PLAYER             → (+30, -20)   — original ArmyRenderer anchor
 *   NOBLE              → (  0, -30)   — original NobleArmyRenderer anchor
 *   BARBARIAN_MOBILE   → (  0, -44)   — original BarbArmyRenderer anchor
 *   BARBARIAN_GARRISON → (+28, +14)   — original BarbArmyRenderer garrison anchor
 */
public class ZoneArmyAnchorRegistry {

    // ── Defaults matching original renderer hard-coded positions ─────────────
    private static final int[] DEFAULT_PLAYER_OFFSET       = {  30, -20 };
    private static final int[] DEFAULT_NOBLE_OFFSET        = {   0, -30 };
    private static final int[] DEFAULT_BARB_MOBILE_OFFSET  = {   0, -44 };
    private static final int[] DEFAULT_BARB_GARRISON_OFFSET= {  28,  14 };

    private static final ZoneArmyAnchors DEFAULT_ANCHORS = new ZoneArmyAnchors(
            DEFAULT_PLAYER_OFFSET,
            DEFAULT_NOBLE_OFFSET,
            DEFAULT_BARB_MOBILE_OFFSET,
            DEFAULT_BARB_GARRISON_OFFSET
    );

    private final Map<String, ZoneArmyAnchors> overrides = new LinkedHashMap<>();

    public ZoneArmyAnchorRegistry() {
        // No overrides by default — all zones use defaults.
        // Overrides are applied via applyOverride() from the editor output.
    }

    /**
     * Returns anchors for the given zone.
     * Falls back to defaults if no override is registered.
     */
    public ZoneArmyAnchors get(String zoneId) {
        return overrides.getOrDefault(zoneId, DEFAULT_ANCHORS);
    }

    /**
     * Registers a custom anchor set for a zone, overriding defaults.
     * Called during game init from generated editor output.
     */
    public void applyOverride(String zoneId, ZoneArmyAnchors anchors) {
        overrides.put(zoneId, anchors);
    }

    /**
     * Convenience: apply a single-slot override without touching other slots.
     * Merges with any existing override (or defaults) for this zone.
     */
    public void applySlotOverride(String zoneId,
                                  ZoneArmyAnchors.Slot slot,
                                  int offsetX, int offsetY) {
        ZoneArmyAnchors existing = get(zoneId);
        int[] player   = copy(existing.getOffset(ZoneArmyAnchors.Slot.PLAYER));
        int[] noble    = copy(existing.getOffset(ZoneArmyAnchors.Slot.NOBLE));
        int[] barbMob  = copy(existing.getOffset(ZoneArmyAnchors.Slot.BARBARIAN_MOBILE));
        int[] barbGarr = copy(existing.getOffset(ZoneArmyAnchors.Slot.BARBARIAN_GARRISON));

        switch (slot) {
            case PLAYER              -> { player[0]   = offsetX; player[1]   = offsetY; }
            case NOBLE               -> { noble[0]    = offsetX; noble[1]    = offsetY; }
            case BARBARIAN_MOBILE    -> { barbMob[0]  = offsetX; barbMob[1]  = offsetY; }
            case BARBARIAN_GARRISON  -> { barbGarr[0] = offsetX; barbGarr[1] = offsetY; }
        }
        overrides.put(zoneId, new ZoneArmyAnchors(player, noble, barbMob, barbGarr));
    }

    private static int[] copy(int[] src) {
        return new int[]{ src[0], src[1] };
    }
}