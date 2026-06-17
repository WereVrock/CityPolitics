package City.main.parameters;

public final class NobleCouncilParams {
     public static final double NOBLE_COUNCIL_PRESTIGE_THRESHOLD    = 0.15;
    public static final int    COUNCIL_ORACLE_IMPRESSION           = 1000;
    public static final int    COUNCIL_PRESTIGIOUS_TOTAL_IMPRESSION= 6500;
    public static final int    COUNCIL_PLAYER_BOOST_INFLUENCE_COST = 10;
    public static final int    COUNCIL_PLAYER_BOOST_IMPRESSION     = 250;
    public static final int    COUNCIL_DEAL_GOLD_PER_IMPRESSION    = 3;
    public static final int    COUNCIL_DEAL_INF_PER_IMPRESSION     = 2;
    public static final int    COUNCIL_DEAL_MP_PER_IMPRESSION      = 4;
    public static final int    COUNCIL_FORTIFICATION_SUPPORT_YEARS = 3;
    public static final int    COUNCIL_BORDER_FORT_COST_PER_ZONE   = 20;
    public static final double COUNCIL_UNLAWFUL_REFUSE_THRESHOLD   = 0.70;
    public static final int    COUNCIL_UNLAWFUL_RECIPIENT_OPINION  = 15;
    public static final int    COUNCIL_UNLAWFUL_OWNER_OPINION      = -20;
    public static final int    COUNCIL_UNLAWFUL_PRESTIGE_LOSS_PER_TURN = -5;
    
    
     // =========================================================
    // ── INTERVENTION OPINIONS (DiplomacyParams continued) ───
    // (kept here because they logically extend diplomacy)
    // =========================================================
    public static final int    INTERVENTION_JOIN_ATTACKER_SELF_OPINION   = 10;
    public static final int    INTERVENTION_JOIN_ATTACKER_VICTIM_OPINION = -15;
    public static final int    INTERVENTION_JOIN_DEFENDER_SELF_OPINION   = 10;
    public static final int    INTERVENTION_JOIN_DEFENDER_ATTACKER_OPINION = -10;
    public static final int    INTERVENTION_STOP_ATTACKER_OPINION = -5;
    public static final int    INTERVENTION_STOP_DEFENDER_OPINION = 5;
    public static final int    INTERVENTION_UNJUSTIFIED_BYSTANDER_OPINION = -5;
    public static final int    COUNCIL_REALM_MAX_EVENTS_PER_TURN          = 1;
    public static final int    UNLAWFUL_RETURN_TURNS                      = 4;
}