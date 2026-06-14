package City.main.actions;

import City.main.legislation.LegislationManager;
import City.main.nobles.NobleHouse;
import City.main.nobles.NobleHouseManager;
import City.main.parameters.GameParameters;
import City.main.resources.ResourcePool;
import City.main.resources.StatBlock;

/**
 * Realm action — Send Resources to Nobles.
 * Available for SEND_RESOURCES_WINDOW_TURNS turns after Allow Sending Resources passes.
 * Opens a dialog to choose house and amount. Increases their opinion.
 */
public class SendResourcesToNoblesAction extends AbstractAction {

    private final LegislationManager  legislationManager;
    private final NobleHouseManager   nobleHouseManager;

    public interface SendResourcesDialogCallback {
        void openDialog();
    }

    private SendResourcesDialogCallback dialogCallback;

    public SendResourcesToNoblesAction(LegislationManager legislationManager,
                                       NobleHouseManager nobleHouseManager) {
        super(1);
        this.legislationManager = legislationManager;
        this.nobleHouseManager  = nobleHouseManager;
    }

    public void setDialogCallback(SendResourcesDialogCallback cb) {
        this.dialogCallback = cb;
    }

    @Override public String getName() { return "Send Resources to Nobles"; }

    @Override
    public String getDescription() {
        int remaining = legislationManager.getSendResourcesWindowRemaining();
        return "Send gold to a noble house to improve relations. ("
                + remaining + " use(s) remaining this authorisation).";
    }

    @Override
    public boolean isAvailable() {
        return super.isAvailable() && legislationManager.hasSendResourcesAvailable();
    }

    @Override
    public ActionResult execute(ResourcePool resources, StatBlock stats) {
        if (!isAvailable()) {
            return ActionResult.fail("Sending resources to nobles is not currently authorised.");
        }
        if (dialogCallback != null) {
            dialogCallback.openDialog();
        }
        recordUse();
        return ActionResult.ok("Resource sending dialog opened.");
    }

    /**
     * Called by dialog when player confirms a transfer.
     */
    public ActionResult sendGold(NobleHouse house, int amount,
                                  ResourcePool resources, City.main.ledger.Ledger ledger) {
        if (resources.getMoney() < amount) {
            return ActionResult.fail("Not enough gold.");
        }
        ledger.applyOneTime(City.main.resources.ResourceType.GOLD, "realm", getName(), -amount, resources);
        house.addGold(amount);
        int opinionGain = (amount / GameParameters.SEND_RESOURCES_OPINION_DIVISOR)
                * GameParameters.SEND_RESOURCES_OPINION_PER_GOLD;
        opinionGain = Math.max(1, opinionGain);
        house.adjustPlayerOpinion(opinionGain);
        City.debug.Debug.log("realm-action", "send-resources",
                house.getName() + " gold=" + amount + " opinionGain=" + opinionGain);
        return ActionResult.ok("Sent " + amount + " gold to " + house.getName()
                + ". Opinion +" + opinionGain + ".");
    }
}