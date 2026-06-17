package City.main.nobles.council;

import City.debug.Debug;
import City.main.army.ArmyManager;
import City.main.map.Zone;
import City.main.map.ZoneManager;
import City.main.nobles.NobleHouse;
import City.main.nobles.NobleHouseManager;
import City.main.parameters.CalendarParams;
 
import City.main.parameters.NobleAIParams;
import City.main.parameters.NobleCouncilParams;
import City.main.resources.ResourcePool;

import java.util.*;

/**
 * Builds council sessions, resolves their outcomes, and applies their effects.
 */
public class CouncilSessionManager {

    private final Random rng = new Random();

    // Active fortification support state
    private City.main.map.ZoneManager zoneManager;

    public void setZoneManager(City.main.map.ZoneManager zm) { this.zoneManager = zm; }

    private int fortificationSupportTurnsRemaining = 0;

    // Pending unlawful acquisition (zone must be returned)
    private String pendingUnlawfulZoneId   = null;
    private String pendingUnlawfulOwnerId  = null;
    private int    pendingUnlawfulTurns    = 0;
    private static final int UNLAWFUL_RETURN_TURNS =
            City.main.parameters.NobleCouncilParams.UNLAWFUL_RETURN_TURNS;

    // ── Session creation ──────────────────────────────────────────────────────

    /**
     * Builds a council session for the given action.
     * @param playerPrestige    player's current prestige
     * @param oracleOpinion     oracle party's opinion of player
     * @param allHouses         all noble houses
     */
    public CouncilSession createSession(CouncilAction action,
                                         int playerPrestige,
                                         int oracleOpinion,
                                         int trustBonus,
                                         List<NobleHouse> allHouses) {
        List<CouncilVoter> voters = new ArrayList<>();

        // Player voter — prestige + trust council bonus
        int playerImpression = playerPrestige + trustBonus;
        voters.add(new CouncilVoter("player", "You", CouncilVoter.VoterType.PLAYER,
                null, Math.max(1, playerImpression)));

        // Oracle voter
        int oracleImpression = NobleCouncilParams.COUNCIL_ORACLE_IMPRESSION;
        CouncilVoter oracleVoter = new CouncilVoter("oracle", "Arch Oracle Thessivane",
                CouncilVoter.VoterType.ORACLE, null, oracleImpression);
        oracleVoter.setStance(oracleOpinion >= 50
                ? CouncilVoter.Stance.YES : CouncilVoter.Stance.NO);
        voters.add(oracleVoter);

        // Prestigious noble houses
        int totalPrestige         = CouncilPrestigeEvaluator.getTotalPrestige(allHouses);
        int totalPrestigiousPrestige = CouncilPrestigeEvaluator
                .getTotalPrestigiousPrestige(allHouses, totalPrestige);

        for (NobleHouse house : allHouses) {
            if (house.isEliminated()) continue;
            if (!CouncilPrestigeEvaluator.isPrestigious(house, totalPrestige)) continue;
            double prestigeFraction = totalPrestigiousPrestige > 0
                    ? (double) house.getPrestige() / totalPrestigiousPrestige : 0;
            int impression = (int)(NobleCouncilParams.COUNCIL_PRESTIGIOUS_TOTAL_IMPRESSION
                    * prestigeFraction);
            impression = Math.max(1, impression);
            CouncilVoter voter = new CouncilVoter(house.getId(),
                    City.ui.GrantZoneClaimDialog.stripHousePrefix(house.getName()),
                    CouncilVoter.VoterType.PRESTIGIOUS_NOBLE, house, impression);
            voter.setStance(resolveNaturalStance(house, action, allHouses));
            voters.add(voter);
            Debug.log("council", "voter-prestigious",
                    house.getName() + " impression=" + impression
                    + " stance=" + voter.getStance());
        }

        // Minor / landless nobles (random 60–100 impression each)
        for (NobleHouse house : allHouses) {
            if (house.isEliminated() && house.getZoneIds().isEmpty()) {
                // landless
                int impression = 60 + rng.nextInt(41);
                CouncilVoter voter = new CouncilVoter(house.getId(),
                        City.ui.GrantZoneClaimDialog.stripHousePrefix(house.getName()),
                        CouncilVoter.VoterType.MINOR_NOBLE, house, impression);
                voter.setStance(CouncilVoter.Stance.UNDECIDED);
                voters.add(voter);
            } else if (!house.isEliminated()
                    && !CouncilPrestigeEvaluator.isPrestigious(house, totalPrestige)) {
                int impression = 60 + rng.nextInt(41);
                CouncilVoter voter = new CouncilVoter(house.getId(),
                        City.ui.GrantZoneClaimDialog.stripHousePrefix(house.getName()),
                        CouncilVoter.VoterType.MINOR_NOBLE, house, impression);
                voter.setStance(CouncilVoter.Stance.UNDECIDED);
                voters.add(voter);
            }
        }

        CouncilSession session = new CouncilSession(action, voters);
        // Player starts YES
        CouncilVoter playerVoter = session.getPlayerVoter();
        if (playerVoter != null) playerVoter.setStance(CouncilVoter.Stance.YES);
        return session;
    }

    // ── Natural stance resolution ─────────────────────────────────────────────

    private CouncilVoter.Stance resolveNaturalStance(NobleHouse house,
                                                       CouncilAction action,
                                                       List<NobleHouse> allHouses) {
        return switch (action) {
            case FORTIFICATION_SUPPORT  -> fortificationSupportStance(house);
            case BORDER_FORTIFICATION   -> borderFortificationStance(house, allHouses);
            case UNLAWFUL_ACQUISITION   -> CouncilVoter.Stance.UNDECIDED;
        };
    }

    private CouncilVoter.Stance fortificationSupportStance(NobleHouse house) {
        City.main.nobles.NobleCharacter ch = house.getActiveCharacter();
        if (ch == null) return CouncilVoter.Stance.UNDECIDED;
        // Expansionists in strong positions disagree
        if (ch.getDominantMotivation() == City.main.nobles.Motivation.EXPANSION
                && house.getPrestige() > 60
                && house.getZoneIds().size() >= 3) {
            return CouncilVoter.Stance.NO;
        }
        // Security-motivated houses strongly agree
        if (ch.getDominantMotivation() == City.main.nobles.Motivation.SECURITY) {
            return CouncilVoter.Stance.YES;
        }
        return CouncilVoter.Stance.UNDECIDED;
    }

    private CouncilVoter.Stance borderFortificationStance(NobleHouse house,
                                                            List<NobleHouse> allHouses) {
        // Would this house benefit? (owns a border zone)
        boolean benefits = false;
        for (String zoneId : house.getZoneIds()) {
            if (isAdjacentToDesolate(zoneId)) { benefits = true; break; }
        }
        if (benefits) return CouncilVoter.Stance.YES;

        // Check if a rival benefits — then this house opposes
        // (simplified: if no benefit and has rivals, lean NO)
        City.main.nobles.NobleCharacter ch = house.getActiveCharacter();
        if (ch != null && ch.getDominantMotivation() == City.main.nobles.Motivation.EXPANSION) {
            return CouncilVoter.Stance.NO;
        }
        return CouncilVoter.Stance.UNDECIDED;
    }

    private boolean isAdjacentToDesolate(String zoneId) {
        // This will be injected via the session builder; placeholder returns false
        // Real check done in NobleCouncilManager which has ZoneManager access
        return false;
    }

    // ── Outcome application ───────────────────────────────────────────────────

    /**
     * Apply the effect of a passed council vote.
     * @return log lines
     */
    public List<String> applyOutcome(CouncilSession session,
                                      CouncilAction action,
                                      String selectedZoneId,   // for UNLAWFUL_ACQUISITION
                                      NobleHouseManager houseManager,
                                      ZoneManager zoneManager,
                                      ArmyManager playerArmyManager,
                                      ResourcePool resources,
                                      City.main.nobles.PlayerPrestige playerPrestige,
                                      City.main.nobles.ProtectionManager protectionManager) {
        return switch (action) {
            case FORTIFICATION_SUPPORT ->
                applyFortificationSupport(houseManager, resources);
            case BORDER_FORTIFICATION ->
                applyBorderFortification(houseManager, zoneManager, resources);
            case UNLAWFUL_ACQUISITION ->
                applyUnlawfulAcquisition(selectedZoneId, houseManager,
                        playerArmyManager, playerPrestige);
        };
    }

    private List<String> applyFortificationSupport(NobleHouseManager houseManager,
                                                    ResourcePool resources) {
        List<String> log = new ArrayList<>();
        fortificationSupportTurnsRemaining =
                NobleCouncilParams.COUNCIL_FORTIFICATION_SUPPORT_YEARS
                * CalendarParams.PERIODS_PER_YEAR;
        log.add("⚑ Fortification Support declared. Player will pay half of noble fortification costs for "
                + NobleCouncilParams.COUNCIL_FORTIFICATION_SUPPORT_YEARS + " years.");
        return log;
    }

    private List<String> applyBorderFortification(NobleHouseManager houseManager,
                                                   ZoneManager zoneManager,
                                                   ResourcePool resources) {
        List<String> log = new ArrayList<>();
        int totalCost = 0;
        for (NobleHouse house : houseManager.getHouses()) {
            if (house.isEliminated()) continue;
            for (String zoneId : house.getZoneIds()) {
                Zone zone = zoneManager.getZone(zoneId);
                if (zone == null) continue;
                boolean border = false;
                for (String adjId : zone.getAdjacentIds()) {
                    Zone adj = zoneManager.getZone(adjId);
                    if (adj != null && adj.isDesolate()) { border = true; break; }
                }
                if (border) {
                    house.addFortification(zoneId, 1);
                    totalCost += NobleCouncilParams.COUNCIL_BORDER_FORT_COST_PER_ZONE;
                    log.add("  " + house.getName() + ": " + zoneId.replace("_", " ")
                            + " fortified (+1).");
                }
            }
        }
        int actualCost = Math.min(totalCost, resources.getMoney());
        resources.spendMoney(actualCost);
        log.add(0, "⚑ Border Fortification enacted. " + actualCost + " gold spent.");
        return log;
    }

private List<String> applyUnlawfulAcquisition(String zoneId,
                                               NobleHouseManager houseManager,
                                               ArmyManager playerArmyManager,
                                               City.main.nobles.PlayerPrestige playerPrestige) {
    List<String> log = new ArrayList<>();
    if (zoneId == null || zoneId.isBlank()) {
        log.add("⚠ No zone selected for unlawful acquisition.");
        return log;
    }

    NobleHouse owner = houseManager.getOwnerOfZone(zoneId);
    if (owner == null) {
        log.add("⚠ No noble house owns " + zoneId.replace("_", " ")
                + " — cannot declare unlawful acquisition.");
        return log;
    }

    // Check owner's army vs player
    int ownerArmy  = owner.getTotalGarrisonSize() + owner.getNobleManpower();
    int playerArmy = 0;
    for (City.main.army.Army a : playerArmyManager.getArmies()) playerArmy += a.getSize();

    double ratio = playerArmy > 0 ? (double) ownerArmy / playerArmy : 0;
    if (ratio >= NobleCouncilParams.COUNCIL_UNLAWFUL_REFUSE_THRESHOLD) {
        // Owner refuses — mark the zone as unlawfully acquired and start pressure
        pendingUnlawfulZoneId  = zoneId;
        pendingUnlawfulOwnerId = owner.getId();
        pendingUnlawfulTurns   = UNLAWFUL_RETURN_TURNS;
        // Mark the zone so battle-justification rules apply
        if (zoneManager != null) {
            City.main.map.ZoneState state = zoneManager.getState(zoneId);
            if (state != null) state.markUnlawfullyAcquired();
        }
        log.add("REFUSED");          // sentinel — UI reads this to show refusal dialog
        log.add(owner.getName());    // line 1: owner name
        log.add(zoneId);             // line 2: zone id
        return log;
    }

    // Transfer to a claimant
    List<NobleHouse> claimants = new ArrayList<>();
    for (NobleHouse h : houseManager.getHouses()) {
        if (h == owner || h.isEliminated()) continue;
        if (houseManager.getClaimManager().hasClaim(h.getId(), zoneId)) claimants.add(h);
    }

    NobleHouse recipient = claimants.isEmpty()
            ? null : claimants.get(rng.nextInt(claimants.size()));

    owner.removeZone(zoneId);
    if (recipient != null) {
        recipient.addZone(zoneId);
        recipient.adjustPlayerOpinion(NobleCouncilParams.COUNCIL_UNLAWFUL_RECIPIENT_OPINION);
        log.add("⚑ Unlawful Acquisition declared. "
                + zoneId.replace("_", " ") + " ceded from "
                + owner.getName() + " to " + recipient.getName() + ".");
    } else {
        log.add("⚑ Unlawful Acquisition declared. "
                + zoneId.replace("_", " ") + " stripped from "
                + owner.getName() + " — no claimant found, zone is ungoverned.");
    }
    owner.adjustPlayerOpinion(NobleCouncilParams.COUNCIL_UNLAWFUL_OWNER_OPINION);
    return log;
}

// ── Per-turn processing ───────────────────────────────────────────────────

public List<String> processTurn(NobleHouseManager houseManager,
                                     ResourcePool resources,
                                     City.main.nobles.PlayerPrestige playerPrestige) {
        List<String> log = new ArrayList<>();

        // Fortification support subsidy
        if (fortificationSupportTurnsRemaining > 0) {
            fortificationSupportTurnsRemaining--;
            int subsidy = 0;
            for (NobleHouse house : houseManager.getHouses()) {
                if (house.isEliminated()) continue;
                int houseFortCost = NobleAIParams.AI_FORTIFY_GOLD_COST / 2;
                if (house.getGold() >= houseFortCost) {
                    subsidy += houseFortCost;
                }
            }
            if (subsidy > 0) {
                resources.spendMoney(Math.min(subsidy, resources.getMoney()));
                log.add("⚑ Fortification subsidy paid: " + subsidy + " gold.");
            }
            if (fortificationSupportTurnsRemaining == 0) {
                log.add("⚑ Fortification Support period has ended.");
            }
        }

        // Unlawful acquisition pressure
        if (pendingUnlawfulZoneId != null) {
            NobleHouse owner = houseManager.getOwnerOfZone(pendingUnlawfulZoneId);
            if (owner == null || !owner.getId().equals(pendingUnlawfulOwnerId)) {
                pendingUnlawfulZoneId  = null;
                pendingUnlawfulOwnerId = null;
                pendingUnlawfulTurns   = 0;
            } else {
                pendingUnlawfulTurns--;
                playerPrestige.addPrestige(NobleCouncilParams.COUNCIL_UNLAWFUL_PRESTIGE_LOSS_PER_TURN);
                log.add("⚠ " + owner.getName() + " still holds "
                        + pendingUnlawfulZoneId.replace("_", " ")
                        + " in defiance. Prestige "
                        + NobleCouncilParams.COUNCIL_UNLAWFUL_PRESTIGE_LOSS_PER_TURN + ".");
                if (pendingUnlawfulTurns <= 0) {
                    log.addAll(forceTransfer(pendingUnlawfulZoneId, owner, houseManager));
                    pendingUnlawfulZoneId  = null;
                    pendingUnlawfulOwnerId = null;
                }
            }
        }

        return log;
    }

private List<String> forceTransfer(String zoneId, NobleHouse owner,
                                        NobleHouseManager houseManager) {
        List<String> log = new ArrayList<>();
        List<NobleHouse> claimants = new ArrayList<>();
        for (NobleHouse h : houseManager.getHouses()) {
            if (h == owner || h.isEliminated()) continue;
            if (houseManager.getClaimManager().hasClaim(h.getId(), zoneId)) claimants.add(h);
        }
        owner.removeZone(zoneId);
        if (!claimants.isEmpty()) {
            NobleHouse r = claimants.get(rng.nextInt(claimants.size()));
            r.addZone(zoneId);
            log.add("⚑ " + zoneId.replace("_", " ") + " forcibly transferred from "
                    + owner.getName() + " to " + r.getName() + ".");
        } else {
            log.add("⚑ " + zoneId.replace("_", " ") + " stripped from "
                    + owner.getName() + " — left ungoverned.");
        }
        return log;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public boolean isFortificationSupportActive() {
        return fortificationSupportTurnsRemaining > 0;
    }

    public int getFortificationSupportTurnsRemaining() {
        return fortificationSupportTurnsRemaining;
    }

    public boolean hasPendingUnlawfulAcquisition() {
        return pendingUnlawfulZoneId != null;
    }

    public String getPendingUnlawfulZoneId() { return pendingUnlawfulZoneId; }

    // ── Save/load ─────────────────────────────────────────────────────────────

    public void setFortificationSupportTurns(int v) { fortificationSupportTurnsRemaining = v; }
    public void setPendingUnlawful(String zoneId, String ownerId, int turns) {
        this.pendingUnlawfulZoneId  = zoneId;
        this.pendingUnlawfulOwnerId = ownerId;
        this.pendingUnlawfulTurns   = turns;
    }
    public String getPendingUnlawfulOwnerId() { return pendingUnlawfulOwnerId; }
    public int    getPendingUnlawfulTurns()   { return pendingUnlawfulTurns; }

    public void reset() {
        fortificationSupportTurnsRemaining = 0;
        pendingUnlawfulZoneId  = null;
        pendingUnlawfulOwnerId = null;
        pendingUnlawfulTurns   = 0;
    }
}