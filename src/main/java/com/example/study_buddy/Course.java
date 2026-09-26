package com.example.study_buddy;

/**
 * Domain model representing an academic course (e.g. CSE 2100: Object-Oriented Programming).
 */
public class Course {

    private int id;
    private int userId;
    private String courseCode;
    private String courseTitle;
    private String createdAt;

    public Course() {}

    public Course(int id, int userId, String courseCode, String courseTitle, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.createdAt = createdAt;
    }

    public Course(int userId, String courseCode, String courseTitle) {
        this(0, userId, courseCode, courseTitle, null);
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

    public String getCourseCode() {
        return courseCode != null ? courseCode : "";
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseTitle() {
        return courseTitle != null ? courseTitle : "";
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return getCourseCode() + " - " + getCourseTitle();
    }
}
