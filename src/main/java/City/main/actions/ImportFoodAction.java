package City.main.actions;

import City.main.parameters.ActionParams;
 
import City.main.resources.ResourcePool;
import City.main.resources.StatBlock;

/**
 * Spend money to buy food from outside the realm.
 */
public class ImportFoodAction extends AbstractAction {

    public ImportFoodAction() {
        super(ActionParams.IMPORT_FOOD_MAX_USES);
    }

    @Override
    public String getName() {
        return "Import Food";
    }

    @Override
    public String getDescription() {
        return "Spend " + ActionParams.IMPORT_FOOD_MONEY_COST
            + " money to gain " + ActionParams.IMPORT_FOOD_GAINED + " food.";
    }

    @Override


    public ActionResult execute(ResourcePool resources, StatBlock stats) {
        if (!isAvailable()) {
            return ActionResult.fail("Import Food already used " + getMaxUsesPerTurn() + " time(s) this turn.");
        }
        if (resources.getMoney() < ActionParams.IMPORT_FOOD_MONEY_COST) {
            return ActionResult.fail("Not enough money. Need " + ActionParams.IMPORT_FOOD_MONEY_COST + ".");
        }
        City.main.ledger.Ledger ledger = getLedger();
        ledger.applyOneTime(City.main.resources.ResourceType.GOLD, "action", getName(),
                -ActionParams.IMPORT_FOOD_MONEY_COST, resources);
        ledger.applyOneTime(City.main.resources.ResourceType.FOOD, "action", getName(),
                ActionParams.IMPORT_FOOD_GAINED, resources);
        recordUse();
        return ActionResult.ok("Imported " + ActionParams.IMPORT_FOOD_GAINED + " food for "
                + ActionParams.IMPORT_FOOD_MONEY_COST + " money.");
    }

}