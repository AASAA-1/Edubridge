package com.example.edubridge.parent;

public class AnnouncementItem {
    public String id;
    public String title;
    public String body;
    public String date;
    public String createdByName;

    public AnnouncementItem() {}

    public AnnouncementItem(String id, String title, String body, String date, String createdByName) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.date = date;
        this.createdByName = createdByName;
    }
}
