// ===== DeclareProtectionAction.java (NEW) =====
package City.main.actions;

import City.main.nobles.NobleHouse;
import City.main.nobles.NobleHouseManager;
import City.main.nobles.ProtectionManager;
import City.main.nobles.RelationshipManager;
import City.main.nobles.Relationship;
 
import City.main.parameters.ProtectionParams;
import City.main.resources.ResourcePool;
import City.main.resources.StatBlock;

import java.util.List;

/**
 * Realm action — Declare a noble house under player protection.
 * Costs influence. Grants opinion bonus to target, malus to rivals.
 * Player suffers prestige penalty if the protected house loses a zone.
 */
public class DeclareProtectionAction extends AbstractAction {

    private final ProtectionManager  protectionManager;
    private final NobleHouseManager  nobleHouseManager;

    public interface ProtectionDialogCallback {
        void openDialog();
    }

    private ProtectionDialogCallback dialogCallback;

    public DeclareProtectionAction(ProtectionManager protectionManager,
                                    NobleHouseManager nobleHouseManager) {
        super(1);
        this.protectionManager = protectionManager;
        this.nobleHouseManager = nobleHouseManager;
    }

    public void setDialogCallback(ProtectionDialogCallback cb) {
        this.dialogCallback = cb;
    }

    @Override public String getName() { return "Declare Protection"; }

    @Override
    public String getDescription() {
        return "Place a noble house under your protection. Costs "
                + ProtectionParams.PROTECTION_INFLUENCE_COST + " influence. "
                + "+" + ProtectionParams.PROTECTION_TARGET_OPINION_BONUS + " target opinion, "
                + ProtectionParams.PROTECTION_RIVAL_OPINION_MALUS + " rival opinion. "
                + "You suffer prestige loss if they lose a zone.";
    }

    @Override
    public boolean isAvailable() { return super.isAvailable(); }

    @Override
    public ActionResult execute(ResourcePool resources, StatBlock stats) {
        if (!isAvailable()) {
            return ActionResult.fail("Declare Protection already used this turn.");
        }
        if (dialogCallback != null) {
            dialogCallback.openDialog();
        }
        recordUse();
        return ActionResult.ok("Protection dialog opened.");
    }

    /**
     * Called by dialog to apply protection to a specific house.
     */
    public ActionResult applyProtection(NobleHouse target, ResourcePool resources,
                                         City.main.ledger.Ledger ledger) {
        if (protectionManager.isUnderProtection(target.getId())) {
            return ActionResult.fail(target.getName() + " is already under your protection.");
        }
        if (resources.getInfluence() < ProtectionParams.PROTECTION_INFLUENCE_COST) {
            return ActionResult.fail("Not enough influence. Need "
                    + ProtectionParams.PROTECTION_INFLUENCE_COST + ".");
        }
        ledger.applyOneTime(City.main.resources.ResourceType.INFLUENCE, "realm", getName(),
                -ProtectionParams.PROTECTION_INFLUENCE_COST, resources);
        protectionManager.declareProtection(target.getId());
        target.adjustPlayerOpinion(ProtectionParams.PROTECTION_TARGET_OPINION_BONUS);

        RelationshipManager rm = nobleHouseManager.getRelationships();
        for (NobleHouse other : nobleHouseManager.getHouses()) {
            if (other == target || other.isEliminated()) continue;
            if (rm.get(target.getId(), other.getId()) == Relationship.RIVAL
                    || rm.get(target.getId(), other.getId()) == Relationship.HOSTILE) {
                other.adjustPlayerOpinion(ProtectionParams.PROTECTION_RIVAL_OPINION_MALUS);
            }
        }
        City.debug.Debug.log("realm-action", "declare-protection",
                target.getName() + " now under protection");
        return ActionResult.ok(target.getName() + " is now under your protection. Opinion +"
                + ProtectionParams.PROTECTION_TARGET_OPINION_BONUS + ".");
    }
}