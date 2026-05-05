package com.example.edubridge.parent;

import com.google.firebase.Timestamp;

public class GameItem {
    public String id;
    public String title;
    public String description;
    public String url;
    public String teacherId;
    public Timestamp timestamp;
    public String assignedClass;

    public GameItem() {}

    public GameItem(String id, String title, String description, String url,
                    String teacherId, Timestamp timestamp, String assignedClass) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.url = url;
        this.teacherId = teacherId;
        this.timestamp = timestamp;
        this.assignedClass = assignedClass;
    }
}
