// ===== CommanderRoster.java =====
package City.main.army.commander;

import City.debug.Debug;
import City.main.army.commander.Commander;
import City.main.parameters.GameParameters;
import City.main.politics.PartyManager;
import City.main.politics.PoliticalParty;
import City.main.resources.ResourcePool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns all living commanders the player has recruited.
 * Handles:
 *  - recruit / dismiss lifecycle
 *  - per-turn influence overcap drain
 *  - party power contribution
 *  - battle-death resolution
 */
public class CommanderRoster {

    private final List<Commander> commanders = new ArrayList<>();
    private final ResourcePool resources;
    private final PartyManager    partyManager;

    public CommanderRoster(ResourcePool resources, PartyManager partyManager) {
        this.resources    = resources;
        this.partyManager = partyManager;
    }

    // -----------------------------------------------------------------------
    // Recruitment & Dismissal
    // -----------------------------------------------------------------------

    /**
     * Recruit a commander from the pool.
     * Costs influence; raises affiliated party's opinion.
     * @return false if player cannot afford.
     */

public boolean recruit(Commander c) {
        int cost = GameParameters.COMMANDER_RECRUIT_BASE_COST;
        if (resources.getInfluence() < cost) return false;
        resources.spendInfluence(cost);
        commanders.add(c);
        if (c.getParty() != null) {
            c.getParty().adjustPlayerOpinion(GameParameters.COMMANDER_RECRUIT_OPINION_GAIN);
        }
        Debug.log("commander-roster", "recruit",
                c.getName() + " party=" + c.getPartyName() + " influence spent=" + cost);
        return true;
    }

/**
     * Dismiss a commander.
     * Costs influence; lowers affiliated party's opinion.
     * @return false if player cannot afford.
     */

public boolean dismiss(Commander c) {
        if (!commanders.contains(c)) return false;
        int cost = GameParameters.COMMANDER_DISMISS_COST;
        if (resources.getInfluence() < cost) return false;
        resources.spendInfluence(cost);
        commanders.remove(c);
        if (c.getParty() != null) {
            c.getParty().adjustPlayerOpinion(-GameParameters.COMMANDER_DISMISS_OPINION_LOSS);
        }
        Debug.log("commander-roster", "dismiss",
                c.getName() + " party=" + c.getPartyName() + " influence spent=" + cost);
        return true;
    }

// -----------------------------------------------------------------------
    // Per-Turn Processing
    // -----------------------------------------------------------------------

    /**
     * Called each turn: drains influence for over-cap commanders.
     * Only alive commanders count toward the cap.
     */

public List<String> processTurnUpkeep() {
        List<String> log = new ArrayList<>();
        long alive = commanders.stream().filter(Commander::isAlive).count();
        int  cap   = GameParameters.COMMANDER_FREE_CAP;
        if (alive > cap) {
            long over  = alive - cap;
            int  drain = (int) Math.ceil(over * GameParameters.COMMANDER_OVERCAP_INFLUENCE_COST);
            resources.spendInfluence(drain);
            log.add("Over commander cap by " + over + ": -" + drain + " influence.");
            Debug.log("commander-roster", "overcap", "over=" + over + " drain=" + drain);
        }
        return log;
    }

/**
     * Returns the total gold upkeep owed this turn for all alive commanders.
     */
    public double getTotalGoldUpkeep() {
        return commanders.stream()
                .filter(Commander::isAlive)
                .mapToDouble(Commander::getUpkeepCost)
                .sum();
    }

    // -----------------------------------------------------------------------
    // Party Power Contribution
    // -----------------------------------------------------------------------

    /**
     * Returns how much power this roster contributes to a given party.
     * (10 per alive commander affiliated with that party.)
     */

public int getPartyPowerContribution(City.main.politics.PoliticalParty party) {
        int count = (int) commanders.stream()
                .filter(c -> c.isAlive() && c.getParty() == party)
                .count();
        return count * GameParameters.COMMANDER_PARTY_POWER_PER_ALIVE;
    }

/**
     * Applies commander party power contributions to all parties.
     * Called each turn after upkeep.
     */

public void applyPartyPowerContributions(PartyManager pm) {
        for (City.main.politics.PoliticalParty party : pm.getParties()) {
            int bonus = getPartyPowerContribution(party);
            if (bonus > 0) {
                party.setPower(Math.min(100, party.getPower() + bonus));
                Debug.log("commander-roster", "party-power",
                        party.getName() + " +" + bonus + " power from commanders");
            }
        }
    }

// -----------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------

public ArrayList<Commander> getAliveCommanders() {
ArrayList<Commander> alive = new ArrayList<>();
for (Commander c : commanders) if (c.isAlive()) alive.add(c);
return alive;
}

public List<Commander> getAllCommanders() {
        return Collections.unmodifiableList(commanders);
    }

/**
 * Adds a pre-built commander directly (used by SaveManager on load).
 * Does not spend resources or adjust party opinion.
 */
public void addRestoredCommander(City.main.army.commander.Commander c) {
    commanders.add(c);
    City.debug.Debug.log("commander-roster", "restore", c.getName() + " restored to roster");
}

/**
 * Clears all commanders. Used by SaveManager before restoring from save.
 */
public void reset() {
    commanders.clear();
    City.debug.Debug.log("commander-roster", "reset", "Roster cleared");
}

}