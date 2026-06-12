package main.actions;

import main.legislation.LegislationManager;
import main.parameters.GameParameters;
import main.resources.ResourcePool;
import main.resources.StatBlock;

/**
 * Wartime Taxes — only available when WarTimeTaxesLaw is passed and at war.
 * Each pop pays 10 gold. Has a 6-turn cooldown.
 */
public class WartimeTaxesAction extends AbstractAction {

    private final LegislationManager  legislationManager;
    private final main.pops.PopManager popManager;
    private final main.barbarians.WarStateChecker warStateChecker;

    public WartimeTaxesAction(LegislationManager legislationManager,
                               main.pops.PopManager popManager,
                               main.barbarians.WarStateChecker warStateChecker) {
        super(1);
        this.legislationManager = legislationManager;
        this.popManager         = popManager;
        this.warStateChecker    = warStateChecker;
    }

    @Override public String getName() { return "Wartime Taxes"; }

    @Override
    public String getDescription() {
        int cooldown = legislationManager.getWartimeTaxesCooldown();
        if (cooldown > 0) return "Each pop pays " + GameParameters.WARTIME_TAXES_GOLD_PER_POP
                + " gold. On cooldown: " + cooldown + " turn(s) remaining.";
        return "Each pop pays " + GameParameters.WARTIME_TAXES_GOLD_PER_POP + " gold.";
    }

    @Override
    public boolean isAvailable() {
        return super.isAvailable()
                && legislationManager.isPassed(main.legislation.LegislationType.WARTIME_TAXES_LAW)
                && warStateChecker.isAtWar()
                && !legislationManager.isWartimeTaxesOnCooldown();
    }

    /** Returns why the action is unavailable, or null if available. */
    public String getUnavailableReason() {
        if (!legislationManager.isPassed(main.legislation.LegislationType.WARTIME_TAXES_LAW))
            return "Requires Wartime Taxes Law to be passed.";
        if (!warStateChecker.isAtWar())
            return "Only available during wartime (barbarian ravagers or warboss present).";
        if (legislationManager.isWartimeTaxesOnCooldown())
            return "On cooldown: " + legislationManager.getWartimeTaxesCooldown() + " turn(s) remaining.";
        return null;
    }

    @Override
    public ActionResult execute(ResourcePool resources, StatBlock stats) {
        if (!isAvailable()) {
            String reason = getUnavailableReason();
            return ActionResult.fail(reason != null ? reason : "Wartime Taxes unavailable.");
        }
        int totalGold = 0;
        for (main.pops.Pop pop : popManager.getPops()) {
            totalGold += pop.getCount() * GameParameters.WARTIME_TAXES_GOLD_PER_POP;
        }
        getLedger().applyOneTime(main.resources.ResourceType.GOLD,
                "action", getName(), totalGold, resources);
        legislationManager.triggerWartimeTaxesCooldown();
        recordUse();
        return ActionResult.ok("Wartime taxes collected: +" + totalGold + " gold.");
    }
}