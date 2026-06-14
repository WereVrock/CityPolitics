package City.ui;

import City.main.actions.ActionResult;
import City.main.actions.PlayerAction;
import City.main.core.GameState;
import City.main.legislation.LegislationManager;
import City.main.legislation.LegislationType;
import City.main.actions.ProposeLegislationAction;
import City.main.actions.ActionRegistry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Actions panel with four tabs:
 *   - Authorised (free actions)
 *   - Formal (voted each use) + Legislation — share 1 vote/turn counter shown in header
 *   - Realm (realm-level actions)
 *
 * Formal and Legislation tabs display a shared "0/1 vote used" indicator at top.
 * Actions hidden until their prerequisite legislation passes.
 */
public class ActionsPanel extends JPanel {

    private static final String TAB_AUTH  = "Authorised";
    private static final String TAB_FORMAL= "Formal";
    private static final String TAB_LEG   = "Legislation";
    private static final String TAB_REALM = "Realm";

    private final GameState              gameState;
    private final Consumer<ActionResult> onResult;
    private final JPanel                 cardPanel;
    private final CardLayout             cardLayout;
    private final List<ActionButton>     allButtons = new ArrayList<>();

    private JButton tabAuth;
    private JButton tabFormal;
    private JButton tabLeg;
    private JButton tabRealm;
    private JLabel  sharedVoteLabel;
    private String  currentTab = TAB_AUTH;

    public ActionsPanel(GameState gameState, Consumer<ActionResult> onResult) {
        this.gameState  = gameState;
        this.onResult   = onResult;
        this.cardLayout = new CardLayout();
        this.cardPanel  = new JPanel(cardLayout);
        cardPanel.setBackground(UITheme.BG_DARK);

        setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(8, 8, 4, 8));

        JPanel north = new JPanel(new BorderLayout(0, 2));
        north.setBackground(UITheme.BG_DARK);
        north.add(buildTabBar(),       BorderLayout.NORTH);
        north.add(buildVoteCounter(),  BorderLayout.SOUTH);

        add(north,      BorderLayout.NORTH);
        add(cardPanel,  BorderLayout.CENTER);

        buildCards();
        showTab(TAB_AUTH);
    }

    // ─── Tab bar ─────────────────────────────────────────────────────────────

    private JPanel buildTabBar() {
        JPanel bar = new JPanel(new GridLayout(1, 4, 2, 0));
        bar.setBackground(UITheme.BG_PANEL);
        bar.setBorder(new EmptyBorder(4, 0, 4, 0));

        tabAuth  = makeTabButton("⚔ Authorised", TAB_AUTH);
        tabFormal= makeTabButton("⚑ Formal",     TAB_FORMAL);
        tabLeg   = makeTabButton("📜 Legislation", TAB_LEG);
        tabRealm = makeTabButton("🏛 Realm",       TAB_REALM);

        bar.add(tabAuth);
        bar.add(tabFormal);
        bar.add(tabLeg);
        bar.add(tabRealm);
        return bar;
    }

    private JPanel buildVoteCounter() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        row.setBackground(UITheme.BG_DARK);
        sharedVoteLabel = new JLabel();
        sharedVoteLabel.setFont(UITheme.FONT_SMALL);
        refreshVoteCounter();
        row.add(sharedVoteLabel);
        return row;
    }

    private void refreshVoteCounter() {
        if (sharedVoteLabel == null) return;
        boolean used = gameState.getActionRegistry().isFormalUsedThisTurn();
        boolean pending = gameState.hasActiveSession();
        if (pending) {
            sharedVoteLabel.setText("⚑ Vote pending — no further formal action this turn");
            sharedVoteLabel.setForeground(UITheme.TEXT_GOLD);
        } else if (used) {
            sharedVoteLabel.setText("⚑ Formal/Legislation vote used this turn (0/1 remaining)");
            sharedVoteLabel.setForeground(UITheme.TEXT_RED);
        } else {
            sharedVoteLabel.setText("⚑ Formal/Legislation vote available (1 remaining)");
            sharedVoteLabel.setForeground(UITheme.TEXT_GREEN);
        }
    }

    private JButton makeTabButton(String label, String tab) {
        JButton btn = new JButton(label);
        btn.setFont(UITheme.FONT_BUTTON);
        btn.setForeground(UITheme.TEXT_SECONDARY);
        btn.setBackground(UITheme.BUTTON_BG);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> showTab(tab));
        return btn;
    }

private void showTab(String tab) {
    currentTab = tab;
    cardLayout.show(cardPanel, tab);
    Color active   = UITheme.TEXT_GOLD;
    Color inactive = UITheme.TEXT_SECONDARY;
    Color bgActive = new Color(35, 28, 52);
    tabAuth.setForeground(TAB_AUTH.equals(tab)    ? active : inactive);
    tabFormal.setForeground(TAB_FORMAL.equals(tab)? active : inactive);
    tabLeg.setForeground(TAB_LEG.equals(tab)      ? active : inactive);
    tabRealm.setForeground(TAB_REALM.equals(tab)  ? active : inactive);
    tabAuth.setBackground(TAB_AUTH.equals(tab)    ? bgActive : UITheme.BUTTON_BG);
    tabFormal.setBackground(TAB_FORMAL.equals(tab)? bgActive : UITheme.BUTTON_BG);
    tabLeg.setBackground(TAB_LEG.equals(tab)      ? bgActive : UITheme.BUTTON_BG);
    tabRealm.setBackground(TAB_REALM.equals(tab)  ? bgActive : UITheme.BUTTON_BG);
}

// ─── Cards ───────────────────────────────────────────────────────────────

    private void buildCards() {
        cardPanel.removeAll();
        allButtons.clear();

        cardPanel.add(buildAuthorisedCard(), TAB_AUTH);
        cardPanel.add(buildFormalCard(),     TAB_FORMAL);
        cardPanel.add(buildLegislationCard(),TAB_LEG);
        cardPanel.add(buildRealmCard(),      TAB_REALM);
    }

    private JScrollPane wrapInScroll(JPanel inner) {
        inner.setBackground(UITheme.BG_DARK);
        JScrollPane sp = new JScrollPane(inner);
        sp.setBorder(null);
        sp.setBackground(UITheme.BG_DARK);
        sp.getViewport().setBackground(UITheme.BG_DARK);
        sp.getVerticalScrollBar().setBackground(UITheme.BG_PANEL);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return sp;
    }

    private JPanel buildAuthorisedCard() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(UITheme.BG_DARK);
        outer.add(makeDescLabel("These actions take effect immediately without council approval."),
                BorderLayout.NORTH);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.setBackground(UITheme.BG_DARK);

        LegislationManager lm = gameState.getLegislationManager();
        for (PlayerAction action : gameState.getActionRegistry().getAuthorizedActions()) {
            // Hide legislation-gated actions if law not passed
            if (action instanceof City.main.actions.HireMercenariesAction
                    && !lm.isPassed(LegislationType.MERCENARY_ALLOWANCE_LAW)) continue;
            if (action instanceof City.main.actions.WartimeTaxesAction
                    && !lm.isPassed(LegislationType.WARTIME_TAXES_LAW)) continue;

            ActionButton btn = makeActionButton(action);
            allButtons.add(btn);
            buttons.add(btn);
            buttons.add(Box.createVerticalStrut(6));
        }
        outer.add(wrapInScroll(buttons), BorderLayout.CENTER);
        return outer;
    }

private JPanel buildFormalCard() {
    JPanel outer = new JPanel(new BorderLayout());
    outer.setBackground(UITheme.BG_DARK);
    outer.add(makeDescLabel(
            "These actions require assembly approval. Shares the single formal vote per turn."),
            BorderLayout.NORTH);

    JPanel buttons = new JPanel();
    buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
    buttons.setBackground(UITheme.BG_DARK);

    LegislationManager lm = gameState.getLegislationManager();
    boolean formalUsed = gameState.getActionRegistry().isFormalUsedThisTurn();
    boolean hasPending = gameState.hasActiveSession();

    for (PlayerAction action : gameState.getActionRegistry().getFormalActions()) {
        // Hide Allow Mercenaries if authorization law already passed
        if (action instanceof City.main.actions.AllowMercenariesAction
                && lm.isPassed(LegislationType.MERCENARY_AUTHORIZATION_LAW)) continue;
        // Hide Allow Mercenaries if base law not passed
        if (action instanceof City.main.actions.AllowMercenariesAction
                && !lm.isPassed(LegislationType.MERCENARY_ALLOWANCE_LAW)) continue;
        // Hide WartimeTaxes until law is passed
        if (action instanceof City.main.actions.WartimeTaxesAction
                && !lm.isPassed(LegislationType.WARTIME_TAXES_LAW)) continue;

        ActionButton btn = makeActionButton(action);
        allButtons.add(btn);
        buttons.add(btn);
        buttons.add(Box.createVerticalStrut(6));
    }
    outer.add(wrapInScroll(buttons), BorderLayout.CENTER);
    return outer;
}

private JPanel buildLegislationCard() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(UITheme.BG_DARK);
        outer.add(makeDescLabel(
                "<html>Propose a law to be voted on. Shares the single formal vote per turn.<br>"
                + "Passed laws unlock new actions permanently.</html>"),
                BorderLayout.NORTH);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.setBackground(UITheme.BG_DARK);

        List<LegislationType> proposable = gameState.getActionRegistry()
                .getLegislationManager().getProposableLegislations();

        if (proposable.isEmpty()) {
            JLabel none = new JLabel("  No legislation available to propose.");
            none.setFont(UITheme.FONT_SMALL);
            none.setForeground(UITheme.TEXT_SECONDARY);
            buttons.add(none);
        } else {
            for (LegislationType type : proposable) {
                buttons.add(buildLegislationCard(type));
                buttons.add(Box.createVerticalStrut(6));
            }
        }

        buttons.add(Box.createVerticalStrut(10));
        JLabel passedHeader = new JLabel("PASSED LAWS:");
        passedHeader.setFont(UITheme.FONT_SMALL);
        passedHeader.setForeground(UITheme.TEXT_SECONDARY);
        buttons.add(passedHeader);
        for (LegislationType type : gameState.getActionRegistry()
                .getLegislationManager().getPassedLegislations()) {
            JLabel lbl = new JLabel("  ✓ " + type.getDisplayName());
            lbl.setFont(UITheme.FONT_SMALL);
            lbl.setForeground(UITheme.TEXT_GREEN);
            buttons.add(lbl);
        }

        outer.add(wrapInScroll(buttons), BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildRealmCard() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(UITheme.BG_DARK);
        outer.add(makeDescLabel(
                "<html>Realm actions affect the broader realm and noble relations.<br>"
                + "Some require legislative authorisation.</html>"),
                BorderLayout.NORTH);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.setBackground(UITheme.BG_DARK);

        LegislationManager lm = gameState.getLegislationManager();
        for (PlayerAction action : gameState.getActionRegistry().getRealmActions()) {
            // Hide Send Resources if not authorised
            if (action instanceof City.main.actions.SendResourcesToNoblesAction
                    && !lm.hasSendResourcesAvailable()) continue;

            ActionButton btn = makeActionButton(action);
            allButtons.add(btn);
            buttons.add(btn);
            buttons.add(Box.createVerticalStrut(6));
        }

        if (gameState.getActionRegistry().getRealmActions().stream()
                .noneMatch(a -> !(a instanceof City.main.actions.SendResourcesToNoblesAction)
                        || lm.hasSendResourcesAvailable())) {
            // If all realm actions hidden, show placeholder
            boolean anyVisible = false;
            for (PlayerAction a : gameState.getActionRegistry().getRealmActions()) {
                if (a instanceof City.main.actions.SendResourcesToNoblesAction) {
                    if (lm.hasSendResourcesAvailable()) { anyVisible = true; break; }
                } else {
                    anyVisible = true; break;
                }
            }
            if (!anyVisible) {
                JLabel none = new JLabel("  No realm actions currently available.");
                none.setFont(UITheme.FONT_SMALL);
                none.setForeground(UITheme.TEXT_SECONDARY);
                buttons.add(none);
            }
        }

        outer.add(wrapInScroll(buttons), BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildLegislationCard(LegislationType type) {
        JPanel card = new JPanel(new BorderLayout(8, 0));
        card.setBackground(UITheme.BG_PANEL);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                new EmptyBorder(6, 10, 6, 10)));
        card.setAlignmentX(LEFT_ALIGNMENT);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBackground(UITheme.BG_PANEL);

        JLabel nameLabel = new JLabel(type.getDisplayName());
        nameLabel.setFont(UITheme.FONT_BUTTON);
        nameLabel.setForeground(UITheme.TEXT_GOLD);

        JLabel descLabel = new JLabel("<html><body style='width:220px'>"
                + type.getDescription() + "</body></html>");
        descLabel.setFont(UITheme.FONT_SMALL);
        descLabel.setForeground(UITheme.TEXT_SECONDARY);

        text.add(nameLabel);
        text.add(descLabel);

        boolean canVote = !gameState.hasActiveSession()
                && !gameState.getActionRegistry().isFormalUsedThisTurn();
        JButton proposeBtn = new JButton("PROPOSE");
        proposeBtn.setFont(UITheme.FONT_BUTTON);
        proposeBtn.setForeground(UITheme.TEXT_GOLD);
        proposeBtn.setBackground(UITheme.BUTTON_BG);
        proposeBtn.setBorderPainted(false);
        proposeBtn.setFocusPainted(false);
        proposeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        proposeBtn.setEnabled(canVote);
        if (!canVote) proposeBtn.setToolTipText("Formal/legislation vote already used this turn.");
        proposeBtn.addActionListener(e -> proposeLegislation(type));

        card.add(text,       BorderLayout.CENTER);
        card.add(proposeBtn, BorderLayout.EAST);
        return card;
    }

    private void proposeLegislation(LegislationType type) {
        if (gameState.hasActiveSession()) {
            onResult.accept(City.main.actions.ActionResult.fail("A vote is already pending this turn."));
            return;
        }
        if (gameState.getActionRegistry().isFormalUsedThisTurn()) {
            onResult.accept(City.main.actions.ActionResult.fail(
                    "Only one formal action or legislation per turn."));
            return;
        }
        ProposeLegislationAction action = new ProposeLegislationAction(
                gameState, type, gameState.getLegislationManager());
        action.setLedger(gameState.getLedger());
        ActionResult result = action.execute(gameState.getResources(), gameState.getStats());
        // Mark shared slot used if vote was created
        if (result.isPending()) {
            gameState.getActionRegistry().markFormalUsedThisTurn();
        }
        onResult.accept(result);
        refresh();
    }

    private ActionButton makeActionButton(PlayerAction action) {
        return new ActionButton(gameState, action, result -> {
            onResult.accept(result);
            refresh();
        });
    }

    private JLabel makeDescLabel(String html) {
        JLabel l = new JLabel("<html><body style='width:100%'>" + html + "</body></html>");
        l.setFont(UITheme.FONT_SMALL);
        l.setForeground(UITheme.TEXT_SECONDARY);
        l.setBorder(new EmptyBorder(4, 2, 8, 2));
        return l;
    }

public void refresh() {
    String tabToRestore = currentTab;
    for (ActionButton btn : allButtons) {
        btn.refresh();
    }
    buildCards();
    refreshVoteCounter();
    // Restore the previously selected tab so UI doesn't jump back to Authorised
    showTab(tabToRestore);
    revalidate();
    repaint();
}

public String getCurrentTab() { return currentTab; }

public void showTabByName(String tab) {
    if (tab == null) return;
    showTab(tab);
}

}