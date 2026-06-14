package City.main.nobles.council;

import City.main.nobles.NobleHouse;
import City.main.parameters.GameParameters;

import java.util.List;

/**
 * Determines which noble houses are "prestigious" for noble council voting.
 * A house qualifies if its prestige >= 15% of the total prestige of all houses.
 */
public final class CouncilPrestigeEvaluator {

    private CouncilPrestigeEvaluator() {}

    public static int getTotalPrestige(List<NobleHouse> houses) {
        int total = 0;
        for (NobleHouse h : houses) {
            if (!h.isEliminated()) total += h.getPrestige();
        }
        return total;
    }

    public static boolean isPrestigious(NobleHouse house, int totalPrestige) {
        if (house.isEliminated() || totalPrestige <= 0) return false;
        double fraction = (double) house.getPrestige() / totalPrestige;
        return fraction >= GameParameters.NOBLE_COUNCIL_PRESTIGE_THRESHOLD;
    }

    public static int getTotalPrestigiousPrestige(List<NobleHouse> houses, int totalPrestige) {
        int total = 0;
        for (NobleHouse h : houses) {
            if (isPrestigious(h, totalPrestige)) total += h.getPrestige();
        }
        return total;
    }
}