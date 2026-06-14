package City.debug;

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
    // Maps each type to its parent category (first seen category for that type)
    private final Map<String, String> typeToCategory = new HashMap<>();
    private boolean programmaticFilterActive = false;
    private java.util.Map<String, Boolean> savedTypeSelections;
    private java.util.Map<String, Boolean> savedCategorySelections;
    // Stores original checkbox state for each type (used when category is re-enabled)
    private final Map<String, Boolean> typeOriginalState = new HashMap<>();
    private JPanel categoryPanel;
    private JPanel typePanel;
    private JCheckBox showDetailsCheckbox;
    private JCheckBox stayOnTopCheckbox;
    private int logFontSize = 12;
    private static final int LOG_FONT_SIZE_MIN = 8;
    private static final int LOG_FONT_SIZE_MAX = 24;

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

        JPanel controlBar = new JPanel(new WrapLayout(FlowLayout.LEFT));
        showDetailsCheckbox = new JCheckBox("Show Details (Timestamp, Category, Type)", true);
        stayOnTopCheckbox = new JCheckBox("Stay on Top", false);
        JButton copyButton = new JButton("Copy Messages (Visible)");
        JButton chunkButton = new JButton("Copy Visible in Chunks");
        JButton clearButton = new JButton("Clear All Logs");

        showDetailsCheckbox.addActionListener(e -> toggleColumns());
        stayOnTopCheckbox.addActionListener(e -> setAlwaysOnTop(stayOnTopCheckbox.isSelected()));
        copyButton.addActionListener(e -> copyVisibleMessages());
        chunkButton.addActionListener(e -> openChunkCopyDialog());
        clearButton.addActionListener(e -> clearLogs());

        JButton fontDecBtn = new JButton("A-");
        JButton fontIncBtn = new JButton("A+");
        fontDecBtn.addActionListener(e -> adjustLogFontSize(-1));
        fontIncBtn.addActionListener(e -> adjustLogFontSize(+1));

        controlBar.add(showDetailsCheckbox);
        controlBar.add(stayOnTopCheckbox);
        controlBar.add(fontDecBtn);
        controlBar.add(fontIncBtn);
        controlBar.add(copyButton);
        controlBar.add(chunkButton);
        controlBar.add(clearButton);
        JButton pasteFilterButton = new JButton("Paste Filter from Clipboard");
        pasteFilterButton.addActionListener(e -> pasteFilterFromClipboard());
        controlBar.add(pasteFilterButton);
        JButton copyFilterButton = new JButton("Copy Filter Instructions for AI");
        copyFilterButton.addActionListener(e -> copyFilterInstructionsForAI());
        controlBar.add(copyFilterButton);

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
                // Store the category for this type (first seen)
                typeToCategory.put(entry.type(), entry.category());
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
        cb.addActionListener(e -> {
            rebuildTypePanel();   // rebuild instead of enable/disable
            updateFilter();
        });
        categoryCheckboxes.put(category, cb);
    }

    private void addTypeCheckbox(String type) {
        JCheckBox cb = new JCheckBox(type, true);
        cb.addActionListener(e -> {
            // Save original state for this type when toggled
            typeOriginalState.put(type, cb.isSelected());
            updateFilter();
        });
        typeCheckboxes.put(type, cb);
        // Initially store default state (true)
        typeOriginalState.putIfAbsent(type, true);
    }

    private void rebuildTypePanel() {
        // Determine which categories are currently selected
        Set<String> selectedCategories = new HashSet<>();
        for (Map.Entry<String, JCheckBox> entry : categoryCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedCategories.add(entry.getKey());
            }
        }
        
        // Build a new set of types to show (those whose category is selected)
        Set<String> typesToShow = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : typeToCategory.entrySet()) {
            String type = entry.getKey();
            String category = entry.getValue();
            if (selectedCategories.contains(category)) {
                typesToShow.add(type);
            }
        }
        
        // Remember current selection states for types that are being removed
        for (Map.Entry<String, JCheckBox> entry : typeCheckboxes.entrySet()) {
            typeOriginalState.put(entry.getKey(), entry.getValue().isSelected());
        }
        
        // Remove all current type checkboxes
        typeCheckboxes.clear();
        typePanel.removeAll();
        
        // Add checkboxes for typesToShow
        for (String type : typesToShow) {
            JCheckBox cb = new JCheckBox(type, true);
            Boolean savedState = typeOriginalState.get(type);
            cb.setSelected(savedState != null ? savedState : true);
            cb.addActionListener(e -> {
                typeOriginalState.put(type, cb.isSelected());
                updateFilter();
            });
            typeCheckboxes.put(type, cb);
            typePanel.add(cb);
        }
        
        // Add the All/None buttons for the type panel (they will work on visible types only)
        if (!typeCheckboxes.isEmpty()) {
            JButton all = new JButton("All");
            JButton none = new JButton("None");
            all.addActionListener(e -> {
                for (JCheckBox cb : typeCheckboxes.values()) {
                    cb.setSelected(true);
                    typeOriginalState.put(getTypeFromCheckbox(cb), true);
                }
                updateFilter();
            });
            none.addActionListener(e -> {
                for (JCheckBox cb : typeCheckboxes.values()) {
                    cb.setSelected(false);
                    typeOriginalState.put(getTypeFromCheckbox(cb), false);
                }
                updateFilter();
            });
            typePanel.add(all);
            typePanel.add(none);
        }
        
        typePanel.revalidate();
        typePanel.repaint();
    }
    
    private String getTypeFromCheckbox(JCheckBox cb) {
        for (Map.Entry<String, JCheckBox> entry : typeCheckboxes.entrySet()) {
            if (entry.getValue() == cb) return entry.getKey();
        }
        return null;
    }

    private void revalidateFilterPanels() {
        categoryPanel.removeAll();
        for (JCheckBox cb : categoryCheckboxes.values()) {
            categoryPanel.add(cb);
        }
        addSelectButtons(categoryPanel, categoryCheckboxes);
        
        // Rebuild type panel based on currently selected categories
        rebuildTypePanel();
        
        categoryPanel.revalidate();
        categoryPanel.repaint();
        // typePanel already rebuilt with revalidate/repaint inside rebuildTypePanel
    }

    private void addSelectButtons(JPanel panel, Map<String, JCheckBox> checkboxes) {
        JButton all = new JButton("All");
        JButton none = new JButton("None");
        all.addActionListener(e -> {
            for (JCheckBox cb : checkboxes.values()) cb.setSelected(true);
            if (panel == categoryPanel) {
                rebuildTypePanel();   // rebuild because categories changed
            }
            updateFilter();
        });
        none.addActionListener(e -> {
            for (JCheckBox cb : checkboxes.values()) cb.setSelected(false);
            if (panel == categoryPanel) {
                rebuildTypePanel();
            }
            updateFilter();
        });
        panel.add(all);
        panel.add(none);
    }

    /**
     * Set the exact set of visible log types programmatically (e.g. from AI debugging commands).
     * Category checkboxes are automatically selected if any type of that category is enabled,
     * and all checkboxes become non-interactive until {@link #clearProgrammaticFilter()} is called.
     */

void setVisibleTypes(java.util.Set<String> types) {
        for (java.util.Map.Entry<String, JCheckBox> entry : typeCheckboxes.entrySet()) {
            entry.getValue().setSelected(types.contains(entry.getKey()));
        }
        // Update category checkboxes: selected if any of its types is selected
        for (java.util.Map.Entry<String, JCheckBox> catEntry : categoryCheckboxes.entrySet()) {
            boolean anySelected = false;
            for (String typeName : typeCheckboxes.keySet()) {
                if (catEntry.getKey().equals(typeToCategory.get(typeName))
                        && typeCheckboxes.get(typeName).isSelected()) {
                    anySelected = true;
                    break;
                }
            }
            catEntry.getValue().setSelected(anySelected);
        }
        updateFilter();
    }

void clearProgrammaticFilter() {
        // If any controls were disabled (e.g. from a previous programmatic filter), re-enable them.
        for (JCheckBox cb : typeCheckboxes.values()) {
            cb.setEnabled(true);
            cb.setSelected(true);
        }
        for (JCheckBox cb : categoryCheckboxes.values()) {
            cb.setEnabled(true);
            cb.setSelected(true);
        }
        setFilterButtonsEnabled(true);
        updateFilter();
    }

private void captureUserSelections() {
        savedTypeSelections = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, JCheckBox> e : typeCheckboxes.entrySet()) {
            savedTypeSelections.put(e.getKey(), e.getValue().isSelected());
        }
        savedCategorySelections = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, JCheckBox> e : categoryCheckboxes.entrySet()) {
            savedCategorySelections.put(e.getKey(), e.getValue().isSelected());
        }
    }

    private void setFilterButtonsEnabled(boolean enabled) {
        // Traverse the categoryPanel and typePanel to find All/None buttons and enable/disable them.
        for (java.awt.Component comp : categoryPanel.getComponents()) {
            if (comp instanceof JButton) comp.setEnabled(enabled);
        }
        for (java.awt.Component comp : typePanel.getComponents()) {
            if (comp instanceof JButton) comp.setEnabled(enabled);
        }
    }

    private void updateFilter() {
        // Enabled categories
        Set<String> enabledCategories = categoryCheckboxes.entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        
        // Enabled types (only those currently visible in the UI and selected)
        Set<String> enabledTypes = typeCheckboxes.entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        
        // If no categories selected, show nothing
        if (enabledCategories.isEmpty()) {
            sorter.setRowFilter(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends LogTableModel, ? extends Integer> entry) {
                    return false;
                }
            });
            return;
        }
        
        // If no types selected, show nothing
        if (enabledTypes.isEmpty()) {
            sorter.setRowFilter(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends LogTableModel, ? extends Integer> entry) {
                    return false;
                }
            });
            return;
        }
        
        // Row is visible if its category is selected AND its type is selected (type must belong to that category, but type selection already ensures category because types are hidden otherwise)
        RowFilter<LogTableModel, Integer> filter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends LogTableModel, ? extends Integer> entry) {
                String category = (String) entry.getValue(1);
                String type = (String) entry.getValue(2);
                return enabledCategories.contains(category) && enabledTypes.contains(type);
            }
        };
        sorter.setRowFilter(filter);
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

private void adjustLogFontSize(int delta) {
    logFontSize = Math.max(LOG_FONT_SIZE_MIN, Math.min(LOG_FONT_SIZE_MAX, logFontSize + delta));
    Font f = table.getFont().deriveFont((float) logFontSize);
    table.setFont(f);
    table.setRowHeight(logFontSize + 6);
}

private String getVisibleMessagesText() {
        int visibleCount = sorter.getViewRowCount();
        if (visibleCount == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int viewRow = 0; viewRow < visibleCount; viewRow++) {
            int modelRow = sorter.convertRowIndexToModel(viewRow);
            LogEntry entry = tableModel.getLogAt(modelRow);
            sb.append(entry.message()).append("\n");
        }
        return sb.toString();
    }

private void copyFilterInstructionsForAI() {
        StringBuilder sb = new StringBuilder();
        sb.append("To filter logs, copy ONLY the TYPES (space-separated) you want to see. Do NOT include category names.\n");
        sb.append("Categories will auto-select based on types.\n\n");
        sb.append("Available TYPES by category:\n");

        // Group types by category
        Map<String, List<String>> byCategory = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : typeToCategory.entrySet()) {
            byCategory.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }

        for (Map.Entry<String, List<String>> catEntry : byCategory.entrySet()) {
            sb.append("[").append(catEntry.getKey()).append("] ");
            java.util.Collections.sort(catEntry.getValue());
            sb.append(String.join(" ", catEntry.getValue()));
            sb.append("\n");
        }

        sb.append("\nExample: warchest attack opportunism");
        StringSelection sel = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
    }

private void pasteFilterFromClipboard() {
        try {
            String text = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor);
            if (text != null && !text.trim().isEmpty()) {
                String[] types = text.trim().split("\\s+");
                Debug.setVisibleTypes(types);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to read clipboard: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private java.util.List<String> splitIntoChunks(String text, int maxChunkSize) {
        java.util.List<String> chunks = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        String[] lines = text.split("(?<=\n)", -1); // keep line breaks
        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            if (current.length() + line.length() > maxChunkSize && current.length() > 0) {
                chunks.add(current.toString());
                current.setLength(0);
            }
            current.append(line);
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }
        return chunks;
    }

private void openChunkCopyDialog() {
        String fullText = getVisibleMessagesText();
        if (fullText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No visible logs to copy.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        java.util.List<String> chunks = splitIntoChunks(fullText, 20000);
        JDialog dialog = new JDialog(this, "Copy Logs in Chunks (20k char limit)", false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        JTextField statusField = new JTextField("Ready - click a chunk to copy");
        statusField.setEditable(false);
        statusField.setBackground(UIManager.getColor("Panel.background"));
        statusField.setHorizontalAlignment(JTextField.CENTER);
        
        JPanel buttonPanel = new JPanel(new WrapLayout(WrapLayout.LEFT));
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            int charCount = chunk.length();
            final int index = i;
            final String chunkText = chunk;
            JButton btn = new JButton(String.format("Chunk %d (%d chars)", index + 1, charCount));
            btn.addActionListener(e -> {
                StringSelection sel = new StringSelection(chunkText);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
                statusField.setText(String.format("Last copied: Chunk %d (%d characters)", index + 1, chunkText.length()));
            });
            buttonPanel.add(btn);
        }
        JScrollPane scrollPane = new JScrollPane(buttonPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Select chunk to copy"));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statusField, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

}