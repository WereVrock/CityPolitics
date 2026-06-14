package City.debug;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

record LogEntry(LocalTime timestamp, String category, String type, String message) {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    String formattedTimestamp() {
        return timestamp.format(TIME_FORMATTER);
    }
}