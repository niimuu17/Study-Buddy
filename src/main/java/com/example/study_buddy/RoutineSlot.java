package com.example.study_buddy;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single routine slot for a specific day and time duration.
 */
public class RoutineSlot {
    private int id;
    private int userId;
    private String dayOfWeek;
    private String timeSlot;
    private String subjectName;
    private String teacherCode;
    private List<SpecialActivity> activities;

    public RoutineSlot(int id, int userId, String dayOfWeek, String timeSlot, String subjectName, String teacherCode) {
        this.id = id;
        this.userId = userId;
        this.dayOfWeek = dayOfWeek;
        this.timeSlot = timeSlot;
        this.subjectName = subjectName;
        this.teacherCode = teacherCode;
        this.activities = new ArrayList<>();
    }

    public RoutineSlot(int userId, String dayOfWeek, String timeSlot, String subjectName, String teacherCode) {
        this(0, userId, dayOfWeek, timeSlot, subjectName, teacherCode);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getTeacherCode() {
        return teacherCode;
    }

    public void setTeacherCode(String teacherCode) {
        this.teacherCode = teacherCode;
    }

    public List<SpecialActivity> getActivities() {
        return activities;
    }

    public void setActivities(List<SpecialActivity> activities) {
        this.activities = activities != null ? activities : new ArrayList<>();
    }

    public boolean isEmpty() {
        return (subjectName == null || subjectName.trim().isEmpty()) &&
               (teacherCode == null || teacherCode.trim().isEmpty());
    }
}
