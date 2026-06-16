package City.main.politics;

/**
 * Visual emblems (unicode symbols) assigned to each party for display in UI.
 */
public final class PartyEmblemRegistry {

    private static final java.util.Map<String, String> EMBLEMS = new java.util.LinkedHashMap<>();

    static {
        EMBLEMS.put("Knights of Ruan",  "⚔");
        EMBLEMS.put("Dwarven Front",    "⛏");
        EMBLEMS.put("United Axes",      "🪓");
        EMBLEMS.put("Archivists",       "📜");
        EMBLEMS.put("Merchant Union",   "⚖");
        EMBLEMS.put("Democrats",        "🌿");
        EMBLEMS.put("Oracles",          "✦");
        EMBLEMS.put("Noble Houses",     "👑");
    }

    private PartyEmblemRegistry() {}

    public static String getEmblem(String partyName) {
        return EMBLEMS.getOrDefault(partyName, "◆");
    }

    public static String getEmblemAndName(String partyName) {
        return getEmblem(partyName) + " " + partyName;
    }
}