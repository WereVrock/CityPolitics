package main;

import main.core.GameState;
import ui.MainWindow;

import javax.swing.*;

/**
 * Entry point for FrostVeil.
 */
public class Main {

    public static void main(String[] args) {
         System.setProperty("sun.java2d.uiScale", "1");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {}

            GameState   gameState = new GameState();
            MainWindow  window    = new MainWindow(gameState);
            window.setVisible(true);
        });
    }
}