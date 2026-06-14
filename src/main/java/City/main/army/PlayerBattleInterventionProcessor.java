package City.main.army;

import City.debug.Debug;
import City.main.combat.ArmyForce;
import City.main.combat.CombatResolver;
import City.main.combat.CombatResult;
import City.main.nobles.NobleArmy;
import City.main.nobles.NobleHouse;
import City.main.nobles.NobleHouseManager;
import City.main.parameters.GameParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the player joining or stopping a noble vs noble battle.
 * Called when a player army is in a zone where a noble attack order resolves.
 */
public class PlayerBattleInterventionProcessor {

    public enum PlayerChoice { JOIN_ATTACKER, JOIN_DEFENDER, IGNORE, STOP_FIGHT }

    public interface InterventionCallback {
        /**
         * Ask the player what to do about a battle in zoneId.
         * @param attackerName attacker house name
         * @param defenderName defender house name
         * @param zoneId       zone being attacked
         * @param playerSize   total player army size in zone
         * @param attackerSize total attacker army size
         * @return player's choice
         */
        PlayerChoice ask(String attackerName, String defenderName,
                         String zoneId, int playerSize, int attackerSize);
    }

    private InterventionCallback callback;

    public void setCallback(InterventionCallback cb) { this.callback = cb; }

    /**
     * Called just before a noble attack resolves.
     * If player has army in zone that is >= attacker size, prompt for intervention.
     * Returns the player's choice, or IGNORE if conditions not met or no callback.
     */
    public PlayerChoice checkIntervention(
            NobleHouse attacker, NobleHouse defender, String zoneId,
            ArmyManager playerArmyManager) {

        if (callback == null) return PlayerChoice.IGNORE;

        // Sum player armies in the defender's zone
        int playerSize = 0;
        for (Army a : playerArmyManager.getDeployedArmies()) {
            if (zoneId.equals(a.getZoneId()) && a.isAlive()) playerSize += a.getSize();
        }
        if (playerSize <= 0) return PlayerChoice.IGNORE;

        // Player must be at least as large as the attacker
        int attackerSize = 0;
        // We can only estimate; the actual army size is passed in
        // Count attacker armies (best effort, we check after combat setup)
        // This is called from NobleArmyManager so we can pass size separately
        // For simplicity, always show if player army is present
        if (playerSize <= 0) return PlayerChoice.IGNORE;

        PlayerChoice choice = callback.ask(
                attacker.getName(), defender.getName(),
                zoneId, playerSize, attackerSize);
        Debug.log("player-battle", "intervention", attacker.getName()
                + " vs " + defender.getName() + " at " + zoneId
                + " — player=" + playerSize + " choice=" + choice);
        return choice;
    }

    /**
     * Full version: called with known attacker size for the minimum-size check.
     */
    public PlayerChoice checkIntervention(
            NobleHouse attacker, NobleHouse defender, String zoneId,
            int totalAttackerSize, ArmyManager playerArmyManager) {

        if (callback == null) return PlayerChoice.IGNORE;

        int playerSize = 0;
        for (Army a : playerArmyManager.getDeployedArmies()) {
            if (zoneId.equals(a.getZoneId()) && a.isAlive()) playerSize += a.getSize();
        }
        if (playerSize <= 0) return PlayerChoice.IGNORE;

        // Must be at least as large as attacker
        if (playerSize < totalAttackerSize) return PlayerChoice.IGNORE;

        return callback.ask(attacker.getName(), defender.getName(),
                zoneId, playerSize, totalAttackerSize);
    }
}