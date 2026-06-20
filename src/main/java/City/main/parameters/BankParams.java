package City.main.parameters;

public final class BankParams {

    private BankParams() {}

    public static final String BANK_HOUSE_ID = "house_bank";
    public static final String BANK_ZONE_ID  = "frostpeak_pass";

    public static final int    BANK_STARTING_FORTIFICATION   = 6;
    public static final int    BANK_STARTING_GOLD            = 200;
    public static final int    BANK_STARTING_PRESTIGE        = 50;
    public static final double BANK_MANPOWER_GAIN_MULTIPLIER = 0.5;

    public static final double BANK_BASE_INTEREST_RATE_PER_TURN  = 0.03;
    public static final double BANK_THREATENED_INTEREST_BONUS    = 0.03;
    public static final double BANK_MIN_DEPOSIT_FRACTION_OF_GOLD = 0.10;
    public static final int    BANK_MIN_DEPOSIT_FLAT_GOLD        = 50;
    public static final double BANK_WITHDRAW_RESERVE_FRACTION    = 0.50;
    public static final int    BANK_WITHDRAW_DELAY_TURNS         = 1;
    public static final double BANK_AI_DEPOSIT_EXCESS_FRACTION   = 0.5;

    public static final double BANK_BASE_LOAN_INTEREST_RATE        = 0.10;
    public static final double BANK_LOAN_COLLATERAL_RATE_DISCOUNT  = 0.03;
    public static final double BANK_PROTECTOR_LOAN_RATE_DISCOUNT   = 0.05;
    public static final int    BANK_LOAN_INSTALLMENTS_DEFAULT      = 4;
    public static final int    BANK_CREDIT_RATING_BASE             = 50;
    public static final int    BANK_CREDIT_RATING_MIN              = 0;
    public static final int    BANK_CREDIT_RATING_MAX              = 100;
    public static final int    BANK_CREDIT_PENALTY_PER_DEFAULT     = 30;
    public static final int    BANK_CREDIT_BONUS_PER_REPAYMENT     = 5;
    public static final double BANK_LOW_RESERVE_CALL_THRESHOLD     = 0.20;
    public static final double BANK_MAX_LOAN_PER_CREDIT_POINT      = 4.0;

    public static final double BANK_ABANDON_DEPOSIT_LOSS_FRACTION = 0.8;
    public static final int    BANK_ABANDON_PRESTIGE_PENALTY      = -City.main.parameters.levels.PrestigeLevels.HIGH;
    public static final int    BANK_DEFEND_GOODWILL_CREDIT_BONUS  = 10;
    public static final int    BANK_ROBBER_CREDIT_CAP             = 5;
    public static final int    BANK_PROTECTOR_PRESTIGE_BONUS      = City.main.parameters.levels.PrestigeLevels.HIGH;

    public static final double BANK_MERC_MANPOWER_GROWTH_FRACTION_OF_TOTAL = 0.01;
    public static final double BANK_MERC_UPKEEP_GOLD_PER_MANPOWER         = 1.0 / 3.0;
    public static final double BANK_MERC_RECRUIT_COST_MULTIPLIER          = 2.0;
    public static final double BANK_MERC_UPKEEP_COST_MULTIPLIER           = 2.0;

    public static final double CONQUEST_STEAL_FRACTION_NORMAL  = 0.10;
    public static final double CONQUEST_STEAL_FRACTION_CAPITAL = 0.20;

    public static final double BANK_THREAT_RATIO_TRIGGER = 1.2;

    public static final double BANK_EMERGENCY_FUND_DEPOSIT_FRACTION  = 0.4;
    public static final double BANK_AI_THREAT_COVERAGE_RATIO         = 0.8;
    public static final double BANK_DISSOLVE_CONQUEROR_GOLD_FRACTION = 0.2;
}