// ===== ui/ledger/LedgerPanel.java =====
package ui.ledger;

import main.ledger.Ledger;
import main.ledger.Ledger.Entry;
import main.resources.ResourcePool;
import main.resources.ResourceType;
import ui.UITheme;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Self-contained ledger UI panel.
 *
 * Layout:
 *   [Resource tab bar]
 *   [optional Last Season banner]
 *   [Recurring column | One-time column]
 *   [Footer: projected total, arrow, warnings, Last Season button]
 *
 * Two view modes: current turn and last season (amber wash).
 */
public class LedgerPanel extends JPanel {

    // ── Ledger-specific theme (does not inherit UITheme directly) ─────────────

    private static final Color BG_BASE          = new Color(15, 12, 20);
    private static final Color BG_COLUMN        = new Color(20, 16, 28);
    private static final Color BG_NORMAL        = new Color(20, 16, 28);
    private static final Color BG_LAST_SEASON   = new Color(38, 28, 8);
    private static final Color BG_TAB_BAR       = new Color(10, 8, 14);
    private static final Color BG_TAB_ACTIVE    = new Color(28, 22, 40);
    private static final Color BG_TAB_INACTIVE  = new Color(10, 8, 14);
    private static final Color BG_ROW_A         = new Color(22, 18, 32);
    private static final Color BG_ROW_B         = new Color(28, 22, 40);
    private static final Color BG_CAT_HEADER    = new Color(18, 14, 26);
    private static final Color BG_BANNER        = new Color(60, 44, 8);

    private static final Color COL_BORDER       = new Color(55, 40, 80);
    private static final Color COL_POSITIVE     = new Color(80, 190, 110);
    private static final Color COL_NEGATIVE     = new Color(200, 70, 70);
    private static final Color COL_NEUTRAL      = new Color(130, 115, 160);
    private static final Color COL_TITLE        = new Color(210, 170, 80);
    private static final Color COL_TEXT         = new Color(200, 190, 220);
    private static final Color COL_WARNING      = new Color(220, 150, 30);
    private static final Color COL_WARNING_DIM  = new Color(120, 95, 35);
    private static final Color COL_BANNER_FG    = new Color(210, 170, 80);
    private static final Color COL_TAB_ACTIVE   = new Color(200, 185, 220);
    private static final Color COL_TAB_INACTIVE = new Color(110, 95, 140);

    private static final Font  FONT_TAB         = new Font("Serif", Font.PLAIN, 12);
    private static final Font  FONT_SECTION     = new Font("Serif", Font.BOLD,  13);
    private static final Font  FONT_CAT         = new Font("Serif", Font.BOLD,  12);
    private static final Font  FONT_ENTRY       = new Font("Monospaced", Font.PLAIN, 11);
    private static final Font  FONT_ITALIC      = new Font("Serif", Font.ITALIC, 11);
    private static final Font  FONT_TOTAL       = new Font("Serif", Font.BOLD,  14);
    private static final Font  FONT_BANNER      = new Font("Serif", Font.BOLD,  13);

    // ── State ─────────────────────────────────────────────────────────────────

    private final Ledger       ledger;
    private final ResourcePool resources;

    private ResourceType       activeTab         = ResourceType.GOLD;
    private boolean            viewingLastSeason = false;
    private LedgerSnapshot     snapshot          = null;   // null until first end-turn

    // ── Sub-panels ────────────────────────────────────────────────────────────

    private final JPanel  tabBar      = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JLabel  banner      = new JLabel("Last Season's Ledger", SwingConstants.CENTER);
    private final JPanel  columns     = new JPanel(new GridLayout(1, 2, 4, 0));
    private final JPanel  footer      = new JPanel();

    // tracks expanded state per column+category key, default collapsed
    private final Map<String, Boolean> expandedState = new HashMap<>();

    // ── Constructor ───────────────────────────────────────────────────────────

    public LedgerPanel(Ledger ledger, ResourcePool resources) {
        this.ledger    = ledger;
        this.resources = resources;

        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createLineBorder(COL_BORDER, 1));
        setOpaque(true);
        setBackground(BG_BASE);

        // Banner
        banner.setFont(FONT_BANNER);
        banner.setForeground(COL_BANNER_FG);
        banner.setBackground(BG_BANNER);
        banner.setOpaque(true);
        banner.setBorder(new EmptyBorder(4, 8, 4, 8));
        banner.setVisible(false);

        // Tab bar
        tabBar.setBackground(BG_TAB_BAR);
        tabBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COL_BORDER));

        // North stack
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setOpaque(true);
        north.setBackground(BG_TAB_BAR);
        north.add(banner);
        north.add(tabBar);
        add(north, BorderLayout.NORTH);

        // Columns
        columns.setOpaque(true);
        columns.setBackground(BG_BASE);
        columns.setBorder(new EmptyBorder(6, 6, 0, 6));
        add(columns, BorderLayout.CENTER);

        // Footer
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(true);
        footer.setBackground(BG_BASE);
        footer.setBorder(new EmptyBorder(4, 10, 8, 10));
        add(footer, BorderLayout.SOUTH);

        refresh();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Capture a snapshot of current state. Call at the very start of End Turn,
     * before any ledger mutations.
     */
    public void captureSnapshot() {
        snapshot = LedgerSnapshot.capture(ledger, resources);
        debug.Debug.log("ledger-ui", "snapshot", "Snapshot captured.");
    }

    /** Rebuild entire panel from current ledger/resource state. */
    public void refresh() {
        rebuildTabBar();
        rebuildColumns();
        rebuildFooter();
        revalidate();
        repaint();
    }

    // ── Background ────────────────────────────────────────────────────────────

    @Override


    protected void paintComponent(Graphics g) {
        g.setColor(viewingLastSeason ? BG_LAST_SEASON : BG_NORMAL);
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }

// ── Tab bar ───────────────────────────────────────────────────────────────

private void rebuildTabBar() {
        tabBar.removeAll();
        for (ResourceType res : ResourceType.values()) {
            boolean active  = res == activeTab;
            boolean hasWarn = currentHasWarning(res);
            String  label   = resourceLabel(res) + (hasWarn ? " ⚠" : "");

            JButton tab = new JButton(label);
            tab.setFont(FONT_TAB);
            tab.setBackground(active ? BG_TAB_ACTIVE : BG_TAB_INACTIVE);
            tab.setForeground(active ? COL_TAB_ACTIVE : COL_TAB_INACTIVE);
            tab.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 1, COL_BORDER),
                    BorderFactory.createEmptyBorder(6, 14, 6, 14)));
            tab.setFocusPainted(false);
            tab.setOpaque(true);
            tab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            final ResourceType captured = res;
            tab.addActionListener(e -> {
                activeTab = captured;
                refresh();
            });
            tabBar.add(tab);
        }
    }

// ── Columns ───────────────────────────────────────────────────────────────

    private void rebuildColumns() {
        columns.removeAll();

        List<Entry> rec;
        List<Entry> one;

        if (viewingLastSeason && snapshot != null) {
            rec = snapshot.recurringEntries(activeTab);
            one = snapshot.oneTimeEntries(activeTab);
        } else {
            rec = ledger.getRecurringEntries(activeTab);
            one = ledger.getOneTimeEntries(activeTab);
        }

        columns.add(buildRecurringColumn(rec, viewingLastSeason));
        columns.add(buildOneTimeColumn(one,   viewingLastSeason));
    }

private JPanel buildRecurringColumn(List<Entry> entries, boolean dim) {
        JPanel panel = columnPanel("Standing Income & Upkeep");
        Map<String, List<Entry>> grouped = groupByCategory(entries);

        if (grouped.isEmpty()) {
            panel.add(italicLabel("No standing entries this season.", dim));
        } else {
            for (Map.Entry<String, List<Entry>> cat : grouped.entrySet()) {
                String      catKey  = "rec|" + cat.getKey();
                List<Entry> catList = cat.getValue();
                int         catSum  = catList.stream().mapToInt(e -> e.amount).sum();
                panel.add(collapsibleCategory(catKey, cat.getKey(), catSum, catList, dim, panel));
            }
            int subtotal = entries.stream().mapToInt(e -> e.amount).sum();
            panel.add(subtotalRow("Recurring net", subtotal, dim));
        }

        panel.add(Box.createVerticalGlue());
        return panel;
    }

private JPanel buildOneTimeColumn(List<Entry> entries, boolean dim) {
        JPanel panel = columnPanel("This Season's Expenditures");

        JLabel note = new JLabel("Not carried into next season");
        note.setFont(FONT_ITALIC);
        note.setForeground(COL_NEUTRAL);
        note.setBorder(new EmptyBorder(0, 4, 6, 4));
        panel.add(note);

        if (entries.isEmpty()) {
            panel.add(italicLabel("No expenditures this season.", dim));
        } else {
            Map<String, List<Entry>> grouped = groupByCategory(entries);
            for (Map.Entry<String, List<Entry>> cat : grouped.entrySet()) {
                String      catKey  = "one|" + cat.getKey();
                List<Entry> catList = cat.getValue();
                int         catSum  = catList.stream().mapToInt(e -> e.amount).sum();
                panel.add(collapsibleCategory(catKey, cat.getKey(), catSum, catList, dim, panel));
            }
        }

        panel.add(Box.createVerticalGlue());
        return panel;
    }

// ── Footer ────────────────────────────────────────────────────────────────

    private void rebuildFooter() {
        footer.removeAll();
        footer.add(separator());
        footer.add(Box.createVerticalStrut(6));

        if (viewingLastSeason && snapshot != null) {
            buildLastSeasonFooter();
        } else {
            buildCurrentFooter();
        }

        footer.add(Box.createVerticalStrut(8));
        footer.add(lastSeasonButton());
    }

    private void buildCurrentFooter() {
        int current   = getAmount(activeTab);
        int delta     = ledger.getDelta(activeTab);
        int projected = current + delta;

        // Projected row
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setOpaque(false);
        row.add(styledLabel("Projected: ",        FONT_TOTAL, UITheme.TEXT_PRIMARY));
        row.add(styledLabel(current + "",          FONT_TOTAL, COL_NEUTRAL));
        row.add(styledLabel(delta >= 0 ? " + " : " − ", FONT_TOTAL, COL_NEUTRAL));
        row.add(styledLabel(Math.abs(delta) + "",  FONT_TOTAL, delta >= 0 ? COL_POSITIVE : COL_NEGATIVE));
        row.add(styledLabel(" = ",                 FONT_TOTAL, COL_NEUTRAL));
        row.add(styledLabel(projected + "",        FONT_TOTAL, projected <= 0 ? COL_NEGATIVE : COL_POSITIVE));

        // Direction arrow vs snapshot
        if (snapshot != null) {
            int prev = snapshot.totalAtCapture(activeTab);
            String arrow   = projected > prev ? "▲" : projected < prev ? "▼" : "→";
            Color  arrowCol = projected > prev ? COL_POSITIVE : projected < prev ? COL_NEGATIVE : COL_NEUTRAL;
            row.add(styledLabel(" " + arrow, FONT_TOTAL, arrowCol));
        }

        if (projected <= 0) row.add(warningLabel(" ⚠", false));

        footer.add(row);

        // Warning list — all resources
        List<String> warns = gatherAllWarnings();
        if (!warns.isEmpty()) {
            footer.add(Box.createVerticalStrut(6));
            for (String w : warns) {
                footer.add(warningLabel("⚠  " + w, false));
            }
        }
    }

    private void buildLastSeasonFooter() {
        int total = snapshot.totalAtCapture(activeTab);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setOpaque(false);
        row.add(styledLabel("End of season: ", FONT_TOTAL, COL_NEUTRAL));
        row.add(styledLabel(total + "",        FONT_TOTAL, total <= 0 ? COL_NEGATIVE : COL_NEUTRAL));
        footer.add(row);

        List<String> warns = snapshot.warnings(activeTab);
        if (!warns.isEmpty()) {
            footer.add(Box.createVerticalStrut(6));
            for (String w : warns) footer.add(warningLabel("⚠  " + w, true));
        }
    }

private JButton lastSeasonButton() {
        String label = viewingLastSeason ? "Return to Current Season" : "Last Season";
        JButton btn  = new JButton(label);
        btn.setFont(new Font("Serif", Font.BOLD, 12));
        btn.setEnabled(snapshot != null);
        btn.setBackground(snapshot != null ? new Color(55, 40, 10) : new Color(18, 14, 22));
        btn.setForeground(snapshot != null ? COL_TITLE : COL_NEUTRAL);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorderPainted(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(snapshot != null ? COL_BORDER : new Color(35, 30, 45), 1),
                new EmptyBorder(5, 14, 5, 14)));
        btn.setCursor(snapshot != null
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());

        btn.addActionListener(e -> {
            viewingLastSeason = !viewingLastSeason;
            banner.setVisible(viewingLastSeason);
            refresh();
        });

        return btn;
    }

// ── Warning logic ─────────────────────────────────────────────────────────

    private boolean currentHasWarning(ResourceType res) {
        int delta     = ledger.getDelta(res);
        int projected = getAmount(res) + delta;
        return delta < 0 || projected <= 0;
    }

    private List<String> gatherAllWarnings() {
        List<String> list = new ArrayList<>();
        for (ResourceType res : ResourceType.values()) {
            int delta     = ledger.getDelta(res);
            int projected = getAmount(res) + delta;
            if (delta < 0)
                list.add(resourceLabel(res) + " income is in the red — reserves are draining.");
            if (projected <= 0)
                list.add(resourceLabel(res) + " will be exhausted by next season's end.");
        }
        return list;
    }

    // ── Component builders ────────────────────────────────────────────────────

private JPanel columnPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(true);
        panel.setBackground(BG_COLUMN);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COL_BORDER, 1),
                new EmptyBorder(4, 6, 4, 6)));

        JLabel header = new JLabel(title);
        header.setFont(FONT_SECTION);
        header.setForeground(COL_TITLE);
        header.setBorder(new EmptyBorder(0, 0, 6, 0));
        panel.add(header);
        return panel;
    }

private JPanel collapsibleCategory(String key, String categoryName, int sum,
                                        List<Entry> entries, boolean dim, JPanel parent) {
        boolean expanded = expandedState.getOrDefault(key, false);

        // Outer container: header + children
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setAlignmentX(LEFT_ALIGNMENT);

        // Header row
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CAT_HEADER);
        header.setOpaque(true);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, COL_BORDER),
                new EmptyBorder(3, 4, 3, 4)));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        header.setAlignmentX(LEFT_ALIGNMENT);
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        String arrow = expanded ? "▾ " : "▸ ";
        JLabel nameLabel = new JLabel(arrow + capitalize(categoryName));
        nameLabel.setFont(FONT_CAT);
        nameLabel.setForeground(dim ? COL_NEUTRAL : COL_TITLE);

        String sign = sum >= 0 ? "+" : "−";
        JLabel sumLabel = new JLabel(sign + Math.abs(sum));
        sumLabel.setFont(FONT_CAT);
        sumLabel.setForeground(dim ? COL_NEUTRAL : sum > 0 ? COL_POSITIVE : sum < 0 ? COL_NEGATIVE : COL_NEUTRAL);

        header.add(nameLabel, BorderLayout.WEST);
        header.add(sumLabel,  BorderLayout.EAST);

        // Children
        JPanel children = new JPanel();
        children.setLayout(new BoxLayout(children, BoxLayout.Y_AXIS));
        children.setOpaque(false);
        children.setAlignmentX(LEFT_ALIGNMENT);
        children.setVisible(expanded);

        boolean alt = false;
        for (Entry e : entries) {
            children.add(entryRow(e.name, e.amount, alt, dim));
            alt = !alt;
        }

        // Toggle
        header.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent ev) {
                boolean nowExpanded = !expandedState.getOrDefault(key, false);
                expandedState.put(key, nowExpanded);
                children.setVisible(nowExpanded);
                nameLabel.setText((nowExpanded ? "▾ " : "▸ ") + capitalize(categoryName));
                container.revalidate();
                container.repaint();
                parent.revalidate();
                parent.repaint();
            }
        });

        container.add(header);
        container.add(children);
        return container;
    }

private JPanel entryRow(String name, int amount, boolean alt, boolean dim) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(alt ? BG_ROW_B : BG_ROW_A);
        row.setOpaque(true);
        row.setBorder(new EmptyBorder(2, 16, 2, 4));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(FONT_ENTRY);
        nameLabel.setForeground(dim ? COL_NEUTRAL : COL_TEXT);

        String sign = amount >= 0 ? "+" : "−";
        JLabel amtLabel = new JLabel(sign + Math.abs(amount));
        amtLabel.setFont(FONT_ENTRY);
        amtLabel.setForeground(dim ? COL_NEUTRAL
                : amount > 0 ? COL_POSITIVE
                : amount < 0 ? COL_NEGATIVE
                : COL_NEUTRAL);

        row.add(nameLabel, BorderLayout.WEST);
        row.add(amtLabel,  BorderLayout.EAST);
        return row;
    }

private JPanel subtotalRow(String label, int amount, boolean dim) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(true);
        row.setBackground(BG_CAT_HEADER);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, COL_BORDER),
                new EmptyBorder(4, 4, 4, 4)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(FONT_SECTION);
        nameLabel.setForeground(dim ? COL_NEUTRAL : COL_TEXT);

        String sign = amount >= 0 ? "+" : "−";
        String text = sign + Math.abs(amount) + ((!dim && amount < 0) ? " ⚠" : "");
        JLabel amtLabel = new JLabel(text);
        amtLabel.setFont(FONT_SECTION);
        amtLabel.setForeground(dim ? COL_NEUTRAL
                : amount > 0 ? COL_POSITIVE
                : amount < 0 ? COL_WARNING
                : COL_NEUTRAL);

        row.add(nameLabel, BorderLayout.WEST);
        row.add(amtLabel,  BorderLayout.EAST);
        return row;
    }

private JLabel italicLabel(String text, boolean dim) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_ITALIC);
        l.setForeground(dim ? COL_WARNING_DIM : COL_NEUTRAL);
        l.setBorder(new EmptyBorder(6, 4, 4, 4));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

private JLabel styledLabel(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    private JLabel warningLabel(String text, boolean dim) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_SMALL);
        l.setForeground(dim ? COL_WARNING_DIM : COL_WARNING);
        return l;
    }

private JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(COL_BORDER);
        sep.setBackground(BG_BASE);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

// ── Utility ───────────────────────────────────────────────────────────────

    private int getAmount(ResourceType res) {
        return switch (res) {
            case GOLD      -> resources.getMoney();
            case FOOD      -> resources.getFood();
            case MANPOWER  -> resources.getManpower();
            case INFLUENCE -> resources.getInfluence();
        };
    }

    private String resourceLabel(ResourceType res) {
        return switch (res) {
            case GOLD      -> "Gold";
            case FOOD      -> "Food";
            case MANPOWER  -> "Manpower";
            case INFLUENCE -> "Influence";
        };
    }

    private Map<String, List<Entry>> groupByCategory(List<Entry> entries) {
        Map<String, List<Entry>> map = new LinkedHashMap<>();
        for (Entry e : entries) map.computeIfAbsent(e.category, k -> new ArrayList<>()).add(e);
        return map;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}