package City.debug;

@FunctionalInterface
interface DebugObserver {
    void onLogAdded(LogEntry entry);   // called for each individual log (may be batched later)
}