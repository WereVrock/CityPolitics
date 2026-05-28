package main.automation;

import debug.Debug;
import debug.LogEntry;
import main.core.GameState;

import java.util.List;

/**
 * Handles automation tasks such as multi-turn processing and log reporting.
 */
public class AutomationManager {

    private final GameState gameState;

    public AutomationManager(GameState gameState) {
        this.gameState = gameState;
    }

    /**
     * Processes a specified number of turns and prints the resulting logs to the console.
     * Stops early if a blocking session (e.g., vote) is encountered.
     */
    public void processMultiTurn(int count, Runnable turnProcessor) {
        for (int i = 0; i < count; i++) {
            turnProcessor.run();
            if (gameState.hasActiveSession()) {
                System.out.println("[Automation] Multi-turn stopped early due to active session.");
                break;
            }
        }
        printLogsToConsole();
    }

    /**
     * Retrieves all logs from the Debug system and prints them to System.out.
     */
    public void printLogsToConsole() {
        List<LogEntry> logs = Debug.getLogs();
        System.out.println("--- Automation: System Logs Begin ---");
        for (LogEntry entry : logs) {
            System.out.println("[" + entry.formattedTimestamp() + "] [" + entry.category() + "] [" + entry.type() + "] " + entry.message());
        }
        System.out.println("--- Automation: System Logs End ---");
    }
}
