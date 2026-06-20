package City.main.parameters;

public final class ActionParams {

    public static final int ACCEPT_BRIBE_MAX_USES = 2;
    public static final int DISTRIBUTE_MAX_USES = 1;
    public static final int WARTIME_TAXES_HAPPINESS_COST = City.main.parameters.levels.HappinessLevels.HIGH;
    public static final int ACCEPT_BRIBE_CORRUPTION_GAIN = City.main.parameters.levels.CorruptionLevels.HIGH;
    public static final int LEVY_HAPPINESS_COST = City.main.parameters.levels.HappinessLevels.HIGH;
    public static final int GRANT_CLAIM_TARGET_OPINION_BONUS = City.main.parameters.levels.OpinionLevels.HIGH;
    public static final int FESTIVAL_HAPPINESS_BOOST = City.main.parameters.levels.HappinessLevels.VERY_HIGH;
    public static final int FIGHT_CORRUPTION_REDUCTION = City.main.parameters.levels.CorruptionLevels.HIGH;
    // stat clamps
    public static final int STAT_MIN = 0;
    public static final int FIGHT_CORRUPTION_INFLUENCE_COST = City.main.parameters.levels.InfluenceLevels.VERY_HIGH;
    // fight corruption
    public static final int FIGHT_CORRUPTION_MONEY_COST = City.main.parameters.levels.GoldLevels.MEDIUM;
    // wartime taxes
    public static final int WARTIME_TAXES_GOLD_PER_POP = City.main.parameters.levels.GoldLevels.HIGH / 10;
    public static final int WARTIME_TAXES_COOLDOWN_TURNS = 6;
    public static final int IMPORT_FOOD_GAINED = City.main.parameters.levels.FoodLevels.HIGH;
    public static final int DEAL_MIN_INFLUENCE = City.main.parameters.levels.InfluenceLevels.MEDIUM;
    public static final int HAPPINESS_DECAY_PER_TURN = City.main.parameters.levels.HappinessLevels.LOW;
    public static final int ACCEPT_BRIBE_MONEY_GAINED = City.main.parameters.levels.GoldLevels.MEDIUM;
    public static final double DEAL_FAVOUR_THRESHOLD_2 = 1.8;
    // passive per-turn
    public static final int BASE_INFLUENCE_PER_TURN = City.main.parameters.levels.InfluenceLevels.VERY_LOW;
    // =========================================================
    // ── ACTIONS (ActionParams) ────────────────────────────
    // =========================================================
    // import food
    public static final int IMPORT_FOOD_MONEY_COST = City.main.parameters.levels.GoldLevels.MEDIUM;
    // send resources
    public static final int SEND_RESOURCES_WINDOW_TURNS = 3;
    public static final int WARTIME_TAXES_INFLUENCE_COST = City.main.parameters.levels.InfluenceLevels.HIGH;
    // realm actions
    public static final int GRANT_CLAIM_INFLUENCE_COST = City.main.parameters.levels.InfluenceLevels.MEDIUM;
    public static final int CRACKDOWN_CORRUPTION_REDUCTION = City.main.parameters.levels.CorruptionLevels.VERY_HIGH;
    // festival
    public static final int FESTIVAL_MONEY_COST = City.main.parameters.levels.GoldLevels.ULTRA_HIGH1;
    // corruption happiness malus
    public static final double CORRUPTION_HAPPINESS_MALUS = 0.3;
    public static final int LEVY_MONEY_GAINED = City.main.parameters.levels.GoldLevels.VERY_HIGH;
    // grant claim opinions
    public static final int GRANT_CLAIM_OWNER_OPINION_MALUS = -City.main.parameters.levels.OpinionLevels.VERY_HIGH;
    public static final int BRIBE_INFLUENCE_GAINED = City.main.parameters.levels.InfluenceLevels.ULTRA_HIGH_1;
    public static final int CRACKDOWN_INFLUENCE_COST = City.main.parameters.levels.InfluenceLevels.ULTRA_HIGH_2;
    // crackdown
    public static final int CRACKDOWN_MONEY_COST = City.main.parameters.levels.GoldLevels.HIGH;
    // deal minimums
    public static final int DEAL_MIN_MONEY = City.main.parameters.levels.GoldLevels.VERY_LOW;
    public static final int ALLOW_SEND_RESOURCES_INFLUENCE_COST = City.main.parameters.levels.InfluenceLevels.VERY_HIGH;
    public static final int BRIBE_CORRUPTION_GAIN = City.main.parameters.levels.CorruptionLevels.LOW;
    public static final int PROPOSE_LEGISLATION_INFLUENCE_COST = City.main.parameters.levels.InfluenceLevels.VERY_HIGH;
    public static final int ALLOW_MERCENARIES_INFLUENCE_COST = City.main.parameters.levels.InfluenceLevels.HIGH;
    public static final int SEND_RESOURCES_OPINION_PER_GOLD = 1;
    public static final int IMPORT_FOOD_MAX_USES = 2;
    // distribute resources
    public static final int DISTRIBUTE_MONEY_COST = City.main.parameters.levels.GoldLevels.LOW;
    public static final int FESTIVAL_INFLUENCE_COST = City.main.parameters.levels.InfluenceLevels.ULTRA_HIGH_1;
    public static final int GRANT_CLAIM_OTHER_CLAIMANT_MALUS = -City.main.parameters.levels.OpinionLevels.VERY_LOW;
    public static final int SEND_RESOURCES_OPINION_DIVISOR = 10;
    // bribe
    public static final int BRIBE_MONEY_COST = City.main.parameters.levels.GoldLevels.MEDIUM;
    public static final int STAT_MAX = 100;
    public static final int DISTRIBUTE_HAPPINESS_GAIN = City.main.parameters.levels.HappinessLevels.MEDIUM;
    public static final int FESTIVAL_DURATION_TURNS = 5;
    // deal favour thresholds (reused)
    public static final double DEAL_FAVOUR_THRESHOLD_1 = 1;
    // royal levy
    public static final int LEVY_INFLUENCE_COST = City.main.parameters.levels.InfluenceLevels.VERY_HIGH;
    public static final int FIGHT_CORRUPTION_MAX_USES = 1;
    public static final int BRIBE_MAX_USES = 2;
    public static final int CORRUPTION_DECAY_PER_TURN = City.main.parameters.levels.CorruptionLevels.VERY_LOW;
    // accept bribes
    public static final int ACCEPT_BRIBE_INFLUENCE_COST = City.main.parameters.levels.InfluenceLevels.HIGH;
    // all constants will be moved here
}