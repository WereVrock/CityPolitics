package main.politics;

import main.actions.FormalAction;
import java.util.*;
import main.parameters.GameParameters;

/**
 * Holds the state of a pending assembly vote.
 * Now also supports side-deal negotiations with secondary leaders.
 */
public class VotingSession {

    public enum PartyVoteIntent { YES, NO, ABSTAIN, UNKNOWN }

    private final FormalAction             action;
    private final List<PoliticalParty>     parties;
    private final Map<PoliticalParty, Double>          scores;
    private final Map<PoliticalParty, PartyVoteIntent> intents;
    private final Map<PoliticalParty, Boolean>         dealt;
    private final Map<PoliticalParty, Integer>         favour;

    // Side deal state: tracks side-deal seats won per party
    private final Map<PoliticalParty, Integer> sideDealtSeats = new LinkedHashMap<>();

    private PartyVoteIntent playerIntent = PartyVoteIntent.YES;

    public VotingSession(FormalAction action,
                         List<PoliticalParty> parties,
                         Map<PoliticalParty, Double> scores) {
        this.action  = action;
        this.parties = new ArrayList<>(parties);
        this.scores  = new LinkedHashMap<>(scores);
        this.intents = new LinkedHashMap<>();
        this.dealt   = new LinkedHashMap<>();
        this.favour  = new LinkedHashMap<>();

        for (PoliticalParty p : parties) {
            intents.put(p, resolveIntent(scores.get(p)));
            dealt.put(p, false);
            favour.put(p, p.getFavour());
            sideDealtSeats.put(p, 0);
        }
    }

    /** Restore constructor */
    public VotingSession(FormalAction action,
                         List<PoliticalParty> parties,
                         Map<PoliticalParty, Double> scores,
                         Map<PoliticalParty, PartyVoteIntent> intents,
                         Map<PoliticalParty, Boolean> dealt,
                         PartyVoteIntent playerIntent) {
        this.action       = action;
        this.parties      = new ArrayList<>(parties);
        this.scores       = new LinkedHashMap<>(scores);
        this.intents      = new LinkedHashMap<>(intents);
        this.dealt        = new LinkedHashMap<>(dealt);
        this.playerIntent = playerIntent;
        this.favour       = new LinkedHashMap<>();
        for (PoliticalParty p : parties) {
            this.favour.put(p, p.getFavour());
            this.sideDealtSeats.put(p, 0);
        }
    }

    private PartyVoteIntent resolveIntent(double score) {
        if (score > GameParameters.VOTE_INDECISIVE_THRESHOLD)  return PartyVoteIntent.YES;
        if (score < -GameParameters.VOTE_INDECISIVE_THRESHOLD) return PartyVoteIntent.NO;
        return PartyVoteIntent.UNKNOWN;
    }

    // ─── Main deal ────────────────────────────────────────────────────────────

public void applyDeal(PoliticalParty party) {
    debug.Debug.log("voting", "main-deal", party.getName()
            + " — full party converted to YES");
    intents.put(party, PartyVoteIntent.YES);
    dealt.put(party, true);
    favour.put(party, favour.get(party) - 1);
    party.setFavour(party.getFavour() - 1);
}

// ─── Side deal ────────────────────────────────────────────────────────────

    /**
     * Attempt a side deal with the secondary leader of a party.
     * Cost ≈ half the main deal. Convinces 1 to 75% of the party's seats randomly.
     * Returns the number of seats won. 0 means the secondary leader failed to
     * convince anyone (still possible to try once per session).
     */

public SideDealResult applySideDeal(PoliticalParty party,
                                     main.resources.ResourcePool resources,
                                     main.resources.StatBlock stats) {
    if (hasSideDealt(party)) {
        debug.Debug.log("voting", "side-deal-blocked", party.getName() + " already side-dealt");
        return new SideDealResult(0, "Already negotiated with secondary leader.");
    }
    DealOffer mainOffer  = new DealOffer(party, scores.getOrDefault(party, 0.0));
    // Half of main deal, but never below minimums
    int goldCost      = Math.max(GameParameters.DEAL_MIN_MONEY / 2 + 1,
                                  mainOffer.getMoneyCost() / 2);
    int influenceCost = Math.max(GameParameters.DEAL_MIN_INFLUENCE / 2 + 1,
                                  mainOffer.getInfluenceCost() / 2);

    debug.Debug.log("voting", "side-deal-attempt", party.getName()
            + " goldCost=" + goldCost + " influenceCost=" + influenceCost
            + " available gold=" + resources.getMoney()
            + " available influence=" + resources.getInfluence());

    if (resources.getMoney() < goldCost || resources.getInfluence() < influenceCost) {
        debug.Debug.log("voting", "side-deal-failed", party.getName() + " — cannot afford");
        return new SideDealResult(0, "Cannot afford side deal.");
    }

    resources.spendMoney(goldCost);
    resources.spendInfluence(influenceCost);

    int maxSeats = Math.max(1, (int)(party.getSeats() * 0.75));
    int seatsWon = 1 + (int)(Math.random() * maxSeats);
    seatsWon     = Math.min(seatsWon, party.getSeats());

    sideDealtSeats.put(party, seatsWon);
    dealt.put(party, true);

    debug.Debug.log("voting", "side-deal-success", party.getName()
            + " seatsWon=" + seatsWon + " of " + party.getSeats()
            + " goldCost=" + goldCost + " influenceCost=" + influenceCost);

    return new SideDealResult(seatsWon,
            "The secondary leader convinced " + seatsWon + " of "
            + party.getSeats() + " seats to vote with you.");
}

public boolean hasSideDealt(PoliticalParty party) {
        return dealt.getOrDefault(party, false)
                && sideDealtSeats.getOrDefault(party, 0) > 0;
    }

    public int getSideDealtSeats(PoliticalParty party) {
        return sideDealtSeats.getOrDefault(party, 0);
    }

    public void syncOraclesWithPlayer(PoliticalParty oracles) {
        intents.put(oracles, playerIntent == PartyVoteIntent.ABSTAIN
            ? PartyVoteIntent.ABSTAIN : playerIntent);
    }

    public boolean canDeal(PoliticalParty party) {
        double score = scores.getOrDefault(party, 0.0);
        return Math.abs(score) < GameParameters.VOTE_DEAL_LOCK_THRESHOLD;
    }

    public boolean hasDealt(PoliticalParty party) {
        return dealt.getOrDefault(party, false);
    }

    public FormalAction            getAction()                        { return action; }
    public List<PoliticalParty>    getParties()                       { return Collections.unmodifiableList(parties); }
    public double                  getScore(PoliticalParty p)         { return scores.getOrDefault(p, 0.0); }
    public PartyVoteIntent         getIntent(PoliticalParty p)        { return intents.getOrDefault(p, PartyVoteIntent.UNKNOWN); }
    public void                    setIntent(PoliticalParty p, PartyVoteIntent i) { intents.put(p, i); }
    public PartyVoteIntent         getPlayerIntent()                  { return playerIntent; }
    public void                    setPlayerIntent(PartyVoteIntent i) { playerIntent = i; }
    public int                     getFavourOwed(PoliticalParty p)    { return favour.getOrDefault(p, 0); }

    // ─── Side deal result record ──────────────────────────────────────────────

    public static class SideDealResult {
        public final int    seatsWon;
        public final String message;
        public SideDealResult(int seatsWon, String message) {
            this.seatsWon = seatsWon;
            this.message  = message;
        }
    }
}