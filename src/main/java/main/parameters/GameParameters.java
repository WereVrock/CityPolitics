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
    public static final double HUMAN_INFLUENCE_GENERATION   = 0.0;
    public static final double HUMAN_MANPOWER_CONTRIBUTION  = 1.0;

    public static final double DWARF_FOOD_CONSUMPTION       = 1.2;
    public static final double DWARF_MONEY_GENERATION       = 2.0;
    public static final double DWARF_INFLUENCE_GENERATION   = 0.0;
    public static final double DWARF_MANPOWER_CONTRIBUTION  = 1.3;

    public static final double ORC_FOOD_CONSUMPTION         = 1.8;
    public static final double ORC_MONEY_GENERATION         = 0.6;
    public static final double ORC_INFLUENCE_GENERATION     = 0.0;
    public static final double ORC_MANPOWER_CONTRIBUTION    = 2.0;

    public static final double ELF_FOOD_CONSUMPTION         = 0.6;
    public static final double ELF_MONEY_GENERATION         = 0.9;
    public static final double ELF_INFLUENCE_GENERATION     = 0.0;
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
    public static final int BASE_INFLUENCE_PER_TURN         = 1;
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

    // =========================================================
    // MAP — ZONE PRODUCTION & POPS
    // =========================================================
    public static final int ZONE_CAPITAL_GOLD    = 12;
    public static final int ZONE_CAPITAL_FOOD    = 10;
    public static final int ZONE_CAPITAL_POPS    = 50;

    public static final int ZONE_TOWN_GOLD       = 10;
    public static final int ZONE_TOWN_FOOD       = 4;
    public static final int ZONE_TOWN_POPS       = 20;

    public static final int ZONE_VILLAGE_GOLD    = 3;
    public static final int ZONE_VILLAGE_FOOD    = 12;
    public static final int ZONE_VILLAGE_POPS    = 10;

    // =========================================================
    // NOBLE HOUSES
    // =========================================================

    /** Starting opinion of every noble house toward the player (0–100). */
    public static final int    NOBLE_HOUSE_STARTING_OPINION          = 50;

    /** Starting influence for every noble house. */
    public static final int    NOBLE_HOUSE_STARTING_INFLUENCE        = 10;

    /** Opinion clamps shared with parties for uniformity. */
    public static final int    NOBLE_OPINION_MIN                     = 0;
    public static final int    NOBLE_OPINION_MAX                     = 100;

    /**
     * Opinion at or below which a house is considered hostile and sends
     * nothing to the player.
     */
    public static final int    NOBLE_HOSTILE_OPINION_THRESHOLD       = 15;

    /**
     * Maximum fraction of manpower a house sends to the player.
     * Reached at 100 opinion. Scales linearly from 0 at hostile threshold.
     */
    public static final double NOBLE_MAX_MANPOWER_SEND_FRACTION      = 0.50;

    /** Raw manpower each controlled zone generates per turn (2–10 range). */
    public static final int    NOBLE_ZONE_MANPOWER_PER_TURN          = 6;

    /** Gold each controlled zone generates for the house per turn (half sent to player). */
    public static final int    NOBLE_ZONE_GOLD_PER_TURN              = 5;

    /** Base influence gained per house per turn. */
    public static final double NOBLE_INFLUENCE_BASE_PER_TURN         = 1.0;

    /** Additional influence per controlled zone per turn. */
    public static final double NOBLE_INFLUENCE_PER_ZONE              = 0.3;

    /**
     * Standing army size = manpower-per-turn × this multiplier.
     * Housed in the capital, free of upkeep and recruitment cost.
     */
    public static final int    NOBLE_STANDING_ARMY_MANPOWER_MULTIPLIER = 5;

    /** Gold cost to recruit one soldier into the raised army. */
    public static final int    NOBLE_RECRUIT_COST_PER_SOLDIER        = 3;

    /** Gold cost per soldier per turn to maintain the raised army. */
    public static final int    NOBLE_UPKEEP_COST_PER_SOLDIER         = 1;

    // =========================================================
    // NOBLE HOUSE — PRESTIGE & DEFENSE
    // =========================================================
    public static final int    NOBLE_STARTING_PRESTIGE               = 50;
    public static final int    NOBLE_STARTING_DEFENSE                = 10;
    public static final double NOBLE_INFLUENCE_PRESTIGE_FACTOR       = 0.002; // per prestige point

    // =========================================================
    // COMBAT (placeholder values)
    // =========================================================
    public static final double COMBAT_BASE_CASUALTY_RATE             = 0.20;
    public static final double COMBAT_CASUALTY_VARIANCE              = 0.10;
    public static final double COMBAT_DEFENSE_REDUCTION              = 0.50; // max 50% reduction at 100 defense

    // =========================================================
    // DEMAND FORMULA
    // =========================================================
    public static final double DEMAND_BASE_SCORE                     = 50.0;
    public static final double DEMAND_PRESTIGE_WEIGHT                = 0.3;
    public static final double DEMAND_ARMY_WEIGHT                    = 0.1;
    public static final double DEMAND_ALLIED_BONUS                   = 30.0;
    public static final double DEMAND_RIVAL_PENALTY                  = -40.0;
    public static final double DEMAND_SHARED_RIVAL_BONUS             = 20.0;
    public static final double DEMAND_RANDOM_RANGE                   = 15.0;
    public static final double DEMAND_ACCEPT_THRESHOLD               = 50.0;
    public static final double DEMAND_WEALTH_FRACTION                = 0.20;
    public static final int    DEMAND_ARMY_AMOUNT                    = 10;
    public static final int    DEMAND_PRESTIGE_AMOUNT                = 8;

    // =========================================================
    // NOBLE AI
    // =========================================================
    public static final double AI_DOMINANT_MOTIVATION_CHANCE         = 0.75;
    public static final int    AI_FORTIFY_THRESHOLD                  = 30;
    public static final double AI_RAID_GOLD_FRACTION                 = 0.15;
    public static final double AI_SUPPORT_GOLD_FRACTION              = 0.10;
    public static final int    AI_SCHEME_PRESTIGE_LOSS               = 10;
    public static final int    AI_SCHEME_PRESTIGE_GAIN               = 5;
    public static final int    AI_FORTIFY_GOLD_COST                  = 20;
    public static final int    AI_FORTIFY_DEFENSE_GAIN               = 10;
    public static final int    AI_INFLUENCE_COST_ATTACK              = 3;
    public static final int    AI_INFLUENCE_COST_RAID                = 2;
    public static final int    AI_INFLUENCE_COST_DEMAND              = 2;
    public static final int    AI_INFLUENCE_COST_SCHEME              = 3;
    public static final int    AI_INFLUENCE_COST_ALLY                = 1;
    public static final int    AI_INFLUENCE_COST_SUPPORT             = 2;

    // =========================================================
    // ARMY
    // =========================================================
    /** Zones an army can traverse per turn once orders arrive. */
    // =========================================================
    // MAP CANVAS
    // =========================================================
    public static final int MAP_CANVAS_WIDTH  = 1200;
    public static final int MAP_CANVAS_HEIGHT = 700;
}