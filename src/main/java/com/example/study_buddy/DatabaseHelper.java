package com.example.study_buddy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Helper class to manage SQLite database operations for Study Buddy.
 * Handles table creation, password hashing, user registration, and login validation.
 */
public class DatabaseHelper {

    // SQLite database URL. This creates a file named "study_buddy.db" in the project root folder.
    private static final String DB_URL = "jdbc:sqlite:study_buddy.db";

    /**
     * Gets a connection to the SQLite database.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Initializes the database by creating the `users` table if it doesn't already exist.
     */
    public static void initDatabase() {
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "email TEXT UNIQUE NOT NULL, "
                + "username TEXT UNIQUE NOT NULL, "
                + "password_hash TEXT NOT NULL, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ");";

        String createSlotsTable = "CREATE TABLE IF NOT EXISTS routine_slots ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_id INTEGER NOT NULL, "
                + "day_of_week TEXT NOT NULL, "
                + "time_slot TEXT NOT NULL, "
                + "subject_name TEXT, "
                + "teacher_code TEXT, "
                + "UNIQUE(user_id, day_of_week, time_slot), "
                + "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE"
                + ");";

        String createActivitiesTable = "CREATE TABLE IF NOT EXISTS routine_activities ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "slot_id INTEGER NOT NULL, "
                + "activity_type TEXT NOT NULL, "
                + "deadline_info TEXT NOT NULL, "
                + "FOREIGN KEY(slot_id) REFERENCES routine_slots(id) ON DELETE CASCADE"
                + ");";

        String createConfigTable = "CREATE TABLE IF NOT EXISTS user_routine_config ("
                + "user_id INTEGER PRIMARY KEY, "
                + "weekdays TEXT NOT NULL, "
                + "time_slots TEXT NOT NULL, "
                + "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE"
                + ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createSlotsTable);
            stmt.execute(createActivitiesTable);
            stmt.execute(createConfigTable);
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Hashes a plain password using standard SHA-256 for secure storage.
     */
    public static String hashPassword(String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Checks if an email is already registered.
     */
    public static boolean isEmailTaken(String email) {
        String sql = "SELECT id FROM users WHERE LOWER(email) = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks if a username is already registered.
     */
    public static boolean isUsernameTaken(String username) {
        String sql = "SELECT id FROM users WHERE LOWER(username) = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Registers a new user with email, username, and password.
     * Returns null if successful, or an error message if failed.
     */
    public static String registerUser(String email, String username, String plainPassword) {
        String cleanEmail = email.trim().toLowerCase();
        String cleanUsername = username.trim();

        if (isEmailTaken(cleanEmail)) {
            return "An account with this Gmail address already exists.";
        }
        if (isUsernameTaken(cleanUsername)) {
            return "This username is already taken. Please choose another.";
        }

        String sql = "INSERT INTO users (email, username, password_hash) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cleanEmail);
            pstmt.setString(2, cleanUsername);
            pstmt.setString(3, hashPassword(plainPassword));
            pstmt.executeUpdate();
            return null; // Success
        } catch (SQLException e) {
            e.printStackTrace();
            return "Database error: " + e.getMessage();
        }
    }

    /**
     * Validates user credentials. The identifier can be either the username or Gmail address.
     * Returns the User object if authentication is successful, or null otherwise.
     */
    public static User authenticateUser(String usernameOrEmail, String plainPassword) {
        String cleanIdentifier = usernameOrEmail.trim().toLowerCase();
        String sql = "SELECT id, username, email, password_hash FROM users "
                + "WHERE LOWER(username) = ? OR LOWER(email) = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cleanIdentifier);
            pstmt.setString(2, cleanIdentifier);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String providedHash = hashPassword(plainPassword);
                    if (storedHash.equals(providedHash)) {
                        return new User(rs.getInt("id"), rs.getString("username"), rs.getString("email"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Authentication failed
    }

    /**
     * Saves or updates a routine slot along with its special activities.
     */
    public static boolean saveRoutineSlot(RoutineSlot slot) {
        String upsertSlotSql = "INSERT INTO routine_slots (user_id, day_of_week, time_slot, subject_name, teacher_code) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON CONFLICT(user_id, day_of_week, time_slot) DO UPDATE SET "
                + "subject_name = excluded.subject_name, teacher_code = excluded.teacher_code;";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Begin transaction

            int slotId = 0;
            try (PreparedStatement pstmt = conn.prepareStatement(upsertSlotSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, slot.getUserId());
                pstmt.setString(2, slot.getDayOfWeek());
                pstmt.setString(3, slot.getTimeSlot());
                pstmt.setString(4, slot.getSubjectName());
                pstmt.setString(5, slot.getTeacherCode());
                pstmt.executeUpdate();

                // Get generated or existing slot id
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        slotId = rs.getInt(1);
                    }
                }
            }

            // If upsert updated an existing row, fetch its ID
            if (slotId == 0) {
                String selectIdSql = "SELECT id FROM routine_slots WHERE user_id = ? AND day_of_week = ? AND time_slot = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(selectIdSql)) {
                    pstmt.setInt(1, slot.getUserId());
                    pstmt.setString(2, slot.getDayOfWeek());
                    pstmt.setString(3, slot.getTimeSlot());
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            slotId = rs.getInt("id");
                        }
                    }
                }
            }

            // Clear old activities for this slot
            String deleteActivitiesSql = "DELETE FROM routine_activities WHERE slot_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteActivitiesSql)) {
                pstmt.setInt(1, slotId);
                pstmt.executeUpdate();
            }

            // Insert updated activities
            if (slot.getActivities() != null && !slot.getActivities().isEmpty()) {
                String insertActivitySql = "INSERT INTO routine_activities (slot_id, activity_type, deadline_info) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertActivitySql)) {
                    for (SpecialActivity activity : slot.getActivities()) {
                        pstmt.setInt(1, slotId);
                        pstmt.setString(2, activity.getActivityType());
                        pstmt.setString(3, activity.getDeadlineInfo());
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
            }

            conn.commit(); // Commit transaction
            slot.setId(slotId);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a routine slot and all its associated activities.
     */
    public static boolean deleteRoutineSlot(int userId, String dayOfWeek, String timeSlot) {
        String sql = "DELETE FROM routine_slots WHERE user_id = ? AND day_of_week = ? AND time_slot = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, dayOfWeek);
            pstmt.setString(3, timeSlot);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Fetches all routine slots and their activities for a specific user.
     */
    public static java.util.Map<String, RoutineSlot> getAllRoutineSlots(int userId) {
        java.util.Map<String, RoutineSlot> slots = new java.util.HashMap<>();
        String sql = "SELECT id, day_of_week, time_slot, subject_name, teacher_code FROM routine_slots WHERE user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int slotId = rs.getInt("id");
                    String day = rs.getString("day_of_week");
                    String time = rs.getString("time_slot");
                    String subject = rs.getString("subject_name");
                    String teacher = rs.getString("teacher_code");

                    RoutineSlot slot = new RoutineSlot(slotId, userId, day, time, subject, teacher);
                    slot.setActivities(getActivitiesForSlot(conn, slotId));

                    String key = day + "|||" + time;
                    slots.put(key, slot);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return slots;
    }

    private static java.util.List<SpecialActivity> getActivitiesForSlot(Connection conn, int slotId) {
        java.util.List<SpecialActivity> list = new java.util.ArrayList<>();
        String sql = "SELECT id, activity_type, deadline_info FROM routine_activities WHERE slot_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, slotId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new SpecialActivity(
                            rs.getInt("id"),
                            slotId,
                            rs.getString("activity_type"),
                            rs.getString("deadline_info")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Retrieves the user's active weekdays (default: Monday to Friday).
     */
    public static java.util.List<String> getUserWeekdays(int userId) {
        String sql = "SELECT weekdays FROM user_routine_config WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String raw = rs.getString("weekdays");
                    if (raw != null && !raw.trim().isEmpty()) {
                        return new java.util.ArrayList<>(java.util.Arrays.asList(raw.split(",")));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Defaults: 5 weekdays
        return new java.util.ArrayList<>(java.util.Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday"));
    }

    /**
     * Retrieves the user's active time slots.
     */
    public static java.util.List<String> getUserTimeSlots(int userId) {
        String sql = "SELECT time_slots FROM user_routine_config WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String raw = rs.getString("time_slots");
                    if (raw != null && !raw.trim().isEmpty()) {
                        return new java.util.ArrayList<>(java.util.Arrays.asList(raw.split(",")));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Default time slots
        return new java.util.ArrayList<>(java.util.Arrays.asList(
                "08:30 - 09:50", "10:00 - 11:20", "11:30 - 12:50", "01:30 - 02:50", "03:00 - 04:20"
        ));
    }

    /**
     * Saves the user's configured weekdays and time slots.
     */
    public static void saveUserRoutineConfig(int userId, java.util.List<String> weekdays, java.util.List<String> timeSlots) {
        String upsertSql = "INSERT INTO user_routine_config (user_id, weekdays, time_slots) VALUES (?, ?, ?) "
                + "ON CONFLICT(user_id) DO UPDATE SET weekdays = excluded.weekdays, time_slots = excluded.time_slots;";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(upsertSql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, String.join(",", weekdays));
            pstmt.setString(3, String.join(",", timeSlots));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
