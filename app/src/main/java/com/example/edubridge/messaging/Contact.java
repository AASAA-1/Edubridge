package com.example.edubridge.messaging;

public class Contact {

    private String userId;
    private String fullName;
    private String userType;
    private String lastMessage;
    private long lastMessageTime;
    private int unreadCount;

    public Contact() {
    }

    public Contact(String userId, String fullName, String userType) {
        this.userId = userId;
        this.fullName = fullName;
        this.userType = userType;
        this.lastMessage = "";
        this.lastMessageTime = 0;
        this.unreadCount = 0;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
