package com.example.study_buddy;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Robust utility helper for parsing class durations, normalizing formats,
 * and detecting time overlaps/conflicts in academic schedules.
 */
public class TimeSlotHelper {

    // Regex to parse a single time element like "08:30", "8.30", "8:30 AM", "8AM", "14:00", etc.
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "^\\s*(\\d{1,2})(?:[:.](\\d{1,2}))?\\s*(AM|PM)?\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Parses a single time string into minutes from midnight (0 to 1439).
     * Automatically handles 24h, 12h with AM/PM, and applies smart daytime heuristics
     * for academic timetables (hours 1-6 without AM/PM default to afternoon PM).
     *
     * Returns -1 if the time is invalid.
     */
    public static int parseTimeToMinutes(String timeStr) {
        if (timeStr == null) return -1;
        String clean = timeStr.trim();

        Matcher m = TIME_PATTERN.matcher(clean);
        if (!m.matches()) {
            return -1;
        }

        int hour;
        int minute = 0;
        try {
            hour = Integer.parseInt(m.group(1));
            if (m.group(2) != null) {
                minute = Integer.parseInt(m.group(2));
            }
        } catch (NumberFormatException e) {
            return -1;
        }

        if (minute < 0 || minute > 59) {
            return -1;
        }

        String ampm = m.group(3) != null ? m.group(3).toUpperCase() : null;

        if (ampm != null) {
            if (hour < 1 || hour > 12) return -1;
            if (ampm.equals("AM")) {
                if (hour == 12) hour = 0;
            } else if (ampm.equals("PM")) {
                if (hour != 12) hour += 12;
            }
        } else {
            // No AM/PM specified
            if (hour < 0 || hour > 23) return -1;

            // Smart daytime timetable heuristic:
            // Classes rarely happen at 1:00 AM - 6:00 AM.
            // If hour is 1..6, infer PM (e.g. 1:30 -> 13:30, 2:00 -> 14:00).
            if (hour >= 1 && hour <= 6) {
                hour += 12;
            }
        }

        return hour * 60 + minute;
    }

    /**
     * Normalizes a duration string by replacing dashes, dots, and words.
     * E.g. "08.30 – 09.50" -> "08:30 - 09:50".
     */
    public static String normalizeDurationString(String slotStr) {
        if (slotStr == null) return "";
        return slotStr.trim()
                .replace("–", "-")      // en-dash
                .replace("—", "-")      // em-dash
                .replaceAll("(?i)\\s+to\\s+", "-"); // "8:30 to 10:00"
    }

    /**
     * Parses a slot duration into an int array [startMinutes, endMinutes].
     * Returns null if syntax is invalid or if end <= start.
     */
    public static int[] parseSlotRange(String slotStr) {
        if (slotStr == null) return null;
        String normalized = normalizeDurationString(slotStr);

        if (!normalized.contains("-")) {
            return null;
        }

        String[] parts = normalized.split("-");
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
     * Fully validates a new or edited time slot against format rules and existing slots.
     *
     * @param newSlotStr    the candidate duration string (e.g. "08:30 - 09:50")
     * @param existingSlots current list of time slots
     * @param ignoredSlot   slot name to ignore when editing an existing slot, or null
     * @return null if completely valid, or a user-friendly error message if invalid.
     */
    public static String validateSlot(String newSlotStr, List<String> existingSlots, String ignoredSlot) {
        if (newSlotStr == null || newSlotStr.trim().isEmpty()) {
            return "Duration cannot be empty.";
        }

        String normalized = normalizeDurationString(newSlotStr);
        if (!normalized.contains("-")) {
            return "Please use format 'Start - End' (e.g. 08:30 - 09:50 or 01:30 PM - 02:50 PM).";
        }

        String[] parts = normalized.split("-");
        if (parts.length != 2) {
            return "Invalid duration format. Must have a start time and an end time separated by '-'.";
        }

        int startMin = parseTimeToMinutes(parts[0]);
        int endMin = parseTimeToMinutes(parts[1]);

        if (startMin < 0) {
            return "Invalid start time: \"" + parts[0].trim() + "\". Example valid times: 08:30, 9:00 AM, 14:00.";
        }
        if (endMin < 0) {
            return "Invalid end time: \"" + parts[1].trim() + "\". Example valid times: 09:50, 11:30 AM, 15:30.";
        }
        if (endMin <= startMin) {
            return "Invalid time range: End time must be after start time.";
        }

        // Check for overlap against all existing slots
        if (existingSlots != null) {
            for (String existing : existingSlots) {
                if (ignoredSlot != null && existing.trim().equalsIgnoreCase(ignoredSlot.trim())) {
                    continue;
                }

                int[] exRange = parseSlotRange(existing);
                if (exRange != null) {
                    int exStart = exRange[0];
                    int exEnd = exRange[1];

                    // Interval overlap: max(start1, start2) < min(end1, end2)
                    if (Math.max(startMin, exStart) < Math.min(endMin, exEnd)) {
                        return "Duration Conflict! This overlaps with existing class slot: \"" + existing.trim() + "\".";
                    }
                }
            }
        }

        return null; // Valid and conflict-free!
    }

    /**
     * Backward-compatible conflict check method.
     * Returns the conflicting slot string if an overlap exists, or null.
     */
    public static String findConflict(String newSlotStr, List<String> existingSlots, String ignoredSlot) {
        String validation = validateSlot(newSlotStr, existingSlots, ignoredSlot);
        if (validation != null && validation.contains("overlaps with existing class slot:")) {
            int idx = validation.indexOf('"');
            int lastIdx = validation.lastIndexOf('"');
            if (idx != -1 && lastIdx > idx) {
                return validation.substring(idx + 1, lastIdx);
            }
            return validation;
        }
        return null;
    }
}
