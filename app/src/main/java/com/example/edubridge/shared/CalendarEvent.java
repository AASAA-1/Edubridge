package com.example.edubridge.shared;

import java.util.Date;

public class CalendarEvent {
    private String id;
    private String title;
    private String description;
    private String type; // "Field Trip", "School Event", "Important", "Other"
    private String startAt;
    private String endAt;
    private String classId;
    private String className;
    private Date startDate;
    private Date endDate;

    public CalendarEvent() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStartAt() { return startAt; }
    public void setStartAt(String startAt) { this.startAt = startAt; }

    public String getEndAt() { return endAt; }
    public void setEndAt(String endAt) { this.endAt = endAt; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    // Helper method for display
    public String getTypeDisplayName() {
        if (type == null) return "Event";
        switch (type) {
            case "Field Trip": return "🚌 Field Trip";
            case "School Event": return "🎉 School Event";
            case "Important": return "⭐ Important";
            case "Other": return "📌 Other";
            default: return type;
        }
    }
}