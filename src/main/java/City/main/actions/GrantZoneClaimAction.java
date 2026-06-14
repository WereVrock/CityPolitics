package City.main.actions;

import City.main.nobles.Claim;
import City.main.nobles.ClaimManager;
import City.main.nobles.NobleHouse;
import City.main.nobles.NobleHouseManager;
import City.main.parameters.GameParameters;
import City.main.resources.ResourcePool;
import City.main.resources.StatBlock;

import java.util.List;

/**
 * Realm action — Grant Zone Claim to a Noble.
 * Costs 5 influence. Opens dialog. Use is only consumed when a claim is actually granted.
 */
public class GrantZoneClaimAction extends AbstractAction {

    private final NobleHouseManager nobleHouseManager;
    private final ClaimManager      claimManager;

    public interface GrantClaimDialogCallback {
        /** Return true if a grant was actually made (consumes use), false if cancelled. */
        boolean openDialog();
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
        return "Grant a claim on a zone to a noble house. Costs "
                + GameParameters.GRANT_CLAIM_INFLUENCE_COST
                + " influence. Owner suffers opinion loss; target gains opinion.";
    }

    @Override
    public boolean isAvailable() { return super.isAvailable(); }

    @Override
    public ActionResult execute(ResourcePool resources, StatBlock stats) {
        if (!isAvailable()) {
            return ActionResult.fail("Grant Zone Claim already used this turn.");
        }
        if (dialogCallback != null) {
            boolean granted = dialogCallback.openDialog();
            if (granted) {
                recordUse();
                return ActionResult.ok("Claim granted successfully.");
            } else {
                // Cancelled — do NOT consume use
                return ActionResult.ok("Grant Zone Claim cancelled.");
            }
        }
        recordUse();
        return ActionResult.ok("Grant Zone Claim dialog opened.");
    }

    /**
     * Called by dialog when player confirms grant.
     * Deducts influence cost, applies opinion changes, adds claim.
     */
    public ActionResult grantClaim(String zoneId, NobleHouse target,
                                    ResourcePool resources, City.main.ledger.Ledger ledger) {
        if (resources.getInfluence() < GameParameters.GRANT_CLAIM_INFLUENCE_COST) {
            return ActionResult.fail("Not enough influence. Need "
                    + GameParameters.GRANT_CLAIM_INFLUENCE_COST + ".");
        }
        if (claimManager.hasClaim(target.getId(), zoneId)) {
            return ActionResult.fail(target.getName() + " already has a claim on this zone.");
        }
        ledger.applyOneTime(City.main.resources.ResourceType.INFLUENCE, "realm", getName(),
                -GameParameters.GRANT_CLAIM_INFLUENCE_COST, resources);

        claimManager.addClaim(target.getId(), zoneId);

        NobleHouse owner = nobleHouseManager.getOwnerOfZone(zoneId);
        if (owner != null && owner != target) {
            owner.adjustPlayerOpinion(GameParameters.GRANT_CLAIM_OWNER_OPINION_MALUS);
        }
        target.adjustPlayerOpinion(GameParameters.GRANT_CLAIM_TARGET_OPINION_BONUS);

        for (NobleHouse house : nobleHouseManager.getHouses()) {
            if (house == target || house.isEliminated()) continue;
            if (claimManager.hasClaim(house.getId(), zoneId)) {
                house.adjustPlayerOpinion(GameParameters.GRANT_CLAIM_OTHER_CLAIMANT_MALUS);
            }
        }

        String ownerMsg = owner != null
                ? " " + owner.getName() + " is displeased ("
                  + GameParameters.GRANT_CLAIM_OWNER_OPINION_MALUS + " opinion)." : "";
        City.debug.Debug.log("realm-action", "grant-claim",
                zoneId + " → " + target.getName());
        return ActionResult.ok("Granted claim on " + zoneId + " to "
                + target.getName() + ". Opinion +"
                + GameParameters.GRANT_CLAIM_TARGET_OPINION_BONUS + "." + ownerMsg);
    }
}