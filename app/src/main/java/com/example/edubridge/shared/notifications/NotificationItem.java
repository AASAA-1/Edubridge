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
    /** For announcements/events: the Firestore document ID of the target. */
    public String    refId;
    /** For announcements/events: the date string shown in the detail screen. */
    public String    refDate;

    public NotificationItem() {}
}
