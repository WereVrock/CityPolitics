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
    /** Discount multiplier for armies that are defending (no pending attack/raid order and in friendly zone). */
    public static final double NOBLE_UPKEEP_DEFENSE_DISCOUNT         = 0.25;

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
    // =========================================================
    // CLAIM FABRICATION
    // =========================================================
    // =========================================================
    // COALITION
    // =========================================================
    public static final int    COALITION_ZONE_THRESHOLD              = 3;
    public static final double COALITION_ARMY_THRESHOLD              = 0.90;

    // =========================================================
    // COALITION ZONE AWARD WEIGHTS
    // =========================================================
    /** Flat bonus added to coordinator's weight in zone award roll. */
    public static final double COALITION_COORDINATOR_BONUS           = 3.0;
    /** Per cunning point added to weight. */
    public static final double COALITION_CUNNING_WEIGHT              = 0.4;
    /** Per diplomacy point added to weight. */
    public static final double COALITION_DIPLOMACY_WEIGHT            = 0.4;
    /** Per prestige point added to weight. */
    public static final double COALITION_PRESTIGE_WEIGHT             = 0.02;
    /** Army participation fraction multiplier added to weight. */
    public static final double COALITION_ARMY_PARTICIPATION_WEIGHT   = 2.0;

    // =========================================================
    // THREATENED
    // =========================================================
    public static final double THREATENED_BASE_CHANCE_MULTIPLIER     = 5.0;
    public static final double THREATENED_DECAY_CHANCE               = 0.05;
    /** Extra multiplier applied to threat chance for claimless attacks. */
    public static final double THREATENED_CLAIMLESS_MULTIPLIER       = 2.0;

    // =========================================================
    // RAID COOLDOWN & PRODUCTION MALUS
    // =========================================================
    public static final int    RAID_COOLDOWN_TURNS                   = 3;
    public static final double RAID_PRODUCTION_MALUS                 = 0.30;

    // =========================================================
    // CONQUEST MALUS
    // =========================================================
    public static final int    CONQUEST_MALUS_DECAY_PER_TURN         = 10;
    public static final double CONQUEST_MALUS_GOLD_COST_PER_PERCENT  = 0.05;
    public static final double CONQUEST_MALUS_INFLUENCE_COST_PER_PERCENT = 0.02;

    // =========================================================
    // ACKNOWLEDGE SUPERIORITY
    // =========================================================
    public static final double SUPERIORITY_BASE_ACCEPT_CHANCE        = 0.60;
    public static final double SUPERIORITY_RANDOM_RANGE              = 0.20;

   

    public static final double CLAIM_BASE_SUCCESS_CHANCE             = 0.40;
    public static final double CLAIM_CUNNING_BONUS_PER_POINT         = 0.15;
    public static final double CLAIM_OWNER_CUNNING_PENALTY_PER_POINT = 0.10;
    /** Multiplier applied when target zone is not adjacent to any owned zone. */
    public static final double CLAIM_ADJACENCY_PENALTY               = 0.50;

    // =========================================================
    // SCHEME SUCCESS
    // =========================================================
    public static final double SCHEME_BASE_SUCCESS_CHANCE            = 0.40;
    public static final double SCHEME_CUNNING_BONUS_PER_POINT        = 0.15;

    // =========================================================
    // MILITARY SKILL
    // =========================================================
    public static final double MILITARY_SKILL_BONUS_PER_POINT        = 0.10;

    // =========================================================
    // ALLIANCE RULES
    // =========================================================
    public static final int    ALLIANCE_MAX_PER_HOUSE                = 2;
    public static final double ALLIANCE_MIN_ARMY_FRACTION            = 0.50;
    public static final double ALLIANCE_BREAK_CLEAN_BASE             = 0.30;
    public static final double ALLIANCE_BREAK_CLEAN_PER_DIPLOMACY    = 0.15;
    public static final double ALLY_BASE_ACCEPT_CHANCE               = 0.50;
    public static final double ALLY_DIPLOMACY_BONUS_PER_POINT        = 0.10;
    public static final double ALLY_DEFENSE_MIN_STRENGTH_FRACTION    = 0.35;

    // =========================================================
    // RELATIONSHIP DECAY
    // =========================================================
    public static final int    HOSTILE_DECAY_TURNS                   = 3;

    // =========================================================
    // RAID CAP
    // =========================================================
    public static final double RAID_MAX_GOLD_ZONE_MULTIPLIER         = 3.0;

    // =========================================================
    // DEMAND — DIPLOMACY BONUS
    // =========================================================
    public static final double DEMAND_DIPLOMACY_BONUS_PER_POINT      = 5.0;

    // =========================================================
    // GIFT
    // =========================================================
    public static final int    GIFT_MONEY_AMOUNT                     = 30;
    public static final double GIFT_WEIGHT_SECURITY                  = 0.60;
    public static final double GIFT_WEIGHT_WEALTH                    = 0.25;
    public static final double GIFT_WEIGHT_PRESTIGE                  = 0.30;
    public static final double GIFT_WEIGHT_EXPANSION                 = 0.10;
    public static final int    GIFT_WEALTH_GOLD_THRESHOLD            = 150;

    // =========================================================
    // NOBLE AI — INFLUENCE COSTS (new)
    // =========================================================
    public static final int    AI_INFLUENCE_COST_FABRICATE           = 2;

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
    public static final int    AI_INFLUENCE_COST_SABOTAGE            = 3;

    // =========================================================
    // NOBLE GARRISON
    // =========================================================
    /** Garrison size at capital = manpower-per-turn × this multiplier. */
    public static final int    GARRISON_CAPITAL_MULTIPLIER              = 3;
    /** Garrison size at other zones = manpower-per-turn × this multiplier. */
    public static final int    GARRISON_OTHER_MULTIPLIER                = 1;

    // =========================================================
    // NOBLE ARMY RECRUITMENT
    // =========================================================
    /** Minimum army size the AI will recruit. */
    public static final int    NOBLE_ARMY_MIN_RECRUIT_SIZE              = 5;
    /** AI recruits this fraction of available noble manpower per turn. */
    public static final double NOBLE_ARMY_RECRUIT_FRACTION              = 0.5;
    /** AI won't recruit if gold below this threshold. */
    public static final int    NOBLE_ARMY_RECRUIT_GOLD_THRESHOLD        = 30;
    /** AI disbands army if gold below this threshold (to avoid forced disband). */
    public static final int    NOBLE_ARMY_DISBAND_GOLD_THRESHOLD        = 15;

    // =========================================================
    // NOBLE FORTIFICATION
    // =========================================================
    /** Fortification gained per FORTIFY action. */
    public static final int    NOBLE_FORTIFY_GAIN                       = 10;
    /** Garrison soldiers added per FORTIFY action. */
    public static final int    FORTIFY_GARRISON_GAIN                    = 5;
    /** Maximum total garrison bonus that can be accumulated from fortifying. */
    public static final int    FORTIFY_GARRISON_MAX_BONUS               = 50;
    /** Gold cost per FORTIFY action. */
    public static final int    NOBLE_FORTIFY_GOLD_COST                  = 20;

    // =========================================================
    // RAID INTERCEPT
    // =========================================================
    /** Base chance defender army intercepts a raid (20%). */
    public static final double RAID_INTERCEPT_BASE_CHANCE               = 0.20;
    /** Per military skill point bonus to intercept chance. */
    public static final double RAID_INTERCEPT_MILITARY_BONUS            = 0.05;
    /** Gold stolen per raiding soldier. */
    public static final double RAID_GOLD_PER_SOLDIER                    = 1.0;
    /** Max gold stolen = zone gold production × this multiplier. */
    public static final double RAID_GOLD_ZONE_MULTIPLIER                = 3.0;

    // =========================================================
    // CLAIM DECAY
    // =========================================================
    /** Per-turn chance a house must defend a random claim or lose it. */
    public static final double CLAIM_DECAY_CHANCE                     = 0.30;
    /** Influence cost to maintain a claim when decay triggers. */
    public static final int    CLAIM_DECAY_INFLUENCE_COST             = 3;

    // =========================================================
    // ATTACK STRENGTH THRESHOLDS
    // =========================================================
    /** Minimum attacker/defender power ratio for normal attacks. */
    public static final double NORMAL_ATTACK_STRENGTH_THRESHOLD = 0.5;
    /** Minimum coalition estimated power ratio vs defender to proceed. */
    public static final double COALITION_STRENGTH_THRESHOLD      = 0.4;
    /** Minimum attacker/defender power ratio for reckless claimless attacks. */
    public static final double RECKLESS_MIN_STRENGTH             = 0.6;
    /** Reckless target must be this many times better than best claimed zone. */
    public static final double RECKLESS_VALUE_MULTIPLIER         = 2.0;

    // =========================================================
    // SABOTAGE
    // =========================================================
    /** Gold cost for sabotage action. */
    public static final int    AI_SABOTAGE_GOLD_COST                  = 30;
    /** Base success chance for sabotage. */
    public static final double SABOTAGE_BASE_SUCCESS_CHANCE           = 0.40;
    /** Per cunning point bonus to sabotage success. */
    public static final double SABOTAGE_CUNNING_BONUS_PER_POINT       = 0.15;

    // =========================================================
    // ARMY
    // =========================================================
    // =========================================================
    // MAP CANVAS
    // =========================================================
    // =========================================================
    // OPPORTUNISM (OpportunismEvaluator)
    // =========================================================
    /** Attacker field army / defender(est.field+garrison) for hostile/rival targets. */
    public static final double OPPORTUNISM_STRENGTH_RATIO_HOSTILE  = 2.0;
    /** Same ratio for neutral targets. */
    public static final double OPPORTUNISM_STRENGTH_RATIO_NEUTRAL  = 2.5;
    /** Ratio for security‑driven attacks (threats only). */
    public static final double OPPORTUNISM_SECURITY_STRENGTH_RATIO = 1.7;
    /** Ratio for prestige‑driven attacks (rivals only). */
    public static final double OPPORTUNISM_PRESTIGE_STRENGTH_RATIO = 3.0;
    /** Ratio for wealth‑driven attacks (high‑gold zones only). */
    public static final double OPPORTUNISM_WEALTH_STRENGTH_RATIO   = 3.0;
    /** Turns a house skips opportunism after a failed fabrication. */
    public static final int    OPPORTUNISM_FABRICATE_COOLDOWN      = 1;

    // =========================================================
    // WAR CHEST
    // =========================================================
    /** Base savings priority per motivation (fraction of target to maintain). */
    public static final double WAR_CHEST_PRIORITY_EXPANSION = 0.95;
    public static final double WAR_CHEST_PRIORITY_SECURITY  = 0.90;
    public static final double WAR_CHEST_PRIORITY_WEALTH    = 0.75;
    public static final double WAR_CHEST_PRIORITY_PRESTIGE  = 0.60;
    /** Expected upkeep turns for war‑chest calculation. */
    public static final int    WAR_CHEST_UPKEEP_TURNS        = 2;
    /** Readiness threshold for reckless leaders (fraction of target). */
    public static final double RECKLESS_READINESS_THRESHOLD = 0.70;
    /** Base fuzziness for war‑chest estimates. */
    public static final double WAR_CHEST_FUZZ_BASE          = 0.07;
    /** Per missing cunning point fuzziness for war‑chest estimates. */
    public static final double WAR_CHEST_FUZZ_PER_MISSING   = 0.13;

    // =========================================================
    // MAP CANVAS
    // =========================================================
    public static final int MAP_CANVAS_WIDTH  = 1200;
    public static final int MAP_CANVAS_HEIGHT = 700;
}