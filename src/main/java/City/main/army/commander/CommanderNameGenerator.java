// ===== CommanderNameGenerator.java =====
package City.main.army.commander;

import java.util.Random;

/**
 * Procedurally generates commander names and races for the recruitment pool.
 */
public class CommanderNameGenerator {

    private static final Random RNG = new Random();

    // Expanded human first names (was 10, now 20)
    private static final String[] HUMAN_FIRST = {
        "Aldric", "Syla", "Maren", "Dorin", "Veth", "Cassia", "Torvald", "Elia", "Brant", "Sera",
        "Roland", "Fenna", "Gareth", "Liana", "Conrad", "Tamsin", "Harlan", "Brenna", "Oswin", "Lyra"
    };

    // Expanded orc first names (was 8, now 16)
    private static final String[] ORC_FIRST = {
        "Grunn", "Ashka", "Brunn", "Mogra", "Drek", "Skara", "Korr", "Ultha",
        "Thrag", "Vorla", "Gromm", "Zarqa", "Harg", "Mazoga", "Urzul", "Gortak"
    };

    // Expanded elf first names (was 6, now 15)
    private static final String[] ELF_FIRST = {
        "Aelith", "Soren", "Lyrae", "Thalos", "Miriel", "Corin",
        "Faelan", "Nimue", "Caelum", "Elara", "Thandril", "Ithil", "Orophin", "Silvara", "Valandor"
    };

    // New dwarf first names (15 entries)
    private static final String[] DWARF_FIRST = {
        "Borri", "Helga", "Thrain", "Grunni", "Kazra", "Durin", "Torvi", "Orin",
        "Brynja", "Magni", "Hilda", "Storri", "Anvildottir", "Vigdis", "Jorund"
    };

    // Expanded last names (was 10, now 18)
    private static final String[] LAST = {
        "Vane", "Dorn", "Ashfist", "Ironclaw", "Greyveil", "Salthorn",
        "Duskmantle", "Redthorn", "Coldwater", "Brasstone",
        "Hammerfall", "Grimbeard", "Swiftarrow", "Doomforged", "Blightwood",
        "Stonewall", "Embervein", "Moonshadow"
    };

    // Expanded titles (was 5, now 8)
    private static final String[] TITLES = {
        "General", "Commander", "Warchief", "Captain", "Marshal",
        "Legate", "Ser", "Count"
    };

    // Races: added Dwarf
    private static final String[] RACES = { "Human", "Orc", "Elf", "Dwarf" };

    public static String randomName(String race) {
        String[] pool = switch (race) {
            case "Orc"   -> ORC_FIRST;
            case "Elf"   -> ELF_FIRST;
            case "Dwarf" -> DWARF_FIRST;
            default      -> HUMAN_FIRST;
        };
        String first = pool[RNG.nextInt(pool.length)];
        String last  = LAST[RNG.nextInt(LAST.length)];
        String title = TITLES[RNG.nextInt(TITLES.length)];
        return title + " " + first + " " + last;
    }

    public static String randomRace() {
        return RACES[RNG.nextInt(RACES.length)];
    }
}