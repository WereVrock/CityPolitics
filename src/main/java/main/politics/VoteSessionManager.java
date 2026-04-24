// VoteSessionManager.java
package main.politics;

import main.actions.FormalAction;
import main.resources.ResourcePool;
import main.resources.StatBlock;

import java.util.*;
import main.parameters.GameParameters;

/**
 * Creates voting sessions and resolves them into VoteResults.
 */
public class VoteSessionManager {

    private final VotingEngine engine = new VotingEngine();

    public VotingSession createSession(FormalAction action,
                                       List<PoliticalParty> parties,
                                       ResourcePool resources,
                                       StatBlock stats) {
        Map<PoliticalParty, Double> scores = new LinkedHashMap<>();
        for (PoliticalParty p : parties) {
            double score = engine.scoreForParty(p, action.getVoteConditions(), resources, stats);
            scores.put(p, score);
        }
        return new VotingSession(action, parties, scores);
    }

    public VoteResult finalize(VotingSession session, ResourcePool resources, StatBlock stats) {
        List<VoteScore> voteScores = new ArrayList<>();
        int totalYes = 0, totalNo = 0, totalAbstain = 0;

        // Player row: 1 seat
        int pYes = 0, pNo = 0, pAbs = 0;
        switch (session.getPlayerIntent()) {
            case YES     -> pYes = 1;
            case NO      -> pNo  = 1;
            default      -> pAbs = 1;
        }
        totalYes     += pYes;
        totalNo      += pNo;
        totalAbstain += pAbs;

        for (PoliticalParty party : session.getParties()) {
            VotingSession.PartyVoteIntent intent = session.getIntent(party);
            int seats = party.getSeats();
            int yes = 0, no = 0, abs = 0;
            switch (intent) {
                case YES     -> yes = seats;
                case NO      -> no  = seats;
                case ABSTAIN -> abs = seats;
                case UNKNOWN -> {
                    // random split for truly undecided at finalization
                    Random rng = new Random();
                    for (int i = 0; i < seats; i++) {
                        int r = rng.nextInt(3);
                        if      (r == 0) yes++;
                        else if (r == 1) no++;
                        else             abs++;
                    }
                }
            }
            totalYes     += yes;
            totalNo      += no;
            totalAbstain += abs;
            voteScores.add(new VoteScore(party, session.getScore(party), yes, no, abs));
        }

        return new VoteResult(voteScores, totalYes, totalNo, totalAbstain,
                              GameParameters.SEATS_NEEDED);
    }
}