package main.actions;

import main.core.GameState;
import main.legislation.LegislationManager;
import main.legislation.LegislationType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Categorised registry of all player actions.
 *
 * Categories:
 *   AUTHORIZED  — free actions (no council needed)
 *   FORMAL      — require council vote each use
 *   REALM       — realm-level actions (may require legislation window)
 *
 * Shared vote counter: only 1 formal action OR legislation per turn.
 */
public class ActionRegistry {

    private final List<PlayerAction> allActions        = new ArrayList<>();
    private final List<PlayerAction> authorizedActions = new ArrayList<>();
    private final List<PlayerAction> formalActions     = new ArrayList<>();
    private final List<PlayerAction> realmActions      = new ArrayList<>();

    // Shared formal/legislation use flag (1 per turn)
    private boolean formalUsedThisTurn = false;

    private final HireMercenariesAction       hireMercenariesAction;
    private final WartimeTaxesAction          wartimeTaxesAction;
    private final AllowMercenariesAction      allowMercenariesAction;
    private final AllowSendResourcesAction    allowSendResourcesAction;
    private final SendResourcesToNoblesAction sendResourcesToNoblesAction;
    private final GrantZoneClaimAction        grantZoneClaimAction;

    private final LegislationManager legislationManager;

    public ActionRegistry(GameState gameState) {
        this.legislationManager = gameState.getLegislationManager();

        // ── Authorized (free) actions ──────────────────────────────────────
        ImportFoodAction importFood         = new ImportFoodAction();
        AcceptBribesAction acceptBribes     = new AcceptBribesAction();
        BribeAction bribe                   = new BribeAction();
        DistributeResourcesAction distribute = new DistributeResourcesAction();
        FightCorruptionAction fightCorruption = new FightCorruptionAction();

        authorizedActions.add(importFood);
        authorizedActions.add(acceptBribes);
        authorizedActions.add(bribe);
        authorizedActions.add(distribute);
        authorizedActions.add(fightCorruption);

        wartimeTaxesAction = new WartimeTaxesAction(
                gameState.getLegislationManager(),
                gameState.getPopManager(),
                gameState.getWarStateChecker());
        hireMercenariesAction = new HireMercenariesAction(
                gameState.getLegislationManager(),
                gameState.getMercenaryManager(),
                gameState.getZoneManager());

        authorizedActions.add(wartimeTaxesAction);
        authorizedActions.add(hireMercenariesAction);

        // ── Formal (voted) actions ─────────────────────────────────────────
        OrganizeFestivalAction festival     = new OrganizeFestivalAction(gameState);
        CrackdownCorruptionAction crackdown = new CrackdownCorruptionAction(gameState);
        RoyalLevyAction levy                = new RoyalLevyAction(gameState);
        allowMercenariesAction              = new AllowMercenariesAction(
                gameState, gameState.getLegislationManager());
        allowSendResourcesAction            = new AllowSendResourcesAction(
                gameState, gameState.getLegislationManager());

        formalActions.add(festival);
        formalActions.add(crackdown);
        formalActions.add(levy);
        formalActions.add(allowMercenariesAction);
        formalActions.add(allowSendResourcesAction);

        // ── Realm actions ──────────────────────────────────────────────────
        sendResourcesToNoblesAction = new SendResourcesToNoblesAction(
                gameState.getLegislationManager(),
                gameState.getNobleHouseManager());
        grantZoneClaimAction = new GrantZoneClaimAction(
                gameState.getNobleHouseManager(),
                gameState.getNobleHouseManager().getClaimManager());

        realmActions.add(sendResourcesToNoblesAction);
        realmActions.add(grantZoneClaimAction);

        // Combine all
        allActions.addAll(authorizedActions);
        allActions.addAll(formalActions);
        allActions.addAll(realmActions);

        // Wire ledger
        main.ledger.Ledger ledger = gameState.getLedger();
        for (PlayerAction action : allActions) {
            if (action instanceof AbstractAction aa) {
                aa.setLedger(ledger);
            }
        }
    }

    // ─── Shared vote counter ──────────────────────────────────────────────────

    public boolean isFormalUsedThisTurn()    { return formalUsedThisTurn; }
    public void    markFormalUsedThisTurn()  {
        formalUsedThisTurn = true;
        debug.Debug.log("action-registry", "formal-used", "Formal/legislation slot consumed this turn");
    }
    public void    resetFormalUsed()         { formalUsedThisTurn = false; }

    public List<PlayerAction> getActions()            { return Collections.unmodifiableList(allActions); }
    public List<PlayerAction> getAuthorizedActions()  { return Collections.unmodifiableList(authorizedActions); }
    public List<PlayerAction> getFormalActions()      { return Collections.unmodifiableList(formalActions); }
    public List<PlayerAction> getRealmActions()       { return Collections.unmodifiableList(realmActions); }

    public HireMercenariesAction       getHireMercenariesAction()       { return hireMercenariesAction; }
    public WartimeTaxesAction          getWartimeTaxesAction()          { return wartimeTaxesAction; }
    public AllowMercenariesAction      getAllowMercenariesAction()       { return allowMercenariesAction; }
    public AllowSendResourcesAction    getAllowSendResourcesAction()     { return allowSendResourcesAction; }
    public SendResourcesToNoblesAction getSendResourcesToNoblesAction()  { return sendResourcesToNoblesAction; }
    public GrantZoneClaimAction        getGrantZoneClaimAction()         { return grantZoneClaimAction; }
    public LegislationManager          getLegislationManager()           { return legislationManager; }

    public void resetAllActions() {
        for (PlayerAction action : allActions) {
            action.resetUses();
        }
        resetFormalUsed();
        debug.Debug.log("action-registry", "reset", "All actions reset for new turn");
    }
}