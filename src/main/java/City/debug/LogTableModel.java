package City.debug;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

final class LogTableModel extends AbstractTableModel {
    private final List<LogEntry> logs = new ArrayList<>();
    private static final int COL_TIMESTAMP = 0;
    private static final int COL_CATEGORY = 1;
    private static final int COL_TYPE = 2;
    private static final int COL_MESSAGE = 3;
    private static final String[] COLUMN_NAMES = {"Timestamp", "Category", "Type", "Message"};

    @Override
    public int getRowCount() { return logs.size(); }

    @Override
    public int getColumnCount() { return COLUMN_NAMES.length; }

    @Override
    public String getColumnName(int column) { return COLUMN_NAMES[column]; }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        LogEntry entry = logs.get(rowIndex);
        return switch (columnIndex) {
            case COL_TIMESTAMP -> entry.formattedTimestamp();
            case COL_CATEGORY  -> entry.category();
            case COL_TYPE      -> entry.type();
            case COL_MESSAGE   -> entry.message();
            default -> null;
        };
    }

    /** Add a single log (legacy, but kept for compatibility) */
    void addLog(LogEntry entry) {
        logs.add(entry);
        fireTableRowsInserted(logs.size() - 1, logs.size() - 1);
    }

    /** Add multiple logs in one batch (preferred for performance) */
    void addLogs(List<LogEntry> entries) {
        if (entries.isEmpty()) return;
        int start = logs.size();
        logs.addAll(entries);
        int end = logs.size() - 1;
        fireTableRowsInserted(start, end);
    }

    void clear() {
        int size = logs.size();
        logs.clear();
        if (size > 0) {
            fireTableRowsDeleted(0, size - 1);
        }
    }

    LogEntry getLogAt(int row) { return logs.get(row); }
}