package main.nobles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tracks which noble houses the player has declared under protection.
 * Protection grants opinion bonus to target, malus to rivals, and ties player prestige to target's fate.
 */
public class ProtectionManager {

    private final List<String> protectedHouseIds = new ArrayList<>();

    public void declareProtection(String houseId) {
        if (!protectedHouseIds.contains(houseId)) {
            protectedHouseIds.add(houseId);
        }
    }

    public void removeProtection(String houseId) {
        protectedHouseIds.remove(houseId);
    }

    public boolean isUnderProtection(String houseId) {
        return protectedHouseIds.contains(houseId);
    }

    public List<String> getProtectedHouseIds() {
        return Collections.unmodifiableList(protectedHouseIds);
    }

    public void reset() {
        protectedHouseIds.clear();
    }
}