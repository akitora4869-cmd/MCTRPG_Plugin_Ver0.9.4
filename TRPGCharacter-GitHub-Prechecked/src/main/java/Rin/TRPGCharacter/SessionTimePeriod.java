package Rin.TRPGCharacter;

import java.util.Locale;

public enum SessionTimePeriod {
    LATE_NIGHT("深夜", 0, 0),
    EARLY_MORNING("早朝", 5, 0),
    MORNING("朝", 7, 0),
    NOON("昼", 12, 0),
    EVENING("夕方", 17, 0),
    NIGHT("夜", 20, 0);

    private final String displayName;
    private final int hour;
    private final int minute;

    SessionTimePeriod(String displayName, int hour, int minute) {
        this.displayName = displayName;
        this.hour = hour;
        this.minute = minute;
    }

    public String displayName() {
        return displayName;
    }

    public int startMinutes() {
        return hour * 60 + minute;
    }

    public static SessionTimePeriod fromInput(String input) {
        if (input == null) return null;
        String key = input.trim().toLowerCase(Locale.ROOT);

        return switch (key) {
            case "深夜", "late_night", "latenight", "midnight" -> LATE_NIGHT;
            case "早朝", "early_morning", "earlymorning", "dawn" -> EARLY_MORNING;
            case "朝", "morning" -> MORNING;
            case "昼", "noon", "day" -> NOON;
            case "夕方", "evening", "sunset" -> EVENING;
            case "夜", "night" -> NIGHT;
            default -> null;
        };
    }

    public static SessionTimePeriod fromMinutes(int totalMinutes) {
        int m = Math.floorMod(totalMinutes, 1440);
        if (m < 5 * 60) return LATE_NIGHT;
        if (m < 7 * 60) return EARLY_MORNING;
        if (m < 12 * 60) return MORNING;
        if (m < 17 * 60) return NOON;
        if (m < 20 * 60) return EVENING;
        return NIGHT;
    }

    public static int periodEndMinutes(SessionTimePeriod period) {
        return switch (period) {
            case LATE_NIGHT -> 5 * 60;
            case EARLY_MORNING -> 7 * 60;
            case MORNING -> 12 * 60;
            case NOON -> 17 * 60;
            case EVENING -> 20 * 60;
            case NIGHT -> 24 * 60;
        };
    }
}
