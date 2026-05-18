package debug;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalTime;
import java.util.concurrent.CopyOnWriteArrayList;

final class DebugCore {
    private static final DebugCore INSTANCE = new DebugCore();
    private final List<LogEntry> logs = new ArrayList<>();
    private final List<DebugObserver> observers = new CopyOnWriteArrayList<>();

    private DebugCore() {}

    static DebugCore getInstance() { return INSTANCE; }

    void addLog(String category, String type, String message) {
        LogEntry entry = new LogEntry(LocalTime.now(), category, type, message);
        synchronized (logs) {
            logs.add(entry);
        }
        for (DebugObserver obs : observers) {
            obs.onLogAdded(entry);
        }
    }

    List<LogEntry> getAllLogs() {
        synchronized (logs) {
            return new ArrayList<>(logs);
        }
    }

    void addObserver(DebugObserver observer) {
        observers.add(observer);
        // Send existing logs to the new observer
        for (LogEntry entry : getAllLogs()) {
            observer.onLogAdded(entry);
        }
    }

    void removeObserver(DebugObserver observer) {
        observers.remove(observer);
    }
}