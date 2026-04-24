package main.actions;

import main.core.CostCalculator;
import main.core.GameState;
import main.politics.VoteCondition;
import main.politics.VoteResult;
import main.politics.VotingEngine;
import main.resources.ResourcePool;
import main.resources.StatBlock;

import java.util.List;

/**
 * Base for all formal actions. Handles the voting flow.
 * Subclasses implement costs, effects, and vote conditions.
 */
public abstract class AbstractFormalAction extends AbstractAction implements FormalAction {

    private final GameState    gameState;
    private final VotingEngine votingEngine;

    protected AbstractFormalAction(GameState gameState) {
        super(1); // formal actions: max 1 use per turn
        this.gameState   = gameState;
        this.votingEngine = new VotingEngine();
    }

    @Override
    public final ActionResult execute(ResourcePool resources, StatBlock stats) {
        if (!isAvailable()) {
            return ActionResult.fail(getName() + " already used this turn.");
        }

        int influenceCost = CostCalculator.apply(getInfluenceCost(), stats.getCorruption());
        if (!resources.spendInfluence(influenceCost)) {
            return ActionResult.fail("Not enough influence. Need " + influenceCost + ".");
        }

        VoteResult vote = votingEngine.process(
            gameState.getPartyManager().getParties(),
            getVoteConditions(),
            resources,
            stats
        );

        recordUse();

        if (!vote.isPassed()) {
            return ActionResult.voteFailure(
                getName() + " was rejected by the assembly. " + influenceCost + " influence spent.",
                vote
            );
        }

        ActionResult effect = applyEffect(resources, stats);
        return ActionResult.votePassed(effect.getMessage(), vote);
    }

    /**
     * Called only when the vote passes. Apply the action's actual effect here.
     */
    protected abstract ActionResult applyEffect(ResourcePool resources, StatBlock stats);

    protected GameState getGameState() { return gameState; }
}