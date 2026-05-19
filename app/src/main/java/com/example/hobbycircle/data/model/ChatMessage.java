package com.example.hobbycircle.data.model;

public class ChatMessage {
    private String senderId;
    private String senderName;
    private String message;
    private long timestamp;

    public ChatMessage() {
        this.senderId = "";
        this.senderName = "";
        this.message = "";
        this.timestamp = 0L;
    }

    public ChatMessage(String senderId, String senderName, String message, long timestamp) {
        this.senderId = senderId != null ? senderId : "";
        this.senderName = senderName != null ? senderName : "";
        this.message = message != null ? message : "";
        this.timestamp = timestamp;
    }

    public String getSenderId() {
        return senderId != null ? senderId : "";
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId != null ? senderId : "";
    }

    public String getSenderName() {
        return senderName != null ? senderName : "";
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName != null ? senderName : "";
    }

    public String getMessage() {
        return message != null ? message : "";
    }

    public void setMessage(String message) {
        this.message = message != null ? message : "";
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
