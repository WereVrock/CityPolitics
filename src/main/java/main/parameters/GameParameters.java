package main.parameters;

public final class GameParameters {

    private GameParameters() {}

    // =========================================================
    // CALENDAR
    // =========================================================
    public static final int START_YEAR                   = 184;
    public static final int FROST_GIANT_ARRIVAL_YEAR     = 200;
    public static final int PERIODS_PER_YEAR             = 2;

    // =========================================================
    // STARTING RESOURCES
    // =========================================================
    public static final int STARTING_FOOD                = 200;
    public static final int STARTING_MONEY               = 150;
    public static final int STARTING_MANPOWER            = 80;
    public static final int STARTING_INFLUENCE           = 50;

    // =========================================================
    // STARTING STATS  (0–100 scale)
    // =========================================================
    public static final int STARTING_CORRUPTION          = 10;
    public static final int STARTING_HAPPINESS           = 60;

    // =========================================================
    // STARTING POP COUNTS
    // =========================================================
    public static final int STARTING_HUMANS              = 100;
    public static final int STARTING_DWARVES             = 40;
    public static final int STARTING_ORCS                = 40;
    public static final int STARTING_ELVES               = 20;

    // =========================================================
    // POP STATS — per individual, per turn
    // =========================================================
    public static final double HUMAN_FOOD_CONSUMPTION       = 1.0;
    public static final double HUMAN_MONEY_GENERATION       = 1.2;
    public static final double HUMAN_INFLUENCE_GENERATION   = 0.8;
    public static final double HUMAN_MANPOWER_CONTRIBUTION  = 1.0;

    public static final double DWARF_FOOD_CONSUMPTION       = 1.2;
    public static final double DWARF_MONEY_GENERATION       = 2.0;
    public static final double DWARF_INFLUENCE_GENERATION   = 0.4;
    public static final double DWARF_MANPOWER_CONTRIBUTION  = 1.3;

    public static final double ORC_FOOD_CONSUMPTION         = 1.8;
    public static final double ORC_MONEY_GENERATION         = 0.6;
    public static final double ORC_INFLUENCE_GENERATION     = 0.3;
    public static final double ORC_MANPOWER_CONTRIBUTION    = 2.0;

    public static final double ELF_FOOD_CONSUMPTION         = 0.6;
    public static final double ELF_MONEY_GENERATION         = 0.9;
    public static final double ELF_INFLUENCE_GENERATION     = 2.5;
    public static final double ELF_MANPOWER_CONTRIBUTION    = 0.7;

    // =========================================================
    // POLITICAL AFFILIATIONS — base membership fractions
    // =========================================================
    public static final double HUMAN_SUP_HUMAN_FRACTION     = 0.35;
    public static final double ENVIRON_ELF_FRACTION         = 0.60;
    public static final double ENVIRON_HUMAN_FRACTION       = 0.10;
    public static final double ENVIRON_ORC_FRACTION         = 0.10;
    public static final double WARMONGER_ORC_FRACTION       = 0.50;
    public static final double WARMONGER_HUMAN_FRACTION     = 0.10;
    public static final double WARMONGER_DWARF_FRACTION     = 0.15;

    // =========================================================
    // ACTION: IMPORT FOOD (max 2/turn)
    // =========================================================
    public static final int IMPORT_FOOD_MONEY_COST          = 40;
    public static final int IMPORT_FOOD_GAINED              = 80;
    public static final int IMPORT_FOOD_MAX_USES            = 2;

    // =========================================================
    // ACTION: ACCEPT BRIBES (max 2/turn)
    // =========================================================
    public static final int ACCEPT_BRIBE_INFLUENCE_COST     = 10;
    public static final int ACCEPT_BRIBE_MONEY_GAINED       = 60;
    public static final int ACCEPT_BRIBE_CORRUPTION_GAIN    = 8;
    public static final int ACCEPT_BRIBE_MAX_USES           = 2;

    // =========================================================
    // ACTION: BRIBE (max 2/turn)
    // =========================================================
    public static final int BRIBE_MONEY_COST                = 50;
    public static final int BRIBE_INFLUENCE_GAINED          = 20;
    public static final int BRIBE_CORRUPTION_GAIN           = 5;
    public static final int BRIBE_MAX_USES                  = 2;

    // =========================================================
    // ACTION: DISTRIBUTE RESOURCES (max 1/turn)
    // =========================================================
    public static final int DISTRIBUTE_MONEY_COST           = 30;
    public static final int DISTRIBUTE_HAPPINESS_GAIN       = 5;
    public static final int DISTRIBUTE_MAX_USES             = 1;

    // =========================================================
    // ACTION: FIGHT CORRUPTION (max 1/turn)
    // =========================================================
    public static final int FIGHT_CORRUPTION_MONEY_COST     = 40;
    public static final int FIGHT_CORRUPTION_INFLUENCE_COST = 15;
    public static final int FIGHT_CORRUPTION_REDUCTION      = 12;
    public static final int FIGHT_CORRUPTION_MAX_USES       = 1;

    // =========================================================
    // ACTION: ORGANIZE FESTIVAL (formal, max 1/turn)
    // =========================================================
    public static final int FESTIVAL_MONEY_COST             = 120;
    public static final int FESTIVAL_INFLUENCE_COST         = 20;
    public static final int FESTIVAL_HAPPINESS_BOOST        = 30;
    public static final int FESTIVAL_DURATION_TURNS         = 5;

    // =========================================================
    // ACTION: CRACKDOWN ON CORRUPTION (formal, max 1/turn)
    // =========================================================
    public static final int CRACKDOWN_MONEY_COST            = 100;
    public static final int CRACKDOWN_INFLUENCE_COST        = 30;
    public static final int CRACKDOWN_CORRUPTION_REDUCTION  = 25;

    // =========================================================
    // ACTION: ROYAL LEVY (formal, max 1/turn)
    // =========================================================
    public static final int LEVY_INFLUENCE_COST             = 15;
    public static final int LEVY_MONEY_GAINED               = 150;
    public static final int LEVY_HAPPINESS_COST             = 8;

    // =========================================================
    // VOTING
    // =========================================================
    public static final double VOTE_INDECISIVE_THRESHOLD     = 0.3;
    public static final double VOTE_DEAL_LOCK_THRESHOLD      = 2.0;
    public static final double VOTE_OPINION_NEUTRAL          = 50.0;
    public static final double VOTE_OPINION_MAX_CONTRIBUTION = 1.0;
    public static final int    SEATS_NEEDED                  = 27;
    public static final double DEAL_MONEY_FACTOR             = 18.0;
    public static final double DEAL_INFLUENCE_FACTOR         = 10.0;
    public static final double DEAL_HAPPINESS_FACTOR         = 3.0;

    // Favour is only demanded when the party's opposition is strong enough.
    // Score magnitude must exceed these thresholds (after squaring) for favour to be required.
    public static final double DEAL_FAVOUR_THRESHOLD_1       = 1;  // 1 favour
    public static final double DEAL_FAVOUR_THRESHOLD_2       = 1.8;  // 2 favour

    // =========================================================
    // PASSIVE PER-TURN EFFECTS
    // =========================================================
    public static final int HAPPINESS_DECAY_PER_TURN        = 2;
    public static final int CORRUPTION_DECAY_PER_TURN       = 1;

    // =========================================================
    // CORRUPTION HAPPINESS MALUS
    // =========================================================
    public static final double CORRUPTION_HAPPINESS_MALUS   = 0.3;

    // =========================================================
    // STAT CLAMPS
    // =========================================================
    public static final int STAT_MIN                        = 0;
    public static final int STAT_MAX                        = 100;
}