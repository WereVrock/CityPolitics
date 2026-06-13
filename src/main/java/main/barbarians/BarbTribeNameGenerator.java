package main.barbarians;

import java.util.List;
import java.util.Random;

/**
 * Generates barbarian army display names based on tribal lore.
 *
 * LORE: The barbarians are not conquerors by nature — they are refugees.
 * The Frost Giants march from the uttermost north, consuming everything.
 * The barbarian tribes flee south in desperate waves, pillaging to survive.
 * The first tribes to arrive are the fastest and most desperate —
 * half-starved, travelling light, driven by terror rather than glory.
 * Later waves are the full warbands: organized, armed, and brutal.
 */
public class BarbTribeNameGenerator {

    private static final Random RNG = new Random();

    // ── Early/fleeing tribes (fast, small, desperate) ─────────────────────────
    private static final List<String> EARLY_RAIDER_NAMES = List.of(
        "Ash-Runners",
        "Frostbit Scouts",
        "The Hollow Ones",
        "Shatter-Born",
        "The Starving Blades",
        "Ember Walkers",
        "Pale Remnants",
        "The Fleeing Knives"
    );

    private static final List<String> EARLY_RAVAGER_NAMES = List.of(
        "The Frost-Bitten",
        "Remnants of Grol",
        "Broken Tusks",
        "The Cold-Driven",
        "Shadow Remnant"
    );

    // ── Standard wave tribes (organized warbands) ─────────────────────────────
    private static final List<String> STANDARD_RAIDER_NAMES = List.of(
        "Iron Skulls",
        "The Red Horde",
        "Stoneclaw Raiders",
        "Bloodmarch Kin",
        "The Howling Axes",
        "Dusk Ravagers",
        "The Ashen Tide",
        "Grim-Brow Clan"
    );

    private static final List<String> STANDARD_RAVAGER_NAMES = List.of(
        "The Ruination",
        "Hammerfell Warband",
        "The Grinding Horde",
        "Bonemarch Ravagers",
        "The Crushing Tide",
        "Ironjaw Warband"
    );

    private static final List<String> WARBOSS_NAMES = List.of(
        "The Grand Horde of Grul'khan",
        "Warlord Thrak's Legion",
        "The Unbroken Tide",
        "Horde of the Dying North",
        "The Final Wave"
    );

    public static String generateWarbossName() {
        return WARBOSS_NAMES.get(RNG.nextInt(WARBOSS_NAMES.size()));
    }

    public static String generateRaiderName(boolean earlyWave) {
        List<String> pool = earlyWave ? EARLY_RAIDER_NAMES : STANDARD_RAIDER_NAMES;
        return pool.get(RNG.nextInt(pool.size()));
    }

    public static String generateRavagerName(boolean earlyWave) {
        List<String> pool = earlyWave ? EARLY_RAVAGER_NAMES : STANDARD_RAVAGER_NAMES;
        return pool.get(RNG.nextInt(pool.size()));
    }
}