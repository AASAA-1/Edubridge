package com.example.edubridge.messaging;

import com.google.firebase.Timestamp;

public class Message {

    private String messageId;
    private String senderID;
    private String receiverID;
    private String body;
    private Timestamp sentAt;
    private boolean read;

    public Message() {
        // empty constructor for Firestore
    }

    public Message(String senderID, String receiverID, String body) {
        this.senderID = senderID;
        this.receiverID = receiverID;
        this.body = body;
        this.read = false;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderID() {
        return senderID;
    }

    public void setSenderID(String senderID) {
        this.senderID = senderID;
    }

    public String getReceiverID() {
        return receiverID;
    }

    public void setReceiverID(String receiverID) {
        this.receiverID = receiverID;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Timestamp getSentAt() {
        return sentAt;
    }

    public void setSentAt(Timestamp sentAt) {
        this.sentAt = sentAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
