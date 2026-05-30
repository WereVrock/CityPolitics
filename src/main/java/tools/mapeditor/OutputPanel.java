package tools.mapeditor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class OutputPanel extends JPanel {

    private final EditorState state;
    private final JTextArea   textArea;

    public OutputPanel(EditorState state) {
        this.state = state;
        setLayout(new BorderLayout());
        setBackground(new Color(20, 18, 16));
        setPreferredSize(new Dimension(0, 200));
        setBorder(new EmptyBorder(4, 4, 4, 4));

        JLabel header = new JLabel("  Output — copy this and send to AI");
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        header.setForeground(new Color(180, 160, 100));
        header.setBackground(new Color(30, 25, 20));
        header.setOpaque(true);
        header.setBorder(new EmptyBorder(2, 4, 2, 4));

        textArea = new JTextArea();
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        textArea.setForeground(new Color(200, 210, 190));
        textArea.setBackground(new Color(20, 18, 16));
        textArea.setEditable(false);
        textArea.setLineWrap(false);

        JButton copyBtn = new JButton("Copy All");
        copyBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        copyBtn.setForeground(new Color(200, 210, 180));
        copyBtn.setBackground(new Color(50, 45, 40));
        copyBtn.setBorderPainted(false);
        copyBtn.setFocusPainted(false);
        copyBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        copyBtn.addActionListener(e -> {
            textArea.selectAll();
            textArea.copy();
            textArea.select(0, 0);
            copyBtn.setText("Copied!");
            Timer t = new Timer(1500, ev -> copyBtn.setText("Copy All"));
            t.setRepeats(false);
            t.start();
        });

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(30, 25, 20));
        topBar.add(header, BorderLayout.WEST);
        topBar.add(copyBtn, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
        add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    public void generate(boolean changedOnly) {
        List<EditableZone> zones = state.getZones();
        StringBuilder sb = new StringBuilder();

        for (EditableZone ez : zones) {
            if (changedOnly && !state.isChanged(ez.getId())) continue;

            sb.append("ZONE: ").append(ez.getId()).append("\n");
            sb.append("  name:    ").append(ez.getDisplayName()).append("\n");
            sb.append("  type:    ").append(ez.getSettlementType().name()).append("\n");
            sb.append("  polyX:   ").append(intArrayToString(ez.getPolyX())).append("\n");
            sb.append("  polyY:   ").append(intArrayToString(ez.getPolyY())).append("\n");
            sb.append("  labelX:  ").append(ez.getLabelX()).append("\n");
            sb.append("  labelY:  ").append(ez.getLabelY()).append("\n");
            sb.append("\n");
        }

        if (sb.length() == 0) {
            sb.append("No changes recorded yet.");
        }

        textArea.setText(sb.toString());
        textArea.setCaretPosition(0);
    }

    private String intArrayToString(int[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(arr[i]);
        }
        return sb.toString();
    }
}