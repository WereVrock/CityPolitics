package City.main.actions;

import City.main.parameters.ActionParams;
 
import City.main.resources.ResourcePool;
import City.main.resources.StatBlock;

/**
 * Spend influence to gain money; raises corruption.
 */
public class AcceptBribesAction extends AbstractAction {

    public AcceptBribesAction() {
        super(ActionParams.ACCEPT_BRIBE_MAX_USES);
    }

    @Override
    public String getName() {
        return "Accept Bribes";
    }

    @Override
    public String getDescription() {
        return "Spend " + ActionParams.ACCEPT_BRIBE_INFLUENCE_COST
            + " influence to gain " + ActionParams.ACCEPT_BRIBE_MONEY_GAINED
            + " money. Raises corruption by " + ActionParams.ACCEPT_BRIBE_CORRUPTION_GAIN + ".";
    }

    @Override


    public ActionResult execute(ResourcePool resources, StatBlock stats) {
        if (!isAvailable()) {
            return ActionResult.fail("Accept Bribes already used " + getMaxUsesPerTurn() + " time(s) this turn.");
        }
        if (resources.getInfluence() < ActionParams.ACCEPT_BRIBE_INFLUENCE_COST) {
            return ActionResult.fail("Not enough influence. Need " + ActionParams.ACCEPT_BRIBE_INFLUENCE_COST + ".");
        }
        City.main.ledger.Ledger ledger = getLedger();
        ledger.applyOneTime(City.main.resources.ResourceType.INFLUENCE, "action", getName(),
                -ActionParams.ACCEPT_BRIBE_INFLUENCE_COST, resources);
        ledger.applyOneTime(City.main.resources.ResourceType.GOLD, "action", getName(),
                ActionParams.ACCEPT_BRIBE_MONEY_GAINED, resources);
        stats.addCorruption(ActionParams.ACCEPT_BRIBE_CORRUPTION_GAIN);
        recordUse();
        return ActionResult.ok("Accepted bribes. Gained " + ActionParams.ACCEPT_BRIBE_MONEY_GAINED
                + " money. Corruption +" + ActionParams.ACCEPT_BRIBE_CORRUPTION_GAIN + ".");
    }

}