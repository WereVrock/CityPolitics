package ui;

import main.ledger.Ledger;
import main.resources.ResourceType;
import main.core.GameState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.List;

/**
 * Non-modal dialog showing projected recurring income/costs
 * and one-time changes from the last turn.
 */
public class LedgerDialog extends JDialog {

    private final GameState gameState;
    private final JPanel    projectionPanel;
    private final JPanel    oneTimePanel;

    public LedgerDialog(Window owner, GameState gameState) {
        super(owner, "Ledger", ModalityType.MODELESS);
        this.gameState = gameState;

        setSize(420, 540);
        setMinimumSize(new Dimension(340, 400));
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        getContentPane().setBackground(UITheme.BG_DARK);
        getContentPane().setLayout(new BorderLayout());

        JLabel title = new JLabel("  LEDGER");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_GOLD);
        title.setBorder(new EmptyBorder(12, 12, 8, 12));
        title.setOpaque(true);
        title.setBackground(UITheme.BG_PANEL);
        getContentPane().add(title, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(UITheme.BG_DARK);
        body.setBorder(new EmptyBorder(12, 12, 12, 12));

        projectionPanel = new JPanel();
        projectionPanel.setLayout(new BoxLayout(projectionPanel, BoxLayout.Y_AXIS));
        projectionPanel.setBackground(UITheme.BG_DARK);

        oneTimePanel = new JPanel();
        oneTimePanel.setLayout(new BoxLayout(oneTimePanel, BoxLayout.Y_AXIS));
        oneTimePanel.setBackground(UITheme.BG_DARK);

        body.add(makeSectionHeader("PROJECTED CHANGES (next turn end)"));
        body.add(Box.createVerticalStrut(6));
        body.add(projectionPanel);
        body.add(Box.createVerticalStrut(16));
        body.add(makeSectionHeader("LAST TURN CHANGES"));
        body.add(Box.createVerticalStrut(6));
        body.add(oneTimePanel);
        body.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setBackground(UITheme.BG_DARK);
        scroll.getViewport().setBackground(UITheme.BG_DARK);
        getContentPane().add(scroll, BorderLayout.CENTER);

        refresh();
    }

    public void refresh() {
        Ledger ledger = gameState.getLedger();

        projectionPanel.removeAll();
        for (ResourceType resource : ResourceType.values()) {
            int delta = ledger.getDelta(resource);
            List<Ledger.Entry> entries = ledger.getRecurringEntries(resource);
            if (entries.isEmpty()) continue;

            projectionPanel.add(makeResourceHeader(resource, delta));
            for (Ledger.Entry e : entries) {
                projectionPanel.add(makeEntryRow(e.category + " / " + e.name, e.amount));
            }
            projectionPanel.add(Box.createVerticalStrut(8));
        }

        oneTimePanel.removeAll();
        boolean anyOneTime = false;
        for (ResourceType resource : ResourceType.values()) {
            List<Ledger.Entry> entries = ledger.getOneTimeEntries(resource);
            if (entries.isEmpty()) continue;
            anyOneTime = true;
            oneTimePanel.add(makeResourceHeader(resource, entries.stream().mapToInt(e -> e.amount).sum()));
            for (Ledger.Entry e : entries) {
                oneTimePanel.add(makeEntryRow(e.category + " / " + e.name, e.amount));
            }
            oneTimePanel.add(Box.createVerticalStrut(8));
        }
        if (!anyOneTime) {
            JLabel none = new JLabel("  No changes logged yet.");
            none.setFont(UITheme.FONT_SMALL);
            none.setForeground(UITheme.TEXT_SECONDARY);
            oneTimePanel.add(none);
        }

        projectionPanel.revalidate();
        projectionPanel.repaint();
        oneTimePanel.revalidate();
        oneTimePanel.repaint();
    }

    // ─── Builders ────────────────────────────────────────────────────────────

    private JLabel makeSectionHeader(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UITheme.FONT_HEADER);
        label.setForeground(UITheme.TEXT_GOLD);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(new MatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR));
        return label;
    }

    private JPanel makeResourceHeader(ResourceType resource, int netDelta) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(UITheme.BG_PANEL);
        row.setBorder(new EmptyBorder(3, 6, 3, 6));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel name = new JLabel(resource.name());
        name.setFont(UITheme.FONT_BODY);
        name.setForeground(UITheme.TEXT_PRIMARY);

        JLabel delta = new JLabel((netDelta >= 0 ? "+" : "") + netDelta);
        delta.setFont(UITheme.FONT_BODY);
        delta.setForeground(netDelta >= 0 ? UITheme.TEXT_GREEN : UITheme.TEXT_RED);

        row.add(name,  BorderLayout.WEST);
        row.add(delta, BorderLayout.EAST);
        return row;
    }

    private JPanel makeEntryRow(String label, int amount) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(UITheme.BG_DARK);
        row.setBorder(new EmptyBorder(1, 16, 1, 6));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(UITheme.FONT_SMALL);
        nameLabel.setForeground(UITheme.TEXT_SECONDARY);

        JLabel amountLabel = new JLabel((amount >= 0 ? "+" : "") + amount);
        amountLabel.setFont(UITheme.FONT_SMALL);
        amountLabel.setForeground(amount >= 0 ? UITheme.TEXT_GREEN : UITheme.TEXT_RED);

        row.add(nameLabel,   BorderLayout.WEST);
        row.add(amountLabel, BorderLayout.EAST);
        return row;
    }
}