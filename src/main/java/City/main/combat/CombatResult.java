package City.main.combat;

import java.util.List;

/**
 * Outcome of a combat engagement.
 * Supports both single attacker and combined coalition attacks.
 */
public class CombatResult {

    private final String       winnerId;
    private final String       loserId;
    private final int          attackerLosses;
    private final int          defenderLosses;
    private final List<String> log;

    public CombatResult(String winnerId, String loserId,
                        int attackerLosses, int defenderLosses,
                        List<String> log) {
        this.winnerId        = winnerId;
        this.loserId         = loserId;
        this.attackerLosses  = attackerLosses;
        this.defenderLosses  = defenderLosses;
        this.log             = log;
    }

    public String       getWinnerId()       { return winnerId; }
    public String       getLoserId()        { return loserId; }
    public int          getAttackerLosses() { return attackerLosses; }
    public int          getDefenderLosses() { return defenderLosses; }
    public List<String> getLog()            { return log; }
    public boolean      isDraw()            { return winnerId == null; }
}