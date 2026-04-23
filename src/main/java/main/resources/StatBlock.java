package main.resources;

import main.parameters.GameParameters;

/**
 * Tracks the two bounded stats: corruption and happiness.
 * Both are clamped to [0, 100].
 */
public class StatBlock {

    private static final int MIN = 0;
    private static final int MAX = 100;

    private int corruption;
    private int happiness;

    public StatBlock() {
        this.corruption = GameParameters.STARTING_CORRUPTION;
        this.happiness  = GameParameters.STARTING_HAPPINESS;
    }

    // ─── Corruption ──────────────────────────────────────────────────────────

    public int getCorruption() { return corruption; }

    public void addCorruption(int amount) {
        corruption = clamp(corruption + amount);
    }

    public void reduceCorruption(int amount) {
        corruption = clamp(corruption - amount);
    }

    // ─── Happiness ───────────────────────────────────────────────────────────

    public int getHappiness() { return happiness; }

    public void addHappiness(int amount) {
        happiness = clamp(happiness + amount);
    }

    public void reduceHappiness(int amount) {
        happiness = clamp(happiness - amount);
    }

    // ─── Utility ─────────────────────────────────────────────────────────────

    private int clamp(int value) {
        return Math.max(MIN, Math.min(MAX, value));
    }
}