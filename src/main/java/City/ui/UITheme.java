package City.ui;

import java.awt.Color;
import java.awt.Font;

/**
 * Shared visual constants for the FrostVeil UI.
 */
public class UITheme {

    // ─── Colors ───────────────────────────────────────────────────────────────
    public static final Color BG_DARK        = new Color(18, 14, 22);
    public static final Color BG_PANEL       = new Color(28, 22, 38);
    public static final Color BG_PANEL_LIGHT = new Color(38, 30, 52);
    public static final Color BORDER_COLOR   = new Color(80, 55, 110);
    public static final Color TEXT_PRIMARY   = new Color(220, 210, 240);
    public static final Color TEXT_SECONDARY = new Color(150, 130, 180);
    public static final Color TEXT_GOLD      = new Color(210, 170, 80);
    public static final Color TEXT_RED       = new Color(200, 70, 70);
    public static final Color TEXT_GREEN     = new Color(80, 190, 110);
    public static final Color ACCENT_FROST   = new Color(100, 180, 220);
    public static final Color BUTTON_BG      = new Color(55, 40, 80);
    public static final Color BUTTON_HOVER   = new Color(75, 55, 105);
    public static final Color BUTTON_DISABLED= new Color(35, 30, 45);
    public static final Color LOG_BG         = new Color(12, 10, 18);

    // ─── Font base size (mutable for settings) ────────────────────────────────
    public static int BASE_SIZE = 12;

    // ─── Fonts ────────────────────────────────────────────────────────────────
    public static Font FONT_TITLE   = new Font("Serif",      Font.BOLD,  BASE_SIZE + 8);
    public static Font FONT_HEADER  = new Font("Serif",      Font.BOLD,  BASE_SIZE + 2);
    public static Font FONT_BODY    = new Font("Monospaced", Font.PLAIN, BASE_SIZE);
    public static Font FONT_SMALL   = new Font("Monospaced", Font.PLAIN, BASE_SIZE - 1);
    public static Font FONT_BUTTON  = new Font("Serif",      Font.BOLD,  BASE_SIZE);

    // ─── Map panel font size (mutable for settings) ───────────────────────────
    public static int  MAP_PANEL_SIZE  = 12;

    // ─── Map panel fonts ──────────────────────────────────────────────────────
    public static Font FONT_MAP_BODY   = new Font("Monospaced", Font.PLAIN, MAP_PANEL_SIZE);
    public static Font FONT_MAP_SMALL  = new Font("Monospaced", Font.PLAIN, MAP_PANEL_SIZE - 1);
    public static Font FONT_MAP_HEADER = new Font("Serif",      Font.BOLD,  MAP_PANEL_SIZE + 2);
    public static Font FONT_MAP_BUTTON = new Font("Serif",      Font.BOLD,  MAP_PANEL_SIZE);

    /** Apply a new base font size across all theme fonts. */

/** Apply a new base font size across all theme fonts. */
    public static void applyFontScale(int baseSize) {
        BASE_SIZE  = baseSize;
        FONT_TITLE  = new Font("Serif",      Font.BOLD,  BASE_SIZE + 8);
        FONT_HEADER = new Font("Serif",      Font.BOLD,  BASE_SIZE + 2);
        FONT_BODY   = new Font("Monospaced", Font.PLAIN, BASE_SIZE);
        FONT_SMALL  = new Font("Monospaced", Font.PLAIN, BASE_SIZE - 1);
        FONT_BUTTON = new Font("Serif",      Font.BOLD,  BASE_SIZE);
    }

    /** Apply a new map-panel font size across all map panel fonts. */
    public static void applyMapPanelFontScale(int size) {
        MAP_PANEL_SIZE   = size;
        FONT_MAP_BODY    = new Font("Monospaced", Font.PLAIN, MAP_PANEL_SIZE);
        FONT_MAP_SMALL   = new Font("Monospaced", Font.PLAIN, MAP_PANEL_SIZE - 1);
        FONT_MAP_HEADER  = new Font("Serif",      Font.BOLD,  MAP_PANEL_SIZE + 2);
        FONT_MAP_BUTTON  = new Font("Serif",      Font.BOLD,  MAP_PANEL_SIZE);
    }

private UITheme() {}
}