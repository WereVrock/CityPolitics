package main.actions;

/**
 * Handles use-counting so concrete actions only implement execute().
 */
public abstract class AbstractAction implements PlayerAction {

    private final int maxUsesPerTurn;
    private int usesThisTurn;

    protected AbstractAction(int maxUsesPerTurn) {
        this.maxUsesPerTurn = maxUsesPerTurn;
        this.usesThisTurn   = 0;
    }

    @Override
    public int getMaxUsesPerTurn() { return maxUsesPerTurn; }

    @Override
    public int getUsesThisTurn()   { return usesThisTurn; }

    @Override
    public boolean isAvailable()   { return usesThisTurn < maxUsesPerTurn; }

    @Override
    public void resetUses()        { usesThisTurn = 0; }

    /**
     * Increments the use counter. Call this inside execute() on success.
     */
    protected void recordUse()     { usesThisTurn++; }
}