package City.main.nobles.council;

import City.main.nobles.NobleHouse;
import City.main.parameters.GameParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Represents one active noble council session for a specific council action.
 */
public class CouncilSession {

    private final CouncilAction          action;
    private final List<CouncilVoter>     voters;
    private final List<CouncilDealOffer> deals; // one per voter (generated on demand)
    private       boolean                playerBoostUsed;

    public CouncilSession(CouncilAction action, List<CouncilVoter> voters) {
        this.action         = action;
        this.voters         = new ArrayList<>(voters);
        this.deals          = new ArrayList<>(Collections.nCopies(voters.size(), null));
        this.playerBoostUsed = false;
    }

    public CouncilAction      getAction()         { return action; }
    public List<CouncilVoter> getVoters()         { return Collections.unmodifiableList(voters); }
    public boolean            isPlayerBoostUsed() { return playerBoostUsed; }
    public void               markPlayerBoostUsed(){ playerBoostUsed = true; }

    public CouncilVoter getPlayerVoter() {
        for (CouncilVoter v : voters) {
            if (v.getType() == CouncilVoter.VoterType.PLAYER) return v;
        }
        return null;
    }

    public CouncilVoter getOracleVoter() {
        for (CouncilVoter v : voters) {
            if (v.getType() == CouncilVoter.VoterType.ORACLE) return v;
        }
        return null;
    }

    public List<CouncilVoter> getNoblevoters() {
        List<CouncilVoter> result = new ArrayList<>();
        for (CouncilVoter v : voters) {
            if (v.getType() == CouncilVoter.VoterType.PRESTIGIOUS_NOBLE
                    || v.getType() == CouncilVoter.VoterType.MINOR_NOBLE) {
                result.add(v);
            }
        }
        return result;
    }

    public CouncilDealOffer getDealOffer(CouncilVoter voter,
                                          City.main.nobles.ClaimManager claimManager,
                                          City.main.nobles.ProtectionManager protectionManager,
                                          List<NobleHouse> allHouses,
                                          Random rng) {
        int idx = voters.indexOf(voter);
        if (idx < 0) return null;
        if (deals.get(idx) == null) {
            deals.set(idx, CouncilDealOffer.generate(voter, claimManager,
                    protectionManager, allHouses, rng));
        }
        return deals.get(idx);
    }

    /** Total YES impression across all voters. */
    public int getTotalYes() {
        int total = 0;
        for (CouncilVoter v : voters) total += v.getYesImpression();
        return total;
    }

    /** Total NO impression across all voters. */
    public int getTotalNo() {
        int total = 0;
        for (CouncilVoter v : voters) total += v.getNoImpression();
        return total;
    }

    /** Total impression across all voters. */
    public int getTotalImpression() {
        int total = 0;
        for (CouncilVoter v : voters) total += v.getImpression();
        return total;
    }

    /** True if YES impression strictly exceeds NO impression. */
    public boolean isPassingCurrently() {
        return getTotalYes() > getTotalNo();
    }
}