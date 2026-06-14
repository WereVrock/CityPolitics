package City.main.mercenaries;

import java.util.List;
import java.util.Random;

/**
 * Generates flavourful names for mercenary companies.
 */
public final class MercenaryNameGenerator {

    private static final Random RNG = new Random();

    private static final String[] ADJECTIVES = {
        "Iron", "Black", "Gold", "Storm", "Rust", "Grim", "Silver",
        "Crimson", "Ash", "Steel", "Pale", "Dusk", "Burnt", "Sharp", "Cold"
    };

    private static final String[] NOUNS = {
        "Swords", "Axes", "Shields", "Lances", "Spears", "Blades",
        "Claws", "Wolves", "Ravens", "Dogs", "Fangs", "Hammers", "Tusks"
    };

    private static final String[] SUFFIXES = {
        "Company", "Band", "Regiment", "Warband", "Company", "Legion",
        "Brigade", "Cohort", "Host", "Pack"
    };

    private MercenaryNameGenerator() {}

    public static String generate() {
        String adj    = ADJECTIVES[RNG.nextInt(ADJECTIVES.length)];
        String noun   = NOUNS[RNG.nextInt(NOUNS.length)];
        String suffix = SUFFIXES[RNG.nextInt(SUFFIXES.length)];
        return "The " + adj + " " + noun + " " + suffix;
    }
}