package City.main.actions;

import City.main.parameters.ActionParams;
 
import City.main.resources.ResourcePool;
import City.main.resources.StatBlock;

/**
 * Spend money to gain influence; raises corruption.
 */
public class BribeAction extends AbstractAction {

    public BribeAction() {
        super(ActionParams.BRIBE_MAX_USES);
    }

    @Override
    public String getName() {
        return "Bribe Officials";
    }

    @Override
    public String getDescription() {
        return "Spend " + ActionParams.BRIBE_MONEY_COST
            + " money to gain " + ActionParams.BRIBE_INFLUENCE_GAINED
            + " influence. Raises corruption by " + ActionParams.BRIBE_CORRUPTION_GAIN + ".";
    }

    @Override


    public ActionResult execute(ResourcePool resources, StatBlock stats) {
        if (!isAvailable()) {
            return ActionResult.fail("Bribe Officials already used " + getMaxUsesPerTurn() + " time(s) this turn.");
        }
        if (resources.getMoney() < ActionParams.BRIBE_MONEY_COST) {
            return ActionResult.fail("Not enough money. Need " + ActionParams.BRIBE_MONEY_COST + ".");
        }
        City.main.ledger.Ledger ledger = getLedger();
        ledger.applyOneTime(City.main.resources.ResourceType.GOLD, "action", getName(),
                -ActionParams.BRIBE_MONEY_COST, resources);
        ledger.applyOneTime(City.main.resources.ResourceType.INFLUENCE, "action", getName(),
                ActionParams.BRIBE_INFLUENCE_GAINED, resources);
        stats.addCorruption(ActionParams.BRIBE_CORRUPTION_GAIN);
        recordUse();
        return ActionResult.ok("Bribed officials. Gained " + ActionParams.BRIBE_INFLUENCE_GAINED
                + " influence. Corruption +" + ActionParams.BRIBE_CORRUPTION_GAIN + ".");
    }

}