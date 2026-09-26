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

        // 6. Test Weekday and Timeslot Renaming
        RoutineSlot slotToRename = new RoutineSlot(user.getId(), "Tuesday", "10:15 - 11:15", "EEE1001", "RK");
        DatabaseHelper.saveRoutineSlot(slotToRename);

        boolean dayRenamed = DatabaseHelper.renameWeekday(user.getId(), "Tuesday", "Theory Tuesday");
        assertTrue(dayRenamed);
        Map<String, RoutineSlot> slotsAfterDayRename = DatabaseHelper.getAllRoutineSlots(user.getId());
        assertTrue(slotsAfterDayRename.containsKey("Theory Tuesday|||10:15 - 11:15"));
        assertFalse(slotsAfterDayRename.containsKey("Tuesday|||10:15 - 11:15"));

        boolean timeRenamed = DatabaseHelper.renameTimeSlot(user.getId(), "10:15 - 11:15", "10:30 - 11:30");
        assertTrue(timeRenamed);
        Map<String, RoutineSlot> slotsAfterTimeRename = DatabaseHelper.getAllRoutineSlots(user.getId());
        assertTrue(slotsAfterTimeRename.containsKey("Theory Tuesday|||10:30 - 11:30"));
    }

    @Test
    public void testEmptyRoutineForNewUser() {
        long ts = System.currentTimeMillis();
        String username = "emptyuser" + ts;
        DatabaseHelper.registerUser("empty" + ts + "@gmail.com", username, "Secret@123");
        User user = DatabaseHelper.authenticateUser(username, "Secret@123");
        assertNotNull(user);

        // A new user must start from zero (empty weekdays and empty time slots)
        List<String> weekdays = DatabaseHelper.getUserWeekdays(user.getId());
        List<String> timeSlots = DatabaseHelper.getUserTimeSlots(user.getId());
        assertTrue(weekdays.isEmpty(), "New user should start with 0 weekdays");
        assertTrue(timeSlots.isEmpty(), "New user should start with 0 time slots");
    }

    @Test
    public void testTimeSlotConflictDetection() {
        List<String> existing = Arrays.asList(
                "08:30 - 09:50",
                "10:00 - 11:20",
                "01:30 PM - 02:50 PM"
        );

        // Conflict cases:
        // 1. Partial overlap at start
        String c1 = TimeSlotHelper.findConflict("09:00 - 10:30", existing, null);
        assertNotNull(c1, "Should detect conflict with 08:30 - 09:50");

        // 2. Complete containment
        String c2 = TimeSlotHelper.findConflict("08:45 - 09:15", existing, null);
        assertNotNull(c2, "Should detect conflict inside 08:30 - 09:50");

        // 3. Exact match
        String c3 = TimeSlotHelper.findConflict("10:00 - 11:20", existing, null);
        assertNotNull(c3, "Should detect exact match conflict");

        // 4. Overlap in 12-hour PM format
        String c4 = TimeSlotHelper.findConflict("02:00 PM - 03:00 PM", existing, null);
        assertNotNull(c4, "Should detect conflict with 01:30 PM - 02:50 PM");

        // Non-conflict cases:
        // 5. In between slots (09:50 to 10:00)
        String ok1 = TimeSlotHelper.findConflict("09:50 - 10:00", existing, null);
        assertNull(ok1, "09:50 - 10:00 should have no conflict");

        // 6. Before first slot (07:00 - 08:20)
        String ok2 = TimeSlotHelper.findConflict("07:00 - 08:20", existing, null);
        assertNull(ok2, "07:00 - 08:20 should have no conflict");

        // 7. Editing existing slot (ignoring itself)
        String ok3 = TimeSlotHelper.findConflict("08:30 - 09:55", existing, "08:30 - 09:50");
        assertNull(ok3, "Editing 08:30 - 09:50 to 08:30 - 09:55 should not conflict when ignoring self");

        // 8. Punctuation variations & dot notation
        String c5 = TimeSlotHelper.validateSlot("09.00 – 10.30", existing, null);
        assertNotNull(c5, "En-dash and dot separator should detect conflict");
        assertTrue(c5.contains("overlaps with existing class slot"));

        // 9. Afternoon heuristic without PM
        List<String> afternoonExisting = Arrays.asList("01:30 - 02:50");
        String c6 = TimeSlotHelper.validateSlot("2:00 - 3:00", afternoonExisting, null);
        assertNotNull(c6, "2:00 - 3:00 should conflict with 01:30 - 02:50 using afternoon heuristic");

        // 10. Invalid syntax
        String errSyntax = TimeSlotHelper.validateSlot("Invalid Format", existing, null);
        assertNotNull(errSyntax);
        assertTrue(errSyntax.contains("Please use format"));

        // 11. End time <= start time
        String errBackwards = TimeSlotHelper.validateSlot("11:00 - 10:00", existing, null);
        assertNotNull(errBackwards);
        assertTrue(errBackwards.contains("End time must be after start time"));
    }

    @Test
    public void testNotebookHierarchyCrud() {
        long ts = System.currentTimeMillis();
        String username = "nbuser" + ts;
        DatabaseHelper.registerUser("nb" + ts + "@gmail.com", username, "Secret@123");
        User user = DatabaseHelper.authenticateUser(username, "Secret@123");
        assertNotNull(user);

        // 1. Create Notebook
        int notebookId = DatabaseHelper.createNotebook(user.getId(), "Data Structures", "Course CS201", "#4f46e5");
        assertTrue(notebookId > 0, "Notebook should be created");

        List<Notebook> notebooks = DatabaseHelper.getUserNotebooks(user.getId());
        assertEquals(1, notebooks.size());
        assertEquals("Data Structures", notebooks.get(0).getTitle());
        assertEquals("#4f46e5", notebooks.get(0).getColorHex());

        // 2. Update Notebook
        boolean updated = DatabaseHelper.updateNotebook(notebookId, "Advanced Data Structures", "Updated Desc", "#059669");
        assertTrue(updated);
        Notebook reloadedNb = DatabaseHelper.getNotebookById(notebookId);
        assertNotNull(reloadedNb);
        assertEquals("Advanced Data Structures", reloadedNb.getTitle());
        assertEquals("#059669", reloadedNb.getColorHex());

        // 3. Create Topic
        int topicId = DatabaseHelper.createTopic(notebookId, "Binary Trees");
        assertTrue(topicId > 0);

        List<Topic> topics = DatabaseHelper.getTopicsByNotebook(notebookId);
        assertEquals(1, topics.size());
        assertEquals("Binary Trees", topics.get(0).getTitle());

        // 4. Rename Topic
        boolean topicRenamed = DatabaseHelper.renameTopic(topicId, "AVL & Red-Black Trees");
        assertTrue(topicRenamed);

        // 5. Create Page
        int pageId = DatabaseHelper.createPage(topicId, "Tree Balancing");
        assertTrue(pageId > 0);

        Page page = DatabaseHelper.getPageById(pageId);
        assertNotNull(page);
        assertEquals("Tree Balancing", page.getTitle());

        // 6. Update Page Content with JSON
        List<PageBlock> blocks = new ArrayList<>();
        blocks.add(new PageBlock("b1", PageBlock.TYPE_TEXT, "Notes on rotations", ""));
        blocks.add(new PageBlock("b2", PageBlock.TYPE_CODE, "void rotateLeft() {}", "Java"));
        String json = PageBlock.serializeList(blocks);

        boolean pageUpdated = DatabaseHelper.updatePage(pageId, "Tree Balancing & Rotations", json);
        assertTrue(pageUpdated);

        Page reloadedPage = DatabaseHelper.getPageById(pageId);
        assertNotNull(reloadedPage);
        assertEquals("Tree Balancing & Rotations", reloadedPage.getTitle());
        List<PageBlock> loadedBlocks = PageBlock.deserializeList(reloadedPage.getContentJson());
        assertEquals(2, loadedBlocks.size());
        assertEquals(PageBlock.TYPE_TEXT, loadedBlocks.get(0).getType());
        assertEquals("Notes on rotations", loadedBlocks.get(0).getContent());
        assertEquals(PageBlock.TYPE_CODE, loadedBlocks.get(1).getType());
        assertEquals("Java", loadedBlocks.get(1).getExtra());

        // 7. Add Topic File Attachment
        int fileId = DatabaseHelper.addTopicFile(topicId, "slides.pdf", "C:/study/slides.pdf", "pdf", 1024000);
        assertTrue(fileId > 0);

        List<TopicFile> files = DatabaseHelper.getFilesByTopic(topicId);
        assertEquals(1, files.size());
        assertEquals("slides.pdf", files.get(0).getOriginalName());
        assertEquals("pdf", files.get(0).getFileExtension());
        assertEquals("📕", files.get(0).getFileIcon());

        // 8. Delete Topic File
        boolean fileDeleted = DatabaseHelper.deleteTopicFile(fileId);
        assertTrue(fileDeleted);
        assertEquals(0, DatabaseHelper.getFilesByTopic(topicId).size());

        // 9. Delete Page
        boolean pageDeleted = DatabaseHelper.deletePage(pageId);
        assertTrue(pageDeleted);
        assertEquals(0, DatabaseHelper.getPagesByTopic(topicId).size());

        // 10. Delete Topic
        boolean topicDeleted = DatabaseHelper.deleteTopic(topicId);
        assertTrue(topicDeleted);
        assertEquals(0, DatabaseHelper.getTopicsByNotebook(notebookId).size());

        // 11. Delete Notebook
        boolean nbDeleted = DatabaseHelper.deleteNotebook(notebookId);
        assertTrue(nbDeleted);
        assertEquals(0, DatabaseHelper.getUserNotebooks(user.getId()).size());
    }

    @Test
    public void testPageBlockSerialization() {
        List<PageBlock> original = new ArrayList<>();
        original.add(new PageBlock("id-1", PageBlock.TYPE_TEXT, "Hello world note with \"quotes\" and \n newlines!", ""));
        original.add(new PageBlock("id-2", PageBlock.TYPE_CODE, "System.out.println(\"Hi\");", "Java"));
        original.add(new PageBlock("id-3", PageBlock.TYPE_IMAGE, "C:/images/test.png", "Diagram 1"));

        String json = PageBlock.serializeList(original);
        assertNotNull(json);
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));

        List<PageBlock> parsed = PageBlock.deserializeList(json);
        assertEquals(3, parsed.size());

        assertEquals("id-1", parsed.get(0).getId());
        assertEquals(PageBlock.TYPE_TEXT, parsed.get(0).getType());
        assertEquals("Hello world note with \"quotes\" and \n newlines!", parsed.get(0).getContent());

        assertEquals("id-2", parsed.get(1).getId());
        assertEquals(PageBlock.TYPE_CODE, parsed.get(1).getType());
        assertEquals("System.out.println(\"Hi\");", parsed.get(1).getContent());
        assertEquals("Java", parsed.get(1).getExtra());

        assertEquals("id-3", parsed.get(2).getId());
        assertEquals(PageBlock.TYPE_IMAGE, parsed.get(2).getType());
        assertEquals("C:/images/test.png", parsed.get(2).getContent());
        assertEquals("Diagram 1", parsed.get(2).getExtra());

        // Legacy fallback test
        List<PageBlock> legacy = PageBlock.deserializeList("Plain text note without json brackets");
        assertEquals(1, legacy.size());
        assertEquals(PageBlock.TYPE_TEXT, legacy.get(0).getType());
        assertEquals("Plain text note without json brackets", legacy.get(0).getContent());
    }

    @Test
    public void testRoutineTaskItemParsingAndCountdown() {
        // 1. Parsing various formats
        java.time.LocalDateTime dt1 = RoutineTaskItem.parseDeadline("2026-10-15 (11:59 PM)");
        assertEquals(2026, dt1.getYear());
        assertEquals(10, dt1.getMonthValue());
        assertEquals(15, dt1.getDayOfMonth());
        assertEquals(23, dt1.getHour());
        assertEquals(59, dt1.getMinute());

        java.time.LocalDateTime dt2 = RoutineTaskItem.parseDeadline("2026-12-01 (02:30 PM)");
        assertEquals(14, dt2.getHour());
        assertEquals(30, dt2.getMinute());

        java.time.LocalDateTime dt3 = RoutineTaskItem.parseDeadline("2026-11-20 (09:15 AM)");
        assertEquals(9, dt3.getHour());
        assertEquals(15, dt3.getMinute());

        java.time.LocalDateTime dt4 = RoutineTaskItem.parseDeadline("2026-11-20");
        assertEquals(23, dt4.getHour());
        assertEquals(59, dt4.getMinute());

        // 2. Formatting & Countdown
        RoutineTaskItem futureTask = new RoutineTaskItem(1, "CSE2008", "SH", "Monday", "08:30 - 09:50", "Quiz 1",
                java.time.LocalDateTime.now().plusDays(3).plusHours(5).plusMinutes(3).plusSeconds(32)
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd (hh:mm a)", java.util.Locale.ENGLISH)));

        String countdown = futureTask.getFormattedCountdown();
        assertNotNull(countdown);
        assertTrue(countdown.contains("days") || countdown.contains("hrs"), "Countdown should show remaining time: " + countdown);
        assertEquals(RoutineTaskItem.Urgency.NORMAL, futureTask.getUrgencyLevel());

        // 3. Urgent Task (< 24 hours left)
        RoutineTaskItem urgentTask = new RoutineTaskItem(2, "MATH101", "AK", "Tuesday", "10:00 - 11:20", "CT 2",
                java.time.LocalDateTime.now().plusHours(5).plusMinutes(10)
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd (hh:mm a)", java.util.Locale.ENGLISH)));
        assertEquals(RoutineTaskItem.Urgency.URGENT, urgentTask.getUrgencyLevel());
        assertTrue(urgentTask.getFormattedCountdown().contains("hrs"));

        // 4. Overdue Task (Past deadline)
        RoutineTaskItem overdueTask = new RoutineTaskItem(3, "PHY102", "MS", "Wednesday", "01:30 PM - 02:50 PM", "Assignment 1",
                java.time.LocalDateTime.now().minusHours(2).minusMinutes(15)
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd (hh:mm a)", java.util.Locale.ENGLISH)));
        assertEquals(RoutineTaskItem.Urgency.OVERDUE, overdueTask.getUrgencyLevel());
        assertTrue(overdueTask.getFormattedCountdown().contains("Overdue"));

        // 5. Sorting by nearest deadline first
        List<RoutineTaskItem> taskList = new ArrayList<>();
        taskList.add(futureTask);
        taskList.add(overdueTask);
        taskList.add(urgentTask);

        Collections.sort(taskList);
        assertEquals(overdueTask, taskList.get(0));
        assertEquals(urgentTask, taskList.get(1));
        assertEquals(futureTask, taskList.get(2));
    }

    @Test
    public void testClockCountdownColorThemesAndActivityDeletion() {
        // 1. Clock Countdown format: "Due in DD : HH : MM : SS"
        RoutineTaskItem task1 = new RoutineTaskItem(101, 1, "Math 2207", "AK", "Monday", "08:30 - 09:50", "Quiz 1",
                java.time.LocalDateTime.now().plusDays(3).plusHours(5).plusMinutes(3).plusSeconds(32)
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd (hh:mm a)", java.util.Locale.ENGLISH)));

        assertEquals(101, task1.getActivityId());
        String clockCountdown = task1.getFormattedClockCountdown();
        assertNotNull(clockCountdown);
        assertTrue(clockCountdown.startsWith("Due in "), "Clock format should start with 'Due in ': " + clockCountdown);
        assertTrue(clockCountdown.matches("Due in \\d{2} : \\d{2} : \\d{2} : \\d{2}"), "Format should be DD : HH : MM : SS: " + clockCountdown);

        // 2. Expired Task format: "Ended : Overdue ..." and Ash theme
        RoutineTaskItem expiredTask = new RoutineTaskItem(102, 1, "Math 2207", "AK", "Monday", "08:30 - 09:50", "CT 1",
                java.time.LocalDateTime.now().minusHours(2).minusMinutes(15)
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd (hh:mm a)", java.util.Locale.ENGLISH)));
        assertTrue(expiredTask.getFormattedClockCountdown().startsWith("Ended"), "Expired task should show Ended");

        RoutineTaskItem.TaskColorTheme ashTheme = expiredTask.getColorTheme();
        assertTrue(ashTheme.isAsh(), "Theme should be Ash when time has ended");
        assertEquals("#94a3b8", ashTheme.getAccentColor());
        assertEquals("#f8fafc", ashTheme.getCardBg());

        // 3. Color theme progression:
        // > 10 Days: Calm Emerald Green
        RoutineTaskItem day12Task = new RoutineTaskItem(103, 1, "CSE2008", "SH", "Monday", "08:30 - 09:50", "Project",
                java.time.LocalDateTime.now().plusDays(12)
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd (hh:mm a)", java.util.Locale.ENGLISH)));
        RoutineTaskItem.TaskColorTheme theme12 = day12Task.getColorTheme();
        assertFalse(theme12.isAsh());
        assertEquals("#059669", theme12.getAccentColor());

        // 8 Days: Fresh Leaf Green
        RoutineTaskItem day8Task = new RoutineTaskItem(104, 1, "CSE2008", "SH", "Monday", "08:30 - 09:50", "CT 2",
                java.time.LocalDateTime.now().plusDays(8)
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd (hh:mm a)", java.util.Locale.ENGLISH)));
        assertEquals("#16a34a", day8Task.getColorTheme().getAccentColor());

        // 5 Days: Golden Amber / Yellow
        RoutineTaskItem day5Task = new RoutineTaskItem(105, 1, "CSE2008", "SH", "Monday", "08:30 - 09:50", "Assignment",
                java.time.LocalDateTime.now().plusDays(5)
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd (hh:mm a)", java.util.Locale.ENGLISH)));
        assertEquals("#d97706", day5Task.getColorTheme().getAccentColor());

        // 2 Days: Coral Orange
        RoutineTaskItem day2Task = new RoutineTaskItem(106, 1, "CSE2008", "SH", "Monday", "08:30 - 09:50", "Quiz",
                java.time.LocalDateTime.now().plusDays(2)
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd (hh:mm a)", java.util.Locale.ENGLISH)));
        assertEquals("#ea580c", day2Task.getColorTheme().getAccentColor());

        // 12 Hours (< 24h): Dark Cherry Red
        RoutineTaskItem hour12Task = new RoutineTaskItem(107, 1, "CSE2008", "SH", "Monday", "08:30 - 09:50", "Final Paper",
                java.time.LocalDateTime.now().plusHours(12)
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd (hh:mm a)", java.util.Locale.ENGLISH)));
        assertEquals("#881337", hour12Task.getColorTheme().getAccentColor());

        // 4. Activity Deletion from Database
        long ts = System.currentTimeMillis();
        DatabaseHelper.registerUser("taskuser" + ts + "@gmail.com", "taskuser" + ts, "Password123!");
        User user = DatabaseHelper.authenticateUser("taskuser" + ts, "Password123!");
        assertNotNull(user);

        RoutineSlot slot = new RoutineSlot(user.getId(), "Monday", "08:30 - 09:50", "Math 2207", "AK");
        List<SpecialActivity> activities = new ArrayList<>();
        activities.add(new SpecialActivity("Midterm CT", "2026-10-20 (10:00 AM)"));
        slot.setActivities(activities);

        boolean saved = DatabaseHelper.saveRoutineSlot(slot);
        assertTrue(saved);

        Map<String, RoutineSlot> fetchedSlots = DatabaseHelper.getAllRoutineSlots(user.getId());
        RoutineSlot fetchedSlot = fetchedSlots.get("Monday|||08:30 - 09:50");
        assertNotNull(fetchedSlot);
        assertEquals(1, fetchedSlot.getActivities().size());

        int actId = fetchedSlot.getActivities().get(0).getId();
        assertTrue(actId > 0, "Saved activity should have positive auto-generated ID");

        // Delete activity
        boolean deleted = DatabaseHelper.deleteRoutineActivity(actId);
        assertTrue(deleted, "deleteRoutineActivity should return true");

        Map<String, RoutineSlot> refreshedSlots = DatabaseHelper.getAllRoutineSlots(user.getId());
        RoutineSlot refreshedSlot = refreshedSlots.get("Monday|||08:30 - 09:50");
        assertNotNull(refreshedSlot);
        assertEquals(0, refreshedSlot.getActivities().size(), "Activity should be deleted from slot");
    }

    @Test
    void testCalendarTasksAndDecoupledRoutine() {
        long ts = System.currentTimeMillis();
        DatabaseHelper.registerUser("caluser" + ts + "@gmail.com", "caluser" + ts, "Password123!");
        User user = DatabaseHelper.authenticateUser("caluser" + ts, "Password123!");
        assertNotNull(user);

        // 1. Create multiple calendar tasks
        int id1 = DatabaseHelper.createCalendarTask(
                user.getId(),
                "Midterm Review Presentation",
                "Math 2207",
                "Presentation",
                "2026-10-15",
                "10:30 AM",
                "Prepare slides 1 to 20"
        );
        assertTrue(id1 > 0, "First calendar task should be created successfully with valid id");

        int id2 = DatabaseHelper.createCalendarTask(
                user.getId(),
                "Lab Report 2",
                "CSE 2100",
                "Assignment",
                "2026-10-22",
                "11:59 PM",
                "Submit PDF via portal"
        );
        assertTrue(id2 > 0, "Second calendar task should be created successfully with valid id");

        // 2. Fetch user calendar tasks
        List<RoutineTaskItem> tasks = DatabaseHelper.getUserCalendarTasks(user.getId());
        assertEquals(2, tasks.size(), "Should have retrieved 2 calendar tasks");

        RoutineTaskItem task1 = tasks.get(0);
        assertEquals("Presentation - Midterm Review Presentation", task1.getActivityType());
        assertEquals("Midterm Review Presentation", task1.getParsedTitle());
        assertEquals("Math 2207", task1.getSubjectName());
        assertEquals("Prepare slides 1 to 20", task1.getNotes());
        assertNotNull(task1.getDeadlineDate());
        assertEquals("2026-10-15", task1.getDeadlineDate().toString());
        assertEquals("Thursday", task1.getWeekdayDisplay());
        assertEquals("10:30 AM", task1.getFormattedTime());

        RoutineTaskItem task2 = tasks.get(1);
        assertEquals("Assignment - Lab Report 2", task2.getActivityType());
        assertEquals("Lab Report 2", task2.getParsedTitle());
        assertEquals("CSE 2100", task2.getSubjectName());
        assertEquals("2026-10-22", task2.getDeadlineDate().toString());
        assertEquals("Thursday", task2.getWeekdayDisplay());

        // 2b. Update task 1
        boolean updated = DatabaseHelper.updateCalendarTask(
                task1.getActivityId(),
                "Updated Presentation Slides",
                "Math 2207 Advanced",
                "Project",
                "2026-10-18",
                "04:00 PM",
                "Include appendix and references"
        );
        assertTrue(updated, "updateCalendarTask should succeed");

        List<RoutineTaskItem> updatedTasks = DatabaseHelper.getUserCalendarTasks(user.getId());
        RoutineTaskItem updatedItem = updatedTasks.stream().filter(t -> t.getActivityId() == task1.getActivityId()).findFirst().orElse(null);
        assertNotNull(updatedItem);
        assertEquals("Project - Updated Presentation Slides", updatedItem.getActivityType());
        assertEquals("Math 2207 Advanced", updatedItem.getSubjectName());
        assertEquals("Include appendix and references", updatedItem.getNotes());
        assertEquals("Project", updatedItem.getParsedCategory());
        assertEquals("Updated Presentation Slides", updatedItem.getParsedTitle());
        assertEquals("2026-10-18", updatedItem.getDeadlineDate().toString());
        assertEquals("04:00 PM", updatedItem.getFormattedTime());

        // 3. Delete task 1
        boolean deleted1 = DatabaseHelper.deleteCalendarTask(task1.getActivityId());
        assertTrue(deleted1, "deleteCalendarTask should succeed");

        List<RoutineTaskItem> afterDelete = DatabaseHelper.getUserCalendarTasks(user.getId());
        assertEquals(1, afterDelete.size(), "Should have 1 task remaining after deletion");
        assertEquals("Assignment - Lab Report 2", afterDelete.get(0).getActivityType());
        assertEquals("Lab Report 2", afterDelete.get(0).getParsedTitle());

        // Delete task 2 via unified deleteRoutineActivity
        boolean deleted2 = DatabaseHelper.deleteRoutineActivity(task2.getActivityId());
        assertTrue(deleted2, "deleteRoutineActivity should delete from calendar_tasks too");

        List<RoutineTaskItem> emptyList = DatabaseHelper.getUserCalendarTasks(user.getId());
        assertEquals(0, emptyList.size(), "All calendar tasks should be deleted");

        // 4. Decoupled Routine Slot: routine slot without activities saves and retrieves cleanly
        RoutineSlot simpleSlot = new RoutineSlot(user.getId(), "Sunday", "10:00 - 11:20", "EEE 2101", "MM");
        boolean slotSaved = DatabaseHelper.saveRoutineSlot(simpleSlot);
        assertTrue(slotSaved, "Simple routine slot should save without activities");

        Map<String, RoutineSlot> slots = DatabaseHelper.getAllRoutineSlots(user.getId());
        RoutineSlot fetched = slots.get("Sunday|||10:00 - 11:20");
        assertNotNull(fetched);
        assertEquals("EEE 2101", fetched.getSubjectName());
        assertEquals("MM", fetched.getTeacherCode());
        assertTrue(fetched.getActivities() == null || fetched.getActivities().isEmpty(), "Slot should have no activities");
    }
}



