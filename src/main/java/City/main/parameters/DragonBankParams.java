package City.main.parameters;

/**
 * Balance constants for the Dragon Bank — an abstract, distant lending and
 * deposit service. It has no zone, no garrison, and cannot be attacked or
 * robbed. In exchange for that guaranteed safety: no deposit interest, high
 * loan interest, and a one-turn courier delay on both loan disbursement and
 * withdrawals. There is no minimum deposit.
 */
public final class DragonBankParams {

    private DragonBankParams() {}

    // ─── Deposits (noble houses only — player deposits are pointless) ──────
    /** Turns the dragon's agents take to physically return withdrawn gold. */
    public static final int    WITHDRAWAL_DELAY_TURNS    = 1;

    // ─── Loans ───────────────────────────────────────────────────────────
    /** Flat, high interest rate — no credit-rating discount system. */
    public static final double LOAN_INTEREST_RATE        = 0.45;

    /** Turns the dragon's agents take to deliver a borrowed sum. */
    public static final int    LOAN_DELAY_TURNS          = 1;

    public static final int    LOAN_INSTALLMENTS_DEFAULT = 6;

    /** Max loan = max(FLAT_FLOOR, currentGold * GOLD_MULTIPLIER). */
    public static final double MAX_LOAN_GOLD_MULTIPLIER  = 3.0;
    public static final int    MAX_LOAN_FLAT_FLOOR       = 100;

    // ─── Default penalty ─────────────────────────────────────────────────
    /** Prestige lost by a noble house that defaults on a Dragon loan. */
    public static final int    DEFAULT_PRESTIGE_PENALTY  = City.main.parameters.levels.PrestigeLevels.HIGH;

    /** Turns a defaulter is refused further Dragon Bank loans. */
    public static final int    DEFAULT_BAN_TURNS         = 8;
}