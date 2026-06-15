package City.main.politics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Immutable record of a single election's outcome.
 */
public class ElectionRecord {

    public static class PartyResult {
        public final String  partyName;
        public final int     seatsAfter;
        public final int     seatsBefore;
        public final int     naturalVotes;
        public final int     boughtVotes;
        public final int     stolenFromVotes;  // votes stolen away FROM this party
        public final int     stolenToVotes;    // votes stolen TO this party
        public final int     totalVotes;
        public final double  votePct;
        public final int     powerBefore;
        public final int     powerAfter;
        public final boolean isFixedSeat;

        public PartyResult(String partyName, int seatsAfter, int seatsBefore,
                           int naturalVotes, int boughtVotes, int stolenFromVotes,
                           int totalVotes, double votePct,
                           int powerBefore, int powerAfter, boolean isFixedSeat) {
            this.partyName       = partyName;
            this.seatsAfter      = seatsAfter;
            this.seatsBefore     = seatsBefore;
            this.naturalVotes    = naturalVotes;
            this.boughtVotes     = boughtVotes;
            this.stolenFromVotes = stolenFromVotes;
            // stolenToVotes = what total would be if only natural + bought, vs actual
            this.stolenToVotes   = Math.max(0, totalVotes - naturalVotes - boughtVotes + stolenFromVotes);
            this.totalVotes      = totalVotes;
            this.votePct         = votePct;
            this.powerBefore     = powerBefore;
            this.powerAfter      = powerAfter;
            this.isFixedSeat     = isFixedSeat;
        }

        public int seatDelta()  { return seatsAfter - seatsBefore; }
        public int powerDelta() { return powerAfter - powerBefore; }
    }

    public static class AffiliationChange {
        public final String  popTypeName;
        public final String  oldAffiliation;
        public final String  newAffiliation;
        public final boolean gained;

        public AffiliationChange(String popTypeName, String oldAffiliation,
                                  String newAffiliation, boolean gained) {
            this.popTypeName    = popTypeName;
            this.oldAffiliation = oldAffiliation;
            this.newAffiliation = newAffiliation;
            this.gained         = gained;
        }
    }

    private final int    year;
    private final String period;
    private final int    totalVotesCast;
    private final int    corruption;
    private final int    stolenVotesTotal;
    private final double stolenVotesPct;
    private final List<PartyResult>       partyResults;
    private final List<AffiliationChange> affiliationChanges;
    private final Map<String, Double>     propagandaSpent;

    public ElectionRecord(int year, String period, int totalVotesCast,
                          int corruption, int stolenVotesTotal,
                          List<PartyResult> partyResults,
                          List<AffiliationChange> affiliationChanges,
                          Map<String, Double> propagandaSpent) {
        this.year               = year;
        this.period             = period;
        this.totalVotesCast     = totalVotesCast;
        this.corruption         = corruption;
        this.stolenVotesTotal   = stolenVotesTotal;
        this.stolenVotesPct     = totalVotesCast > 0
                ? (double) stolenVotesTotal / totalVotesCast * 100.0 : 0;
        this.partyResults       = Collections.unmodifiableList(new ArrayList<>(partyResults));
        this.affiliationChanges = Collections.unmodifiableList(new ArrayList<>(affiliationChanges));
        this.propagandaSpent    = Collections.unmodifiableMap(new LinkedHashMap<>(propagandaSpent));
    }

    public int    getYear()                { return year; }
    public String getPeriod()              { return period; }
    public int    getTotalVotesCast()      { return totalVotesCast; }
    public int    getCorruption()          { return corruption; }
    public int    getStolenVotesTotal()    { return stolenVotesTotal; }
    public double getStolenVotesPct()      { return stolenVotesPct; }

    public List<PartyResult>       getPartyResults()       { return partyResults; }
    public List<AffiliationChange> getAffiliationChanges() { return affiliationChanges; }
    public Map<String, Double>     getPropagandaSpent()    { return propagandaSpent; }

    public PartyResult getWinner() {
        PartyResult best = null;
        for (PartyResult r : partyResults) {
            if (r.isFixedSeat) continue;
            if (best == null || r.seatsAfter > best.seatsAfter) best = r;
        }
        return best;
    }
}