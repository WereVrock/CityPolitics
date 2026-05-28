package debug;

/**
 * Public facade for the debugging log system.
 * Use Debug.log() to send logs; the GUI appears automatically if activateDebugWindow is true.
 */
public class Debug {
    private static boolean activateDebugWindow = true;
    private static boolean consoleOutputEnabled = true;
    private static int batchFlushDelayMs = 100;   // milliseconds

    private Debug() {}

    /**
     * Logs a message with category and type.
     * Prints to System.out if consoleOutputEnabled (default true).
     * If activateDebugWindow is true, the GUI will appear on first log.
     */
    public static void log(String category, String type, String message) {
        if (consoleOutputEnabled) {
            System.out.println(message);
        }
        DebugCore.getInstance().addLog(category, type, message);
        if (activateDebugWindow) {
            DebugUIManager.getInstance().ensureStarted();
        }
    }

    /** Enables/disables automatic appearance of the debug window. */
    public static void setActivateDebugWindow(boolean enable) {
        activateDebugWindow = enable;
        if (!enable) {
            DebugUIManager.getInstance().closeWindow();
        }
    }

    /** Manually opens the debug window (if not already visible). */
    public static void startDebugWindow() {
        if (activateDebugWindow) {
            DebugUIManager.getInstance().startWindow();
        }
    }

    /** Manually closes the debug window. */
    public static void closeDebugWindow() {
        DebugUIManager.getInstance().closeWindow();
    }

    /** Enables/disables console output (default true). */
    public static void setConsoleOutputEnabled(boolean enabled) {
        consoleOutputEnabled = enabled;
    }

    /** Programmatically show only the given log types. All others are hidden.
     *  Categories are automatically selected if any of their types are enabled. */
    public static void setVisibleTypes(String... typeNames) {
        DebugCore.getInstance().setProgrammaticTypeFilter(new java.util.HashSet<>(java.util.Arrays.asList(typeNames)));
    }

    /** Remove any programmatic filter and restore full user control of the debug window. */
    public static void clearVisibleTypes() {
        DebugCore.getInstance().setProgrammaticTypeFilter(null);
    }

    /** Sets the batch flush delay for GUI updates (milliseconds, default 100). */
    public static void setBatchFlushDelay(int ms) {
        batchFlushDelayMs = ms;
        DebugUIManager.getInstance().setFlushDelay(ms);
    }

    /** Prints all accumulated logs to the console. */
    public static void printLogsToConsole() {
        for (LogEntry entry : DebugCore.getInstance().getAllLogs()) {
            System.out.println("[" + entry.formattedTimestamp() + "] [" + entry.category() + "] [" + entry.type() + "] " + entry.message());
        }
    }

    // Package-private access
    static boolean isActivateDebugWindow() { return activateDebugWindow; }
}