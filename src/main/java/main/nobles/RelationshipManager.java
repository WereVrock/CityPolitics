package main.nobles;

import main.parameters.GameParameters;

import java.util.*;

/**
 * Tracks all house-to-house relationships.
 * Keyed by ordered pair (lower id first) to avoid duplicates.
 */
public class RelationshipManager {

    private final Map<String, Relationship> relationships = new HashMap<>();

    // ─── Access ───────────────────────────────────────────────────────────────

    public Relationship get(String idA, String idB) {
        return relationships.getOrDefault(key(idA, idB), Relationship.NEUTRAL);
    }

    public void set(String idA, String idB, Relationship rel) {
        relationships.put(key(idA, idB), rel);
    }

    /**
     * Returns true if both houses share at least one common rival.
     */
    public boolean shareRival(String idA, String idB, List<String> allHouseIds) {
        for (String other : allHouseIds) {
            if (other.equals(idA) || other.equals(idB)) continue;
            if (get(idA, other) == Relationship.RIVAL
                    && get(idB, other) == Relationship.RIVAL) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all house IDs this house is in a given relationship with.
     */
    public List<String> getAll(String houseId, Relationship rel, List<String> allHouseIds) {
        List<String> result = new ArrayList<>();
        for (String other : allHouseIds) {
            if (other.equals(houseId)) continue;
            if (get(houseId, other) == rel) result.add(other);
        }
        return result;
    }

    public void reset() {
        relationships.clear();
    }

    // ─── Key ─────────────────────────────────────────────────────────────────

    private String key(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }
}