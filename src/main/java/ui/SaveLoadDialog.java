package ui;

import main.core.GameState;
import main.save.SaveManager;

import javax.swing.*;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Handles save, load, and new game interactions.
 * No file choosers — save location is managed entirely by SaveManager.
 */
public class SaveLoadDialog {

    private final JFrame           owner;
    private final GameState        gameState;
    private final Consumer<String> onMessage;

    public SaveLoadDialog(JFrame owner, GameState gameState, Consumer<String> onMessage) {
        this.owner      = owner;
        this.gameState  = gameState;
        this.onMessage  = onMessage;
    }

    // ─── Save ────────────────────────────────────────────────────────────────

    public void save() {
        try {
            SaveManager.save(gameState);
            onMessage.accept("Game saved.");
        } catch (IOException e) {
            showError("Save failed: " + e.getMessage());
        }
    }

    // ─── Load ────────────────────────────────────────────────────────────────

    public void load(Runnable onSuccess) {
        if (!SaveManager.saveExists()) {
            onMessage.accept("No save file found.");
            return;
        }
        try {
            SaveManager.load(gameState);
            onSuccess.run();
            onMessage.accept("Game loaded.");
        } catch (IOException e) {
            showError("Load failed: " + e.getMessage());
        }
    }

    // ─── New Game ─────────────────────────────────────────────────────────────

    public void newGame(Runnable onSuccess) {
        int choice = JOptionPane.showConfirmDialog(
            owner,
            "Start a new game? All unsaved progress will be lost.",
            "New Game",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (choice != JOptionPane.YES_OPTION) return;

        gameState.reset();
        onSuccess.run();
    }

    // ─── Utility ─────────────────────────────────────────────────────────────

    private void showError(String message) {
        JOptionPane.showMessageDialog(owner, message, "Error", JOptionPane.ERROR_MESSAGE);
        onMessage.accept("✗ " + message);
    }
}