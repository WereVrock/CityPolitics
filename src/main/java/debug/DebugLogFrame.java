package debug;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.List;
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

DebugLogFrame() {
        setTitle("Debug Log Viewer");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        table.setRowSorter(sorter);
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);

        categoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        categoryPanel.setBorder(BorderFactory.createTitledBorder("Categories"));
        typePanel.setBorder(BorderFactory.createTitledBorder("Types"));

        JPanel filterPanel = new JPanel(new GridLayout(2, 1));
        filterPanel.add(categoryPanel);
        filterPanel.add(typePanel);

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

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                DebugUIManager.getInstance().onWindowClosedByUser();
            }
        });

        toggleColumns();
        updateFilter();

        // Auto-size to fit content, then center, and set a reasonable minimum size
        pack();
        setMinimumSize(new Dimension(600, 300));
        setLocationRelativeTo(null);
    }

/** Add a batch of log entries (called from the flusher) */
    void addLogEntries(List<LogEntry> entries) {
        if (entries.isEmpty()) return;

        // Batch insert into table model
        tableModel.addLogs(entries);

        // Update known categories and types across the batch
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
        // Always update filter because new logs might be filtered
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
            // No filters selected -> show nothing
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
        // Message column always visible
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
        // Optionally keep filter checkboxes – they remain but logs are gone.
        updateFilter();
    }
}