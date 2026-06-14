package main.nobles.council;

import main.nobles.NobleHouse;
import main.parameters.GameParameters;

import java.util.Random;

/**
 * A deal request from a council voter.
 * Prestigious houses may ask for claims/protection; others ask for resources.
 */
public class CouncilDealOffer {

    public enum DealType {
        GOLD, INFLUENCE, MANPOWER,
        GRANT_CLAIM,         // grant claimant a claim on a zone
        REVOKE_RIVAL_CLAIM,  // remove another house's claim on voter's zone
        DECLARE_PROTECTION   // place voter under player protection
    }

    private final CouncilVoter voter;
    private final DealType     type;
    private final int          cost;       // for resource deals
    private final String       targetZoneId;  // for claim deals
    private final String       targetHouseId; // for revoke claim deals
    private final String       description;

    private CouncilDealOffer(CouncilVoter voter, DealType type, int cost,
                             String targetZoneId, String targetHouseId, String description) {
        this.voter         = voter;
        this.type          = type;
        this.cost          = cost;
        this.targetZoneId  = targetZoneId;
        this.targetHouseId = targetHouseId;
        this.description   = description;
    }

    // ── Factories ─────────────────────────────────────────────────────────────

    public static CouncilDealOffer gold(CouncilVoter voter, int amount) {
        return new CouncilDealOffer(voter, DealType.GOLD, amount, null, null,
                "Pay " + amount + " gold");
    }

    public static CouncilDealOffer influence(CouncilVoter voter, int amount) {
        return new CouncilDealOffer(voter, DealType.INFLUENCE, amount, null, null,
                "Pay " + amount + " influence");
    }

    public static CouncilDealOffer manpower(CouncilVoter voter, int amount) {
        return new CouncilDealOffer(voter, DealType.MANPOWER, amount, null, null,
                "Commit " + amount + " manpower");
    }

    public static CouncilDealOffer grantClaim(CouncilVoter voter, String zoneId) {
        return new CouncilDealOffer(voter, DealType.GRANT_CLAIM, 0, zoneId, null,
                "Grant claim on " + zoneId.replace("_", " "));
    }

    public static CouncilDealOffer revokeRivalClaim(CouncilVoter voter,
                                                     String zoneId, String rivalHouseId) {
        return new CouncilDealOffer(voter, DealType.REVOKE_RIVAL_CLAIM, 0,
                zoneId, rivalHouseId,
                "Revoke " + rivalHouseId + "'s claim on " + zoneId.replace("_", " "));
    }

    public static CouncilDealOffer declareProtection(CouncilVoter voter) {
        return new CouncilDealOffer(voter, DealType.DECLARE_PROTECTION, 0, null, null,
                "Declare " + voter.getDisplayName() + " under your protection");
    }

    // ── Generation ────────────────────────────────────────────────────────────

    /**
     * Generate an appropriate deal offer for this voter given the session context.
     */
    public static CouncilDealOffer generate(CouncilVoter voter,
                                             main.nobles.ClaimManager claimManager,
                                             main.nobles.ProtectionManager protectionManager,
                                             java.util.List<NobleHouse> allHouses,
                                             Random rng) {
        NobleHouse house = voter.getHouse();
        int impression   = voter.getImpression();

        // Scale cost to impression weight
        int baseGold      = (int)(impression * GameParameters.COUNCIL_DEAL_GOLD_PER_IMPRESSION);
        int baseInfluence = (int)(impression * GameParameters.COUNCIL_DEAL_INF_PER_IMPRESSION);
        int baseManpower  = (int)(impression * GameParameters.COUNCIL_DEAL_MP_PER_IMPRESSION);

        if (voter.getType() == CouncilVoter.VoterType.PRESTIGIOUS_NOBLE && house != null) {
            int roll = rng.nextInt(3);
            if (roll == 0) {
                // Try grant claim on one of their zones
                if (!house.getZoneIds().isEmpty()) {
                    String zoneId = house.getZoneIds().get(rng.nextInt(house.getZoneIds().size()));
                    return CouncilDealOffer.grantClaim(voter, zoneId);
                }
            } else if (roll == 1) {
                // Try revoke a rival's claim on their zone
                for (String zoneId : house.getZoneIds()) {
                    for (NobleHouse rival : allHouses) {
                        if (rival == house || rival.isEliminated()) continue;
                        if (claimManager.hasClaim(rival.getId(), zoneId)) {
                            return CouncilDealOffer.revokeRivalClaim(voter, zoneId, rival.getId());
                        }
                    }
                }
            } else if (roll == 2 && !protectionManager.isUnderProtection(house.getId())) {
                return CouncilDealOffer.declareProtection(voter);
            }
            // Fallback for prestigious to gold
            return CouncilDealOffer.gold(voter, baseGold);
        }

        // Minor nobles: resource deals only
        int resourceRoll = rng.nextInt(3);
        return switch (resourceRoll) {
            case 0 -> CouncilDealOffer.gold(voter, baseGold);
            case 1 -> CouncilDealOffer.influence(voter, baseInfluence);
            default -> CouncilDealOffer.manpower(voter, baseManpower);
        };
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public CouncilVoter getVoter()        { return voter; }
    public DealType     getType()         { return type; }
    public int          getCost()         { return cost; }
    public String       getTargetZoneId() { return targetZoneId; }
    public String       getTargetHouseId(){ return targetHouseId; }
    public String       getDescription()  { return description; }

    public boolean canAfford(main.resources.ResourcePool resources) {
        return switch (type) {
            case GOLD              -> resources.getMoney()     >= cost;
            case INFLUENCE         -> resources.getInfluence() >= cost;
            case MANPOWER          -> resources.getManpower()  >= cost;
            case GRANT_CLAIM,
                 REVOKE_RIVAL_CLAIM,
                 DECLARE_PROTECTION -> true; // no resource cost
        };
    }
}