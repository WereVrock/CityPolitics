package ui;

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

    // ─── Fonts ────────────────────────────────────────────────────────────────
    public static final Font FONT_TITLE   = new Font("Serif",      Font.BOLD,  20);
    public static final Font FONT_HEADER  = new Font("Serif",      Font.BOLD,  14);
    public static final Font FONT_BODY    = new Font("Monospaced", Font.PLAIN, 12);
    public static final Font FONT_SMALL   = new Font("Monospaced", Font.PLAIN, 11);
    public static final Font FONT_BUTTON  = new Font("Serif",      Font.BOLD,  12);

    private UITheme() {}
}