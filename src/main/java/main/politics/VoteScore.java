package main.politics;

/**
 * Immutable result of a single party's vote on a formal action.
 * Contains the raw score and the seat split.
 */
public class VoteScore {

    private final PoliticalParty party;
    private final double         score;
    private final int            yesSeats;
    private final int            noSeats;
    private final int            abstainSeats;

    public VoteScore(PoliticalParty party, double score,
                     int yesSeats, int noSeats, int abstainSeats) {
        this.party       = party;
        this.score       = score;
        this.yesSeats    = yesSeats;
        this.noSeats     = noSeats;
        this.abstainSeats = abstainSeats;
    }

    public PoliticalParty getParty()       { return party; }
    public double         getScore()       { return score; }
    public int            getYesSeats()    { return yesSeats; }
    public int            getNoSeats()     { return noSeats; }
    public int            getAbstainSeats(){ return abstainSeats; }

    public String getSummary() {
        return String.format("%s → YES: %d  NO: %d  ABSTAIN: %d  (score: %.2f)",
            party.getName(), yesSeats, noSeats, abstainSeats, score);
    }
}