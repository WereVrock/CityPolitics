// ===== CommanderFactory.java =====
package main.army;

import main.politics.PolitcalView;

/**
 * Builds randomised Commander instances for the recruitment pool.
 */
public class CommanderFactory {

    private CommanderFactory() {}

    public static Commander createRandom() {
        String       race        = CommanderNameGenerator.randomRace();
        String       name        = CommanderNameGenerator.randomName(race);
        PolitcalView affiliation = randomAffiliation();
        int          skill       = Commander.rollSkill();
        return new Commander(name, race, affiliation, skill);
    }

    private static PolitcalView randomAffiliation() {
        PolitcalView[] values = PolitcalView.values();
        return values[(int)(Math.random() * values.length)];
    }
}