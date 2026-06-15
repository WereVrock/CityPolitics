package City.main.legislation;

import City.main.parameters.ActionParams;
import java.util.*;

/**
 * Tracks which legislations have been passed and manages legislation state.
 * Also owns the wartime state check.
 */
public class LegislationManager {

    private final Set<LegislationType> passedLegislations = new LinkedHashSet<>();
    private final Set<LegislationType> proposedThisTurn   = new LinkedHashSet<>();

    // Mercenary hire action availability (3-turn window after each hire-vote passes)
    private int mercenaryHireActionsRemaining = 0;

    // Wartime taxes cooldown
    private int wartimeTaxesCooldown = 0;

    public LegislationManager() {}

    // ─── Legislation state ────────────────────────────────────────────────────

    public boolean isPassed(LegislationType type) {
        return passedLegislations.contains(type);
    }

    public void markPassed(LegislationType type) {
        passedLegislations.add(type);
        City.debug.Debug.log("legislation", "passed", type.name());
    }

    public Set<LegislationType> getPassedLegislations() {
        return Collections.unmodifiableSet(passedLegislations);
    }

    /** Returns legislation types that can currently be proposed (prerequisites met, not already passed). */
    public List<LegislationType> getProposableLegislations() {
        List<LegislationType> result = new ArrayList<>();
        for (LegislationType type : LegislationType.values()) {
            if (passedLegislations.contains(type)) continue;
            if (meetsPrerequisites(type)) result.add(type);
        }
        return result;
    }

    private boolean meetsPrerequisites(LegislationType type) {
        return switch (type) {
            case MERCENARY_ALLOWANCE_LAW   -> true;
            case MERCENARY_AUTHORIZATION_LAW ->
                passedLegislations.contains(LegislationType.MERCENARY_ALLOWANCE_LAW);
            case WARTIME_TAXES_LAW         -> true;
        };
    }

    // ─── Send resources window ────────────────────────────────────────────────

    private int sendResourcesWindowRemaining = 0;

    public void grantSendResourcesWindow() {
        sendResourcesWindowRemaining = ActionParams.SEND_RESOURCES_WINDOW_TURNS;
        City.debug.Debug.log("legislation", "send-resources-window",
                "Window granted (" + sendResourcesWindowRemaining + " turns)");
    }

    public boolean hasSendResourcesAvailable() {
        return sendResourcesWindowRemaining > 0;
    }

    public int getSendResourcesWindowRemaining() { return sendResourcesWindowRemaining; }

    public void tickSendResourcesWindow() {
        if (sendResourcesWindowRemaining > 0) {
            sendResourcesWindowRemaining--;
            City.debug.Debug.log("legislation", "send-resources-window",
                    "Tick — remaining=" + sendResourcesWindowRemaining);
        }
    }

    public void setSendResourcesWindowRemaining(int v) { sendResourcesWindowRemaining = v; }

    // ─── Mercenary hire window ────────────────────────────────────────────────

    /** Called when a hire-mercenaries vote passes. Grants 3 turns of free hiring. */
    public void grantMercenaryHireWindow() {
        mercenaryHireActionsRemaining = 3;
        City.debug.Debug.log("legislation", "merc-window", "Mercenary hire window granted (3 turns)");
    }

    public boolean hasMercenaryHireAvailable() {
        return isPassed(LegislationType.MERCENARY_AUTHORIZATION_LAW)
                || mercenaryHireActionsRemaining > 0;
    }

    public boolean isMercenaryHireAuthorized() {
        return isPassed(LegislationType.MERCENARY_AUTHORIZATION_LAW);
    }

    /** Called each turn to tick down the hire window. */
    public void tickMercenaryWindow() {
        if (mercenaryHireActionsRemaining > 0) {
            mercenaryHireActionsRemaining--;
            City.debug.Debug.log("legislation", "merc-window",
                    "Mercenary window tick — remaining=" + mercenaryHireActionsRemaining);
        }
    }

    public int getMercenaryHireActionsRemaining() { return mercenaryHireActionsRemaining; }

    // ─── Wartime taxes cooldown ───────────────────────────────────────────────

    public void triggerWartimeTaxesCooldown() {
        wartimeTaxesCooldown = ActionParams.WARTIME_TAXES_COOLDOWN_TURNS;
        City.debug.Debug.log("legislation", "wartime-tax", "Cooldown set to " + wartimeTaxesCooldown);
    }

    public boolean isWartimeTaxesOnCooldown() { return wartimeTaxesCooldown > 0; }

    public int getWartimeTaxesCooldown() { return wartimeTaxesCooldown; }

    public void tickWartimeTaxesCooldown() {
        if (wartimeTaxesCooldown > 0) wartimeTaxesCooldown--;
    }

    // ─── Save/load helpers ────────────────────────────────────────────────────

    public void setPassedLegislations(Set<LegislationType> passed) {
        passedLegislations.clear();
        passedLegislations.addAll(passed);
    }

    public void setMercenaryHireActionsRemaining(int v) { mercenaryHireActionsRemaining = v; }
    public void setWartimeTaxesCooldown(int v)          { wartimeTaxesCooldown = v; }

    // ─── Council event limiting ───────────────────────────────────────────────

    private boolean councilEventUsedThisTurn  = false;
    private boolean realmCouncilUsedThisTurn  = false;

    public boolean isCouncilEventUsedThisTurn()    { return councilEventUsedThisTurn; }
    public void    markCouncilEventUsedThisTurn()   { councilEventUsedThisTurn = true; }
    public void    resetCouncilEventUsed()          { councilEventUsedThisTurn = false; }

    public boolean isRealmCouncilUsedThisTurn()    { return realmCouncilUsedThisTurn; }
    public void    markRealmCouncilUsedThisTurn()   { realmCouncilUsedThisTurn = true; }
    public void    resetRealmCouncilUsed()          { realmCouncilUsedThisTurn = false; }

    public void reset() {
        passedLegislations.clear();
        proposedThisTurn.clear();
        mercenaryHireActionsRemaining  = 0;
        wartimeTaxesCooldown           = 0;
        sendResourcesWindowRemaining   = 0;
        councilEventUsedThisTurn       = false;
    }
}