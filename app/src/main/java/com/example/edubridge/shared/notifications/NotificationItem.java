package com.example.edubridge.shared.notifications;

import com.google.firebase.Timestamp;

public class NotificationItem {
    public String    id;
    public String    userId;
    public String    senderID;
    public String    senderName;
    public long      count;
    public String    title;
    public String    body;
    public String    type;
    public boolean   read;
    public Timestamp createdAt;

    public NotificationItem() {}
}
