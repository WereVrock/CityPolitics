// ===== CommanderFactory.java =====
package main.army.commander;

import main.politics.PartyManager;
import main.politics.PoliticalParty;

/**
 * Builds randomised Commander instances for the recruitment pool.
 */
public class CommanderFactory {

    private CommanderFactory() {}

public static Commander createRandom(PartyManager partyManager) {
        String         race  = CommanderNameGenerator.randomRace();
        String         name  = CommanderNameGenerator.randomName(race);
        PoliticalParty party = randomParty(partyManager);
        int            skill = Commander.rollSkill();
        return new Commander(name, race, party, skill);
    }

private static PoliticalParty randomParty(PartyManager partyManager) {
        java.util.List<PoliticalParty> parties = partyManager.getParties();
        // Exclude Oracles — they don't field commanders
        java.util.List<PoliticalParty> eligible = new java.util.ArrayList<>();
        for (PoliticalParty p : parties) {
            if (!p.getName().equals("Oracles")) eligible.add(p);
        }
        return eligible.get((int)(Math.random() * eligible.size()));
    }

}