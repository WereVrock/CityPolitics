package City.main.nobles;

/**
 * A fabricated claim on a specific zone held by a house.
 */
public class Claim {

    private final String claimantId; // house that holds the claim
    private final String zoneId;     // zone being claimed

    public Claim(String claimantId, String zoneId) {
        this.claimantId = claimantId;
        this.zoneId     = zoneId;
    }

    public String getClaimantId() { return claimantId; }
    public String getZoneId()     { return zoneId; }
}