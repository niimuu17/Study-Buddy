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
import java.util.ArrayList;
import java.util.List;

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
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
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

        String createNotebooksTable = "CREATE TABLE IF NOT EXISTS notebooks ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_id INTEGER NOT NULL, "
                + "title TEXT NOT NULL, "
                + "description TEXT, "
                + "color_hex TEXT DEFAULT '#4f46e5', "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE"
                + ");";

        String createTopicsTable = "CREATE TABLE IF NOT EXISTS topics ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "notebook_id INTEGER NOT NULL, "
                + "title TEXT NOT NULL, "
                + "order_index INTEGER DEFAULT 0, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY(notebook_id) REFERENCES notebooks(id) ON DELETE CASCADE"
                + ");";

        String createPagesTable = "CREATE TABLE IF NOT EXISTS pages ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "topic_id INTEGER NOT NULL, "
                + "title TEXT NOT NULL, "
                + "content_json TEXT DEFAULT '', "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY(topic_id) REFERENCES topics(id) ON DELETE CASCADE"
                + ");";

        String createTopicFilesTable = "CREATE TABLE IF NOT EXISTS topic_files ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "topic_id INTEGER NOT NULL, "
                + "original_name TEXT NOT NULL, "
                + "stored_file_path TEXT NOT NULL, "
                + "file_extension TEXT, "
                + "file_size_bytes INTEGER DEFAULT 0, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY(topic_id) REFERENCES topics(id) ON DELETE CASCADE"
                + ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createSlotsTable);
            stmt.execute(createActivitiesTable);
            stmt.execute(createConfigTable);
            stmt.execute(createNotebooksTable);
            stmt.execute(createTopicsTable);
            stmt.execute(createPagesTable);
            stmt.execute(createTopicFilesTable);
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
     * Retrieves the dev user 'ikki' (registering if needed) to bypass login during development.
     */
    public static User getFirstOrCreateDevUser() {
        User user = authenticateUser("ikki", "Ikkiis@kuet23");
        if (user != null) {
            return user;
        }

        user = authenticateUser("ikki@gmail.com", "Ikkiis@kuet23");
        if (user != null) {
            return user;
        }

        // If not registered, create the account
        registerUser("ikki@gmail.com", "ikki", "Ikkiis@kuet23");
        user = authenticateUser("ikki", "Ikkiis@kuet23");
        if (user != null) {
            return user;
        }

        // Fallback to existing account named 'ikki'
        String sql = "SELECT id, username, email FROM users WHERE LOWER(username) = 'ikki' LIMIT 1";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("email"), rs.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
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
     * Deletes a specific routine activity by its primary key ID.
     */
    public static boolean deleteRoutineActivity(int activityId) {
        String sql = "DELETE FROM routine_activities WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, activityId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves the user's active weekdays. Returns an empty list if not configured yet (user starts from zero).
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
        // Start from zero / empty if not configured
        return new java.util.ArrayList<>();
    }

    /**
     * Retrieves the user's active time slots. Returns an empty list if not configured yet.
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
        // Start from zero / empty if not configured
        return new java.util.ArrayList<>();
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

    /**
     * Renames a weekday, updating all existing routine_slots and the saved user configuration.
     */
    public static boolean renameWeekday(int userId, String oldDay, String newDay) {
        String updateSlotsSql = "UPDATE routine_slots SET day_of_week = ? WHERE user_id = ? AND day_of_week = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSlotsSql)) {
            pstmt.setString(1, newDay);
            pstmt.setInt(2, userId);
            pstmt.setString(3, oldDay);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        // Update user_routine_config after closing the slots update connection
        java.util.List<String> days = getUserWeekdays(userId);
        int idx = days.indexOf(oldDay);
        if (idx != -1) {
            days.set(idx, newDay);
            saveUserRoutineConfig(userId, days, getUserTimeSlots(userId));
        }
        return true;
    }

    /**
     * Renames a time slot, updating all existing routine_slots and the saved user configuration.
     */
    public static boolean renameTimeSlot(int userId, String oldSlot, String newSlot) {
        String updateSlotsSql = "UPDATE routine_slots SET time_slot = ? WHERE user_id = ? AND time_slot = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSlotsSql)) {
            pstmt.setString(1, newSlot);
            pstmt.setInt(2, userId);
            pstmt.setString(3, oldSlot);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        // Update user_routine_config after closing the slots update connection
        java.util.List<String> slots = getUserTimeSlots(userId);
        int idx = slots.indexOf(oldSlot);
        if (idx != -1) {
            slots.set(idx, newSlot);
            saveUserRoutineConfig(userId, getUserWeekdays(userId), slots);
        }
        return true;
    }

    // ==========================================
    // NOTEBOOK CRUD OPERATIONS
    // ==========================================

    public static int createNotebook(int userId, String title, String description, String colorHex) {
        String sql = "INSERT INTO notebooks (user_id, title, description, color_hex, created_at, updated_at) "
                   + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, title.trim());
            pstmt.setString(3, description != null ? description.trim() : "");
            pstmt.setString(4, (colorHex != null && !colorHex.isEmpty()) ? colorHex : "#4f46e5");
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<Notebook> getUserNotebooks(int userId) {
        List<Notebook> list = new ArrayList<>();
        String sql = "SELECT n.id, n.user_id, n.title, n.description, n.color_hex, n.created_at, n.updated_at, "
                   + "       COUNT(DISTINCT t.id) AS topic_count, "
                   + "       COUNT(DISTINCT p.id) AS page_count "
                   + "FROM notebooks n "
                   + "LEFT JOIN topics t ON t.notebook_id = n.id "
                   + "LEFT JOIN pages p ON p.topic_id = t.id "
                   + "WHERE n.user_id = ? "
                   + "GROUP BY n.id "
                   + "ORDER BY n.updated_at DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Notebook(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getString("color_hex"),
                            rs.getInt("topic_count"),
                            rs.getInt("page_count"),
                            rs.getString("created_at"),
                            rs.getString("updated_at")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static Notebook getNotebookById(int notebookId) {
        String sql = "SELECT n.id, n.user_id, n.title, n.description, n.color_hex, n.created_at, n.updated_at, "
                   + "       COUNT(DISTINCT t.id) AS topic_count, "
                   + "       COUNT(DISTINCT p.id) AS page_count "
                   + "FROM notebooks n "
                   + "LEFT JOIN topics t ON t.notebook_id = n.id "
                   + "LEFT JOIN pages p ON p.topic_id = t.id "
                   + "WHERE n.id = ? "
                   + "GROUP BY n.id";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, notebookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Notebook(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getString("color_hex"),
                            rs.getInt("topic_count"),
                            rs.getInt("page_count"),
                            rs.getString("created_at"),
                            rs.getString("updated_at")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean updateNotebook(int notebookId, String title, String description, String colorHex) {
        String sql = "UPDATE notebooks SET title = ?, description = ?, color_hex = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title.trim());
            pstmt.setString(2, description != null ? description.trim() : "");
            pstmt.setString(3, colorHex);
            pstmt.setInt(4, notebookId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteNotebook(int notebookId) {
        String sql = "DELETE FROM notebooks WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, notebookId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==========================================
    // TOPIC CRUD OPERATIONS
    // ==========================================

    public static int createTopic(int notebookId, String title) {
        String sql = "INSERT INTO topics (notebook_id, title, order_index, created_at) "
                   + "VALUES (?, ?, (SELECT COALESCE(MAX(order_index), 0) + 1 FROM topics WHERE notebook_id = ?), CURRENT_TIMESTAMP)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, notebookId);
            pstmt.setString(2, title.trim());
            pstmt.setInt(3, notebookId);
            pstmt.executeUpdate();

            // Touch notebook's updated_at
            touchNotebookUpdated(conn, notebookId);

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<Topic> getTopicsByNotebook(int notebookId) {
        List<Topic> topics = new ArrayList<>();
        String sql = "SELECT id, notebook_id, title, order_index, created_at FROM topics WHERE notebook_id = ? ORDER BY order_index ASC, id ASC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, notebookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Topic topic = new Topic(
                            rs.getInt("id"),
                            rs.getInt("notebook_id"),
                            rs.getString("title"),
                            rs.getInt("order_index"),
                            rs.getString("created_at")
                    );
                    topic.setPages(getPagesByTopic(topic.getId()));
                    topic.setFiles(getFilesByTopic(topic.getId()));
                    topics.add(topic);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return topics;
    }

    public static boolean renameTopic(int topicId, String newTitle) {
        String sql = "UPDATE topics SET title = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newTitle.trim());
            pstmt.setInt(2, topicId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteTopic(int topicId) {
        String sql = "DELETE FROM topics WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, topicId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==========================================
    // PAGE CRUD OPERATIONS
    // ==========================================

    public static int createPage(int topicId, String title) {
        String sql = "INSERT INTO pages (topic_id, title, content_json, created_at, updated_at) "
                   + "VALUES (?, ?, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, topicId);
            pstmt.setString(2, (title != null && !title.trim().isEmpty()) ? title.trim() : "Untitled Page");
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<Page> getPagesByTopic(int topicId) {
        List<Page> list = new ArrayList<>();
        String sql = "SELECT id, topic_id, title, content_json, created_at, updated_at FROM pages WHERE topic_id = ? ORDER BY id ASC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, topicId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Page(
                            rs.getInt("id"),
                            rs.getInt("topic_id"),
                            rs.getString("title"),
                            rs.getString("content_json"),
                            rs.getString("created_at"),
                            rs.getString("updated_at")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static Page getPageById(int pageId) {
        String sql = "SELECT id, topic_id, title, content_json, created_at, updated_at FROM pages WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, pageId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Page(
                            rs.getInt("id"),
                            rs.getInt("topic_id"),
                            rs.getString("title"),
                            rs.getString("content_json"),
                            rs.getString("created_at"),
                            rs.getString("updated_at")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean updatePage(int pageId, String title, String contentJson) {
        String sql = "UPDATE pages SET title = ?, content_json = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, (title != null && !title.trim().isEmpty()) ? title.trim() : "Untitled Page");
            pstmt.setString(2, contentJson != null ? contentJson : "");
            pstmt.setInt(3, pageId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deletePage(int pageId) {
        String sql = "DELETE FROM pages WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, pageId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==========================================
    // TOPIC FILE ATTACHMENTS CRUD
    // ==========================================

    public static int addTopicFile(int topicId, String originalName, String storedPath, String ext, long sizeBytes) {
        String sql = "INSERT INTO topic_files (topic_id, original_name, stored_file_path, file_extension, file_size_bytes, created_at) "
                   + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, topicId);
            pstmt.setString(2, originalName);
            pstmt.setString(3, storedPath);
            pstmt.setString(4, ext != null ? ext.toLowerCase() : "");
            pstmt.setLong(5, sizeBytes);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<TopicFile> getFilesByTopic(int topicId) {
        List<TopicFile> list = new ArrayList<>();
        String sql = "SELECT id, topic_id, original_name, stored_file_path, file_extension, file_size_bytes, created_at FROM topic_files WHERE topic_id = ? ORDER BY id ASC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, topicId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new TopicFile(
                            rs.getInt("id"),
                            rs.getInt("topic_id"),
                            rs.getString("original_name"),
                            rs.getString("stored_file_path"),
                            rs.getString("file_extension"),
                            rs.getLong("file_size_bytes"),
                            rs.getString("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static boolean deleteTopicFile(int fileId) {
        String sql = "DELETE FROM topic_files WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, fileId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void touchNotebookUpdated(Connection conn, int notebookId) {
        String sql = "UPDATE notebooks SET updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, notebookId);
            pstmt.executeUpdate();
        } catch (SQLException ignored) {}
    }
}
