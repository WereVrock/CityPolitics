package main.actions;

import main.core.GameState;
import main.legislation.LegislationManager;
import main.legislation.LegislationType;
import main.politics.PolitcalView;
import main.politics.VoteCondition;
import main.resources.ResourcePool;
import main.resources.StatBlock;

import java.util.List;

/**
 * Formal action representing a proposed legislation being put to vote.
 * Created dynamically when the player proposes a legislation.
 */
public class ProposeLegislationAction extends AbstractFormalAction {

    private final LegislationType    legislationType;
    private final LegislationManager legislationManager;
    private final List<VoteCondition> conditions;

    public ProposeLegislationAction(GameState gameState,
                                     LegislationType type,
                                     LegislationManager legislationManager) {
        super(gameState);
        this.legislationType    = type;
        this.legislationManager = legislationManager;
        this.conditions         = buildConditions(type);
    }

    @Override public String getName() { return "Legislation: " + legislationType.getDisplayName(); }

    @Override
    public String getDescription() { return legislationType.getDescription(); }

    @Override
    public int getInfluenceCost() {
        return main.parameters.GameParameters.PROPOSE_LEGISLATION_INFLUENCE_COST;
    }

    @Override
    public List<VoteCondition> getVoteConditions() { return conditions; }

    public LegislationType getLegislationType() { return legislationType; }

    @Override
    public ActionResult applyEffect(ResourcePool resources, StatBlock stats) {
        legislationManager.markPassed(legislationType);
        return ActionResult.ok(legislationType.getDisplayName() + " has been passed into law!");
    }

    private static List<VoteCondition> buildConditions(LegislationType type) {
        return switch (type) {
            case MERCENARY_ALLOWANCE_LAW -> List.of(
                new VoteCondition(VoteCondition.Variable.MONEY,
                        VoteCondition.Relation.GREATER_THAN, 100, 0.5, PolitcalView.MERCANTILE),
                new VoteCondition(VoteCondition.Variable.MANPOWER,
                        VoteCondition.Relation.LESS_THAN, 60, 0.6, PolitcalView.MILITARIST),
                new VoteCondition(VoteCondition.Variable.CORRUPTION,
                        VoteCondition.Relation.GREATER_THAN, 40, -0.5, PolitcalView.DEMOCRATIC),
                new VoteCondition(VoteCondition.Variable.HAPPINESS,
                        VoteCondition.Relation.GREATER_THAN, 50, 0.3, null)
            );
            case MERCENARY_AUTHORIZATION_LAW -> List.of(
                new VoteCondition(VoteCondition.Variable.MONEY,
                        VoteCondition.Relation.GREATER_THAN, 150, 0.6, PolitcalView.MERCANTILE),
                new VoteCondition(VoteCondition.Variable.CORRUPTION,
                        VoteCondition.Relation.GREATER_THAN, 50, -0.7, PolitcalView.DEMOCRATIC),
                new VoteCondition(VoteCondition.Variable.MANPOWER,
                        VoteCondition.Relation.LESS_THAN, 40, 0.7, null),
                new VoteCondition(VoteCondition.Variable.HAPPINESS,
                        VoteCondition.Relation.LESS_THAN, 40, -0.4, PolitcalView.TRADITIONALIST)
            );
            case WARTIME_TAXES_LAW -> List.of(
                new VoteCondition(VoteCondition.Variable.MONEY,
                        VoteCondition.Relation.LESS_THAN, 80, 0.7, null),
                new VoteCondition(VoteCondition.Variable.HAPPINESS,
                        VoteCondition.Relation.GREATER_THAN, 50, 0.4, PolitcalView.DEMOCRATIC),
                new VoteCondition(VoteCondition.Variable.CORRUPTION,
                        VoteCondition.Relation.GREATER_THAN, 40, -0.5, PolitcalView.DEMOCRATIC),
                new VoteCondition(VoteCondition.Variable.MANPOWER,
                        VoteCondition.Relation.GREATER_THAN, 80, 0.3, PolitcalView.MILITARIST)
            );
        };
    }
}