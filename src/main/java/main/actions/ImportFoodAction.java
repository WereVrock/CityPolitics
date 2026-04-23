package main.actions;

import main.parameters.GameParameters;
import main.resources.ResourcePool;
import main.resources.StatBlock;

/**
 * Spend money to buy food from outside the realm.
 */
public class ImportFoodAction extends AbstractAction {

    public ImportFoodAction() {
        super(GameParameters.IMPORT_FOOD_MAX_USES);
    }

    @Override
    public String getName() {
        return "Import Food";
    }

    @Override
    public String getDescription() {
        return "Spend " + GameParameters.IMPORT_FOOD_MONEY_COST
            + " money to gain " + GameParameters.IMPORT_FOOD_GAINED + " food.";
    }

    @Override
    public ActionResult execute(ResourcePool resources, StatBlock stats) {
        if (!isAvailable()) {
            return ActionResult.fail("Import Food already used " + getMaxUsesPerTurn() + " time(s) this turn.");
        }
        if (!resources.spendMoney(GameParameters.IMPORT_FOOD_MONEY_COST)) {
            return ActionResult.fail("Not enough money. Need " + GameParameters.IMPORT_FOOD_MONEY_COST + ".");
        }
        resources.addFood(GameParameters.IMPORT_FOOD_GAINED);
        recordUse();
        return ActionResult.ok("Imported " + GameParameters.IMPORT_FOOD_GAINED + " food for "
            + GameParameters.IMPORT_FOOD_MONEY_COST + " money.");
    }
}