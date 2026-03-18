package com.example.edubridge.parent;

public class HomeworkItem {
    public String id;
    public String title;
    public String body;
    public String subject;
    public String fileType;
    public String fileUrl;
    public String createdByName;
    public String dueDate;
    public long createdAtMillis;

    public HomeworkItem() {}

    public HomeworkItem(String id, String title, String body, String subject, String fileType, String fileUrl, String createdByName, String dueDate, long createdAtMillis) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.subject = subject;
        this.fileType = fileType;
        this.fileUrl = fileUrl;
        this.createdByName = createdByName;
        this.dueDate = dueDate;
        this.createdAtMillis = createdAtMillis;
    }
}