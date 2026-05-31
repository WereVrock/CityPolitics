package main.barbarians;

import main.map.ZoneManager;
import main.parameters.GameParameters;

import java.util.*;

/**
 * Tracks ravaged and heavily ravaged zone status.
 * Separate from ZoneState to keep barbarian logic isolated.
 */
public class RavagedZoneManager {

    public enum RavagedLevel { NONE, RAVAGED, HEAVILY_RAVAGED }

    private static class RavagedEntry {
        RavagedLevel level;
        int          turnsRavaged;  // total turns since ravaged
        static final int FLAT_TURNS  = 7 * GameParameters.PERIODS_PER_YEAR;
        static final int DECAY_TURNS = 3 * GameParameters.PERIODS_PER_YEAR;

        RavagedEntry(RavagedLevel level) {
            this.level        = level;
            this.turnsRavaged = 0;
        }

        /** Production penalty 0.0–1.0. */
        double getPenalty() {
            double base = level == RavagedLevel.HEAVILY_RAVAGED
                    ? GameParameters.BARB_HEAVILY_RAVAGED_PENALTY
                    : GameParameters.BARB_RAVAGED_PENALTY;
            if (turnsRavaged < FLAT_TURNS) return base;
            int decayTurn = turnsRavaged - FLAT_TURNS;
            if (decayTurn >= DECAY_TURNS) return 0.0;
            double frac = (double) decayTurn / DECAY_TURNS;
            return base * (1.0 - frac);
        }

        boolean isExpired() {
            return turnsRavaged >= FLAT_TURNS + DECAY_TURNS;
        }
    }

    private final Map<String, RavagedEntry> entries = new LinkedHashMap<>();

    // ─── Mark ────────────────────────────────────────────────────────────────

    public void markRavaged(String zoneId) {
        RavagedEntry existing = entries.get(zoneId);
        if (existing != null && existing.level == RavagedLevel.HEAVILY_RAVAGED) return;
        entries.put(zoneId, new RavagedEntry(RavagedLevel.RAVAGED));
    }

    public void markHeavilyRavaged(String zoneId) {
        entries.put(zoneId, new RavagedEntry(RavagedLevel.HEAVILY_RAVAGED));
    }

    // ─── Query ───────────────────────────────────────────────────────────────

    public RavagedLevel getLevel(String zoneId) {
        RavagedEntry e = entries.get(zoneId);
        return e != null ? e.level : RavagedLevel.NONE;
    }

    public double getProductionMultiplier(String zoneId) {
        RavagedEntry e = entries.get(zoneId);
        if (e == null) return 1.0;
        return 1.0 - e.getPenalty();
    }

    public boolean isRavaged(String zoneId) {
        return entries.containsKey(zoneId);
    }

    // ─── Per-turn tick ───────────────────────────────────────────────────────

    public void tick() {
        for (Iterator<Map.Entry<String, RavagedEntry>> it = entries.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, RavagedEntry> e = it.next();
            e.getValue().turnsRavaged++;
            if (e.getValue().isExpired()) it.remove();
        }
    }

    public Set<String> getRavagedZoneIds() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    public void reset() {
        entries.clear();
    }
}