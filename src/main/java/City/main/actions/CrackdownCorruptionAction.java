package City.main.actions;

import City.main.core.CostCalculator;
import City.main.core.GameState;
import City.main.parameters.ActionParams;
 
import City.main.politics.PolitcalView;
import City.main.politics.VoteCondition;
import City.main.resources.ResourcePool;
import City.main.resources.StatBlock;

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
        return "Spend " + ActionParams.CRACKDOWN_MONEY_COST
            + " money and " + ActionParams.CRACKDOWN_INFLUENCE_COST
            + " influence. Reduces corruption by " + ActionParams.CRACKDOWN_CORRUPTION_REDUCTION
            + ". Requires vote.";
    }

    @Override public int getInfluenceCost() { return ActionParams.CRACKDOWN_INFLUENCE_COST; }

    @Override public List<VoteCondition> getVoteConditions() { return CONDITIONS; }

    @Override

public ActionResult applyEffect(ResourcePool resources, StatBlock stats) {
        int moneyCost = CostCalculator.apply(ActionParams.CRACKDOWN_MONEY_COST, stats.getCorruption());
        if (resources.getMoney() < moneyCost) {
            return ActionResult.fail("Not enough money after vote. Need " + moneyCost + ".");
        }
        getLedger().applyOneTime(City.main.resources.ResourceType.GOLD, "action", getName(),
                -moneyCost, resources);
        stats.reduceCorruption(ActionParams.CRACKDOWN_CORRUPTION_REDUCTION);
        return ActionResult.ok("Crackdown executed. Corruption -" + ActionParams.CRACKDOWN_CORRUPTION_REDUCTION + ".");
    }

}