package City.ui.bank;

import City.main.bank.BankAccount;
import City.main.bank.BankLoan;
import City.main.bank.BankManager;
import City.main.core.GameState;
import City.main.parameters.BankParams;
import City.ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Player-facing panel for the Frostpeak Bank: account overview, deposits,
 * withdrawals, and loans. Free actions — no turn needed.
 */
public class BankPanel extends JPanel {

    private final GameState gameState;
    private final Runnable  onBack;

    private JLabel depositLabel;
    private JLabel creditLabel;
    private JLabel stakeholderLabel;

    private JLabel depositRateLabel;
    private JLabel loanRateLabel;
    private JLabel maxLoanLabel;

    private JPanel  loanActivePanel;
    private JLabel  loanOwedLabel;
    private JLabel  loanNextLabel;
    private JLabel  loanCountLabel;
    private JButton payInstallBtn;
    private JButton repayFullBtn;
    private JLabel  noLoanLabel;

    private JTextField amountField;
    private JButton    depositBtn;
    private JButton    withdrawBtn;
    private JButton    borrowBtn;
    private JLabel     playerGoldLabel;

    private JTextArea logArea;

    public BankPanel(GameState gameState, Runnable onBack) {
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

        JLabel title = new JLabel("🏦  THE FROSTPEAK BANK");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_GOLD);

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
        infoRow.add(buildAccountCard());
        infoRow.add(buildRatesCard());

        JPanel mid = new JPanel(new BorderLayout(0, 8));
        mid.setBackground(UITheme.BG_DARK);
        mid.add(buildLoanSection(),        BorderLayout.NORTH);
        mid.add(buildTransactionSection(), BorderLayout.CENTER);

        body.add(infoRow,         BorderLayout.NORTH);
        body.add(mid,             BorderLayout.CENTER);
        body.add(buildLogPanel(), BorderLayout.SOUTH);
        return body;
    }

    // ─── Account card ────────────────────────────────────────────────────

    private JPanel buildAccountCard() {
        JPanel c = card("YOUR ACCOUNT");
        depositLabel     = lbl("Deposit: —");
        creditLabel      = lbl("Credit Rating: —");
        stakeholderLabel = lbl("Stakeholder: —");
        c.add(depositLabel);
        c.add(creditLabel);
        c.add(stakeholderLabel);
        return c;
    }

    // ─── Rates card ──────────────────────────────────────────────────────

    private JPanel buildRatesCard() {
        JPanel c = card("MARKET RATES");
        depositRateLabel = lbl("Deposit Rate: —");
        loanRateLabel    = lbl("Loan Rate: —");
        maxLoanLabel     = lbl("Max Loan: —");
        c.add(depositRateLabel);
        c.add(loanRateLabel);
        c.add(maxLoanLabel);
        return c;
    }

    // ─── Loan section ────────────────────────────────────────────────────

    private JPanel buildLoanSection() {
        JPanel outer = card("ACTIVE LOAN");

        noLoanLabel = lbl("No active loan.");
        noLoanLabel.setForeground(UITheme.TEXT_SECONDARY);

        loanOwedLabel  = lbl("");
        loanNextLabel  = lbl("");
        loanCountLabel = lbl("");

        payInstallBtn = btn("PAY INSTALLMENT", UITheme.TEXT_GOLD);
        repayFullBtn  = btn("REPAY IN FULL",   new Color(160, 220, 160));

        payInstallBtn.addActionListener(e -> {
            List<String> log = new ArrayList<>();
            bankManager().payInstallmentPlayer(gameState.getResources(), log);
            appendLog(log);
            refresh();
        });
        repayFullBtn.addActionListener(e -> {
            List<String> log = new ArrayList<>();
            bankManager().repayLoanFullPlayer(gameState.getResources(), log);
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

        outer.add(noLoanLabel);
        outer.add(loanActivePanel);
        return outer;
    }

    // ─── Transaction section ─────────────────────────────────────────────

    private JPanel buildTransactionSection() {
        JPanel outer = card("TRANSACTIONS");

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

        depositBtn  = btn("DEPOSIT",  UITheme.TEXT_GOLD);
        withdrawBtn = btn("WITHDRAW", UITheme.TEXT_PRIMARY);
        borrowBtn   = btn("BORROW",   new Color(200, 160, 100));

        depositBtn.addActionListener(e -> {
            List<String> log = new ArrayList<>();
            boolean ok = bankManager().depositPlayer(gameState.getResources(), parseAmount(), log);
            if (!ok && log.isEmpty()) log.add("Cannot deposit that amount.");
            appendLog(log);
            refresh();
        });
        withdrawBtn.addActionListener(e -> {
            List<String> log = new ArrayList<>();
            boolean ok = bankManager().withdrawPlayer(gameState.getResources(), parseAmount(), log);
            if (!ok && log.isEmpty()) log.add("Cannot withdraw that amount.");
            appendLog(log);
            refresh();
        });
        borrowBtn.addActionListener(e -> {
            List<String> log = new ArrayList<>();
            boolean ok = bankManager().requestLoanPlayer(gameState.getResources(), parseAmount(), log);
            if (!ok && log.isEmpty()) log.add("Cannot borrow that amount.");
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
        btnRow.add(depositBtn);
        btnRow.add(withdrawBtn);
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
        BankManager bm = bankManager();
        if (bm == null) return;

        // Handle bank being eliminated
        if (bm.getBankHouse() != null && bm.getBankHouse().isEliminated()) {
            depositLabel.setText("The Bank has been destroyed.");
            loanActivePanel.setVisible(false);
            noLoanLabel.setVisible(true);
            noLoanLabel.setText("The Bank no longer operates.");
            depositBtn.setEnabled(false);
            withdrawBtn.setEnabled(false);
            borrowBtn.setEnabled(false);
            return;
        }

        BankAccount acc  = bm.getOrCreateAccount(BankManager.PLAYER_HOUSE_ID);
        BankLoan    loan = bm.getLoan(BankManager.PLAYER_HOUSE_ID);
        int         gold = gameState.getResources().getMoney();

        // Account card
        depositLabel.setText("Deposit: " + acc.getDeposit() + " gold");
        creditLabel.setText("Credit Rating: " + acc.getCreditRating() + " / 100");
        boolean stakeholder = acc.isStakeholder();
        stakeholderLabel.setText("Stakeholder: " + (stakeholder ? "✓ Yes" : "No"));
        stakeholderLabel.setForeground(stakeholder ? new Color(160, 220, 160) : UITheme.TEXT_SECONDARY);

        // Rates card
        boolean threatened = bm.isBankThreatened(gameState.getNobleHouseManager().getHouses());
        double depRate  = BankParams.BANK_BASE_INTEREST_RATE_PER_TURN
                + (threatened ? BankParams.BANK_THREATENED_INTEREST_BONUS : 0);
        double loanRate = bm.getInterestRateForPlayer(acc);
        int    maxLoan  = bm.getMaxLoanAmountPlayer(acc);

        depositRateLabel.setText("Deposit Rate: " + String.format("%.1f%%", depRate * 100)
                + " / turn" + (threatened ? "  ▲ threat bonus" : ""));
        depositRateLabel.setForeground(threatened ? new Color(220, 160, 60) : UITheme.TEXT_PRIMARY);
        loanRateLabel.setText("Loan Rate: ~" + String.format("%.1f%%", loanRate * 100));
        maxLoanLabel.setText("Max Loan: " + maxLoan + " gold");

        // Loan section
        boolean hasLoan = loan != null && !loan.isPaidOff();
        noLoanLabel.setVisible(!hasLoan);
        loanActivePanel.setVisible(hasLoan);
        if (hasLoan) {
            loanOwedLabel.setText("Remaining: " + loan.getFullRepaymentAmount() + " gold"
                    + "  (Rate: " + String.format("%.1f%%", loan.getInterestRate() * 100) + ")");
            loanNextLabel.setText("Next installment: " + loan.getNextInstallmentDue() + " gold");
            loanCountLabel.setText("Installments left: " + loan.getInstallmentsRemaining());
            payInstallBtn.setEnabled(gold >= loan.getNextInstallmentDue());
            repayFullBtn.setEnabled(gold >= loan.getFullRepaymentAmount());
        }

        // Transaction section
        playerGoldLabel.setText("Your gold: " + gold);
        boolean bankRobber = bm.isBankRobber(BankManager.PLAYER_HOUSE_ID);
        depositBtn.setEnabled(gold > 0 && !bankRobber);
        withdrawBtn.setEnabled(acc.getDeposit() > 0);
        borrowBtn.setEnabled(!hasLoan && maxLoan > 0 && !bankRobber);

        if (bankRobber) {
            borrowBtn.setToolTipText("The Bank refuses to deal with you.");
        }

        revalidate();
        repaint();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private BankManager bankManager() {
        return gameState.getNobleHouseManager().getBankManager();
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