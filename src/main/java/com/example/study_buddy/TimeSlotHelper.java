package com.example.study_buddy;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * Utility helper for parsing class durations and detecting time conflicts / overlaps.
 */
public class TimeSlotHelper {

    private static final DateTimeFormatter[] TIME_FORMATTERS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("H:mm", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("hh:mma", Locale.ENGLISH)
    };

    /**
     * Parses a single time string (e.g., "08:30", "8:30 AM", "1:30 PM") into minutes from midnight.
     * Returns -1 if parsing fails.
     */
    public static int parseTimeToMinutes(String timeStr) {
        if (timeStr == null) return -1;
        String trimmed = timeStr.trim().toUpperCase();

        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                LocalTime time = LocalTime.parse(trimmed, formatter);
                return time.getHour() * 60 + time.getMinute();
            } catch (DateTimeParseException ignored) {
            }
        }
        return -1;
    }

    /**
     * Parses a slot duration (e.g. "08:30 - 09:50" or "01:30 PM - 02:50 PM") into an int array: [startMinutes, endMinutes].
     * Returns null if the slot cannot be parsed or if end <= start.
     */
    public static int[] parseSlotRange(String slotStr) {
        if (slotStr == null || !slotStr.contains("-")) {
            return null;
        }

        String[] parts = slotStr.split("-");
        if (parts.length != 2) {
            return null;
        }

        int startMin = parseTimeToMinutes(parts[0]);
        int endMin = parseTimeToMinutes(parts[1]);

        if (startMin < 0 || endMin < 0 || endMin <= startMin) {
            return null;
        }

        return new int[]{startMin, endMin};
    }

    /**
     * Checks if a new time slot conflicts/overlaps with any existing time slot.
     * Returns the conflicting slot string if an overlap is found, or null if no conflict.
     *
     * @param newSlotStr     the new or updated time slot to test
     * @param existingSlots  list of all existing time slots
     * @param ignoredSlot    an existing slot name to ignore (useful when editing an existing slot), or null
     */
    public static String findConflict(String newSlotStr, List<String> existingSlots, String ignoredSlot) {
        int[] newRange = parseSlotRange(newSlotStr);
        if (newRange == null) {
            return null; // Let format validation handle syntax errors
        }

        int newStart = newRange[0];
        int newEnd = newRange[1];

        for (String existing : existingSlots) {
            if (ignoredSlot != null && existing.trim().equalsIgnoreCase(ignoredSlot.trim())) {
                continue;
            }

            int[] existingRange = parseSlotRange(existing);
            if (existingRange != null) {
                int exStart = existingRange[0];
                int exEnd = existingRange[1];

                // Overlap condition: max(start1, start2) < min(end1, end2)
                if (Math.max(newStart, exStart) < Math.min(newEnd, exEnd)) {
                    return existing; // Conflict found!
                }
            }
        }

        return null; // No conflict
    }
}
