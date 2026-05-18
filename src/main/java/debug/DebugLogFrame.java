package debug;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

final class DebugLogFrame extends JFrame {
    private final LogTableModel tableModel = new LogTableModel();
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<LogTableModel> sorter = new TableRowSorter<>(tableModel);
    private final Set<String> knownCategories = new HashSet<>();
    private final Set<String> knownTypes = new HashSet<>();
    private final Map<String, JCheckBox> categoryCheckboxes = new LinkedHashMap<>();
    private final Map<String, JCheckBox> typeCheckboxes = new LinkedHashMap<>();
    private JPanel categoryPanel;
    private JPanel typePanel;
    private JCheckBox showDetailsCheckbox;
    private JCheckBox stayOnTopCheckbox;

    // Preferences keys
    private static final String PREFS_NODE = "debug_log_viewer";
    private static final String KEY_X = "window_x";
    private static final String KEY_Y = "window_y";
    private static final String KEY_WIDTH = "window_width";
    private static final String KEY_HEIGHT = "window_height";
    private static final String KEY_SHOW_DETAILS = "show_details";
    private static final String KEY_STAY_ON_TOP = "stay_on_top";

    DebugLogFrame() {
        setTitle("Debug Log Viewer");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        table.setRowSorter(sorter);
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);

        // Create panels with wrap layout and put them in scroll panes
        categoryPanel = new JPanel(new WrapLayout(FlowLayout.LEFT));
        typePanel = new JPanel(new WrapLayout(FlowLayout.LEFT));
        categoryPanel.setBorder(BorderFactory.createTitledBorder("Categories"));
        typePanel.setBorder(BorderFactory.createTitledBorder("Types"));

        JScrollPane categoryScroll = new JScrollPane(categoryPanel);
        categoryScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        categoryScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        JScrollPane typeScroll = new JScrollPane(typePanel);
        typeScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        typeScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Give scroll panes a reasonable preferred height
        categoryScroll.setPreferredSize(new Dimension(0, 80));
        typeScroll.setPreferredSize(new Dimension(0, 80));

        JPanel filterPanel = new JPanel(new GridLayout(2, 1));
        filterPanel.add(categoryScroll);
        filterPanel.add(typeScroll);

        JPanel controlBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        showDetailsCheckbox = new JCheckBox("Show Details (Timestamp, Category, Type)", true);
        stayOnTopCheckbox = new JCheckBox("Stay on Top", false);
        JButton copyButton = new JButton("Copy Messages (Visible)");
        JButton clearButton = new JButton("Clear All Logs");

        showDetailsCheckbox.addActionListener(e -> toggleColumns());
        stayOnTopCheckbox.addActionListener(e -> setAlwaysOnTop(stayOnTopCheckbox.isSelected()));
        copyButton.addActionListener(e -> copyVisibleMessages());
        clearButton.addActionListener(e -> clearLogs());

        controlBar.add(showDetailsCheckbox);
        controlBar.add(stayOnTopCheckbox);
        controlBar.add(copyButton);
        controlBar.add(clearButton);

        add(filterPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(controlBar, BorderLayout.SOUTH);

        // Load saved preferences
        loadPreferences();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                savePreferences();
                DebugUIManager.getInstance().onWindowClosedByUser();
            }
        });

        // Also save on resize / move (real‑time)
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                savePreferences();
            }
            @Override
            public void componentMoved(ComponentEvent e) {
                savePreferences();
            }
        });

        toggleColumns();
        updateFilter();

        // Pack and then apply saved size/location (if any)
        pack();
        if (getWidth() == 0 || getHeight() == 0) {
            setSize(900, 600);
        }
        setLocationRelativeTo(null); // fallback, but preferences will override if saved
        applySavedBounds();
    }

    private void loadPreferences() {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        showDetailsCheckbox.setSelected(prefs.getBoolean(KEY_SHOW_DETAILS, true));
        stayOnTopCheckbox.setSelected(prefs.getBoolean(KEY_STAY_ON_TOP, false));
        setAlwaysOnTop(stayOnTopCheckbox.isSelected());
    }

    private void savePreferences() {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        prefs.putBoolean(KEY_SHOW_DETAILS, showDetailsCheckbox.isSelected());
        prefs.putBoolean(KEY_STAY_ON_TOP, stayOnTopCheckbox.isSelected());
        prefs.putInt(KEY_X, getX());
        prefs.putInt(KEY_Y, getY());
        prefs.putInt(KEY_WIDTH, getWidth());
        prefs.putInt(KEY_HEIGHT, getHeight());
    }

    private void applySavedBounds() {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        int x = prefs.getInt(KEY_X, -1);
        int y = prefs.getInt(KEY_Y, -1);
        int w = prefs.getInt(KEY_WIDTH, -1);
        int h = prefs.getInt(KEY_HEIGHT, -1);
        if (x != -1 && y != -1 && w != -1 && h != -1) {
            setBounds(x, y, w, h);
        } else {
            setSize(900, 600);
            setLocationRelativeTo(null);
        }
    }

    void addLogEntries(List<LogEntry> entries) {
        if (entries.isEmpty()) return;

        tableModel.addLogs(entries);

        boolean categoriesChanged = false;
        boolean typesChanged = false;
        for (LogEntry entry : entries) {
            if (knownCategories.add(entry.category())) {
                addCategoryCheckbox(entry.category());
                categoriesChanged = true;
            }
            if (knownTypes.add(entry.type())) {
                addTypeCheckbox(entry.type());
                typesChanged = true;
            }
        }

        if (categoriesChanged || typesChanged) {
            revalidateFilterPanels();
        }
        updateFilter();
    }

    private void addCategoryCheckbox(String category) {
        JCheckBox cb = new JCheckBox(category, true);
        cb.addActionListener(e -> updateFilter());
        categoryCheckboxes.put(category, cb);
    }

    private void addTypeCheckbox(String type) {
        JCheckBox cb = new JCheckBox(type, true);
        cb.addActionListener(e -> updateFilter());
        typeCheckboxes.put(type, cb);
    }

    private void revalidateFilterPanels() {
        categoryPanel.removeAll();
        for (JCheckBox cb : categoryCheckboxes.values()) {
            categoryPanel.add(cb);
        }
        addSelectButtons(categoryPanel, categoryCheckboxes);

        typePanel.removeAll();
        for (JCheckBox cb : typeCheckboxes.values()) {
            typePanel.add(cb);
        }
        addSelectButtons(typePanel, typeCheckboxes);

        categoryPanel.revalidate();
        categoryPanel.repaint();
        typePanel.revalidate();
        typePanel.repaint();
    }

    private void addSelectButtons(JPanel panel, Map<String, JCheckBox> checkboxes) {
        JButton all = new JButton("All");
        JButton none = new JButton("None");
        all.addActionListener(e -> {
            for (JCheckBox cb : checkboxes.values()) cb.setSelected(true);
            updateFilter();
        });
        none.addActionListener(e -> {
            for (JCheckBox cb : checkboxes.values()) cb.setSelected(false);
            updateFilter();
        });
        panel.add(all);
        panel.add(none);
    }

    private void updateFilter() {
        List<RowFilter<LogTableModel, Integer>> filters = new ArrayList<>();

        Set<String> enabledCategories = categoryCheckboxes.entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (!enabledCategories.isEmpty()) {
            filters.add(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends LogTableModel, ? extends Integer> entry) {
                    String category = (String) entry.getValue(1);
                    return enabledCategories.contains(category);
                }
            });
        }

        Set<String> enabledTypes = typeCheckboxes.entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (!enabledTypes.isEmpty()) {
            filters.add(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends LogTableModel, ? extends Integer> entry) {
                    String type = (String) entry.getValue(2);
                    return enabledTypes.contains(type);
                }
            });
        }

        RowFilter<LogTableModel, Integer> compound;
        if (enabledCategories.isEmpty() && enabledTypes.isEmpty()) {
            compound = new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends LogTableModel, ? extends Integer> entry) {
                    return false;
                }
            };
        } else if (filters.isEmpty()) {
            compound = null;
        } else {
            compound = RowFilter.andFilter(filters);
        }
        sorter.setRowFilter(compound);
    }

    private void toggleColumns() {
        boolean show = showDetailsCheckbox.isSelected();
        int[] widths = show ? new int[]{100, 100, 100} : new int[]{0, 0, 0};
        for (int i = 0; i < 3; i++) {
            table.getColumnModel().getColumn(i).setMinWidth(widths[i]);
            table.getColumnModel().getColumn(i).setMaxWidth(show ? 200 : 0);
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(3).setPreferredWidth(400);
    }

    private void copyVisibleMessages() {
        int visibleCount = sorter.getViewRowCount();
        if (visibleCount == 0) {
            JOptionPane.showMessageDialog(this, "No visible logs to copy.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int viewRow = 0; viewRow < visibleCount; viewRow++) {
            int modelRow = sorter.convertRowIndexToModel(viewRow);
            LogEntry entry = tableModel.getLogAt(modelRow);
            sb.append(entry.message()).append("\n");
        }
        StringSelection selection = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }

    private void clearLogs() {
        tableModel.clear();
        updateFilter();
    }
}