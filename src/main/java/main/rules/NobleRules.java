package main.rules;

/**
 * Hard rules that noble houses cannot disobey, regardless of AI decisions.
 * These act as global constraints enforced before any AI action is executed.
 */
public final class NobleRules {

    private NobleRules() {}

    // ── War ──────────────────────────────────────────────────────────────────

    /** If false, no noble house may declare war or issue ATTACK orders. */
    public static final boolean WAR_DECLARATION_ALLOWED = false;
}