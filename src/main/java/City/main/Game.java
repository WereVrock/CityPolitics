package City.main;

import City.debug.Debug;

public class Game {
    public static void main(String[] args) {
        // Optional configuration
        Debug.setBatchFlushDelay(50);   // faster flush if needed
        Debug.setConsoleOutputEnabled(false); // quiet console

        // Simulate end turn with many logs
        for (int turn = 0; turn < 3; turn++) {
            for (int i = 0; i < 50; i++) {
                Debug.log("Turn" + turn, "Action", "Performed action #" + i);
            }
            // GUI auto-opens on first log; all 50 logs appear in one batch ~50ms later
        }

        // Manual control (optional)
        // Debug.startDebugWindow();
        // Debug.closeDebugWindow();
    }
}