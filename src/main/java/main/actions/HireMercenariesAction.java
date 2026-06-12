package main.actions;

import main.legislation.LegislationManager;
import main.legislation.LegislationType;
import main.parameters.GameParameters;
import main.resources.ResourcePool;
import main.resources.StatBlock;

/**
 * Hire Mercenaries — available only when legislation allows it (either via
 * the 3-turn window after a hire-vote passes, or permanent authorization).
 * Costs 3× normal recruitment cost.
 */
public class HireMercenariesAction extends AbstractAction {

    private final LegislationManager legislationManager;
    private final main.mercenaries.MercenaryManager mercenaryManager;
    private final main.map.ZoneManager zoneManager;

    // Callback so UI can open the hiring dialog
    public interface HireDialogCallback {
        void openHireDialog();
    }

    private HireDialogCallback hireDialogCallback;

    public HireMercenariesAction(LegislationManager legislationManager,
                                  main.mercenaries.MercenaryManager mercenaryManager,
                                  main.map.ZoneManager zoneManager) {
        super(1);
        this.legislationManager = legislationManager;
        this.mercenaryManager   = mercenaryManager;
        this.zoneManager        = zoneManager;
    }

    public void setHireDialogCallback(HireDialogCallback cb) {
        this.hireDialogCallback = cb;
    }

    @Override
    public String getName() { return "Hire Mercenaries"; }

    @Override
    public String getDescription() {
        int remaining = legislationManager.getMercenaryHireActionsRemaining();
        if (legislationManager.isMercenaryHireAuthorized()) {
            return "Open mercenary hiring. Cost: " + GameParameters.MERCENARY_COST_MULTIPLIER
                    + "× normal rates.";
        }
        return "Open mercenary hiring (" + remaining + " use(s) remaining this authorization)."
                + " Cost: " + GameParameters.MERCENARY_COST_MULTIPLIER + "× normal rates.";
    }

    @Override
    public boolean isAvailable() {
        return super.isAvailable() && legislationManager.hasMercenaryHireAvailable();
    }

    @Override
    public ActionResult execute(ResourcePool resources, StatBlock stats) {
        if (!isAvailable()) {
            return ActionResult.fail("Hire Mercenaries is not available.");
        }
        if (hireDialogCallback != null) {
            hireDialogCallback.openHireDialog();
        }
        recordUse();
        return ActionResult.ok("Mercenary hiring dialog opened.");
    }
}