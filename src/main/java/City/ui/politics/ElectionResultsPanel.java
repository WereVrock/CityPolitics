package City.ui.politics;

import City.main.calendar.GameCalendar;
import City.main.parameters.PoliticalParams;
import City.main.politics.ElectionRecord;
import City.main.politics.ElectionRecord.PartyResult;
import City.ui.UITheme;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

/**
 * Full-screen election results panel shown automatically when an election fires.
 * Shows party performance, seat distribution, player impact, and affiliation changes.
 */
public class ElectionResultsPanel extends JPanel {

    // ── Theme ─────────────────────────────────────────────────────────────────
    private static final Color BG_PANEL_WARM   = new Color(32, 24, 14);
    private static final Color BG_CARD         = new Color(42, 32, 18);
    private static final Color BG_CARD_HOVER   = new Color(55, 42, 24);
    private static final Color BG_HEADER       = new Color(22, 16, 8);
    private static final Color BG_FIXED        = new Color(28, 28, 35);
    private static final Color COL_GOLD        = new Color(210, 170, 70);
    private static final Color COL_GOLD_BRIGHT = new Color(240, 200, 90);
    private static final Color COL_PARCHMENT   = new Color(220, 205, 175);
    private static final Color COL_MUTED       = new Color(160, 145, 110);
    private static final Color COL_GREEN       = new Color(80,  190, 100);
    private static final Color COL_RED         = new Color(200,  70,  60);
    private static final Color COL_NEUTRAL     = new Color(160, 160, 180);
    private static final Color COL_BORDER      = new Color(100,  80,  40);
    private static final Color COL_BAR_BG      = new Color(28,  22,  10);
    private static final Color COL_WARN        = new Color(220, 150,  30);

    private static final Font FONT_TITLE   = new Font("Serif", Font.BOLD,  20);
    private static final Font FONT_HEADER  = new Font("Serif", Font.BOLD,  14);
    private static final Font FONT_BODY    = new Font("Serif", Font.PLAIN, 13);
    private static final Font FONT_SMALL   = new Font("Serif", Font.PLAIN, 11);
    private static final Font FONT_ITALIC  = new Font("Serif", Font.ITALIC,12);
    private static final Font FONT_BOLD    = new Font("Serif", Font.BOLD,  13);

    private final ElectionRecord record;
    private final GameCalendar   calendar;
    private final Runnable       onClose;
    private final Runnable       onViewCouncil;

    // Advanced breakdown toggle
    private boolean advancedVisible = false;
    private JPanel  advancedPanel;
    private JButton advancedToggle;

    public ElectionResultsPanel(ElectionRecord record,
                                 GameCalendar calendar,
                                 Runnable onClose,
                                 Runnable onViewCouncil) {
        this.record        = record;
        this.calendar      = calendar;
        this.onClose       = onClose;
        this.onViewCouncil = onViewCouncil;

        setBackground(BG_PANEL_WARM);
        setLayout(new BorderLayout(0, 0));
        setBorder(new EmptyBorder(0, 0, 0, 0));

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildBody(),    BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_HEADER);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, COL_BORDER),
                new EmptyBorder(16, 24, 14, 24)));

        JLabel crown = new JLabel("♛");
        crown.setFont(new Font("Serif", Font.PLAIN, 28));
        crown.setForeground(COL_GOLD);
        crown.setBorder(new EmptyBorder(0, 0, 0, 12));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setBackground(BG_HEADER);

        JLabel title = new JLabel("ELECTION RESULTS — " + calendar.getDisplayString().toUpperCase());
        title.setFont(FONT_TITLE);
        title.setForeground(COL_GOLD_BRIGHT);

        PartyResult winner = record.getWinner();
        String subtitle = winner != null
                ? "♛  " + winner.partyName + " leads with " + winner.seatsAfter + " seats"
                : "No clear winner";
        JLabel sub = new JLabel(subtitle);
        sub.setFont(FONT_ITALIC);
        sub.setForeground(COL_MUTED);

        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(sub);

        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(BG_HEADER);
        left.add(crown,      BorderLayout.WEST);
        left.add(titleBlock, BorderLayout.CENTER);

        panel.add(left, BorderLayout.WEST);
        return panel;
    }

    // ── Body ──────────────────────────────────────────────────────────────────

    private JScrollPane buildBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BG_PANEL_WARM);
        body.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Top row: party performance + seat chart side by side
        JPanel topRow = new JPanel(new GridLayout(1, 2, 16, 0));
        topRow.setBackground(BG_PANEL_WARM);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
        topRow.add(buildPerformancePanel());
        topRow.add(buildSeatChart());
        body.add(topRow);

        body.add(Box.createVerticalStrut(16));
        body.add(buildImpactPanel());

        // Affiliation changes (if any)
        if (!record.getAffiliationChanges().isEmpty()) {
            body.add(Box.createVerticalStrut(12));
            body.add(buildAffiliationPanel());
        }

        // Advanced breakdown (collapsible)
        body.add(Box.createVerticalStrut(12));
        advancedToggle = makeTextButton("▸  Show Advanced Breakdown");
        advancedToggle.setFont(FONT_SMALL);
        advancedToggle.setForeground(COL_MUTED);
        advancedToggle.setAlignmentX(LEFT_ALIGNMENT);
        advancedToggle.addActionListener(e -> toggleAdvanced(body));
        body.add(advancedToggle);

        advancedPanel = buildAdvancedPanel();
        advancedPanel.setVisible(false);
        body.add(advancedPanel);

        body.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setBackground(BG_PANEL_WARM);
        scroll.getViewport().setBackground(BG_PANEL_WARM);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    // ── Party Performance ────────────────────────────────────────────────────

    private JPanel buildPerformancePanel() {
        JPanel panel = sectionPanel("⚑  Party Performance");

        List<PartyResult> sorted = new java.util.ArrayList<>(record.getPartyResults());
        sorted.sort((a, b) -> Integer.compare(b.seatsAfter, a.seatsAfter));

        for (PartyResult r : sorted) {
            panel.add(buildPartyRow(r));
            panel.add(Box.createVerticalStrut(4));
        }

        return panel;
    }

    private JPanel buildPartyRow(PartyResult r) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(r.isFixedSeat ? BG_FIXED : BG_CARD);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(r.isFixedSeat
                        ? new Color(60, 60, 80) : COL_BORDER, 1),
                new EmptyBorder(6, 10, 6, 10)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Tooltip with detail
        String tooltip = buildPartyTooltip(r);

        // Name
        JLabel nameLabel = new JLabel(r.partyName);
        nameLabel.setFont(FONT_BODY);
        nameLabel.setForeground(r.isFixedSeat ? COL_NEUTRAL : COL_PARCHMENT);
        nameLabel.setToolTipText(tooltip);

        // Right side: pct + arrow + seats
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setBackground(row.getBackground());

        if (!r.isFixedSeat) {
            JLabel pctLabel = new JLabel(String.format("%.1f%%", r.votePct));
            pctLabel.setFont(FONT_SMALL);
            pctLabel.setForeground(COL_MUTED);

            int    delta    = r.seatDelta();
            String arrow    = delta > 0 ? "▲" : delta < 0 ? "▼" : "─";
            Color  arrowCol = delta > 0 ? COL_GREEN : delta < 0 ? COL_RED : COL_NEUTRAL;
            JLabel arrowLbl = new JLabel(arrow + (delta != 0 ? Math.abs(delta) : ""));
            arrowLbl.setFont(FONT_BOLD);
            arrowLbl.setForeground(arrowCol);
            arrowLbl.setToolTipText(buildArrowTooltip(r));

            right.add(pctLabel);
            right.add(arrowLbl);
        } else {
            JLabel fixedLabel = new JLabel("fixed");
            fixedLabel.setFont(FONT_SMALL);
            fixedLabel.setForeground(COL_NEUTRAL);
            right.add(fixedLabel);
        }

        JLabel seatsLabel = new JLabel(r.seatsAfter + " seats");
        seatsLabel.setFont(FONT_BOLD);
        seatsLabel.setForeground(r == record.getWinner() ? COL_GOLD : COL_PARCHMENT);
        right.add(seatsLabel);

        // Warning icon for 0 seats or new entry
        if (!r.isFixedSeat && r.seatsAfter == 0) {
            JLabel warn = new JLabel(" ⚠");
            warn.setFont(FONT_SMALL);
            warn.setForeground(COL_WARN);
            warn.setToolTipText(r.seatsBefore > 0
                    ? r.partyName + " lost all seats!"
                    : r.partyName + " has no seats.");
            right.add(warn);
        } else if (!r.isFixedSeat && r.seatsBefore == 0 && r.seatsAfter > 0) {
            JLabel newEntry = new JLabel(" ★");
            newEntry.setFont(FONT_SMALL);
            newEntry.setForeground(COL_GREEN);
            newEntry.setToolTipText(r.partyName + " enters the council!");
            right.add(newEntry);
        }

        // Hover effect
        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                row.setBackground(r.isFixedSeat ? BG_FIXED : BG_CARD_HOVER);
                right.setBackground(row.getBackground());
            }
            @Override public void mouseExited(MouseEvent e) {
                row.setBackground(r.isFixedSeat ? BG_FIXED : BG_CARD);
                right.setBackground(row.getBackground());
            }
        });

        row.add(nameLabel, BorderLayout.WEST);
        row.add(right,     BorderLayout.EAST);
        return row;
    }

    private String buildPartyTooltip(PartyResult r) {
        if (r.isFixedSeat) return r.partyName + " holds a fixed seat — not subject to election.";
        return "<html><b>" + r.partyName + "</b><br>"
                + "Seats: " + r.seatsBefore + " → " + r.seatsAfter
                + "  (Δ " + (r.seatDelta() >= 0 ? "+" : "") + r.seatDelta() + ")<br>"
                + "Vote %: " + String.format("%.1f", r.votePct) + "%<br>"
                + "Natural votes: "  + r.naturalVotes + "<br>"
                + "Bought votes: +"  + r.boughtVotes  + "<br>"
                + "Stolen from: −"   + r.stolenVotes  + "<br>"
                + "Power: "          + r.powerBefore  + " → " + r.powerAfter
                + "  (Δ " + (r.powerDelta() >= 0 ? "+" : "") + r.powerDelta() + ")"
                + "</html>";
    }

    private String buildArrowTooltip(PartyResult r) {
        int d = r.seatDelta();
        if (d == 0) return "Seat count unchanged.";
        String dir = d > 0 ? "Gained " : "Lost ";
        return dir + Math.abs(d) + " seat(s). Natural: " + r.naturalVotes
                + "  Bought: " + r.boughtVotes
                + "  Stolen from: " + r.stolenVotes;
    }

    // ── Seat Chart ────────────────────────────────────────────────────────────

    private JPanel buildSeatChart() {
        JPanel panel = sectionPanel("▮▮  Seat Distribution");

        int maxSeats = record.getPartyResults().stream()
                .mapToInt(r -> r.seatsAfter).max().orElse(1);
        if (maxSeats == 0) maxSeats = 1;

        List<PartyResult> sorted = new java.util.ArrayList<>(record.getPartyResults());
        sorted.sort((a, b) -> Integer.compare(b.seatsAfter, a.seatsAfter));

        for (PartyResult r : sorted) {
            panel.add(buildBarRow(r, maxSeats));
            panel.add(Box.createVerticalStrut(4));
        }

        return panel;
    }

    private JPanel buildBarRow(PartyResult r, int maxSeats) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(BG_PANEL_WARM);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(LEFT_ALIGNMENT);

        // Party name (abbreviated)
        String shortName = abbreviate(r.partyName, 12);
        JLabel nameLabel = new JLabel(shortName);
        nameLabel.setFont(FONT_SMALL);
        nameLabel.setForeground(r.isFixedSeat ? COL_NEUTRAL : COL_PARCHMENT);
        nameLabel.setPreferredSize(new Dimension(90, 20));

        // Bar
        JPanel barContainer = new JPanel(new BorderLayout(2, 0));
        barContainer.setBackground(BG_PANEL_WARM);

        double fraction = (double) r.seatsAfter / maxSeats;
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                // Background track
                g2.setColor(COL_BAR_BG);
                g2.fillRoundRect(0, 2, getWidth(), getHeight() - 4, 4, 4);
                // Fill
                int fillW = (int)(getWidth() * fraction);
                if (fillW > 0) {
                    Color barColor = r.isFixedSeat
                            ? new Color(80, 80, 100)
                            : (r == record.getWinner()
                                    ? new Color(180, 140, 40)
                                    : new Color(100, 75, 30));
                    g2.setColor(barColor);
                    g2.fillRoundRect(0, 2, fillW, getHeight() - 4, 4, 4);
                    // Shine
                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.fillRoundRect(0, 2, fillW, (getHeight() - 4) / 2, 4, 4);
                }
                // Dashed border for fixed seats
                if (r.isFixedSeat) {
                    g2.setColor(new Color(80, 80, 100, 150));
                    float[] dash = {4f, 3f};
                    g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND, 1f, dash, 0f));
                    g2.drawRoundRect(0, 2, getWidth() - 1, getHeight() - 5, 4, 4);
                    g2.setStroke(new BasicStroke(1f));
                }
            }
        };
        bar.setBackground(BG_PANEL_WARM);
        bar.setPreferredSize(new Dimension(0, 22));
        bar.setToolTipText(r.partyName + ": " + r.seatsAfter + " seats");

        // Seat count label
        JLabel countLabel = new JLabel(r.seatsAfter > 0 ? String.valueOf(r.seatsAfter) : "—");
        countLabel.setFont(FONT_SMALL);
        countLabel.setForeground(r.seatsAfter > 0 ? COL_PARCHMENT : COL_NEUTRAL);
        countLabel.setPreferredSize(new Dimension(28, 20));
        countLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        barContainer.add(bar,        BorderLayout.CENTER);
        barContainer.add(countLabel, BorderLayout.EAST);

        row.add(nameLabel,     BorderLayout.WEST);
        row.add(barContainer,  BorderLayout.CENTER);
        return row;
    }

    // ── Player Impact ─────────────────────────────────────────────────────────

    private JPanel buildImpactPanel() {
        JPanel panel = sectionPanel("⚖  Details & Player Impact");

        // Corruption effect
        double stolenPct = record.getStolenVotesPct();
        addImpactRow(panel, "🪙",
                String.format("Corruption (%d%%) caused %.1f%% of votes to be contested by rivals.",
                        record.getCorruption(), stolenPct),
                stolenPct > 10 ? COL_RED : stolenPct > 3 ? COL_WARN : COL_MUTED);

        // Propaganda
        Map<String, Double> propMap = record.getPropagandaSpent();
        for (Map.Entry<String, Double> e : propMap.entrySet()) {
            if (e.getValue() < 0.5) continue;
            addImpactRow(panel, "📜",
                    String.format("%s spent %.0f propaganda → boosted election vote share.",
                            e.getKey(), e.getValue()),
                    COL_MUTED);
        }

        // Stolen votes total
        if (record.getStolenVotesTotal() > 0) {
            addImpactRow(panel, "⚠",
                    record.getStolenVotesTotal() + " votes were contested through corruption.",
                    COL_WARN);
        }

        // Seat changes narrative
        for (PartyResult r : record.getPartyResults()) {
            if (r.isFixedSeat || r.seatDelta() == 0) continue;
            int d = r.seatDelta();
            String dir = d > 0 ? "gained " : "lost ";
            addImpactRow(panel,
                    d > 0 ? "▲" : "▼",
                    r.partyName + " " + dir + Math.abs(d) + " seat(s). "
                    + "Power: " + r.powerBefore + " → " + r.powerAfter + ".",
                    d > 0 ? COL_GREEN : COL_RED);
        }

        return panel;
    }

    private void addImpactRow(JPanel panel, String icon, String text, Color textColor) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(BG_CARD);
        row.setBorder(new EmptyBorder(5, 10, 5, 10));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(FONT_BODY);
        iconLabel.setForeground(COL_GOLD);

        JLabel textLabel = new JLabel("<html><body style='width:400px'>" + text + "</body></html>");
        textLabel.setFont(FONT_SMALL);
        textLabel.setForeground(textColor);

        row.add(iconLabel,  BorderLayout.WEST);
        row.add(textLabel,  BorderLayout.CENTER);
        panel.add(row);
        panel.add(Box.createVerticalStrut(3));
    }

    // ── Affiliation Changes ───────────────────────────────────────────────────

    private JPanel buildAffiliationPanel() {
        JPanel panel = sectionPanel("↺  Affiliation Changes");
        for (ElectionRecord.AffiliationChange c : record.getAffiliationChanges()) {
            String text;
            Color  color;
            if (c.gained) {
                text  = c.popTypeName + " pops affiliated with "
                        + c.newAffiliation + ".";
                color = COL_GREEN;
            } else {
                text  = c.popTypeName + " pops lost their party affiliation.";
                color = COL_WARN;
            }
            addImpactRow(panel, c.gained ? "★" : "✗", text, color);
        }
        return panel;
    }

    // ── Advanced Breakdown ────────────────────────────────────────────────────

    private JPanel buildAdvancedPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(20, 15, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COL_BORDER, 1),
                new EmptyBorder(10, 14, 10, 14)));
        panel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel hdr = new JLabel("Advanced Vote Breakdown");
        hdr.setFont(FONT_HEADER);
        hdr.setForeground(COL_GOLD);
        panel.add(hdr);
        panel.add(Box.createVerticalStrut(8));

        // Table header
        JPanel tableHdr = buildAdvancedTableRow("Party", "Natural", "Bought", "Stolen", "Total", true);
        panel.add(tableHdr);
        panel.add(Box.createVerticalStrut(3));

        List<PartyResult> sorted = new java.util.ArrayList<>(record.getPartyResults());
        sorted.sort((a, b) -> Integer.compare(b.totalVotes, a.totalVotes));

        for (PartyResult r : sorted) {
            if (r.isFixedSeat) continue;
            JPanel tableRow = buildAdvancedTableRow(
                    abbreviate(r.partyName, 16),
                    String.valueOf(r.naturalVotes),
                    "+" + r.boughtVotes,
                    "−" + r.stolenVotes,
                    String.valueOf(r.totalVotes),
                    false);
            panel.add(tableRow);
            panel.add(Box.createVerticalStrut(2));
        }

        panel.add(Box.createVerticalStrut(8));

        // Power drift info
        JLabel driftHdr = new JLabel("Power Drift");
        driftHdr.setFont(FONT_BOLD);
        driftHdr.setForeground(COL_GOLD);
        panel.add(driftHdr);
        panel.add(Box.createVerticalStrut(4));

        for (PartyResult r : record.getPartyResults()) {
            if (r.isFixedSeat) continue;
            int threshold = r.seatsAfter * GameParameters_POWER_DRIFT;
            String driftText = r.partyName + ": Power " + r.powerAfter
                    + "  threshold=" + threshold
                    + (r.powerAfter > threshold ? "  → will drift down" : "  → stable");
            JLabel driftLbl = new JLabel(driftText);
            driftLbl.setFont(FONT_SMALL);
            driftLbl.setForeground(r.powerAfter > threshold ? COL_WARN : COL_MUTED);
            panel.add(driftLbl);
        }

        panel.add(Box.createVerticalStrut(8));

        // Corruption formula
        double cheatPct = 0.05 + 0.15 * (1.0 - Math.exp(-3.0 * record.getCorruption() / 100.0));
        JLabel corrLbl = new JLabel(String.format(
                "Corruption formula: corruption=%d%% → %.1f%% votes cheat-eligible",
                record.getCorruption(), cheatPct * 100));
        corrLbl.setFont(FONT_SMALL);
        corrLbl.setForeground(COL_MUTED);
        panel.add(corrLbl);

        return panel;
    }

    private static final int GameParameters_POWER_DRIFT =
            PoliticalParams.POWER_DRIFT_SEAT_MULTIPLIER;

    private JPanel buildAdvancedTableRow(String name, String natural, String bought,
                                          String stolen, String total, boolean isHeader) {
        JPanel row = new JPanel(new GridLayout(1, 5, 4, 0));
        row.setBackground(isHeader ? new Color(30, 22, 10) : new Color(20, 15, 8));
        row.setBorder(new EmptyBorder(3, 4, 3, 4));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        row.setAlignmentX(LEFT_ALIGNMENT);

        Color fg = isHeader ? COL_GOLD : COL_PARCHMENT;
        Font  f  = isHeader ? FONT_BOLD : FONT_SMALL;

        for (String val : new String[]{name, natural, bought, stolen, total}) {
            JLabel lbl = new JLabel(val);
            lbl.setFont(f);
            lbl.setForeground(fg);
            row.add(lbl);
        }
        return row;
    }

    private void toggleAdvanced(JPanel body) {
        advancedVisible = !advancedVisible;
        advancedPanel.setVisible(advancedVisible);
        advancedToggle.setText(advancedVisible
                ? "▾  Hide Advanced Breakdown"
                : "▸  Show Advanced Breakdown");
        body.revalidate();
        body.repaint();
    }

    // ── Footer ────────────────────────────────────────────────────────────────

    private JPanel buildFooter() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_HEADER);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, COL_BORDER),
                new EmptyBorder(10, 24, 10, 24)));

        JButton viewCouncilBtn = new JButton("View Full Council  →");
        viewCouncilBtn.setFont(FONT_BOLD);
        viewCouncilBtn.setForeground(COL_GOLD);
        viewCouncilBtn.setBackground(new Color(55, 40, 15));
        viewCouncilBtn.setBorderPainted(true);
        viewCouncilBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COL_BORDER, 1),
                new EmptyBorder(6, 16, 6, 16)));
        viewCouncilBtn.setFocusPainted(false);
        viewCouncilBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        viewCouncilBtn.addActionListener(e -> onViewCouncil.run());

        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(FONT_BODY);
        closeBtn.setForeground(COL_MUTED);
        closeBtn.setBackground(new Color(35, 28, 14));
        closeBtn.setBorderPainted(true);
        closeBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COL_BORDER, 1),
                new EmptyBorder(6, 16, 6, 16)));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> onClose.run());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setBackground(BG_HEADER);
        btnRow.add(closeBtn);
        btnRow.add(viewCouncilBtn);

        panel.add(btnRow, BorderLayout.EAST);
        return panel;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JPanel sectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PANEL_WARM);
        panel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel hdr = new JLabel(title);
        hdr.setFont(FONT_HEADER);
        hdr.setForeground(COL_GOLD);
        hdr.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, COL_BORDER),
                new EmptyBorder(0, 0, 6, 0)));
        hdr.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(hdr);
        panel.add(Box.createVerticalStrut(8));
        return panel;
    }

    private JButton makeTextButton(String text) {
        JButton btn = new JButton(text);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private String abbreviate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 1) + "…";
    }

}