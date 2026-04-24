package main.core;

import main.parameters.GameParameters;

/**
 * Applies the corruption cost multiplier to any base cost.
 * All actions must route costs through here.
 */
public class CostCalculator {

    private CostCalculator() {}

    /**
     * Returns the effective cost after applying the corruption multiplier.
     * effectiveCost = baseCost * (1 + corruption / 100)
     */
    public static int apply(int baseCost, int corruption) {
        double multiplier = 1.0 + (corruption / 100.0);
        return (int) Math.ceil(baseCost * multiplier);
    }
}