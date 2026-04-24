package main.politics;

import java.util.Collections;
import java.util.List;

/**
 * Immutable result of a full assembly vote.
 */
public class VoteResult {

    private final List<VoteScore> partyScores;
    private final int             totalYes;
    private final int             totalNo;
    private final int             totalAbstain;
    private final int             seatsNeeded;
    private final boolean         passed;

    public VoteResult(List<VoteScore> partyScores,
                      int totalYes, int totalNo, int totalAbstain,
                      int seatsNeeded) {
        this.partyScores  = partyScores;
        this.totalYes     = totalYes;
        this.totalNo      = totalNo;
        this.totalAbstain = totalAbstain;
        this.seatsNeeded  = seatsNeeded;
        this.passed       = totalYes > seatsNeeded;
    }

    public List<VoteScore> getPartyScores()  { return Collections.unmodifiableList(partyScores); }
    public int             getTotalYes()     { return totalYes; }
    public int             getTotalNo()      { return totalNo; }
    public int             getTotalAbstain() { return totalAbstain; }
    public int             getSeatsNeeded()  { return seatsNeeded; }
    public boolean         isPassed()        { return passed; }
}