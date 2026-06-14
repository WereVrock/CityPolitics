// ===== CommanderRecruitPool.java =====
package City.main.army.commander;

import City.debug.Debug;
import City.main.parameters.CommanderParams;
 
import City.main.resources.ResourcePool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the pool of commanders available for recruitment.
 * Base pool: 3 candidates. Player may pay influence to reveal 3 more.
 */
public class CommanderRecruitPool {

    private final List<Commander> pool = new ArrayList<>();
    private final ResourcePool    resources;
    private boolean               refreshUsedThisTurn = false;

    private final City.main.politics.PartyManager partyManager;

    public CommanderRecruitPool(ResourcePool resources,
                                City.main.politics.PartyManager partyManager) {
        this.resources    = resources;
        this.partyManager = partyManager;
        refillBase();
    }

    /** Called at the start of each turn to rotate the base pool. */

/** Called at the start of each turn to rotate the base pool and reset refresh flag. */
    public void newTurnRefresh() {
        pool.clear();
        refillBase();
        refreshUsedThisTurn = false;
        Debug.log("recruit-pool", "new-turn", "Pool refreshed for new turn.");
    }

private void refillBase() {
        for (int i = 0; i < CommanderParams.COMMANDER_POOL_BASE_SIZE; i++) {
            pool.add(CommanderFactory.createRandom(partyManager));
        }
    }

/**
     * Pay influence to add 3 more candidates.
     * @return false if player cannot afford it.
     */

/**
     * Pay influence to add 3 more candidates. Can only be used once per turn.
     * @return false if already used this turn or player cannot afford.
     */

public boolean refreshPool() {
        if (refreshUsedThisTurn) {
            Debug.log("recruit-pool", "refresh-denied", "Already refreshed this turn.");
            return false;
        }
        int cost = CommanderParams.COMMANDER_POOL_REFRESH_COST;
        if (!resources.spendInfluence(cost)) {
            Debug.log("recruit-pool", "refresh-denied", "Cannot afford cost=" + cost);
            return false;
        }
        refreshUsedThisTurn = true;
        for (int i = 0; i < CommanderParams.COMMANDER_POOL_REFRESH_SIZE; i++) {
            pool.add(CommanderFactory.createRandom(partyManager));
        }
        Debug.log("recruit-pool", "refresh", "Added " + CommanderParams.COMMANDER_POOL_REFRESH_SIZE
                + " candidates. Total pool=" + pool.size());
        return true;
    }

/** Remove a specific candidate after recruitment. */
    public void removeCandidate(Commander c) {
        pool.remove(c);
    }

    public List<Commander> getCandidates() {
        return Collections.unmodifiableList(pool);
    }

/** True if the extra-candidate refresh has already been used this turn. */
    public boolean isRefreshUsedThisTurn() { return refreshUsedThisTurn; }

}