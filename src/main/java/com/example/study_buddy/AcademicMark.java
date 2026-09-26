package com.example.study_buddy;

/**
 * Domain model representing an academic assessment mark (e.g. CT, CT Assignment, Lab Test, Lab Quiz).
 */
public class AcademicMark {

    private int id;
    private int courseId;
    private String assessmentType; // 'CT', 'CT Assignment', 'Lab Test', 'Lab Quiz'
    private String assessmentName; // e.g. 'CT 1'
    private double obtainedMarks;
    private double totalMarks;
    private String examDate;
    private String createdAt;

    public AcademicMark() {}

    public AcademicMark(int id, int courseId, String assessmentType, String assessmentName,
                        double obtainedMarks, double totalMarks, String examDate, String createdAt) {
        this.id = id;
        this.courseId = courseId;
        this.assessmentType = assessmentType;
        this.assessmentName = assessmentName;
        this.obtainedMarks = obtainedMarks;
        this.totalMarks = totalMarks;
        this.examDate = examDate;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getAssessmentType() {
        return assessmentType != null ? assessmentType : "Assessment";
    }

    public void setAssessmentType(String assessmentType) {
        this.assessmentType = assessmentType;
    }

    public String getAssessmentName() {
        return assessmentName != null ? assessmentName : "";
    }

    public void setAssessmentName(String assessmentName) {
        this.assessmentName = assessmentName;
    }

    public double getObtainedMarks() {
        return obtainedMarks;
    }

    public void setObtainedMarks(double obtainedMarks) {
        this.obtainedMarks = obtainedMarks;
    }

    public double getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(double totalMarks) {
        this.totalMarks = totalMarks;
    }

    public String getExamDate() {
        return examDate;
    }

    public void setExamDate(String examDate) {
        this.examDate = examDate;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public double getPercentage() {
        if (totalMarks <= 0) return 0.0;
        return (obtainedMarks / totalMarks) * 100.0;
    }
}
