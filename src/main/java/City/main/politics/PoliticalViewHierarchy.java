package City.main.politics;

import java.util.List;
import java.util.Map;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Hierarchical political views with contradiction tracking.
 *
 * HIERARCHY (higher index = higher priority in conflict resolution):
 *   NONE < ENVIRONMENTALIST < DEMOCRATIC < TRADITIONALIST < MERCANTILE
 *   < MILITARIST < ISOLATIONIST < WARMONGERING < ARCANE < HUMAN_SUPREMACIST
 *
 * CONTRADICTIONS:
 *   DEMOCRATIC      ↔ HUMAN_SUPREMACIST
 *   ENVIRONMENTALIST ↔ WARMONGERING
 *   ISOLATIONIST    ↔ MERCANTILE
 *   MILITARIST      ↔ DEMOCRATIC  (partial: militarist suppresses democratic)
 */
public final class PoliticalViewHierarchy {

    private PoliticalViewHierarchy() {}

    /** Priority order — higher index wins when contradictions arise. */
    private static final List<PolitcalView> PRIORITY_ORDER = List.of(
        PolitcalView.NONE,
        PolitcalView.ENVIRONMENTALIST,
        PolitcalView.DEMOCRATIC,
        PolitcalView.TRADITIONALIST,
        PolitcalView.MERCANTILE,
        PolitcalView.ISOLATIONIST,
        PolitcalView.MILITARIST,
        PolitcalView.WARMONGERING,
        PolitcalView.ARCANE,
        PolitcalView.HUMAN_SUPREMACIST
    );

    /** Pairs of mutually exclusive views (either direction). */
    private static final List<PolitcalView[]> CONTRADICTIONS = List.of(
        new PolitcalView[]{ PolitcalView.DEMOCRATIC,       PolitcalView.HUMAN_SUPREMACIST },
        new PolitcalView[]{ PolitcalView.ENVIRONMENTALIST, PolitcalView.WARMONGERING      },
        new PolitcalView[]{ PolitcalView.ISOLATIONIST,     PolitcalView.MERCANTILE        },
        new PolitcalView[]{ PolitcalView.MILITARIST,       PolitcalView.DEMOCRATIC        }
    );

    public static int priority(PolitcalView view) {
        int idx = PRIORITY_ORDER.indexOf(view);
        return idx < 0 ? 0 : idx;
    }

    public static boolean areContradictory(PolitcalView a, PolitcalView b) {
        for (PolitcalView[] pair : CONTRADICTIONS) {
            if ((pair[0] == a && pair[1] == b) || (pair[0] == b && pair[1] == a)) return true;
        }
        return false;
    }

    /**
     * Returns the view that should be removed when two contradictory views are held.
     * The lower-priority view is removed.
     */
    public static PolitcalView resolveLowerPriority(PolitcalView a, PolitcalView b) {
        return priority(a) <= priority(b) ? a : b;
    }

    /**
     * Filter a set of views to remove contradictions, keeping highest priority.
     */
    public static List<PolitcalView> resolveContradictions(List<PolitcalView> views) {
        List<PolitcalView> result = new ArrayList<>(views);
        boolean changed = true;
        while (changed) {
            changed = false;
            outer:
            for (int i = 0; i < result.size(); i++) {
                for (int j = i + 1; j < result.size(); j++) {
                    if (areContradictory(result.get(i), result.get(j))) {
                        PolitcalView lower = resolveLowerPriority(result.get(i), result.get(j));
                        result.remove(lower);
                        changed = true;
                        break outer;
                    }
                }
            }
        }
        return result;
    }
}