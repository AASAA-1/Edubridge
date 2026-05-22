package com.example.edubridge.shared.messaging;

import com.google.firebase.Timestamp;

public class Message {

    public static final int TYPE_TEXT = 0;
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_FILE = 2;
    public static final int TYPE_VOICE = 3;

    private String messageId;
    private String senderID;
    private String receiverID;
    private String body;  // Text content or caption
    private int messageType = TYPE_TEXT;
    private String fileUrl;  // Storage URL for media/files
    private String fileName;  // Original file name
    private long fileSize;  // File size in bytes
    private int voiceDuration;  // Duration in seconds (voice messages)
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

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getVoiceDuration() {
        return voiceDuration;
    }

    public void setVoiceDuration(int voiceDuration) {
        this.voiceDuration = voiceDuration;
    }
}
