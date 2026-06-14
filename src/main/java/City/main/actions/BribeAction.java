package City.main.actions;

import City.main.parameters.GameParameters;
import City.main.resources.ResourcePool;
import City.main.resources.StatBlock;

/**
 * Spend money to gain influence; raises corruption.
 */
public class BribeAction extends AbstractAction {

    public BribeAction() {
        super(GameParameters.BRIBE_MAX_USES);
    }

    @Override
    public String getName() {
        return "Bribe Officials";
    }

    @Override
    public String getDescription() {
        return "Spend " + GameParameters.BRIBE_MONEY_COST
            + " money to gain " + GameParameters.BRIBE_INFLUENCE_GAINED
            + " influence. Raises corruption by " + GameParameters.BRIBE_CORRUPTION_GAIN + ".";
    }

    @Override


    public ActionResult execute(ResourcePool resources, StatBlock stats) {
        if (!isAvailable()) {
            return ActionResult.fail("Bribe Officials already used " + getMaxUsesPerTurn() + " time(s) this turn.");
        }
        if (resources.getMoney() < GameParameters.BRIBE_MONEY_COST) {
            return ActionResult.fail("Not enough money. Need " + GameParameters.BRIBE_MONEY_COST + ".");
        }
        City.main.ledger.Ledger ledger = getLedger();
        ledger.applyOneTime(City.main.resources.ResourceType.GOLD, "action", getName(),
                -GameParameters.BRIBE_MONEY_COST, resources);
        ledger.applyOneTime(City.main.resources.ResourceType.INFLUENCE, "action", getName(),
                GameParameters.BRIBE_INFLUENCE_GAINED, resources);
        stats.addCorruption(GameParameters.BRIBE_CORRUPTION_GAIN);
        recordUse();
        return ActionResult.ok("Bribed officials. Gained " + GameParameters.BRIBE_INFLUENCE_GAINED
                + " influence. Corruption +" + GameParameters.BRIBE_CORRUPTION_GAIN + ".");
    }

}