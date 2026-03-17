package com.example.edubridge.parent;

public class MaterialItem {
    public String id;
    public String title;
    public String subject;
    public String fileType;
    public String fileUrl;
    public String createdByName;
    public long createdAtMillis;

    public MaterialItem() {}

    public MaterialItem(String id, String title, String subject, String fileType, String fileUrl, String createdByName, long createdAtMillis) {
        this.id = id;
        this.title = title;
        this.subject = subject;
        this.fileType = fileType;
        this.fileUrl = fileUrl;
        this.createdByName = createdByName;
        this.createdAtMillis = createdAtMillis;
    }
}