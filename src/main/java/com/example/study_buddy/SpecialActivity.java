package com.example.study_buddy;

/**
 * Represents a special academic activity for a class (e.g., CT, Assignment Deadline, Project Showcase).
 */
public class SpecialActivity {
    private int id;
    private int slotId;
    private String activityType;
    private String deadlineInfo;

    public SpecialActivity(int id, int slotId, String activityType, String deadlineInfo) {
        this.id = id;
        this.slotId = slotId;
        this.activityType = activityType;
        this.deadlineInfo = deadlineInfo;
    }

    public SpecialActivity(String activityType, String deadlineInfo) {
        this(0, 0, activityType, deadlineInfo);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSlotId() {
        return slotId;
    }

    public void setSlotId(int slotId) {
        this.slotId = slotId;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getDeadlineInfo() {
        return deadlineInfo;
    }

    public void setDeadlineInfo(String deadlineInfo) {
        this.deadlineInfo = deadlineInfo;
    }
}
