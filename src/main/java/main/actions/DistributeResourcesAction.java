package main.actions;

import main.parameters.GameParameters;
import main.resources.ResourcePool;
import main.resources.StatBlock;

/**
 * Pay money to improve public happiness slightly.
 */
public class DistributeResourcesAction extends AbstractAction {

    public DistributeResourcesAction() {
        super(GameParameters.DISTRIBUTE_MAX_USES);
    }

    @Override
    public String getName() {
        return "Distribute Resources";
    }

    @Override
    public String getDescription() {
        return "Spend " + GameParameters.DISTRIBUTE_MONEY_COST
            + " money to gain " + GameParameters.DISTRIBUTE_HAPPINESS_GAIN + " happiness.";
    }

    @Override
    public ActionResult execute(ResourcePool resources, StatBlock stats) {
        if (!isAvailable()) {
            return ActionResult.fail("Distribute Resources already used this turn.");
        }
        if (!resources.spendMoney(GameParameters.DISTRIBUTE_MONEY_COST)) {
            return ActionResult.fail("Not enough money. Need " + GameParameters.DISTRIBUTE_MONEY_COST + ".");
        }
        stats.addHappiness(GameParameters.DISTRIBUTE_HAPPINESS_GAIN);
        recordUse();
        return ActionResult.ok("Distributed resources. Happiness +" + GameParameters.DISTRIBUTE_HAPPINESS_GAIN + ".");
    }
}