package debug;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public record LogEntry(LocalTime timestamp, String category, String type, String message) {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public String formattedTimestamp() {
        return timestamp.format(TIME_FORMATTER);
    }
}