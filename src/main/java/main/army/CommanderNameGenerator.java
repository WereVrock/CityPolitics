// ===== CommanderNameGenerator.java =====
package main.army;

import java.util.Random;

/**
 * Procedurally generates commander names and races for the recruitment pool.
 */
public class CommanderNameGenerator {

    private static final Random RNG = new Random();

    private static final String[] HUMAN_FIRST = {
        "Aldric","Syla","Maren","Dorin","Veth","Cassia","Torvald","Elia","Brant","Sera"
    };
    private static final String[] ORC_FIRST = {
        "Grunn","Ashka","Brunn","Mogra","Drek","Skara","Korr","Ultha"
    };
    private static final String[] ELF_FIRST = {
        "Aelith","Soren","Lyrae","Thalos","Miriel","Corin"
    };
    private static final String[] LAST = {
        "Vane","Dorn","Ashfist","Ironclaw","Greyveil","Salthorn",
        "Duskmantle","Redthorn","Coldwater","Brasstone"
    };

    private static final String[] RACES   = { "Human", "Orc", "Elf" };
    private static final String[] TITLES  = { "General", "Commander", "Warchief", "Captain", "Marshal" };

    public static String randomName(String race) {
        String[] pool = switch (race) {
            case "Orc"  -> ORC_FIRST;
            case "Elf"  -> ELF_FIRST;
            default     -> HUMAN_FIRST;
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