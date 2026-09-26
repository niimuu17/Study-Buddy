package com.example.study_buddy;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Represents an academic activity/task derived from a routine slot
 * with deadline parsing and live ticking countdown calculations.
 */
public class RoutineTaskItem implements Comparable<RoutineTaskItem> {

    public enum Urgency {
        NORMAL,  // > 24 hours left
        URGENT,  // <= 24 hours left
        OVERDUE  // Past deadline
    }

    private final int activityId;
    private final int slotId;
    private final String subjectName;
    private final String teacherCode;
    private final String weekday;
    private final String timeSlot;
    private final String activityType;
    private final String rawDeadline;
    private final String notes;
    private final LocalDateTime targetDateTime;

    public RoutineTaskItem(int activityId, int slotId, String subjectName, String teacherCode, String weekday,
                           String timeSlot, String activityType, String rawDeadline, String notes) {
        this.activityId = activityId;
        this.slotId = slotId;
        this.subjectName = (subjectName != null && !subjectName.isEmpty()) ? subjectName : "General";
        this.teacherCode = (teacherCode != null) ? teacherCode : "";
        this.weekday = (weekday != null) ? weekday : "";
        this.timeSlot = (timeSlot != null) ? timeSlot : "";
        this.activityType = (activityType != null && !activityType.isEmpty()) ? activityType : "Task";
        this.rawDeadline = (rawDeadline != null) ? rawDeadline.trim() : "";
        this.notes = (notes != null) ? notes.trim() : "";
        this.targetDateTime = parseDeadline(this.rawDeadline);
    }

    public RoutineTaskItem(int activityId, int slotId, String subjectName, String teacherCode, String weekday,
                           String timeSlot, String activityType, String rawDeadline) {
        this(activityId, slotId, subjectName, teacherCode, weekday, timeSlot, activityType, rawDeadline, "");
    }

    public RoutineTaskItem(int slotId, String subjectName, String teacherCode, String weekday,
                           String timeSlot, String activityType, String rawDeadline) {
        this(0, slotId, subjectName, teacherCode, weekday, timeSlot, activityType, rawDeadline, "");
    }

    public String getNotes() {
        return notes != null ? notes : "";
    }

    public LocalDate getDeadlineDate() {
        return targetDateTime != null ? targetDateTime.toLocalDate() : null;
    }

    public LocalTime getDeadlineTime() {
        return targetDateTime != null ? targetDateTime.toLocalTime() : null;
    }

    public String getFormattedDate() {
        if (targetDateTime == null) return rawDeadline;
        return targetDateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH));
    }

    public String getFormattedTime() {
        if (targetDateTime == null) return timeSlot;
        return targetDateTime.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));
    }

    public String getWeekdayDisplay() {
        if (targetDateTime != null) {
            return targetDateTime.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH);
        }
        return weekday != null ? weekday : "";
    }

    public int getActivityId() {
        return activityId;
    }

    public int getSlotId() {
        return slotId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getTeacherCode() {
        return teacherCode;
    }

    public String getWeekday() {
        return weekday;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public String getActivityType() {
        return activityType;
    }

    public String getParsedCategory() {
        if (activityType != null && activityType.contains(" - ")) {
            return activityType.substring(0, activityType.indexOf(" - ")).trim();
        }
        return activityType != null ? activityType : "Task";
    }

    public String getParsedTitle() {
        if (activityType != null && activityType.contains(" - ")) {
            return activityType.substring(activityType.indexOf(" - ") + 3).trim();
        }
        return activityType != null ? activityType : "";
    }

    public String getRawDeadline() {
        return rawDeadline;
    }

    public LocalDateTime getTargetDateTime() {
        return targetDateTime;
    }

    /**
     * Parses deadlines like "2026-09-30 (11:59 PM)", "2026-09-30", or "2026-09-30 (14:00)".
     */
    public static LocalDateTime parseDeadline(String deadlineStr) {
        if (deadlineStr == null || deadlineStr.trim().isEmpty()) {
            return LocalDateTime.now().plusDays(7).withHour(23).withMinute(59).withSecond(59);
        }

        String trimmed = deadlineStr.trim();
        LocalDate datePart = null;
        LocalTime timePart = null;

        // Extract date (first 10 chars if YYYY-MM-DD)
        if (trimmed.length() >= 10) {
            String candidateDate = trimmed.substring(0, 10);
            try {
                datePart = LocalDate.parse(candidateDate);
            } catch (DateTimeParseException ignored) {}
        }

        // Extract time inside parentheses "(...)"
        int openParen = trimmed.indexOf('(');
        int closeParen = trimmed.indexOf(')');
        if (openParen != -1 && closeParen > openParen) {
            String timeStr = trimmed.substring(openParen + 1, closeParen).trim();
            timePart = tryParseTime(timeStr);
        }

        if (datePart == null) {
            try {
                datePart = LocalDate.parse(trimmed);
            } catch (Exception e) {
                datePart = LocalDate.now().plusDays(3);
            }
        }

        if (timePart == null) {
            timePart = LocalTime.of(23, 59, 59); // Default to end of day
        }

        return LocalDateTime.of(datePart, timePart);
    }

    private static LocalTime tryParseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return null;

        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));
        } catch (DateTimeParseException ignored) {}

        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH));
        } catch (DateTimeParseException ignored) {}

        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException ignored) {}

        return null;
    }

    public String getFormattedTarget() {
        if (targetDateTime == null) return rawDeadline;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a", Locale.ENGLISH);
        return targetDateTime.format(dtf);
    }

    /**
     * Formats the live countdown string according to user's specification:
     * e.g., "3 days 5 hrs 3 mins 32 seconds" or "⚠️ Overdue by 1 hr 15 mins".
     */
    public String getFormattedCountdown() {
        if (targetDateTime == null) return "No deadline";

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(now, targetDateTime);
        long totalSeconds = duration.getSeconds();

        if (totalSeconds > 0) {
            long days = totalSeconds / 86400;
            long hours = (totalSeconds % 86400) / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;

            if (days > 0) {
                return String.format("%d days %d hrs %d mins %d seconds", days, hours, minutes, seconds);
            } else if (hours > 0) {
                return String.format("%d hrs %d mins %d seconds", hours, minutes, seconds);
            } else if (minutes > 0) {
                return String.format("%d mins %d seconds", minutes, seconds);
            } else {
                return String.format("%d seconds", seconds);
            }
        } else {
            long overdueSecs = Math.abs(totalSeconds);
            long days = overdueSecs / 86400;
            long hours = (overdueSecs % 86400) / 3600;
            long minutes = (overdueSecs % 3600) / 60;

            if (days > 0) {
                return String.format("⚠️ Overdue by %d days %d hrs", days, hours);
            } else if (hours > 0) {
                return String.format("⚠️ Overdue by %d hrs %d mins", hours, minutes);
            } else {
                return String.format("⚠️ Overdue by %d mins", Math.max(1, minutes));
            }
        }
    }

    public Urgency getUrgencyLevel() {
        if (targetDateTime == null) return Urgency.NORMAL;
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(now, targetDateTime);
        long totalSeconds = duration.getSeconds();

        if (totalSeconds <= 0) {
            return Urgency.OVERDUE;
        } else if (totalSeconds <= 86400) { // Within 24 hours
            return Urgency.URGENT;
        } else {
            return Urgency.NORMAL;
        }
    }

    /**
     * Formats the countdown in the clean clock format:
     * "Due in DD : HH : MM : SS" (or "Ended : Overdue by ...").
     */
    public String getFormattedClockCountdown() {
        if (targetDateTime == null) return "Due in -- : -- : -- : --";

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(now, targetDateTime);
        long totalSeconds = duration.getSeconds();

        if (totalSeconds > 0) {
            long days = totalSeconds / 86400;
            long hours = (totalSeconds % 86400) / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;

            return String.format("Due in %02d : %02d : %02d : %02d", days, hours, minutes, seconds);
        } else {
            long overdueSecs = Math.abs(totalSeconds);
            long days = overdueSecs / 86400;
            long hours = (overdueSecs % 86400) / 3600;
            long minutes = (overdueSecs % 3600) / 60;
            long seconds = overdueSecs % 60;

            if (days > 0) {
                return String.format("Ended : Overdue %02dd : %02dh", days, hours);
            } else if (hours > 0) {
                return String.format("Ended : Overdue %02dh : %02dm", hours, minutes);
            } else {
                return String.format("Ended : Overdue %02dm : %02ds", minutes, seconds);
            }
        }
    }

    /**
     * Visual color styling tokens for dynamic progression over time.
     */
    public static class TaskColorTheme {
        private final String accentColor;
        private final String borderColor;
        private final String cardBg;
        private final String badgeBg;
        private final String badgeText;
        private final boolean isAsh;

        public TaskColorTheme(String accentColor, String borderColor, String cardBg,
                              String badgeBg, String badgeText, boolean isAsh) {
            this.accentColor = accentColor;
            this.borderColor = borderColor;
            this.cardBg = cardBg;
            this.badgeBg = badgeBg;
            this.badgeText = badgeText;
            this.isAsh = isAsh;
        }

        public String getAccentColor() { return accentColor; }
        public String getBorderColor() { return borderColor; }
        public String getCardBg() { return cardBg; }
        public String getBadgeBg() { return badgeBg; }
        public String getBadgeText() { return badgeText; }
        public boolean isAsh() { return isAsh; }
    }

    /**
     * Calculates the dynamic color theme based on remaining time:
     * - > 10 days: Calm Emerald Green
     * - 7-10 days: Fresh Leaf Green
     * - 4-7 days: Golden Amber / Yellow
     * - 1-4 days: Warm Coral / Orange
     * - < 24 hours: Dark Cherry Red
     * - Expired (<= 0): Ash Gray
     */
    public TaskColorTheme getColorTheme() {
        if (targetDateTime == null) {
            return new TaskColorTheme("#059669", "#a7f3d0", "#f0fdf4", "#d1fae5", "#065f46", false);
        }

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(now, targetDateTime);
        long totalSeconds = duration.getSeconds();

        if (totalSeconds <= 0) {
            return new TaskColorTheme("#94a3b8", "#cbd5e1", "#f8fafc", "#e2e8f0", "#475569", true);
        }

        double daysLeft = totalSeconds / 86400.0;
        if (daysLeft >= 10.0) {
            return new TaskColorTheme("#059669", "#a7f3d0", "#f0fdf4", "#d1fae5", "#065f46", false);
        } else if (daysLeft >= 7.0) {
            return new TaskColorTheme("#16a34a", "#bbf7d0", "#f0fdf4", "#dcfce7", "#15803d", false);
        } else if (daysLeft >= 4.0) {
            return new TaskColorTheme("#d97706", "#fde68a", "#fffbeb", "#fef3c7", "#92400e", false);
        } else if (daysLeft >= 1.0) {
            return new TaskColorTheme("#ea580c", "#fed7aa", "#fff7ed", "#ffedd5", "#9a3412", false);
        } else {
            return new TaskColorTheme("#881337", "#fecdd3", "#fff1f2", "#ffe4e6", "#881337", false);
        }
    }

    @Override
    public int compareTo(RoutineTaskItem other) {
        if (this.targetDateTime == null && other.targetDateTime == null) return 0;
        if (this.targetDateTime == null) return 1;
        if (other.targetDateTime == null) return -1;
        return this.targetDateTime.compareTo(other.targetDateTime);
    }
}

