package City.main.actions;

import City.main.core.GameState;
import City.main.legislation.LegislationManager;
import City.main.legislation.LegislationType;
import City.main.parameters.GameParameters;
import City.main.politics.PolitcalView;
import City.main.politics.VoteCondition;
import City.main.pops.PopManager;
import City.main.resources.ResourcePool;
import City.main.resources.StatBlock;

import java.util.List;

/**
 * Formal action — Wartime Taxes.
 * Only available when WarTimeTaxesLaw is passed and at war.
 * Requires assembly approval. Has a 6-turn cooldown.
 * Each pop pays WARTIME_TAXES_GOLD_PER_POP gold; happiness is reduced.
 */
public class WartimeTaxesAction extends AbstractFormalAction {

    private final LegislationManager            legislationManager;
    private final PopManager                    popManager;
    private final City.main.barbarians.WarStateChecker warStateChecker;

    private static final List<VoteCondition> CONDITIONS = List.of(
        new VoteCondition(VoteCondition.Variable.MONEY,
                VoteCondition.Relation.LESS_THAN, 100, 0.8, null),
        new VoteCondition(VoteCondition.Variable.HAPPINESS,
                VoteCondition.Relation.GREATER_THAN, 40, 0.4, PolitcalView.DEMOCRATIC),
        new VoteCondition(VoteCondition.Variable.CORRUPTION,
                VoteCondition.Relation.GREATER_THAN, 50, -0.6, PolitcalView.DEMOCRATIC),
        new VoteCondition(VoteCondition.Variable.MANPOWER,
                VoteCondition.Relation.GREATER_THAN, 60, 0.3, PolitcalView.MILITARIST)
    );

    public WartimeTaxesAction(GameState gameState,
                               LegislationManager legislationManager,
                               PopManager popManager,
                               City.main.barbarians.WarStateChecker warStateChecker) {
        super(gameState);
        this.legislationManager = legislationManager;
        this.popManager         = popManager;
        this.warStateChecker    = warStateChecker;
    }

    @Override public String getName() { return "Wartime Taxes"; }

    @Override


public String getDescription() {
    int cooldown  = legislationManager.getWartimeTaxesCooldown();
    int gold      = computeGold();
    int happiness = computeHappinessCost();
    if (cooldown > 0)
        return "On cooldown: " + cooldown + " turn(s). Would yield +" + gold
                + " gold, -" + happiness + " happiness.";
    if (!warStateChecker.isAtWar())
        return "Only available during wartime. Would yield +" + gold
                + " gold, -" + happiness + " happiness.";
    return "Each pop pays " + GameParameters.WARTIME_TAXES_GOLD_PER_POP
            + " gold. Yields +" + gold + " gold, -" + happiness + " happiness. Requires vote.";
}

@Override
    public int getInfluenceCost() {
        return GameParameters.WARTIME_TAXES_INFLUENCE_COST;
    }

    @Override
    public List<VoteCondition> getVoteConditions() { return CONDITIONS; }

    @Override

public boolean isAvailable() {
        if (!super.isAvailable()) return false;
        if (!legislationManager.isPassed(LegislationType.WARTIME_TAXES_LAW)) return false;
        if (!warStateChecker.isAtWar()) return false;
        if (legislationManager.isWartimeTaxesOnCooldown()) return false;
        return true;
    }

public String getUnavailableReason() {
        if (!legislationManager.isPassed(LegislationType.WARTIME_TAXES_LAW))
            return "Requires Wartime Taxes Law to be passed.";
        if (!warStateChecker.isAtWar())
            return "Only available during wartime (barbarian ravagers or warboss present).";
        if (legislationManager.isWartimeTaxesOnCooldown())
            return "On cooldown: " + legislationManager.getWartimeTaxesCooldown() + " turn(s).";
        return null;
    }

    @Override
    public ActionResult applyEffect(ResourcePool resources, StatBlock stats) {
        int totalGold = computeGold();
        int happiness = computeHappinessCost();
        getLedger().applyOneTime(City.main.resources.ResourceType.GOLD,
                "action", getName(), totalGold, resources);
        stats.reduceHappiness(happiness);
        legislationManager.triggerWartimeTaxesCooldown();
        City.debug.Debug.log("action", "wartime-taxes",
                "gold=" + totalGold + " happiness=-" + happiness);
        return ActionResult.ok("Wartime taxes collected: +" + totalGold
                + " gold, -" + happiness + " happiness.");
    }

    public int computeGold() {
        int total = 0;
        for (City.main.pops.Pop pop : popManager.getPops()) {
            total += pop.getCount() * GameParameters.WARTIME_TAXES_GOLD_PER_POP;
        }
        return total;
    }

    public int computeHappinessCost() {
        return GameParameters.WARTIME_TAXES_HAPPINESS_COST;
    }
}