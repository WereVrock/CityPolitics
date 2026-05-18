package debug;

import java.awt.*;
import javax.swing.*;

/**
 * A FlowLayout that wraps components to the next line when the container width is exceeded.
 */
public class WrapLayout extends FlowLayout {
    public WrapLayout() {
        super();
    }
    public WrapLayout(int align) {
        super(align);
    }
    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        return layoutSize(target, false);
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int maxWidth = target.getWidth() - (insets.left + insets.right);
            if (maxWidth <= 0 && preferred) {
                maxWidth = Integer.MAX_VALUE;
            }
            Dimension dim = new Dimension(0, 0);
            int x = 0;
            int y = insets.top;
            int rowHeight = 0;
            int n = target.getComponentCount();
            for (int i = 0; i < n; i++) {
                Component c = target.getComponent(i);
                if (c.isVisible()) {
                    Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                    if (x + d.width > maxWidth && x != 0) {
                        x = 0;
                        y += rowHeight + vgap;
                        rowHeight = 0;
                    }
                    x += d.width + hgap;
                    rowHeight = Math.max(rowHeight, d.height);
                    dim.width = Math.max(dim.width, x - hgap);
                    dim.height = Math.max(dim.height, y + rowHeight);
                }
            }
            dim.width += insets.left + insets.right;
            dim.height += insets.bottom;
            return dim;
        }
    }
}