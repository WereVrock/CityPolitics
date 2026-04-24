package main.politics;

/**
 * Political views that pops and parties can hold.
 * These represent ideological positions, not group identities.
 */
public enum PolitcalView {

    NONE             ("None",             "No political alignment."),
    HUMAN_SUPREMACIST("Human Supremacy","Humans alone should rule the realm."),
    ENVIRONMENTALIST ("Environmentalism", "Protect the land and its creatures."),
    WARMONGERING     ("Warmongering",     "Glory through conquest and strength."),
    DEMOCRATIC       ("Democracy",       "Power should rest with the people and their representatives."),
    ISOLATIONIST     ("Isolationism",     "The realm should look inward. Foreign entanglements bring ruin."),
    MILITARIST       ("Militarism",       "A strong army is the foundation of all prosperity."),
    MERCANTILE       ("Mercantile",       "Trade and wealth are the true measures of power."),
    TRADITIONALIST   ("Traditionalism",   "Ancient ways have kept us alive. Change is dangerous."),
    ARCANE           ("Arcane",           "Knowledge and magic are the highest pursuits of civilization.");

    private final String displayName;
    private final String description;

    PolitcalView(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}