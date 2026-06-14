package City.main.actions;

import City.main.core.GameState;
import City.main.legislation.LegislationManager;
import City.main.legislation.LegislationType;
import City.main.parameters.ActionParams;
import City.main.politics.PolitcalView;
import City.main.politics.VoteCondition;
import City.main.resources.ResourcePool;
import City.main.resources.StatBlock;

import java.util.List;

/**
 * Formal action to vote on hiring mercenaries (one-time authorization window).
 * Only available after Mercenary Allowance Law is passed.
 * If the vote passes, grants a 3-turn mercenary hire window.
 */
public class AllowMercenariesAction extends AbstractFormalAction {

    private final LegislationManager legislationManager;

    private static final List<VoteCondition> CONDITIONS = List.of(
        new VoteCondition(VoteCondition.Variable.MONEY, VoteCondition.Relation.GREATER_THAN,
                100, 0.5, PolitcalView.MERCANTILE),
        new VoteCondition(VoteCondition.Variable.MANPOWER, VoteCondition.Relation.LESS_THAN,
                50, 0.6, PolitcalView.MILITARIST),
        new VoteCondition(VoteCondition.Variable.CORRUPTION, VoteCondition.Relation.GREATER_THAN,
                40, -0.4, PolitcalView.DEMOCRATIC),
        new VoteCondition(VoteCondition.Variable.HAPPINESS, VoteCondition.Relation.LESS_THAN,
                50, -0.3, null)
    );

    public AllowMercenariesAction(GameState gameState,
                                   LegislationManager legislationManager) {
        super(gameState);
        this.legislationManager = legislationManager;
    }

    @Override public String getName() { return "Allow Mercenaries"; }

    @Override
    public String getDescription() {
        return "Vote to authorize mercenary hiring for 3 turns. Costs "
                + ActionParams.ALLOW_MERCENARIES_INFLUENCE_COST
                + " influence. Requires Mercenary Allowance Law.";
    }

    @Override
    public int getInfluenceCost() {
        return ActionParams.ALLOW_MERCENARIES_INFLUENCE_COST;
    }

    @Override
    public List<VoteCondition> getVoteConditions() { return CONDITIONS; }

    @Override
    public boolean isAvailable() {
        return super.isAvailable()
                && legislationManager.isPassed(LegislationType.MERCENARY_ALLOWANCE_LAW)
                && !legislationManager.isPassed(LegislationType.MERCENARY_AUTHORIZATION_LAW);
    }

    @Override
    public ActionResult applyEffect(ResourcePool resources, StatBlock stats) {
        legislationManager.grantMercenaryHireWindow();
        return ActionResult.ok("Mercenary hiring authorized for 3 turns.");
    }
}