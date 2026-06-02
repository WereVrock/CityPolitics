package main.ledger;

import debug.Debug;
import main.resources.ResourceType;

import java.util.*;

/**
 * Single source of truth for all resource changes.
 *
 * Recurring entries  — per-turn projection (nobles, pops, decay, upkeep).
 * One-time entries   — what actually changed this turn (actions, spending).
 *
 * UI projection = sum of recurring entries per resource.
 * Last-turn summary = one-time entries (cleared at turn start).
 */
public class Ledger {

    // ─── Entry ───────────────────────────────────────────────────────────────

    public static class Entry {
        public final ResourceType resource;
        public final String       category;
        public final String       name;
        public final int          amount;

        public Entry(ResourceType resource, String category, String name, int amount) {
            this.resource = resource;
            this.category = category;
            this.name     = name;
            this.amount   = amount;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s / %s : %+d", resource, category, name, amount);
        }
    }

    // ─── Storage ─────────────────────────────────────────────────────────────

    // key = resource + "|" + category + "|" + name  →  unique recurring line item
    private final Map<String, Entry> recurringEntries = new LinkedHashMap<>();

    private final List<Entry> oneTimeEntries = new ArrayList<>();

    // ─── Recurring ───────────────────────────────────────────────────────────

    /**
     * Upsert a recurring entry. Same category+name+resource always overwrites.
     * Call whenever a subsystem's contribution changes.
     */
    public void setRecurring(ResourceType resource, String category, String name, int amount) {
        String key   = buildKey(resource, category, name);
        Entry  entry = new Entry(resource, category, name, amount);
        recurringEntries.put(key, entry);
        Debug.log("ledger", "recurring-set",
                String.format("%s / %s / %s : %+d", resource, category, name, amount));
    }

    /**
     * Remove all recurring entries registered under this category+name.
     * Use when a noble is eliminated, an army disbanded, etc.
     */
    public void removeRecurring(String category, String name) {
        for (ResourceType res : ResourceType.values()) {
            String removed = recurringEntries.remove(buildKey(res, category, name)) != null
                    ? "removed" : "not-found";
            Debug.log("ledger", "recurring-remove",
                    String.format("%s / %s / %s : %s", res, category, name, removed));
        }
    }

    // ─── One-time ────────────────────────────────────────────────────────────

    /**
     * Log a one-time change (action cost, army spending, event reward, etc.).
     * These are NOT included in the projection delta.
     * Cleared at the start of each turn.
     */
    public void logOneTime(ResourceType resource, String category, String name, int amount) {
        Entry entry = new Entry(resource, category, name, amount);
        oneTimeEntries.add(entry);
        Debug.log("ledger", "one-time",
                String.format("%s / %s / %s : %+d", resource, category, name, amount));
    }

    /**
     * Called by TurnProcessor at the start of each turn.
     */
    public void clearOneTime() {
        oneTimeEntries.clear();
        Debug.log("ledger", "one-time-clear", "One-time entries cleared for new turn.");
    }

    // ─── Delta query ─────────────────────────────────────────────────────────

    /**
     * Net projected change for this resource (sum of all recurring entries).
     * This is what the UI should display as "end-of-turn delta".
     */
    public int getDelta(ResourceType resource) {
        int sum = 0;
        for (Entry e : recurringEntries.values()) {
            if (e.resource == resource) sum += e.amount;
        }
        return sum;
    }

    // ─── List access (for UI panels) ─────────────────────────────────────────

    public List<Entry> getRecurringEntries(ResourceType resource) {
        List<Entry> result = new ArrayList<>();
        for (Entry e : recurringEntries.values()) {
            if (e.resource == resource) result.add(e);
        }
        return Collections.unmodifiableList(result);
    }

    public List<Entry> getAllRecurringEntries() {
        return Collections.unmodifiableList(new ArrayList<>(recurringEntries.values()));
    }

    public List<Entry> getOneTimeEntries(ResourceType resource) {
        List<Entry> result = new ArrayList<>();
        for (Entry e : oneTimeEntries) {
            if (e.resource == resource) result.add(e);
        }
        return Collections.unmodifiableList(result);
    }

    public List<Entry> getAllOneTimeEntries() {
        return Collections.unmodifiableList(new ArrayList<>(oneTimeEntries));
    }

    // ─── Internal ────────────────────────────────────────────────────────────

    private String buildKey(ResourceType resource, String category, String name) {
        return resource.name() + "|" + category + "|" + name;
    }
}