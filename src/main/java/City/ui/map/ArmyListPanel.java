// ArmyListPanel.java
package City.ui.map;

import City.main.army.Army;
import City.main.army.ArmyManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import City.ui.UITheme;

/**
 * Displays armies currently in the city (heartland).
 * Drag a card onto the map to deploy. Right-click deployed army recalls it here.
 * Being in this list and being in heartland are the same state.
 */
public class ArmyListPanel extends JPanel {

    public interface DragDropCallback {
        void onDrop(Army army, String zoneId);
        void onDragCancelled(Army army);
    }

    static final DataFlavor ARMY_FLAVOR = new DataFlavor(Army.class, "Army");

    private       ArmyManager      armyManager;
    private       DragDropCallback callback;
    private final JPanel           listContainer;

    public ArmyListPanel(ArmyManager armyManager) {
        this.armyManager = armyManager;

        setBackground(UITheme.BG_PANEL);
        setPreferredSize(new Dimension(200, 160));
        setBorder(new MatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR));
        setLayout(new BorderLayout());

        JLabel header = new JLabel("  ARMIES IN CITY");
        header.setFont(UITheme.FONT_SMALL);
        header.setForeground(UITheme.TEXT_GOLD);
        header.setBorder(new EmptyBorder(4, 4, 4, 4));
        header.setBackground(UITheme.BG_PANEL_LIGHT);
        header.setOpaque(true);

        listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setBackground(UITheme.BG_PANEL);

        JScrollPane scroll = new JScrollPane(listContainer);
        scroll.setBorder(null);
        scroll.setBackground(UITheme.BG_PANEL);
        scroll.getViewport().setBackground(UITheme.BG_PANEL);
        scroll.getVerticalScrollBar().setUnitIncrement(28);

        add(header, BorderLayout.NORTH);
        add(scroll,  BorderLayout.CENTER);

        refresh();
    }

    public void setOnDragDropCallback(DragDropCallback cb) { this.callback = cb; }
    public DragDropCallback getCallback()                   { return callback; }

public void refresh() {
        listContainer.removeAll();
        // Player armies in city
        for (Army army : armyManager.getCityArmies()) {
            listContainer.add(buildArmyCard(army));
            listContainer.add(Box.createVerticalStrut(3));
        }
        if (armyManager.getCityArmies().isEmpty()) {
            JLabel empty = new JLabel("  No armies in city");
            empty.setFont(UITheme.FONT_MAP_SMALL);
            empty.setForeground(UITheme.TEXT_SECONDARY);
            listContainer.add(empty);
        }
        // Re-apply font to header
        Component north = ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.NORTH);
        if (north instanceof JLabel header) {
            header.setFont(UITheme.FONT_MAP_SMALL);
        }
        listContainer.revalidate();
        listContainer.repaint();
    }

private JPanel buildArmyCard(Army army) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UITheme.BG_PANEL_LIGHT);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            new EmptyBorder(4, 8, 4, 8)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        card.setAlignmentX(LEFT_ALIGNMENT);

        JLabel name = new JLabel("⚔ " + army.getDisplayName());
        name.setFont(UITheme.FONT_MAP_BUTTON);
        name.setForeground(UITheme.ACCENT_FROST);

        card.add(name, BorderLayout.WEST);

        DragSource ds = DragSource.getDefaultDragSource();
        ds.createDefaultDragGestureRecognizer(card, DnDConstants.ACTION_MOVE, dge -> {
            army.startDrag();
            refresh();
            repaint();

            Transferable t = new Transferable() {
                @Override public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{ARMY_FLAVOR}; }
                @Override public boolean isDataFlavorSupported(DataFlavor f) { return f.equals(ARMY_FLAVOR); }
                @Override public Object getTransferData(DataFlavor f) { return army; }
            };

            DragSourceAdapter dsa = new DragSourceAdapter() {
                @Override
                public void dragDropEnd(DragSourceDropEvent dsde) {
                    if (!dsde.getDropSuccess()) {
                        army.cancelDrag();
                        refresh();
                        repaint();
                        if (callback != null) callback.onDragCancelled(army);
                    }
                }
            };
            dge.startDrag(DragSource.DefaultMoveDrop, t, dsa);
        });

        return card;
    }

/**
     * Re-wires the ArmyManager reference after gameState.reset() (new game).
     */

public void reinitialize(ArmyManager newArmyManager) {
        this.armyManager = newArmyManager;
        refresh();
    }

/**
     * Re-applies UITheme map-panel fonts to this panel.
     * Called from MapView after the user changes the map-panel font size in Settings.
     */
    public void applyMapPanelFonts() {
        // The header label and army cards are rebuilt on refresh(), so just refresh.
        refresh();
    }

}