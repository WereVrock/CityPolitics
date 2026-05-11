package main.nobles;

import main.parameters.GameParameters;

import java.util.*;

/**
 * Manages all active claims across all houses.
 * Fabrication success is based on cunning skill.
 */
public class ClaimManager {

    private final List<Claim> claims = new ArrayList<>();

    // ─── Fabrication ─────────────────────────────────────────────────────────

    /**
     * Attempt to fabricate a claim. Returns true on success.
     * Success chance = base + cunning * per_cunning_bonus.
     */
    public boolean fabricate(String claimantId, String zoneId, int cunning, Random rng) {
        if (hasClaim(claimantId, zoneId)) return false;
        double chance = GameParameters.CLAIM_BASE_SUCCESS_CHANCE
            + cunning * GameParameters.CLAIM_CUNNING_BONUS_PER_POINT;
        if (rng.nextDouble() < chance) {
            claims.add(new Claim(claimantId, zoneId));
            return true;
        }
        return false;
    }

    // ─── Query ───────────────────────────────────────────────────────────────

    public boolean hasClaim(String claimantId, String zoneId) {
        for (Claim c : claims) {
            if (c.getClaimantId().equals(claimantId)
                    && c.getZoneId().equals(zoneId)) return true;
        }
        return false;
    }

    /** All claims held by a given house. */
    public List<Claim> getClaimsFor(String claimantId) {
        List<Claim> result = new ArrayList<>();
        for (Claim c : claims) {
            if (c.getClaimantId().equals(claimantId)) result.add(c);
        }
        return result;
    }

    /** All houses that hold a claim on any zone owned by this house. */
    public List<String> getClaimantsAgainst(String houseId, List<NobleHouse> allHouses) {
        Set<String> zoneIds = new HashSet<>();
        for (NobleHouse h : allHouses) {
            if (h.getId().equals(houseId)) {
                zoneIds.addAll(h.getZoneIds());
                break;
            }
        }
        List<String> result = new ArrayList<>();
        for (Claim c : claims) {
            if (zoneIds.contains(c.getZoneId())
                    && !c.getClaimantId().equals(houseId)) {
                result.add(c.getClaimantId());
            }
        }
        return result;
    }

    /** Remove all claims by a house on a specific zone (e.g. after conquest). */
    public void removeClaim(String claimantId, String zoneId) {
        claims.removeIf(c -> c.getClaimantId().equals(claimantId)
                          && c.getZoneId().equals(zoneId));
    }

    /** Remove all claims on a zone (e.g. after zone changes hands). */
    public void removeAllClaimsOnZone(String zoneId) {
        claims.removeIf(c -> c.getZoneId().equals(zoneId));
    }

    public void reset() { claims.clear(); }
}