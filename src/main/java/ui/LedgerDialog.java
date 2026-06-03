package ui;

import main.ledger.Ledger;
import main.resources.ResourceType;
import main.core.GameState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Non-modal dialog showing projected recurring income/costs
 * and one-time changes from the last turn.
 * Recurring entries are grouped by category with collapsible rows.
 */
public class LedgerDialog extends JDialog {

    private final GameState gameState;
    private final JPanel    projectionPanel;
    private final JPanel    oneTimePanel;

    // track expanded state per category+resource key
    private final Map<String, Boolean> expandedState = new HashMap<>();

    public LedgerDialog(Window owner, GameState gameState) {
        super(owner, "Ledger", ModalityType.MODELESS);
        this.gameState = gameState;

        setSize(440, 580);
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

        // ── Projection (recurring, grouped by resource then category) ──────────
        projectionPanel.removeAll();

        for (ResourceType resource : ResourceType.values()) {
            List<Ledger.Entry> entries = ledger.getRecurringEntries(resource);
            if (entries.isEmpty()) continue;

            // Group entries by category
            Map<String, List<Ledger.Entry>> byCategory = new LinkedHashMap<>();
            for (Ledger.Entry e : entries) {
                byCategory.computeIfAbsent(e.category, k -> new ArrayList<>()).add(e);
            }

            // Resource header (net total)
            int resourceNet = ledger.getDelta(resource);
            projectionPanel.add(makeResourceHeader(resource, resourceNet));

            for (Map.Entry<String, List<Ledger.Entry>> catEntry : byCategory.entrySet()) {
                String category    = catEntry.getKey();
                List<Ledger.Entry> catEntries = catEntry.getValue();
                int catTotal = catEntries.stream().mapToInt(e -> e.amount).sum();
                String expandKey = resource.name() + "|" + category;

                // Category row (clickable, shows total)
                JPanel categoryRow = makeCategoryRow(category, catTotal, expandKey, catEntries);
                projectionPanel.add(categoryRow);
            }

            projectionPanel.add(Box.createVerticalStrut(8));
        }

        // ── One-time (last turn, flat list grouped by resource) ────────────────
        oneTimePanel.removeAll();
        boolean anyOneTime = false;

        for (ResourceType resource : ResourceType.values()) {
            List<Ledger.Entry> entries = ledger.getOneTimeEntries(resource);
            if (entries.isEmpty()) continue;
            anyOneTime = true;
            int total = entries.stream().mapToInt(e -> e.amount).sum();
            oneTimePanel.add(makeResourceHeader(resource, total));
            for (Ledger.Entry e : entries) {
                oneTimePanel.add(makeEntryRow(e.category + " / " + e.name, e.amount, 16));
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
        row.setBorder(new EmptyBorder(4, 6, 4, 6));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
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

    /**
     * A category row with a toggle button. Clicking expands/collapses
     * the individual entries beneath it in the same parent panel.
     */
    private JPanel makeCategoryRow(String category, int total,
                                    String expandKey, List<Ledger.Entry> entries) {
        boolean expanded = expandedState.getOrDefault(expandKey, false);

        // Container holds the header + collapsible children
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(UITheme.BG_DARK);
        container.setAlignmentX(LEFT_ALIGNMENT);

        // Header row
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(40, 42, 48));
        header.setBorder(new EmptyBorder(3, 16, 3, 6));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        header.setAlignmentX(LEFT_ALIGNMENT);
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        String arrow = expanded ? "▾ " : "▸ ";
        JLabel categoryLabel = new JLabel(arrow + capitalize(category));
        categoryLabel.setFont(UITheme.FONT_SMALL);
        categoryLabel.setForeground(UITheme.TEXT_SECONDARY);

        JLabel totalLabel = new JLabel((total >= 0 ? "+" : "") + total);
        totalLabel.setFont(UITheme.FONT_SMALL);
        totalLabel.setForeground(total >= 0 ? UITheme.TEXT_GREEN : UITheme.TEXT_RED);

        header.add(categoryLabel, BorderLayout.WEST);
        header.add(totalLabel,    BorderLayout.EAST);

        // Children panel
        JPanel children = new JPanel();
        children.setLayout(new BoxLayout(children, BoxLayout.Y_AXIS));
        children.setBackground(UITheme.BG_DARK);
        children.setAlignmentX(LEFT_ALIGNMENT);
        children.setVisible(expanded);

        for (Ledger.Entry e : entries) {
            children.add(makeEntryRow(e.name, e.amount, 28));
        }

        // Toggle on click
        header.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent ev) {
                boolean nowExpanded = !expandedState.getOrDefault(expandKey, false);
                expandedState.put(expandKey, nowExpanded);
                children.setVisible(nowExpanded);
                categoryLabel.setText((nowExpanded ? "▾ " : "▸ ") + capitalize(category));
                container.revalidate();
                container.repaint();
                projectionPanel.revalidate();
                projectionPanel.repaint();
            }
        });

        container.add(header);
        container.add(children);
        return container;
    }

    private JPanel makeEntryRow(String label, int amount, int leftPad) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(UITheme.BG_DARK);
        row.setBorder(new EmptyBorder(1, leftPad, 1, 6));
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

    private JLabel makeSectionHeader2(String text) {
        return makeSectionHeader(text);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}