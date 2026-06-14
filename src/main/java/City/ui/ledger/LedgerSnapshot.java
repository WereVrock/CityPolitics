// ===== ui/ledger/LedgerSnapshot.java =====
package City.ui.ledger;

import City.main.ledger.Ledger;
import City.main.ledger.Ledger.Entry;
import City.main.resources.ResourcePool;
import City.main.resources.ResourceType;

import java.util.*;

/**
 * Immutable capture of ledger state taken just before End Turn resolves.
 * Powers the "Last Season" view in LedgerPanel.
 */
public class LedgerSnapshot {

    private final Map<ResourceType, List<Entry>> recurring;
    private final Map<ResourceType, List<Entry>> oneTime;
    private final Map<ResourceType, Integer>     totalsAtCapture;
    private final Map<ResourceType, Integer>     recurringDeltas;
    private final Map<ResourceType, List<String>> warnings;

    private LedgerSnapshot(
            Map<ResourceType, List<Entry>>  recurring,
            Map<ResourceType, List<Entry>>  oneTime,
            Map<ResourceType, Integer>      totalsAtCapture,
            Map<ResourceType, Integer>      recurringDeltas,
            Map<ResourceType, List<String>> warnings) {
        this.recurring       = recurring;
        this.oneTime         = oneTime;
        this.totalsAtCapture = totalsAtCapture;
        this.recurringDeltas = recurringDeltas;
        this.warnings        = warnings;
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    public static LedgerSnapshot capture(Ledger ledger, ResourcePool resources) {
        Map<ResourceType, List<Entry>>  rec    = new EnumMap<>(ResourceType.class);
        Map<ResourceType, List<Entry>>  one    = new EnumMap<>(ResourceType.class);
        Map<ResourceType, Integer>      totals = new EnumMap<>(ResourceType.class);
        Map<ResourceType, Integer>      deltas = new EnumMap<>(ResourceType.class);
        Map<ResourceType, List<String>> warns  = new EnumMap<>(ResourceType.class);

        for (ResourceType res : ResourceType.values()) {
            List<Entry> recList = new ArrayList<>(ledger.getRecurringEntries(res));
            List<Entry> oneList = new ArrayList<>(ledger.getOneTimeEntries(res));

            int current   = getAmount(res, resources);
            int delta     = ledger.getDelta(res);
            int projected = current + delta;

            rec.put(res,    recList);
            one.put(res,    oneList);
            totals.put(res, current);
            deltas.put(res, delta);

            List<String> w = new ArrayList<>();
            if (delta < 0)      w.add(label(res) + " income was in the red.");
            if (projected <= 0) w.add(label(res) + " was near exhaustion.");
            warns.put(res, w);
        }

        return new LedgerSnapshot(rec, one, totals, deltas, warns);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public List<Entry> recurringEntries(ResourceType res) {
        return Collections.unmodifiableList(recurring.getOrDefault(res, List.of()));
    }

    public List<Entry> oneTimeEntries(ResourceType res) {
        return Collections.unmodifiableList(oneTime.getOrDefault(res, List.of()));
    }

    public int totalAtCapture(ResourceType res) {
        return totalsAtCapture.getOrDefault(res, 0);
    }

    public int recurringDelta(ResourceType res) {
        return recurringDeltas.getOrDefault(res, 0);
    }

    public List<String> warnings(ResourceType res) {
        return Collections.unmodifiableList(warnings.getOrDefault(res, List.of()));
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private static int getAmount(ResourceType res, ResourcePool pool) {
        return switch (res) {
            case GOLD      -> pool.getMoney();
            case FOOD      -> pool.getFood();
            case MANPOWER  -> pool.getManpower();
            case INFLUENCE -> pool.getInfluence();
        };
    }

    private static String label(ResourceType res) {
        return switch (res) {
            case GOLD      -> "Gold";
            case FOOD      -> "Food";
            case MANPOWER  -> "Manpower";
            case INFLUENCE -> "Influence";
        };
    }
}