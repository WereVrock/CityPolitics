package main.actions;

import main.core.CostCalculator;
import main.core.GameState;
import main.parameters.GameParameters;
import main.politics.PolitcalView;
import main.politics.VoteCondition;
import main.resources.ResourcePool;
import main.resources.StatBlock;

import java.util.List;

/**
 * Formal Action: Crackdown on Corruption
 * High cost in money and influence. Strong corruption reduction.
 * Requires assembly approval.
 */
public class CrackdownCorruptionAction extends AbstractFormalAction {

    private static final List<VoteCondition> CONDITIONS = List.of(// Militarists support crackdowns — order and discipline
        new VoteCondition(VoteCondition.Variable.CORRUPTION, VoteCondition.Relation.GREATER_THAN,
            30, 0.7, PolitcalView.MILITARIST),
        // Democrats support it when corruption is visibly high
        new VoteCondition(VoteCondition.Variable.CORRUPTION, VoteCondition.Relation.GREATER_THAN,
            50, 0.5, PolitcalView.DEMOCRATIC),
        // Mercantile parties dislike disruption to business networks
        new VoteCondition(VoteCondition.Variable.CORRUPTION, VoteCondition.Relation.LESS_THAN,
            60, -0.4, PolitcalView.MERCANTILE),
        // If money is low, even supporters hesitate at the cost
        new VoteCondition(VoteCondition.Variable.MONEY, VoteCondition.Relation.LESS_THAN,
            80, -0.5, null),
        // Traditionalists resist — corruption is just how things work
        new VoteCondition(VoteCondition.Variable.CORRUPTION, VoteCondition.Relation.LESS_THAN,
            70, -0.3, PolitcalView.TRADITIONALIST),
        // Warmongering parties don't care about corruption if army is strong
        new VoteCondition(VoteCondition.Variable.MANPOWER, VoteCondition.Relation.GREATER_THAN,
            100, -0.3, PolitcalView.WARMONGERING)
    );

    public CrackdownCorruptionAction(GameState gameState) {
        super(gameState);
    }

    @Override public String getName() { return "Crackdown on Corruption"; }

    @Override
    public String getDescription() {
        return "Spend " + GameParameters.CRACKDOWN_MONEY_COST
            + " money and " + GameParameters.CRACKDOWN_INFLUENCE_COST
            + " influence. Reduces corruption by " + GameParameters.CRACKDOWN_CORRUPTION_REDUCTION
            + ". Requires vote.";
    }

    @Override public int getInfluenceCost() { return GameParameters.CRACKDOWN_INFLUENCE_COST; }

    @Override public List<VoteCondition> getVoteConditions() { return CONDITIONS; }

    @Override
    protected ActionResult applyEffect(ResourcePool resources, StatBlock stats) {
        int moneyCost = CostCalculator.apply(GameParameters.CRACKDOWN_MONEY_COST, stats.getCorruption());
        if (!resources.spendMoney(moneyCost)) {
            return ActionResult.fail("Not enough money after vote. Need " + moneyCost + ".");
        }
        stats.reduceCorruption(GameParameters.CRACKDOWN_CORRUPTION_REDUCTION);
        return ActionResult.ok("Crackdown executed. Corruption -" + GameParameters.CRACKDOWN_CORRUPTION_REDUCTION + ".");
    }
}