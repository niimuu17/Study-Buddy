package com.example.study_buddy;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain model representing a chapter in a course syllabus, containing subtopics.
 */
public class SyllabusChapter {

    private int id;
    private int courseId;
    private int chapterNumber;
    private String title;
    private List<SyllabusTopic> topics = new ArrayList<>();

    public SyllabusChapter() {}

    public SyllabusChapter(int id, int courseId, int chapterNumber, String title) {
        this.id = id;
        this.courseId = courseId;
        this.chapterNumber = chapterNumber;
        this.title = title;
    }

    public SyllabusChapter(int chapterNumber, String title) {
        this(0, 0, chapterNumber, title);
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

    public int getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(int chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<SyllabusTopic> getTopics() {
        return topics;
    }

    public void setTopics(List<SyllabusTopic> topics) {
        this.topics = topics != null ? topics : new ArrayList<>();
    }

    public int getCompletedTopicsCount() {
        int count = 0;
        for (SyllabusTopic t : topics) {
            if (t.isCompleted()) count++;
        }
        return count;
    }

    public int getTotalTopicsCount() {
        return topics.size();
    }
}
