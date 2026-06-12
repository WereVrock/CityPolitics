package main.actions;

import main.nobles.Claim;
import main.nobles.ClaimManager;
import main.nobles.NobleHouse;
import main.nobles.NobleHouseManager;
import main.parameters.GameParameters;
import main.resources.ResourcePool;
import main.resources.StatBlock;

import java.util.List;

/**
 * Realm action — Grant Zone Claim to a Noble.
 * Player picks a zone and a noble to receive the claim.
 * Owner gets large opinion malus, target gets bonus, other claimants get minor malus.
 */
public class GrantZoneClaimAction extends AbstractAction {

    private final NobleHouseManager nobleHouseManager;
    private final ClaimManager      claimManager;

    public interface GrantClaimDialogCallback {
        void openDialog();
    }

    private GrantClaimDialogCallback dialogCallback;

    public GrantZoneClaimAction(NobleHouseManager nobleHouseManager,
                                 ClaimManager claimManager) {
        super(1);
        this.nobleHouseManager = nobleHouseManager;
        this.claimManager      = claimManager;
    }

    public void setDialogCallback(GrantClaimDialogCallback cb) {
        this.dialogCallback = cb;
    }

    @Override public String getName() { return "Grant Zone Claim"; }

    @Override
    public String getDescription() {
        return "Grant a claim on a zone to a noble house. "
                + "Owner suffers opinion loss; target gains opinion.";
    }

    @Override
    public boolean isAvailable() { return super.isAvailable(); }

    @Override
    public ActionResult execute(ResourcePool resources, StatBlock stats) {
        if (!isAvailable()) {
            return ActionResult.fail("Grant Zone Claim already used this turn.");
        }
        if (dialogCallback != null) {
            dialogCallback.openDialog();
        }
        recordUse();
        return ActionResult.ok("Grant Zone Claim dialog opened.");
    }

    /**
     * Called by dialog when player confirms grant.
     */
    public ActionResult grantClaim(String zoneId, NobleHouse target) {
        if (claimManager.hasClaim(target.getId(), zoneId)) {
            return ActionResult.fail(target.getName() + " already has a claim on this zone.");
        }

        claimManager.addClaim(target.getId(), zoneId);

        // Opinion effects
        NobleHouse owner = nobleHouseManager.getOwnerOfZone(zoneId);
        if (owner != null && owner != target) {
            owner.adjustPlayerOpinion(GameParameters.GRANT_CLAIM_OWNER_OPINION_MALUS);
            debug.Debug.log("realm-action", "grant-claim",
                    "Owner " + owner.getName() + " opinion " + GameParameters.GRANT_CLAIM_OWNER_OPINION_MALUS);
        }

        target.adjustPlayerOpinion(GameParameters.GRANT_CLAIM_TARGET_OPINION_BONUS);
        debug.Debug.log("realm-action", "grant-claim",
                "Target " + target.getName() + " opinion +" + GameParameters.GRANT_CLAIM_TARGET_OPINION_BONUS);

        // Other claimants get minor malus
        for (NobleHouse house : nobleHouseManager.getHouses()) {
            if (house == target || house.isEliminated()) continue;
            if (claimManager.hasClaim(house.getId(), zoneId)) {
                house.adjustPlayerOpinion(GameParameters.GRANT_CLAIM_OTHER_CLAIMANT_MALUS);
                debug.Debug.log("realm-action", "grant-claim",
                        "Other claimant " + house.getName()
                        + " opinion " + GameParameters.GRANT_CLAIM_OTHER_CLAIMANT_MALUS);
            }
        }

        String ownerMsg = owner != null
                ? " " + owner.getName() + " is displeased ("
                  + GameParameters.GRANT_CLAIM_OWNER_OPINION_MALUS + " opinion)." : "";
        return ActionResult.ok("Granted claim on " + zoneId + " to " + target.getName()
                + ". Opinion +" + GameParameters.GRANT_CLAIM_TARGET_OPINION_BONUS + "."
                + ownerMsg);
    }
}