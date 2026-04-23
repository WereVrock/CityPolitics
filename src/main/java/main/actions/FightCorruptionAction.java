package main.actions;

import main.parameters.GameParameters;
import main.resources.ResourcePool;
import main.resources.StatBlock;

/**
 * Spend money and influence to reduce corruption.
 */
public class FightCorruptionAction extends AbstractAction {

    public FightCorruptionAction() {
        super(GameParameters.FIGHT_CORRUPTION_MAX_USES);
    }

    @Override
    public String getName() {
        return "Fight Corruption";
    }

    @Override
    public String getDescription() {
        return "Spend " + GameParameters.FIGHT_CORRUPTION_MONEY_COST
            + " money and " + GameParameters.FIGHT_CORRUPTION_INFLUENCE_COST
            + " influence to reduce corruption by " + GameParameters.FIGHT_CORRUPTION_REDUCTION + ".";
    }

    @Override
    public ActionResult execute(ResourcePool resources, StatBlock stats) {
        if (!isAvailable()) {
            return ActionResult.fail("Fight Corruption already used this turn.");
        }
        if (resources.getMoney() < GameParameters.FIGHT_CORRUPTION_MONEY_COST) {
            return ActionResult.fail("Not enough money. Need " + GameParameters.FIGHT_CORRUPTION_MONEY_COST + ".");
        }
        if (resources.getInfluence() < GameParameters.FIGHT_CORRUPTION_INFLUENCE_COST) {
            return ActionResult.fail("Not enough influence. Need " + GameParameters.FIGHT_CORRUPTION_INFLUENCE_COST + ".");
        }
        resources.spendMoney(GameParameters.FIGHT_CORRUPTION_MONEY_COST);
        resources.spendInfluence(GameParameters.FIGHT_CORRUPTION_INFLUENCE_COST);
        stats.reduceCorruption(GameParameters.FIGHT_CORRUPTION_REDUCTION);
        recordUse();
        return ActionResult.ok("Corruption reduced by " + GameParameters.FIGHT_CORRUPTION_REDUCTION + ".");
    }
}