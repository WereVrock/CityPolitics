package main.nobles.ai;

import main.nobles.NobleHouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Package-private utilities shared across all NobleAI classes. */
final class NobleAIUtils {

    static final Random RNG = new Random();

    private NobleAIUtils() {}

    static List<String> allHouseIds(List<NobleHouse> houses) {
        List<String> ids = new ArrayList<>();
        for (NobleHouse h : houses) ids.add(h.getId());
        return ids;
    }

    static NobleHouse findById(String id, List<NobleHouse> houses) {
        for (NobleHouse h : houses) if (h.getId().equals(id)) return h;
        return null;
    }
}