package main.actions;

import main.parameters.GameParameters;
import main.resources.ResourcePool;
import main.resources.StatBlock;

/**
 * Spend influence to gain money; raises corruption.
 */
public class AcceptBribesAction extends AbstractAction {

    public AcceptBribesAction() {
        super(GameParameters.ACCEPT_BRIBE_MAX_USES);
    }

    @Override
    public String getName() {
        return "Accept Bribes";
    }

    @Override
    public String getDescription() {
        return "Spend " + GameParameters.ACCEPT_BRIBE_INFLUENCE_COST
            + " influence to gain " + GameParameters.ACCEPT_BRIBE_MONEY_GAINED
            + " money. Raises corruption by " + GameParameters.ACCEPT_BRIBE_CORRUPTION_GAIN + ".";
    }

    @Override
    public ActionResult execute(ResourcePool resources, StatBlock stats) {
        if (!isAvailable()) {
            return ActionResult.fail("Accept Bribes already used " + getMaxUsesPerTurn() + " time(s) this turn.");
        }
        if (!resources.spendInfluence(GameParameters.ACCEPT_BRIBE_INFLUENCE_COST)) {
            return ActionResult.fail("Not enough influence. Need " + GameParameters.ACCEPT_BRIBE_INFLUENCE_COST + ".");
        }
        resources.addMoney(GameParameters.ACCEPT_BRIBE_MONEY_GAINED);
        stats.addCorruption(GameParameters.ACCEPT_BRIBE_CORRUPTION_GAIN);
        recordUse();
        return ActionResult.ok("Accepted bribes. Gained " + GameParameters.ACCEPT_BRIBE_MONEY_GAINED
            + " money. Corruption +" + GameParameters.ACCEPT_BRIBE_CORRUPTION_GAIN + ".");
    }
}