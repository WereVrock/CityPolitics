package main.nobles;

import main.parameters.GameParameters;

import java.util.*;

/**
 * Tracks all house-to-house relationships.
 * Handles raid counters, aggression decay, and relationship transitions.
 */
public class RelationshipManager {

    private final Map<String, Relationship> relationships = new HashMap<>();
    private final Map<String, Integer>      raidCounts    = new HashMap<>(); // key = "raiderID|targetID"
    private final Map<String, Integer>      peaceTurns    = new HashMap<>(); // turns since last aggression

    // ─── Access ───────────────────────────────────────────────────────────────

    public Relationship get(String idA, String idB) {
        return relationships.getOrDefault(key(idA, idB), Relationship.NEUTRAL);
    }

    public void set(String idA, String idB, Relationship rel) {
        relationships.put(key(idA, idB), rel);
        if (rel == Relationship.ALLIED || rel == Relationship.FRIENDLY
                || rel == Relationship.NEUTRAL) {
            peaceTurns.put(key(idA, idB), 0);
        }
    }

    /**
     * Improve relationship by one tier. RIVAL → HOSTILE → NEUTRAL → FRIENDLY → ALLIED.
     */
    public void improve(String idA, String idB) {
        Relationship current = get(idA, idB);
        Relationship next = switch (current) {
            case RIVAL   -> Relationship.HOSTILE;
            case HOSTILE -> Relationship.NEUTRAL;
            case NEUTRAL -> Relationship.FRIENDLY;
            case FRIENDLY-> Relationship.ALLIED;
            case ALLIED  -> Relationship.ALLIED;
        };
        set(idA, idB, next);
    }

    /**
     * Worsen relationship by one tier.
     */
    public void worsen(String idA, String idB) {
        Relationship current = get(idA, idB);
        Relationship next = switch (current) {
            case ALLIED  -> Relationship.FRIENDLY;
            case FRIENDLY-> Relationship.NEUTRAL;
            case NEUTRAL -> Relationship.HOSTILE;
            case HOSTILE -> Relationship.RIVAL;
            case RIVAL   -> Relationship.RIVAL;
        };
        set(idA, idB, next);
    }

    // ─── Raid tracking ───────────────────────────────────────────────────────

    /**
     * Record a raid. Returns new raid count.
     * First raid → HOSTILE. Second raid by same house → RIVAL.
     */
    public int recordRaid(String raiderId, String targetId) {
        String k     = raiderId + "|" + targetId;
        int    count = raidCounts.getOrDefault(k, 0) + 1;
        raidCounts.put(k, count);

        if (count == 1) {
            Relationship current = get(raiderId, targetId);
            if (current == Relationship.ALLIED || current == Relationship.FRIENDLY
                    || current == Relationship.NEUTRAL) {
                set(raiderId, targetId, Relationship.HOSTILE);
            }
        } else if (count >= 2) {
            set(raiderId, targetId, Relationship.RIVAL);
        }
        return count;
    }

    // ─── Decay ───────────────────────────────────────────────────────────────

    /**
     * Called each turn. HOSTILE relationships with no aggression for
     * HOSTILE_DECAY_TURNS decay back to NEUTRAL.
     */
    public void tickDecay(List<String> allIds) {
        for (int i = 0; i < allIds.size(); i++) {
            for (int j = i + 1; j < allIds.size(); j++) {
                String a = allIds.get(i);
                String b = allIds.get(j);
                String k = key(a, b);
                if (get(a, b) == Relationship.HOSTILE) {
                    int turns = peaceTurns.getOrDefault(k, 0) + 1;
                    peaceTurns.put(k, turns);
                    if (turns >= GameParameters.HOSTILE_DECAY_TURNS) {
                        set(a, b, Relationship.NEUTRAL);
                        peaceTurns.put(k, 0);
                    }
                }
            }
        }
    }

    // ─── Shared rival ────────────────────────────────────────────────────────

    public boolean shareRival(String idA, String idB, List<String> allIds) {
        for (String other : allIds) {
            if (other.equals(idA) || other.equals(idB)) continue;
            if (get(idA, other) == Relationship.RIVAL
                    && get(idB, other) == Relationship.RIVAL) return true;
        }
        return false;
    }

    public List<String> getAll(String houseId, Relationship rel,
                                List<String> allIds) {
        List<String> result = new ArrayList<>();
        for (String other : allIds) {
            if (other.equals(houseId)) continue;
            if (get(houseId, other) == rel) result.add(other);
        }
        return result;
    }

    public void reset() {
        relationships.clear();
        raidCounts.clear();
        peaceTurns.clear();
    }

    // ─── Key ─────────────────────────────────────────────────────────────────

    private String key(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }
}