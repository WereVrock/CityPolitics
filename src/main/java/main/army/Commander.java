// Commander.java
package main.army;

import main.politics.PolitcalView;

/**
 * A named commander attached to a player army.
 * Mirrors NobleCharacter's military skill role for player armies.
 * Commanding skill (1–4) multiplies combat effectiveness exactly as noble military skill does.
 */
public class Commander {

    private final String      name;
    private final String      race;
    private final PolitcalView affiliation;
    private final int         commandingSkill; // 1–4

    public Commander(String name, String race, PolitcalView affiliation, int commandingSkill) {
        this.name            = name;
        this.race            = race;
        this.affiliation     = affiliation;
        this.commandingSkill = Math.max(1, Math.min(4, commandingSkill));
    }

    public String       getName()            { return name; }
    public String       getRace()            { return race; }
    public PolitcalView getAffiliation()     { return affiliation; }
    public int          getCommandingSkill() { return commandingSkill; }
}