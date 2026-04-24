package main.actions;

import main.politics.VoteCondition;
import java.util.List;

/**
 * Marker interface for actions that require an assembly vote.
 */
public interface FormalAction extends PlayerAction {

    /** The influence cost paid regardless of vote outcome. */
    int getInfluenceCost();

    /** Conditions used by VotingEngine to score party votes. */
    List<VoteCondition> getVoteConditions();

    /** Called only when the vote passes. Applies the action's actual effect. */
    ActionResult applyEffect(main.resources.ResourcePool resources, main.resources.StatBlock stats);
}