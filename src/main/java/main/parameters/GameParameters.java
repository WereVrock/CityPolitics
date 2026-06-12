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
    public static final int STARTING_MONEY               = 1000;
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
    // ACTION: WARTIME TAXES
    // =========================================================
    public static final int    WARTIME_TAXES_GOLD_PER_POP   = 10;
    public static final int    WARTIME_TAXES_COOLDOWN_TURNS = 6;

    // =========================================================
    // ACTION: ALLOW MERCENARIES (voted formal action)
    // =========================================================
    public static final int ALLOW_MERCENARIES_INFLUENCE_COST = 10;

    // =========================================================
    // LEGISLATION
    // =========================================================
    public static final int PROPOSE_LEGISLATION_INFLUENCE_COST = 15;

    // =========================================================
    // DEAL MINIMUMS
    // =========================================================
    public static final int DEAL_MIN_MONEY     = 10;
    public static final int DEAL_MIN_INFLUENCE = 5;

    // =========================================================
    // REALM ACTIONS
    // =========================================================
    public static final int ALLOW_SEND_RESOURCES_INFLUENCE_COST = 12;
    public static final int SEND_RESOURCES_WINDOW_TURNS         = 3;
    public static final int SEND_RESOURCES_OPINION_PER_GOLD     = 1;   // opinion per 10 gold
    public static final int SEND_RESOURCES_OPINION_DIVISOR      = 10;
    public static final int GRANT_CLAIM_OWNER_OPINION_MALUS     = -25;
    public static final int GRANT_CLAIM_TARGET_OPINION_BONUS    = 20;
    public static final int GRANT_CLAIM_OTHER_CLAIMANT_MALUS    = -5;

    // =========================================================
    // MERCENARIES
    // =========================================================
    /** Multiplier on normal soldier recruit AND upkeep cost for mercenaries. */
    public static final double MERCENARY_COST_MULTIPLIER    = 3.0;
    /** Ally force must be > this × merc size to suppress raiding. */
    public static final double MERCENARY_RAID_ALLY_THRESHOLD = 1.5;
    /** Chance per turn that an unsupervised mercenary army raids. */
    public static final double MERCENARY_RAID_CHANCE        = 0.30;

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
    public static final double DEAL_FAVOUR_THRESHOLD_1       = 1;
    public static final double DEAL_FAVOUR_THRESHOLD_2       = 1.8;

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
    public static final int ZONE_CAPITAL_FOOD    = 20;
    public static final int ZONE_CAPITAL_POPS    = 50;

    public static final int ZONE_TOWN_GOLD       = 10;
    public static final int ZONE_TOWN_FOOD       = 8;
    public static final int ZONE_TOWN_POPS       = 20;

    public static final int ZONE_VILLAGE_GOLD    = 3;
    public static final int ZONE_VILLAGE_FOOD    = 24;
    public static final int ZONE_VILLAGE_POPS    = 10;

    // =========================================================
    // NOBLE HOUSES
    // =========================================================
    public static final int    NOBLE_HOUSE_STARTING_OPINION          = 50;
    public static final int    NOBLE_HOUSE_STARTING_INFLUENCE        = 10;
    public static final int    NOBLE_OPINION_MIN                     = 0;
    public static final int    NOBLE_OPINION_MAX                     = 100;
    public static final int    NOBLE_HOSTILE_OPINION_THRESHOLD       = 15;
    public static final double NOBLE_MAX_MANPOWER_SEND_FRACTION      = 0.50;
    public static final int    NOBLE_ZONE_MANPOWER_PER_TURN          = 6;
    public static final int    NOBLE_ZONE_GOLD_PER_TURN              = 5;
    public static final double NOBLE_INFLUENCE_BASE_PER_TURN         = 1.0;
    public static final double NOBLE_INFLUENCE_PER_ZONE              = 0.3;
    public static final int    NOBLE_STANDING_ARMY_MANPOWER_MULTIPLIER = 5;
    public static final int    NOBLE_RECRUIT_COST_PER_SOLDIER        = 1;
    public static final int    NOBLE_UPKEEP_COST_PER_SOLDIER         = 1;
    public static final double NOBLE_UPKEEP_DEFENSE_DISCOUNT         = 0.25;

    // =========================================================
    // NOBLE HOUSE — PRESTIGE & DEFENSE
    // =========================================================
    public static final int    NOBLE_STARTING_PRESTIGE               = 50;
    public static final int    NOBLE_STARTING_DEFENSE                = 10;
    public static final double NOBLE_INFLUENCE_PRESTIGE_FACTOR       = 0.002;

    // =========================================================
    // COMBAT
    // =========================================================
    public static final double COMBAT_BASE_CASUALTY_RATE             = 0.20;
    public static final double COMBAT_CASUALTY_VARIANCE              = 0.10;
    public static final double COMBAT_DEFENSE_REDUCTION              = 0.50;

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
    // COALITION
    // =========================================================
    public static final int    COALITION_ZONE_THRESHOLD              = 3;
    public static final double COALITION_ARMY_THRESHOLD              = 0.90;
    public static final double COALITION_COORDINATOR_BONUS           = 3.0;
    public static final double COALITION_CUNNING_WEIGHT              = 0.4;
    public static final double COALITION_DIPLOMACY_WEIGHT            = 0.4;
    public static final double COALITION_PRESTIGE_WEIGHT             = 0.02;
    public static final double COALITION_ARMY_PARTICIPATION_WEIGHT   = 2.0;

    // =========================================================
    // THREATENED
    // =========================================================
    public static final double THREATENED_BASE_CHANCE_MULTIPLIER     = 5.0;
    public static final double THREATENED_DECAY_CHANCE               = 0.05;
    public static final double THREATENED_CLAIMLESS_MULTIPLIER       = 2.0;

    // =========================================================
    // RAID
    // =========================================================
    public static final int    RAID_COOLDOWN_TURNS                   = 3;
    public static final double RAID_PRODUCTION_MALUS                 = 0.30;
    public static final double RAID_INTERCEPT_BASE_CHANCE            = 0.20;
    public static final double RAID_INTERCEPT_MILITARY_BONUS         = 0.05;
    public static final double RAID_GOLD_PER_SOLDIER                 = 1.0;
    public static final double RAID_GOLD_ZONE_MULTIPLIER             = 3.0;
    public static final double RAID_MAX_GOLD_ZONE_MULTIPLIER         = 3.0;

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

    // =========================================================
    // CLAIM
    // =========================================================
    public static final double CLAIM_BASE_SUCCESS_CHANCE             = 0.40;
    public static final double CLAIM_CUNNING_BONUS_PER_POINT         = 0.15;
    public static final double CLAIM_OWNER_CUNNING_PENALTY_PER_POINT = 0.10;
    public static final double CLAIM_ADJACENCY_PENALTY               = 0.50;
    public static final double CLAIM_DECAY_CHANCE                    = 0.30;
    public static final int    CLAIM_DECAY_INFLUENCE_COST            = 3;

    // =========================================================
    // SCHEME
    // =========================================================
    public static final double SCHEME_BASE_SUCCESS_CHANCE            = 0.40;
    public static final double SCHEME_CUNNING_BONUS_PER_POINT        = 0.15;

    // =========================================================
    // MILITARY SKILL
    // =========================================================
    public static final double MILITARY_SKILL_BONUS_PER_POINT        = 0.10;

    // =========================================================
    // ALLIANCE
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
    // NOBLE AI — INFLUENCE COSTS
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
    public static final int    GARRISON_CAPITAL_MULTIPLIER           = 3;
    public static final int    GARRISON_OTHER_MULTIPLIER             = 1;

    // =========================================================
    // NOBLE ARMY RECRUITMENT
    // =========================================================
    public static final int    NOBLE_ARMY_MIN_RECRUIT_SIZE           = 5;
    public static final int    NOBLE_ARMY_RECRUIT_GOLD_THRESHOLD     = 30;
    public static final int    NOBLE_ARMY_DISBAND_GOLD_THRESHOLD     = 15;

    // =========================================================
    // NOBLE FORTIFICATION
    // =========================================================
    public static final int    NOBLE_FORTIFY_GAIN                    = 10;
    public static final int    FORTIFY_GARRISON_GAIN                 = 5;
    public static final int    FORTIFY_GARRISON_MAX_BONUS            = 50;
    public static final int    NOBLE_FORTIFY_GOLD_COST               = 20;

    // =========================================================
    // SABOTAGE
    // =========================================================
    public static final int    AI_SABOTAGE_GOLD_COST                 = 30;
    public static final double SABOTAGE_BASE_SUCCESS_CHANCE          = 0.40;
    public static final double SABOTAGE_CUNNING_BONUS_PER_POINT      = 0.15;

    // =========================================================
    // OPPORTUNISM
    // =========================================================
    public static final double OPPORTUNISM_STRENGTH_RATIO_HOSTILE    = 2.0;
    public static final double OPPORTUNISM_STRENGTH_RATIO_NEUTRAL    = 2.5;
    public static final double OPPORTUNISM_SECURITY_STRENGTH_RATIO   = 1.7;
    public static final double OPPORTUNISM_PRESTIGE_STRENGTH_RATIO   = 3.0;
    public static final double OPPORTUNISM_WEALTH_STRENGTH_RATIO     = 3.0;
    public static final int    OPPORTUNISM_FABRICATE_COOLDOWN        = 1;

    // =========================================================
    // WAR CHEST
    // =========================================================
    public static final double WAR_CHEST_PRIORITY_EXPANSION          = 0.95;
    public static final double WAR_CHEST_PRIORITY_SECURITY           = 0.90;
    public static final double WAR_CHEST_PRIORITY_WEALTH             = 0.75;
    public static final double WAR_CHEST_PRIORITY_PRESTIGE           = 0.60;
    public static final int    WAR_CHEST_UPKEEP_TURNS                = 2;
    public static final double RECKLESS_READINESS_THRESHOLD          = 0.70;
    public static final double WAR_CHEST_FUZZ_BASE                   = 0.07;
    public static final double WAR_CHEST_FUZZ_PER_MISSING            = 0.13;

    // =========================================================
    // ATTACK STRENGTH THRESHOLDS
    // =========================================================
    public static final double NORMAL_ATTACK_STRENGTH_THRESHOLD      = 0.5;
    public static final double COALITION_STRENGTH_THRESHOLD          = 0.4;
    public static final double RECKLESS_MIN_STRENGTH                 = 0.6;
    public static final double RECKLESS_VALUE_MULTIPLIER             = 2.0;

    // =========================================================
    // REBELLION
    // =========================================================
    public static final int    ADMIN_CAPACITY_BASE                   = 3;
    public static final double REBELLION_BASE_CHANCE                 = 0.30;
    public static final double REBELLION_OVEREXTENSION_PER_ZONE      = 0.05;
    public static final int    REBELLION_POWER_INCREASE              = 5;
    public static final double REBELLION_DECAY_BASE_CHANCE           = 0.30;
    public static final double REBELLION_DECAY_CUNNING_PER_POINT     = 0.05;
    public static final int    REBELLION_POWER_DECREASE              = 5;
    public static final double REBELLION_FLIP_MULTIPLIER             = 1.5;

    // =========================================================
    // MAP CANVAS
    // =========================================================
    public static final int MAP_CANVAS_WIDTH                         = 1200;
    public static final int MAP_CANVAS_HEIGHT                        = 700;

    // =========================================================
    // BARBARIAN INVASION — COUNTDOWN
    // =========================================================
    public static final int    BARB_COUNTDOWN_MIN_YEARS              = 7;
    public static final int    BARB_COUNTDOWN_MAX_YEARS              = 15;

    // =========================================================
    // BARBARIAN INVASION — WARBOSS
    // =========================================================
    public static final int    BARB_WARBOSS_BASE_SIZE                = 800;
    public static final int    BARB_WARBOSS_SIZE_PER_TURN            = 10;
    public static final double BARB_WARBOSS_RAIDER_FRACTION          = 0.05;
    public static final int    BARB_WARBOSS_RAIDER_MIN               = 30;
    public static final double BARB_WARBOSS_DETOUR_CHANCE            = 0.50;

    // =========================================================
    // BARBARIAN INVASION — WAVES
    // =========================================================
    public static final int    BARB_WAVE_MIN_TURNS                   = 8;
    public static final int    BARB_WAVE_MAX_TURNS                   = 16;
    public static final int    BARB_WAVE_RAIDER_COUNT                = 2;
    public static final int    BARB_WAVE_RAVAGER_COUNT               = 1;
    public static final int    BARB_WAVE_RAIDER_SIZE                 = 60;
    public static final int    BARB_WAVE_RAVAGER_SIZE                = 120;

    // =========================================================
    // BARBARIAN INVASION — COMBAT
    // =========================================================
    public static final double BARB_DEFENDER_BONUS                   = 0.30;
    public static final int    BARB_WARBOSS_GARRISON_SIZE            = 15;
    public static final int    BARB_RAVAGER_GARRISON_SIZE            = 5;

    // =========================================================
    // BARBARIAN INVASION — PAY-OFF
    // =========================================================
    public static final int    BARB_PAYOFF_GOLD_PER_MAN              = 2;
    public static final int    BARB_PAYOFF_FOOD_PER_MAN              = 1;
    public static final int    BARB_DISMISS_GOLD_PER_MAN             = 6;
    public static final double BARB_WARBOSS_DISMISS_MULTIPLIER       = 1.3;
    public static final double BARB_WARBOSS_DISMISS_COST_MULTIPLIER  = 1.3;

    // =========================================================
    // BARBARIAN INVASION — RAVAGED STATUS
    // =========================================================
    public static final double BARB_RAVAGED_PENALTY                  = 0.50;
    public static final double BARB_HEAVILY_RAVAGED_PENALTY          = 0.80;

    // =========================================================
    // PLAYER ARMY — BASE
    // =========================================================
    public static final int    PLAYER_ARMY_STARTING_SIZE             = 100;

    // =========================================================
    // COMMANDER RECRUITMENT
    // =========================================================
    public static final int    COMMANDER_POOL_BASE_SIZE              = 3;
    public static final int    COMMANDER_POOL_REFRESH_SIZE           = 3;
    public static final int    COMMANDER_POOL_REFRESH_COST           = 2;
    public static final int    COMMANDER_RECRUIT_BASE_COST           = 3;
    public static final int    COMMANDER_RECRUIT_OPINION_GAIN        = 8;
    public static final int    COMMANDER_DISMISS_COST                = 2;
    public static final int    COMMANDER_DISMISS_OPINION_LOSS        = 15;

    // =========================================================
    // COMMANDER SKILL ROLL
    // =========================================================
    public static final int    COMMANDER_SKILL_WEIGHT_0              = 25;
    public static final int    COMMANDER_SKILL_WEIGHT_1              = 80;
    public static final int    COMMANDER_SKILL_WEIGHT_2              = 95;

    // =========================================================
    // COMMANDER CAP & OVERCAP COST
    // =========================================================
    public static final int    COMMANDER_FREE_CAP                    = 3;
    public static final double COMMANDER_OVERCAP_INFLUENCE_COST      = 0.5;

    // =========================================================
    // COMMANDER GOLD UPKEEP — by skill level
    // =========================================================
    public static final double[] COMMANDER_UPKEEP_BY_SKILL           = { 0.8, 1.2, 1.5, 2.0 };

    // =========================================================
    // COMMANDER PARTY POWER
    // =========================================================
    public static final int    COMMANDER_PARTY_POWER_PER_ALIVE       = 10;

    // =========================================================
    // COMMANDER XP THRESHOLDS
    // =========================================================
    public static final int[]  COMMANDER_XP_THRESHOLDS               = { 100, 250, 500 };

    // =========================================================
    // COMMANDER DEATH IN BATTLE
    // =========================================================
    public static final double COMMANDER_DEATH_CASUALTY_LOWER        = 0.30;
    public static final double COMMANDER_DEATH_CASUALTY_UPPER        = 0.80;
    public static final double COMMANDER_DEATH_WIN_MODIFIER          = 0.70;

    // =========================================================
    // SOLDIER RECRUITMENT
    // =========================================================
    public static final int    SOLDIER_RECRUIT_GOLD_COST             = 2;
    public static final int    SOLDIER_RECRUIT_MANPOWER_COST         = 1;
    public static final double SOLDIER_UPKEEP_GOLD                   = 0.3;

    // =========================================================
    // SOLDIER DESERTION (when upkeep skipped)
    // =========================================================
    public static final double SOLDIER_DESERTION_MIN_FRACTION        = 0.10;
    public static final double SOLDIER_DESERTION_MAX_FRACTION        = 0.30;

    // =========================================================
    // PRESTIGE & XP FROM COMBAT
    // =========================================================
    public static final double COMBAT_WIN_PRESTIGE_MULTIPLIER        = 1.30;
    public static final double PRESTIGE_COEFFICIENT                  = 1.0;
    public static final double XP_COEFFICIENT                        = 10.0;
    public static final double COMBAT_SCORE_RATIO_MIN                = 0.5;
    public static final double COMBAT_SCORE_RATIO_MAX                = 2.0;

    public static final int LIBERATED_ZONE_OPINION_BONUS = 15;

    // =========================================================
    // NOBLE BARBARIAN INTERACTION
    // =========================================================
    public static final double NOBLE_BARB_PRESTIGE_PER_KILL      = 0.08;
    public static final int    NOBLE_BARB_PRESTIGE_MIN_WIN        = 5;
    public static final double NOBLE_BARB_HUNT_STRENGTH_RATIO     = 1.8;
    public static final double NOBLE_BARB_DESPERATE_DEFENSE_RATIO = 0.5;
    public static final double NOBLE_WARBOSS_DEFENSE_ARMY_FRACTION = 0.90;
}