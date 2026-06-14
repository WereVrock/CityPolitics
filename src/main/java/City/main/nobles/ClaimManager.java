package City.main.nobles;

import City.main.parameters.GameParameters;

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
     * Success chance = base + cunning * per_cunning_bonus, halved if not adjacent.
     * @param claimantZones zones owned by the claimant (for adjacency check)
     * @param allZones list of all Zone objects (for adjacency data)
     */
    public boolean fabricate(String claimantId, String zoneId, int cunning, int ownerCunning,
                              Random rng, List<String> claimantZones, List<City.main.map.Zone> allZones) {
        if (hasClaim(claimantId, zoneId)) return false;
        double chance = GameParameters.CLAIM_BASE_SUCCESS_CHANCE
            + cunning * GameParameters.CLAIM_CUNNING_BONUS_PER_POINT
            - ownerCunning * GameParameters.CLAIM_OWNER_CUNNING_PENALTY_PER_POINT;
        if (chance <= 0) return false;

        // Adjacency penalty: halve chance if target not adjacent to any owned zone
        boolean adjacent = false;
        City.main.map.Zone targetZone = null;
        for (City.main.map.Zone z : allZones) {
            if (z.getId().equals(zoneId)) { targetZone = z; break; }
        }
        if (targetZone != null && !claimantZones.isEmpty()) {
            for (String adjId : targetZone.getAdjacentIds()) {
                if (claimantZones.contains(adjId)) { adjacent = true; break; }
            }
        }
        if (!adjacent) {
            chance *= GameParameters.CLAIM_ADJACENCY_PENALTY;
        }

        if (rng.nextDouble() < chance) {
            claims.add(new Claim(claimantId, zoneId));
            return true;
        }
        return false;
    }

    /** Directly add a claim without a fabrication check. Used for automatic loser claims. */
    public void addClaim(String claimantId, String zoneId) {
        if (!hasClaim(claimantId, zoneId)) {
            claims.add(new Claim(claimantId, zoneId));
        }
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

    // ─── Claim decay ─────────────────────────────────────────────────────────

    /**
     * Roll for claim decay on a house. Returns a random claim the house must defend
     * (pay influence to keep), or null if no decay triggers.
     */
    public Claim rollClaimDecay(String houseId, Random rng) {
        List<Claim> houseClaims = getClaimsFor(houseId);
        if (houseClaims.isEmpty()) return null;
        if (rng.nextDouble() >= GameParameters.CLAIM_DECAY_CHANCE) return null;
        return houseClaims.get(rng.nextInt(houseClaims.size()));
    }
}