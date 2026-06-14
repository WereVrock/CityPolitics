package City.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Walks the entire Swing component tree and re-applies UITheme fonts
 * to every component after a font scale change.
 */
public final class FontPropagator {

    private FontPropagator() {}

    public static void applyToWindow(Window window) {
        applyToComponent(window);
        window.revalidate();
        window.repaint();
    }

    private static void applyToComponent(Component c) {
        Font mapped = mapFont(c.getFont());
        if (mapped != null) c.setFont(mapped);

        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyToComponent(child);
            }
        }
    }

    /**
     * Maps a component's current font to the corresponding UITheme font
     * by matching family and style, then returns the new scaled version.
     * Returns null if the font cannot be matched (leave it alone).
     */
    private static Font mapFont(Font f) {
        if (f == null) return null;
        String family = f.getFamily();
        int    style  = f.getStyle();
        int    size   = f.getSize();

        // Match by family + style to the closest UITheme font size
        if (family.startsWith("Serif") || family.equals("Serif")) {
            if (style == Font.BOLD) {
                // Could be TITLE, HEADER, or BUTTON — match by approximate old size
                if (size >= UITheme.BASE_SIZE + 6)  return UITheme.FONT_TITLE;
                if (size >= UITheme.BASE_SIZE)       return UITheme.FONT_HEADER;
                return UITheme.FONT_BUTTON;
            }
            // Italic serif = keep relative size
            return new Font(family, style, UITheme.BASE_SIZE);
        }
        if (family.startsWith("Monospaced") || family.equals("Monospaced")) {
            if (size <= UITheme.BASE_SIZE - 1) return UITheme.FONT_SMALL;
            return UITheme.FONT_BODY;
        }
        if (family.equals("Dialog") && style == Font.BOLD) {
            // Step buttons
            return new Font("Dialog", Font.BOLD, UITheme.BASE_SIZE + 8);
        }
        return null;
    }
}