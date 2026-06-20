package City.ui.bank;

import City.main.bank.DragonBankLoan;
import City.main.bank.DragonBankManager;
import City.main.core.GameState;
import City.main.parameters.DragonBankParams;
import City.ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Player-facing panel for the Dragon Bank. The player can only borrow here —
 * deposits offer no benefit to the player's treasury. Loans carry high
 * interest and a courier delay before gold arrives.
 */
public class DragonBankPanel extends JPanel {

    private final GameState gameState;
    private final Runnable  onBack;

    private JLabel infoLabel;
    private JLabel rateLabel;
    private JLabel maxLoanLabel;
    private JLabel playerGoldLabel;

    private JPanel pendingLoanPanel;
    private JLabel pendingLoanLabel;

    private JPanel banPanel;
    private JLabel banLabel;

    private JPanel  loanActivePanel;
    private JLabel  loanOwedLabel;
    private JLabel  loanNextLabel;
    private JLabel  loanCountLabel;
    private JButton payInstallBtn;
    private JButton repayFullBtn;
    private JLabel  noLoanLabel;

    private JTextField amountField;
    private JButton    borrowBtn;

    private JTextArea logArea;

    public DragonBankPanel(GameState gameState, Runnable onBack) {
        this.gameState = gameState;
        this.onBack    = onBack;
        setLayout(new BorderLayout(0, 6));
        setBackground(UITheme.BG_DARK);
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);
        refresh();
    }

    // ─── Header ──────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(UITheme.BG_PANEL_LIGHT);
        h.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel title = new JLabel("🐉  THE DRAGON'S BANK");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(new Color(200, 120, 255));

        JButton back = btn("← BACK", UITheme.TEXT_SECONDARY);
        back.addActionListener(e -> onBack.run());

        h.add(title, BorderLayout.CENTER);
        h.add(back,  BorderLayout.EAST);
        return h;
    }

    // ─── Body ────────────────────────────────────────────────────────────

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setBackground(UITheme.BG_DARK);
        body.setBorder(new EmptyBorder(10, 14, 6, 14));

        JPanel infoRow = new JPanel(new GridLayout(1, 2, 8, 0));
        infoRow.setBackground(UITheme.BG_DARK);
        infoRow.add(buildInfoCard());
        infoRow.add(buildRatesCard());

        JPanel mid = new JPanel(new BorderLayout(0, 8));
        mid.setBackground(UITheme.BG_DARK);
        mid.add(buildStatusSection(),      BorderLayout.NORTH);
        mid.add(buildTransactionSection(), BorderLayout.CENTER);

        body.add(infoRow,         BorderLayout.NORTH);
        body.add(mid,             BorderLayout.CENTER);
        body.add(buildLogPanel(), BorderLayout.SOUTH);
        return body;
    }

    private JPanel buildInfoCard() {
        JPanel c = card("THE ARRANGEMENT");
        infoLabel = lbl("<html>No minimum, but the agents are slow —<br>"
                + "gold takes " + DragonBankParams.LOAN_DELAY_TURNS + " turn(s) to arrive,<br>"
                + "and the price is steep.</html>");
        infoLabel.setForeground(UITheme.TEXT_SECONDARY);
        c.add(infoLabel);
        return c;
    }

    private JPanel buildRatesCard() {
        JPanel c = card("TERMS");
        rateLabel    = lbl("Loan Rate: —");
        maxLoanLabel = lbl("Max Loan: —");
        c.add(rateLabel);
        c.add(maxLoanLabel);
        return c;
    }

    // ─── Status: pending loan / ban / active loan ───────────────────────

    private JPanel buildStatusSection() {
        JPanel outer = card("YOUR ARRANGEMENT");

        pendingLoanLabel = lbl("");
        pendingLoanPanel = new JPanel();
        pendingLoanPanel.setLayout(new BoxLayout(pendingLoanPanel, BoxLayout.Y_AXIS));
        pendingLoanPanel.setBackground(UITheme.BG_PANEL);
        pendingLoanPanel.add(pendingLoanLabel);

        banLabel = lbl("");
        banLabel.setForeground(new Color(220, 100, 100));
        banPanel = new JPanel();
        banPanel.setLayout(new BoxLayout(banPanel, BoxLayout.Y_AXIS));
        banPanel.setBackground(UITheme.BG_PANEL);
        banPanel.add(banLabel);

        noLoanLabel = lbl("No active loan.");
        noLoanLabel.setForeground(UITheme.TEXT_SECONDARY);

        loanOwedLabel  = lbl("");
        loanNextLabel  = lbl("");
        loanCountLabel = lbl("");

        payInstallBtn = btn("PAY INSTALLMENT", UITheme.TEXT_GOLD);
        repayFullBtn  = btn("REPAY IN FULL",   new Color(160, 220, 160));

        payInstallBtn.addActionListener(e -> {
            List<String> log = new ArrayList<>();
            dragonBank().payInstallmentPlayer(gameState.getResources(), log);
            appendLog(log);
            refresh();
        });
        repayFullBtn.addActionListener(e -> {
            List<String> log = new ArrayList<>();
            dragonBank().repayLoanFullPlayer(gameState.getResources(), log);
            appendLog(log);
            refresh();
        });

        JPanel loanBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        loanBtns.setBackground(UITheme.BG_PANEL);
        loanBtns.add(payInstallBtn);
        loanBtns.add(repayFullBtn);

        loanActivePanel = new JPanel();
        loanActivePanel.setLayout(new BoxLayout(loanActivePanel, BoxLayout.Y_AXIS));
        loanActivePanel.setBackground(UITheme.BG_PANEL);
        loanActivePanel.add(loanOwedLabel);
        loanActivePanel.add(loanNextLabel);
        loanActivePanel.add(loanCountLabel);
        loanActivePanel.add(loanBtns);

        outer.add(pendingLoanPanel);
        outer.add(banPanel);
        outer.add(noLoanLabel);
        outer.add(loanActivePanel);
        return outer;
    }

    // ─── Transactions ─────────────────────────────────────────────────────

    private JPanel buildTransactionSection() {
        JPanel outer = card("BORROW");

        playerGoldLabel = lbl("Your gold: —");
        playerGoldLabel.setForeground(UITheme.TEXT_SECONDARY);

        amountField = new JTextField("0", 8);
        amountField.setFont(UITheme.FONT_BODY);
        amountField.setBackground(UITheme.BG_DARK);
        amountField.setForeground(UITheme.TEXT_PRIMARY);
        amountField.setCaretColor(UITheme.TEXT_GOLD);
        amountField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
                new EmptyBorder(2, 4, 2, 4)));

        borrowBtn = btn("BORROW", new Color(200, 120, 255));
        borrowBtn.addActionListener(e -> {
            List<String> log = new ArrayList<>();
            boolean ok = dragonBank().requestLoanPlayer(gameState.getResources(), parseAmount(), log);
            if (!ok && log.isEmpty()) log.add("Cannot request that loan.");
            appendLog(log);
            refresh();
        });

        JLabel amtLabel = new JLabel("Amount:");
        amtLabel.setFont(UITheme.FONT_BODY);
        amtLabel.setForeground(UITheme.TEXT_SECONDARY);

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        inputRow.setBackground(UITheme.BG_PANEL);
        inputRow.add(amtLabel);
        inputRow.add(amountField);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btnRow.setBackground(UITheme.BG_PANEL);
        btnRow.add(borrowBtn);

        outer.add(playerGoldLabel);
        outer.add(inputRow);
        outer.add(btnRow);
        return outer;
    }

    // ─── Log ─────────────────────────────────────────────────────────────

    private JPanel buildLogPanel() {
        JPanel outer = card("LOG");
        logArea = new JTextArea(4, 0);
        logArea.setFont(UITheme.FONT_SMALL);
        logArea.setForeground(UITheme.TEXT_SECONDARY);
        logArea.setBackground(UITheme.BG_DARK);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(null);
        scroll.setBackground(UITheme.BG_DARK);
        scroll.getViewport().setBackground(UITheme.BG_DARK);
        scroll.setPreferredSize(new Dimension(0, 80));
        outer.add(scroll);
        return outer;
    }

    // ─── Refresh ─────────────────────────────────────────────────────────

    public void refresh() {
        DragonBankManager bank = dragonBank();
        if (bank == null) return;

        int gold = gameState.getResources().getMoney();
        String pid = DragonBankManager.PLAYER_ID;

        rateLabel.setText("Loan Rate: " + String.format("%.0f%%", DragonBankParams.LOAN_INTEREST_RATE * 100));
        maxLoanLabel.setText("Max Loan: " + bank.getMaxLoanAmount(gold) + " gold");

        boolean pending = bank.hasPendingLoan(pid);
        pendingLoanPanel.setVisible(pending);
        if (pending) {
            pendingLoanLabel.setText("⏳ " + bank.getPendingLoanAmount(pid) + " gold is en route — "
                    + bank.getPendingLoanTurnsRemaining(pid) + " turn(s) remaining.");
        }

        boolean banned = bank.isBanned(pid);
        banPanel.setVisible(banned);
        if (banned) {
            banLabel.setText("🚫 The dragon's agents refuse you for " + bank.getBanTurnsRemaining(pid) + " more turn(s).");
        }

        DragonBankLoan loan = bank.getLoan(pid);
        boolean hasLoan = loan != null && !loan.isPaidOff();
        noLoanLabel.setVisible(!hasLoan);
        loanActivePanel.setVisible(hasLoan);
        if (hasLoan) {
            loanOwedLabel.setText("Remaining: " + loan.getFullRepaymentAmount() + " gold"
                    + "  (Rate: " + String.format("%.0f%%", loan.getInterestRate() * 100) + ")");
            loanNextLabel.setText("Next installment: " + loan.getNextInstallmentDue() + " gold");
            loanCountLabel.setText("Installments left: " + loan.getInstallmentsRemaining());
            payInstallBtn.setEnabled(gold >= loan.getNextInstallmentDue());
            repayFullBtn.setEnabled(gold >= loan.getFullRepaymentAmount());
        }

        playerGoldLabel.setText("Your gold: " + gold);
        borrowBtn.setEnabled(!hasLoan && !pending && !banned && bank.getMaxLoanAmount(gold) > 0);

        revalidate();
        repaint();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private DragonBankManager dragonBank() {
        return gameState.getNobleHouseManager().getDragonBankManager();
    }

    private int parseAmount() {
        try { return Math.max(0, Integer.parseInt(amountField.getText().trim())); }
        catch (NumberFormatException ex) { return 0; }
    }

    private void appendLog(List<String> lines) { for (String l : lines) appendLog(l); }

    private void appendLog(String line) {
        if (!logArea.getText().isEmpty()) logArea.append("\n");
        logArea.append("> " + line);
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private JPanel card(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(UITheme.BG_PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                new EmptyBorder(8, 10, 8, 10)));
        JLabel h = new JLabel(title);
        h.setFont(UITheme.FONT_SMALL);
        h.setForeground(UITheme.TEXT_SECONDARY);
        h.setAlignmentX(Component.LEFT_ALIGNMENT);
        h.setBorder(new EmptyBorder(0, 0, 5, 0));
        p.add(h);
        return p;
    }

    private JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_BODY);
        l.setForeground(UITheme.TEXT_PRIMARY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JButton btn(String label, Color fg) {
        JButton b = new JButton(label);
        b.setFont(UITheme.FONT_BUTTON);
        b.setForeground(fg);
        b.setBackground(UITheme.BUTTON_BG);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}