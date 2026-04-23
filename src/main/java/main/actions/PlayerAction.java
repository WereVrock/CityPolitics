package main.actions;

import main.resources.ResourcePool;
import main.resources.StatBlock;

/**
 * Contract for all player-executable actions.
 */
public interface PlayerAction {

    /** Human-readable name shown in the UI. */
    String getName();

    /** Short description of what the action does. */
    String getDescription();

    /** Maximum times this action may be used per turn. */
    int getMaxUsesPerTurn();

    /** How many times it has been used this turn. */
    int getUsesThisTurn();

    /** Whether the action can still be used this turn. */
    boolean isAvailable();

    /**
     * Attempt to execute the action.
     * Implementations mutate resources/stats if successful.
     */
    ActionResult execute(ResourcePool resources, StatBlock stats);

    /** Reset use counter at the start of each new turn. */
    void resetUses();
}