package City.main.actions;

/**
 * Handles use-counting so concrete actions only implement execute().
 */
public abstract class AbstractAction implements PlayerAction {

    private final int maxUsesPerTurn;
    private int usesThisTurn;
    private City.main.ledger.Ledger ledger;

    protected AbstractAction(int maxUsesPerTurn) {
        this.maxUsesPerTurn = maxUsesPerTurn;
        this.usesThisTurn   = 0;
    }

    public void setLedger(City.main.ledger.Ledger ledger) { this.ledger = ledger; }

    protected City.main.ledger.Ledger getLedger() { return ledger; }

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