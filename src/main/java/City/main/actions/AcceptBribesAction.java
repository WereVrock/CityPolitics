package City.main.actions;

import City.main.parameters.GameParameters;
import City.main.resources.ResourcePool;
import City.main.resources.StatBlock;

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
        if (resources.getInfluence() < GameParameters.ACCEPT_BRIBE_INFLUENCE_COST) {
            return ActionResult.fail("Not enough influence. Need " + GameParameters.ACCEPT_BRIBE_INFLUENCE_COST + ".");
        }
        City.main.ledger.Ledger ledger = getLedger();
        ledger.applyOneTime(City.main.resources.ResourceType.INFLUENCE, "action", getName(),
                -GameParameters.ACCEPT_BRIBE_INFLUENCE_COST, resources);
        ledger.applyOneTime(City.main.resources.ResourceType.GOLD, "action", getName(),
                GameParameters.ACCEPT_BRIBE_MONEY_GAINED, resources);
        stats.addCorruption(GameParameters.ACCEPT_BRIBE_CORRUPTION_GAIN);
        recordUse();
        return ActionResult.ok("Accepted bribes. Gained " + GameParameters.ACCEPT_BRIBE_MONEY_GAINED
                + " money. Corruption +" + GameParameters.ACCEPT_BRIBE_CORRUPTION_GAIN + ".");
    }

}