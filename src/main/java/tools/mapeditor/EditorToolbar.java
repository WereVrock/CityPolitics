package tools.mapeditor;

import javax.swing.*;
import java.awt.*;

public class EditorToolbar extends JPanel {

    private final EditorState   state;
    private final MapEditorCanvas canvas;
    private final OutputPanel   outputPanel;

    private boolean outputChangedOnly = false;

    public EditorToolbar(EditorState state, MapEditorCanvas canvas,
                         OutputPanel outputPanel) {
        this.state       = state;
        this.canvas      = canvas;
        this.outputPanel = outputPanel;

        setBackground(new Color(30, 25, 20));
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));

        JLabel title = new JLabel("Zone Map Editor");
        title.setForeground(new Color(220, 190, 100));
        title.setFont(new Font("Serif", Font.BOLD, 14));
        add(title);

        addSeparator();

        JButton generateBtn = makeButton("Generate Output");
        generateBtn.addActionListener(e -> outputPanel.generate(outputChangedOnly));
        add(generateBtn);

        JToggleButton toggleChanged = new JToggleButton("Changed Only");
        toggleChanged.setFont(new Font("SansSerif", Font.PLAIN, 11));
        toggleChanged.setForeground(new Color(200, 200, 200));
        toggleChanged.setBackground(new Color(55, 50, 45));
        toggleChanged.setFocusPainted(false);
        toggleChanged.addActionListener(e -> outputChangedOnly = toggleChanged.isSelected());
        add(toggleChanged);

        addSeparator();

        JToggleButton snapBtn = new JToggleButton("⦿ Snap");
        snapBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        snapBtn.setForeground(new Color(200, 200, 200));
        snapBtn.setBackground(new Color(55, 50, 45));
        snapBtn.setFocusPainted(false);
        snapBtn.setToolTipText("Snap dragged vertices to nearby vertices of other zones");
        snapBtn.addActionListener(e -> state.setSnappingEnabled(snapBtn.isSelected()));
        add(snapBtn);

        addSeparator();

        JButton resetViewBtn = makeButton("Reset View");
        resetViewBtn.addActionListener(e -> canvas.resetView());
        add(resetViewBtn);

        JButton clearSelBtn = makeButton("Deselect");
        clearSelBtn.addActionListener(e -> {
            state.clearSelection();
            canvas.repaint();
        });
        add(clearSelBtn);

        JButton undoBtn = makeButton("Undo");
        undoBtn.addActionListener(e -> {
            if (state.canUndo()) {
                state.undo();
                canvas.repaint();
            }
        });
        add(undoBtn);

        addSeparator();

        JLabel hint = new JLabel(
            "Click zone/river/sea to select  |  Drag vertex  |  Double‑click edge to add vertex  |  Right‑click vertex to delete  |  Drag name dot to move label");
        hint.setForeground(new Color(150, 145, 130));
        hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        add(hint);
    }

    private JButton makeButton(String label) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btn.setForeground(new Color(220, 210, 180));
        btn.setBackground(new Color(55, 50, 45));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void addSeparator() {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 24));
        sep.setForeground(new Color(80, 75, 65));
        add(sep);
    }
}