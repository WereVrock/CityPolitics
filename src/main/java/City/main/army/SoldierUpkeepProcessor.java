// ===== SoldierUpkeepProcessor.java =====
package City.main.army;

import City.debug.Debug;
import City.main.parameters.GameParameters;
import City.main.resources.ResourcePool;

import java.util.Random;

/**
 * Handles per-turn soldier upkeep payment and desertion when skipped.
 *
 * Desertion:
 *   A random fraction of soldiers in range [DESERTION_MIN, DESERTION_MAX]
 *   abandons and the equivalent manpower is returned to the pool.
 *
 * The gold cost for the turn is registered as a single ledger entry.
 */
public class SoldierUpkeepProcessor {

    private static final Random RNG = new Random();

    private final ResourcePool resources;
    private final Army         army;

    public SoldierUpkeepProcessor(ResourcePool resources, Army army) {
        this.resources = resources;
        this.army      = army;
    }

    /**
     * @return the gold cost due this turn for the army's soldiers
     */
    public double computeUpkeepCost() {
        return army.getSoldierCount() * GameParameters.SOLDIER_UPKEEP_GOLD;
    }

    /**
     * Player pays upkeep. Deducts gold; registers ledger entry.
     * @return false if cannot afford (triggers desertion path)
     */

public boolean payUpkeep() {
        int cost = (int) Math.ceil(computeUpkeepCost());
        if (!resources.spendMoney(cost)) return false;
        Debug.log("soldier-upkeep", "paid", "cost=" + cost);
        return true;
    }

/**
     * Skipping upkeep: desertion fires. Returns number of deserters.
     * Deserters are removed from the army and returned to manpower.
     */

public int processDesertion() {
        int soldiers = army.getSize();
        if (soldiers == 0) return 0;
        double minFrac = GameParameters.SOLDIER_DESERTION_MIN_FRACTION;
        double maxFrac = GameParameters.SOLDIER_DESERTION_MAX_FRACTION;
        double frac    = minFrac + RNG.nextDouble() * (maxFrac - minFrac);
        int    lost    = (int) Math.ceil(soldiers * frac);
        army.applyLosses(lost);
        resources.addManpower(lost);
        Debug.log("soldier-upkeep", "desertion", "lost=" + lost + " returned to manpower");
        return lost;
    }

}