package debug;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class DebugUIManager implements DebugObserver {
    private static final DebugUIManager INSTANCE = new DebugUIManager();
    private DebugLogFrame frame;
    private boolean started = false;
    private final List<LogEntry> buffer = new ArrayList<>();
    private ScheduledExecutorService scheduler;
    private int flushDelayMs = 100;

    private DebugUIManager() {
        startScheduler();
    }

    static DebugUIManager getInstance() { return INSTANCE; }

    private void startScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DebugUIManager-Flush");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::flushBuffer, flushDelayMs, flushDelayMs, TimeUnit.MILLISECONDS);
    }

    void setFlushDelay(int ms) {
        this.flushDelayMs = ms;
        startScheduler(); // restart scheduler with new delay
    }

    private void flushBuffer() {
        List<LogEntry> toFlush;
        synchronized (buffer) {
            if (buffer.isEmpty()) return;
            toFlush = new ArrayList<>(buffer);
            buffer.clear();
        }
        SwingUtilities.invokeLater(() -> {
            if (frame != null && !toFlush.isEmpty()) {
                frame.addLogEntries(toFlush);
            }
        });
    }

    synchronized void ensureStarted() {
        if (!started && Debug.isActivateDebugWindow()) {
            startWindow();
        }
    }

    synchronized void startWindow() {
        if (frame != null && frame.isVisible()) {
            return;
        }
        started = true;
        SwingUtilities.invokeLater(() -> {
            frame = new DebugLogFrame();
            DebugCore.getInstance().addObserver(this);
            frame.setVisible(true);
        });
    }

    synchronized void closeWindow() {
        if (frame != null) {
            SwingUtilities.invokeLater(() -> {
                frame.dispose();
                frame = null;
            });
            DebugCore.getInstance().removeObserver(this);
        }
        started = false;
        // Clear buffer on close to avoid memory leak
        synchronized (buffer) {
            buffer.clear();
        }
    }

    void applyProgrammaticTypeFilter(java.util.Set<String> types) {
        if (frame != null) {
            SwingUtilities.invokeLater(() -> {
                if (types == null) {
                    frame.clearProgrammaticFilter();
                } else {
                    frame.setVisibleTypes(types);
                }
            });
        }
    }

    @Override
    public void onLogAdded(LogEntry entry) {
        synchronized (buffer) {
            buffer.add(entry);
        }
    }

    // Called by the frame when user closes it
    void onWindowClosedByUser() {
        DebugCore.getInstance().removeObserver(this);
        started = false;
        frame = null;
    }
}