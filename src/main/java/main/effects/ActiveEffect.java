package main.effects;

/**
 * A single active effect that decays over turns.
 * Decay is percentage-based per turn.
 */
public class ActiveEffect {

    public enum Type {
        HAPPINESS_BOOST
    }

    private final Type   type;
    private       double remainingAmount;
    private       int    turnsRemaining;
    private final double decayRate; // fraction lost per turn e.g. 0.20 = 20%

    public ActiveEffect(Type type, double initialAmount, int durationTurns) {
        this.type             = type;
        this.remainingAmount  = initialAmount;
        this.turnsRemaining   = durationTurns;
        this.decayRate        = 1.0 - Math.pow(0.0001, 1.0 / durationTurns);
    }

    /**
     * Restores a saved effect. DecayRate is recalculated from remaining turns so decay
     * continues at the same rate it was on when saved.
     */
    public ActiveEffect(Type type, double remainingAmount, int turnsRemaining, boolean restored) {
        this.type            = type;
        this.remainingAmount = remainingAmount;
        this.turnsRemaining  = turnsRemaining;
        this.decayRate       = turnsRemaining > 0
            ? 1.0 - Math.pow(0.0001, 1.0 / turnsRemaining)
            : 1.0;
    }

    /**
     * Applies one turn of decay. Returns the delta applied this turn (positive = gain).
     */
    public double decay() {
        double delta = remainingAmount * decayRate;
        remainingAmount -= delta;
        turnsRemaining--;
        return delta;
    }

    public boolean isExpired()        { return turnsRemaining <= 0; }
    public Type    getType()          { return type; }
    public double  getRemainingAmount(){ return remainingAmount; }
    public int     getTurnsRemaining() { return turnsRemaining; }
}