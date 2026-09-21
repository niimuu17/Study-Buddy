package com.example.study_buddy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseHelperTest {

    @BeforeAll
    public static void setUp() {
        DatabaseHelper.initDatabase();
    }

    @Test
    public void testRegistrationAndAuthentication() {
        long ts = System.currentTimeMillis();
        String testEmail = "student" + ts + "@gmail.com";
        String testUsername = "student" + ts;
        String testPass = "myPassword123";

        // 1. Initial existence checks
        assertFalse(DatabaseHelper.isEmailTaken(testEmail));
        assertFalse(DatabaseHelper.isUsernameTaken(testUsername));

        // 2. Register user
        String regResult = DatabaseHelper.registerUser(testEmail, testUsername, testPass);
        assertNull(regResult, "Registration should succeed and return null");

        // 3. Post-registration existence checks
        assertTrue(DatabaseHelper.isEmailTaken(testEmail));
        assertTrue(DatabaseHelper.isUsernameTaken(testUsername));

        // 4. Duplicate registration with same email should fail
        String duplicateEmailResult = DatabaseHelper.registerUser(testEmail, "differentUser" + ts, "pass456");
        assertNotNull(duplicateEmailResult, "Duplicate email registration should fail");
        assertTrue(duplicateEmailResult.contains("Gmail"));

        // 5. Duplicate registration with same username should fail
        String duplicateUserResult = DatabaseHelper.registerUser("different" + ts + "@gmail.com", testUsername, "pass456");
        assertNotNull(duplicateUserResult, "Duplicate username registration should fail");
        assertTrue(duplicateUserResult.contains("username"));

        // 6. Authenticate with username
        User userByUsername = DatabaseHelper.authenticateUser(testUsername, testPass);
        assertNotNull(userByUsername, "Authentication with username should succeed");
        assertEquals(testUsername, userByUsername.getUsername());
        assertEquals(testEmail.toLowerCase(), userByUsername.getEmail());

        // 7. Authenticate with email
        User userByEmail = DatabaseHelper.authenticateUser(testEmail, testPass);
        assertNotNull(userByEmail, "Authentication with email should succeed");
        assertEquals(testUsername, userByEmail.getUsername());

        // 8. Authenticate with case-insensitive identifier (e.g. UPPERCASE email)
        User userCaseInsensitive = DatabaseHelper.authenticateUser(testEmail.toUpperCase(), testPass);
        assertNotNull(userCaseInsensitive, "Authentication should be case-insensitive for email/username");

        // 9. Authenticate with wrong password
        User failedPasswordUser = DatabaseHelper.authenticateUser(testUsername, "wrongPassword!");
        assertNull(failedPasswordUser, "Authentication with wrong password should return null");

        // 10. Authenticate with non-existent account
        User nonExistentUser = DatabaseHelper.authenticateUser("doesnotexist@gmail.com", "anyPassword");
        assertNull(nonExistentUser, "Authentication with non-existent user should return null");
    }

    @Test
    public void testStrongPasswordValidation() {
        // Null or empty
        assertNotNull(LoginController.validateStrongPassword(null));
        assertNotNull(LoginController.validateStrongPassword(""));

        // Too short (< 8 chars)
        String shortError = LoginController.validateStrongPassword("Ab1!");
        assertNotNull(shortError);
        assertTrue(shortError.contains("8 characters"));

        // Missing uppercase letter
        String noUpperError = LoginController.validateStrongPassword("lowercase1@");
        assertNotNull(noUpperError);
        assertTrue(noUpperError.contains("capital letter"));

        // Missing lowercase letter
        String noLowerError = LoginController.validateStrongPassword("UPPERCASE1@");
        assertNotNull(noLowerError);
        assertTrue(noLowerError.contains("lowercase letter"));

        // Missing digit/number
        String noDigitError = LoginController.validateStrongPassword("NoNumberHere@");
        assertNotNull(noDigitError);
        assertTrue(noDigitError.contains("number/digit"));

        // Missing special character/symbol
        String noSymbolError = LoginController.validateStrongPassword("NoSymbol1234");
        assertNotNull(noSymbolError);
        assertTrue(noSymbolError.contains("special character"));

        // Valid strong passwords
        assertNull(LoginController.validateStrongPassword("StudyBuddy@2026"));
        assertNull(LoginController.validateStrongPassword("P@ssw0rd!"));
        assertNull(LoginController.validateStrongPassword("Secret#99"));
    }

    @Test
    public void testRoutineOperations() {
        // Register a test user
        long ts = System.currentTimeMillis();
        String email = "routine" + ts + "@gmail.com";
        String username = "routine" + ts;
        DatabaseHelper.registerUser(email, username, "Valid@1234");
        User user = DatabaseHelper.authenticateUser(username, "Valid@1234");
        assertNotNull(user);

        // 1. Save a routine slot with activities
        RoutineSlot slot = new RoutineSlot(user.getId(), "Monday", "08:30 - 09:50", "CSE2008", "SH");
        List<SpecialActivity> activities = new ArrayList<>();
        activities.add(new SpecialActivity("CT 1", "2026-09-25"));
        activities.add(new SpecialActivity("Assignment", "2026-09-30"));
        slot.setActivities(activities);

        boolean saved = DatabaseHelper.saveRoutineSlot(slot);
        assertTrue(saved, "Slot should save successfully");

        // 2. Retrieve all routine slots
        Map<String, RoutineSlot> userSlots = DatabaseHelper.getAllRoutineSlots(user.getId());
        String key = "Monday|||08:30 - 09:50";
        assertTrue(userSlots.containsKey(key), "Slot key should exist in userSlots map");

        RoutineSlot loadedSlot = userSlots.get(key);
        assertEquals("CSE2008", loadedSlot.getSubjectName());
        assertEquals("SH", loadedSlot.getTeacherCode());
        assertEquals(2, loadedSlot.getActivities().size(), "Should have 2 special activities");
        assertEquals("CT 1", loadedSlot.getActivities().get(0).getActivityType());

        // 3. Update the routine slot
        loadedSlot.setSubjectName("CSE2009");
        loadedSlot.setTeacherCode("ABCD");
        loadedSlot.getActivities().remove(0); // remove one activity
        boolean updated = DatabaseHelper.saveRoutineSlot(loadedSlot);
        assertTrue(updated, "Updated slot should save");

        Map<String, RoutineSlot> updatedSlots = DatabaseHelper.getAllRoutineSlots(user.getId());
        RoutineSlot reloaded = updatedSlots.get(key);
        assertEquals("CSE2009", reloaded.getSubjectName());
        assertEquals("ABCD", reloaded.getTeacherCode());
        assertEquals(1, reloaded.getActivities().size());

        // 4. Test User Routine Config (weekdays and timeslots)
        List<String> customDays = Arrays.asList("Sunday", "Monday", "Tuesday", "Wednesday");
        List<String> customSlots = Arrays.asList("09:00 - 10:00", "10:15 - 11:15");
        DatabaseHelper.saveUserRoutineConfig(user.getId(), customDays, customSlots);

        List<String> loadedDays = DatabaseHelper.getUserWeekdays(user.getId());
        List<String> loadedTimes = DatabaseHelper.getUserTimeSlots(user.getId());
        assertEquals(4, loadedDays.size());
        assertEquals("Sunday", loadedDays.get(0));
        assertEquals(2, loadedTimes.size());
        assertEquals("09:00 - 10:00", loadedTimes.get(0));

        // 5. Delete routine slot
        boolean deleted = DatabaseHelper.deleteRoutineSlot(user.getId(), "Monday", "08:30 - 09:50");
        assertTrue(deleted, "Slot should delete successfully");
        Map<String, RoutineSlot> slotsAfterDelete = DatabaseHelper.getAllRoutineSlots(user.getId());
        assertFalse(slotsAfterDelete.containsKey(key), "Deleted slot should not exist anymore");
    }
}
