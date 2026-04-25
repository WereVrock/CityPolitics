package main.effects;

import main.resources.StatBlock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages all active decaying effects.
 * Called each turn by TurnProcessor.
 */
public class EffectManager {

    private final List<ActiveEffect> activeEffects = new ArrayList<>();

    public void addEffect(ActiveEffect effect) {
        activeEffects.add(effect);
    }

    /**
     * Applies all active effects for this turn.
     * Returns log lines describing what happened.
     */
    public List<String> processTurn(StatBlock stats) {
        List<String> log      = new ArrayList<>();
        Iterator<ActiveEffect> it = activeEffects.iterator();

        while (it.hasNext()) {
            ActiveEffect effect = it.next();
            double delta = effect.decay();
            applyEffect(effect.getType(), delta, stats);
            log.add(formatLog(effect, delta));
            if (effect.isExpired()) it.remove();
        }

        return log;
    }

    private void applyEffect(ActiveEffect.Type type, double delta, StatBlock stats) {
        switch (type) {
            case HAPPINESS_BOOST -> stats.addHappiness((int) Math.round(delta));
        }
    }

    private String formatLog(ActiveEffect effect, double delta) {
        return String.format("Festival mood fading: Happiness +%d this turn (%d turns remaining).",
            (int) Math.round(delta), effect.getTurnsRemaining());
    }

    public List<ActiveEffect> getActiveEffects() {
        return new ArrayList<>(activeEffects);
    }

    public void reset() {
        activeEffects.clear();
    }
}