package City.main.pops;

import City.main.politics.PolitcalView;
import City.main.politics.PoliticalViewHierarchy;

import java.util.*;

/**
 * Tracks a single pop's political view intensities, electoral behaviour,
 * and affiliation streak tracking for the election system.
 *
 * Views: map of view → intensity (0–100). A pop can hold multiple views
 *        as long as they are not contradictory.
 * Affiliation streak: how many consecutive elections the pop voted for its
 *                     affiliated party (3 = becomes affiliated).
 * Override streak: how many consecutive elections the affiliation overrode
 *                  the pop's natural choice (3 = affiliation lost).
 */
public class PopElectoralData {

    private final Map<PolitcalView, Integer> viewIntensities = new LinkedHashMap<>();

    // Streak tracking (per pop)
    private int consecutiveVotesForAffiliated = 0;
    private int consecutiveOverrides          = 0;

    // Per-pop randomized thresholds (0–3), set at construction
    private final int affiliationGainThreshold;
    private final int affiliationLossThreshold;

    // Last election result
    private String lastVotedPartyName = null;
    private boolean lastOverridden    = false;

    public PopElectoralData(Random rng) {
        // Randomise initial streaks between 0 and 3
        this.consecutiveVotesForAffiliated = rng.nextInt(4);
        this.consecutiveOverrides          = rng.nextInt(4);
        // Each pop has a slightly randomized threshold (2, 3, or 4)
        this.affiliationGainThreshold = 2 + rng.nextInt(3);
        this.affiliationLossThreshold = 2 + rng.nextInt(3);
    }

    public int getAffiliationGainThreshold() { return affiliationGainThreshold; }
    public int getAffiliationLossThreshold() { return affiliationLossThreshold; }

    // ─── View management ─────────────────────────────────────────────────────

    public Map<PolitcalView, Integer> getViewIntensities() {
        return Collections.unmodifiableMap(viewIntensities);
    }

    public int getIntensity(PolitcalView view) {
        return viewIntensities.getOrDefault(view, 0);
    }

    /**
     * Sets or updates intensity of a view. Resolves contradictions by removing
     * the lower-priority contradicting view if conflict arises.
     */
    public void setViewIntensity(PolitcalView view, int intensity) {
        intensity = Math.max(0, Math.min(100, intensity));
        if (intensity == 0) {
            viewIntensities.remove(view);
            return;
        }
        // Remove contradicting views that have lower priority
        List<PolitcalView> toRemove = new ArrayList<>();
        for (PolitcalView existing : viewIntensities.keySet()) {
            if (PoliticalViewHierarchy.areContradictory(view, existing)) {
                PolitcalView lower = PoliticalViewHierarchy.resolveLowerPriority(view, existing);
                if (lower == existing) toRemove.add(existing);
                else return; // new view has lower priority — ignore it
            }
        }
        toRemove.forEach(viewIntensities::remove);
        viewIntensities.put(view, intensity);
    }

    /**
     * Adjusts intensity by delta. Clamped to 0–100.
     */
    public void adjustIntensity(PolitcalView view, int delta) {
        // setViewIntensity already handles contradiction resolution
        int current = viewIntensities.getOrDefault(view, 0);
        setViewIntensity(view, current + delta);
    }

    /** Dominant view (highest intensity). */
    public PolitcalView getDominantView() {
        return viewIntensities.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(PolitcalView.NONE);
    }

    // ─── Streak tracking ─────────────────────────────────────────────────────

    public int  getConsecutiveVotesForAffiliated() { return consecutiveVotesForAffiliated; }
    public int  getConsecutiveOverrides()           { return consecutiveOverrides; }
    public String getLastVotedPartyName()           { return lastVotedPartyName; }
    public boolean wasLastOverridden()              { return lastOverridden; }

    public void recordVote(String partyName, boolean overridden) {
        lastVotedPartyName = partyName;
        lastOverridden     = overridden;
        if (overridden) {
            consecutiveOverrides++;
            consecutiveVotesForAffiliated = 0;
        } else {
            consecutiveOverrides = 0;
            consecutiveVotesForAffiliated++;
        }
    }

    // ─── Save/load ───────────────────────────────────────────────────────────

    public void setConsecutiveVotesForAffiliated(int v) { consecutiveVotesForAffiliated = v; }
    public void setConsecutiveOverrides(int v)           { consecutiveOverrides = v; }
    public void setLastVotedPartyName(String v)          { lastVotedPartyName = v; }
    public void setLastOverridden(boolean v)             { lastOverridden = v; }
    public void setRawIntensities(Map<PolitcalView, Integer> m) {
        viewIntensities.clear();
        viewIntensities.putAll(m);
    }
}