// VotingSession.java
package main.politics;

import main.actions.FormalAction;
import java.util.*;
import main.parameters.GameParameters;

/**
 * Holds the state of a pending assembly vote.
 * Created when a formal action is triggered. Finalized when player confirms.
 */
public class VotingSession {

    public enum PartyVoteIntent { YES, NO, ABSTAIN, UNKNOWN }

    private final FormalAction             action;
    private final List<PoliticalParty>     parties;
    private final Map<PoliticalParty, Double>          scores;
    private final Map<PoliticalParty, PartyVoteIntent> intents;
    private final Map<PoliticalParty, Boolean>         dealt;
    private final Map<PoliticalParty, Integer>         favour;

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
        }
    }

    private PartyVoteIntent resolveIntent(double score) {
        if (score > GameParameters.VOTE_INDECISIVE_THRESHOLD)  return PartyVoteIntent.YES;
        if (score < -GameParameters.VOTE_INDECISIVE_THRESHOLD) return PartyVoteIntent.NO;
        return PartyVoteIntent.UNKNOWN;
    }

    public void applyDeal(PoliticalParty party) {
        intents.put(party, PartyVoteIntent.YES);
        dealt.put(party, true);
        favour.put(party, favour.get(party) - 1);
        party.setFavour(party.getFavour() - 1);
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
}