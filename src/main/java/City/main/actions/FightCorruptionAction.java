package City.main.actions;

import City.main.parameters.ActionParams;
 
import City.main.resources.ResourcePool;
import City.main.resources.StatBlock;

/**
 * Spend money and influence to reduce corruption.
 */
public class FightCorruptionAction extends AbstractAction {

    public FightCorruptionAction() {
        super(ActionParams.FIGHT_CORRUPTION_MAX_USES);
    }

    @Override
    public String getName() {
        return "Fight Corruption";
    }

    @Override
    public String getDescription() {
        return "Spend " + ActionParams.FIGHT_CORRUPTION_MONEY_COST
            + " money and " + ActionParams.FIGHT_CORRUPTION_INFLUENCE_COST
            + " influence to reduce corruption by " + ActionParams.FIGHT_CORRUPTION_REDUCTION + ".";
    }

    @Override


    public ActionResult execute(ResourcePool resources, StatBlock stats) {
        if (!isAvailable()) {
            return ActionResult.fail("Fight Corruption already used this turn.");
        }
        if (resources.getMoney() < ActionParams.FIGHT_CORRUPTION_MONEY_COST) {
            return ActionResult.fail("Not enough money. Need " + ActionParams.FIGHT_CORRUPTION_MONEY_COST + ".");
        }
        if (resources.getInfluence() < ActionParams.FIGHT_CORRUPTION_INFLUENCE_COST) {
            return ActionResult.fail("Not enough influence. Need " + ActionParams.FIGHT_CORRUPTION_INFLUENCE_COST + ".");
        }
        City.main.ledger.Ledger ledger = getLedger();
        ledger.applyOneTime(City.main.resources.ResourceType.GOLD, "action", getName(),
                -ActionParams.FIGHT_CORRUPTION_MONEY_COST, resources);
        ledger.applyOneTime(City.main.resources.ResourceType.INFLUENCE, "action", getName(),
                -ActionParams.FIGHT_CORRUPTION_INFLUENCE_COST, resources);
        stats.reduceCorruption(ActionParams.FIGHT_CORRUPTION_REDUCTION);
        recordUse();
        return ActionResult.ok("Corruption reduced by " + ActionParams.FIGHT_CORRUPTION_REDUCTION + ".");
    }

}