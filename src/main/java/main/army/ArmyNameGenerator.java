package main.army;

import java.util.Random;

/**
 * Generates flavourful names for player armies.
 */
public final class ArmyNameGenerator {

    private static final Random RNG = new Random();

    private static final String[] ADJECTIVES = {
        "Iron", "Frost", "Ember", "Stone", "Shadow", "Silver", "Crimson",
        "Thunder", "Ash", "Dusk", "Steel", "Blood", "Gold", "Storm", "Dark",
        "Pale", "Grim", "Swift", "Bone", "Ancient"
    };

    private static final String[] NOUNS = {
        "Legion", "Guard", "Watch", "Host", "Company", "Vanguard", "Shield",
        "Spear", "Blade", "Banner", "Order", "Wolves", "Ravens", "Fang",
        "Claw", "Keep", "Hammer", "Warband", "Lance", "Tide"
    };

    private static final String[] PREFIXES = {
        "The", "Order of the", "Company of the", "Brotherhood of the",
        "Sons of", "Wardens of"
    };

    private ArmyNameGenerator() {}

    /**
     * Generates a random army name such as "The Iron Legion" or "Order of the Frost Shield".
     */
    public static String generate() {
        String prefix = PREFIXES[RNG.nextInt(PREFIXES.length)];
        String adj    = ADJECTIVES[RNG.nextInt(ADJECTIVES.length)];
        String noun   = NOUNS[RNG.nextInt(NOUNS.length)];
        return prefix + " " + adj + " " + noun;
    }

    /**
     * Generates a name guaranteed not to match any existing army display name.
     */
    public static String generateUnique(java.util.List<String> existingNames) {
        String candidate;
        int attempts = 0;
        do {
            candidate = generate();
            attempts++;
        } while (existingNames.contains(candidate) && attempts < 100);
        return candidate;
    }
}