package City.tools.mapeditor;

import City.main.map.ZoneDecorationRegistry;
import City.main.map.ZoneManager;
import City.main.map.WorldGeography;

import javax.swing.*;
import java.awt.*;

public class MapEditorMain {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ZoneManager zoneManager = new ZoneManager();
            WorldGeography geography = new WorldGeography();
            ZoneDecorationRegistry decorationRegistry = new ZoneDecorationRegistry();

            EditorState state = new EditorState(zoneManager, decorationRegistry, geography);
            OutputPanel outputPanel = new OutputPanel(state);
            MapEditorCanvas canvas = new MapEditorCanvas(state, geography, outputPanel);
            EditorToolbar toolbar = new EditorToolbar(state, canvas, outputPanel);

            JFrame frame = new JFrame("Zone Map Editor");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            frame.add(toolbar, BorderLayout.NORTH);
            frame.add(canvas, BorderLayout.CENTER);
            frame.add(outputPanel, BorderLayout.SOUTH);

            frame.setSize(1400, 900);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}