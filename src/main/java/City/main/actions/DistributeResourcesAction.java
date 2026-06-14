package City.main.actions;

import City.main.parameters.ActionParams;
 
import City.main.resources.ResourcePool;
import City.main.resources.StatBlock;

/**
 * Pay money to improve public happiness slightly.
 */
public class DistributeResourcesAction extends AbstractAction {

    public DistributeResourcesAction() {
        super(ActionParams.DISTRIBUTE_MAX_USES);
    }

    @Override
    public String getName() {
        return "Distribute Resources";
    }

    @Override
    public String getDescription() {
        return "Spend " + ActionParams.DISTRIBUTE_MONEY_COST
            + " money to gain " + ActionParams.DISTRIBUTE_HAPPINESS_GAIN + " happiness.";
    }

    @Override


    public ActionResult execute(ResourcePool resources, StatBlock stats) {
        if (!isAvailable()) {
            return ActionResult.fail("Distribute Resources already used this turn.");
        }
        if (resources.getMoney() < ActionParams.DISTRIBUTE_MONEY_COST) {
            return ActionResult.fail("Not enough money. Need " + ActionParams.DISTRIBUTE_MONEY_COST + ".");
        }
        getLedger().applyOneTime(City.main.resources.ResourceType.GOLD, "action", getName(),
                -ActionParams.DISTRIBUTE_MONEY_COST, resources);
        stats.addHappiness(ActionParams.DISTRIBUTE_HAPPINESS_GAIN);
        recordUse();
        return ActionResult.ok("Distributed resources. Happiness +" + ActionParams.DISTRIBUTE_HAPPINESS_GAIN + ".");
    }

}