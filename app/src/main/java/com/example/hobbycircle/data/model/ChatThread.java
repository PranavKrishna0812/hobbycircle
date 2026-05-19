package com.example.hobbycircle.data.model;

public class ChatThread {
    private String id; // eventId_userId
    private String eventId;
    private String eventTitle;
    private String userId;
    private String userName;
    private String organiserId;
    private String organiserName;
    private String lastMessage;
    private long lastMessageTimestamp;

    public ChatThread() {
        this.id = "";
        this.eventId = "";
        this.eventTitle = "";
        this.userId = "";
        this.userName = "";
        this.organiserId = "";
        this.organiserName = "";
        this.lastMessage = "";
        this.lastMessageTimestamp = 0L;
    }

    public ChatThread(String id, String eventId, String eventTitle, String userId, String userName,
                      String organiserId, String organiserName, String lastMessage, long lastMessageTimestamp) {
        this.id = id != null ? id : "";
        this.eventId = eventId != null ? eventId : "";
        this.eventTitle = eventTitle != null ? eventTitle : "";
        this.userId = userId != null ? userId : "";
        this.userName = userName != null ? userName : "";
        this.organiserId = organiserId != null ? organiserId : "";
        this.organiserName = organiserName != null ? organiserName : "";
        this.lastMessage = lastMessage != null ? lastMessage : "";
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id != null ? id : "";
    }

    public String getEventId() {
        return eventId != null ? eventId : "";
    }

    public void setEventId(String eventId) {
        this.eventId = eventId != null ? eventId : "";
    }

    public String getEventTitle() {
        return eventTitle != null ? eventTitle : "";
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle != null ? eventTitle : "";
    }

    public String getUserId() {
        return userId != null ? userId : "";
    }

    public void setUserId(String userId) {
        this.userId = userId != null ? userId : "";
    }

    public String getUserName() {
        return userName != null ? userName : "";
    }

    public void setUserName(String userName) {
        this.userName = userName != null ? userName : "";
    }

    public String getOrganiserId() {
        return organiserId != null ? organiserId : "";
    }

    public void setOrganiserId(String organiserId) {
        this.organiserId = organiserId != null ? organiserId : "";
    }

    public String getOrganiserName() {
        return organiserName != null ? organiserName : "";
    }

    public void setOrganiserName(String organiserName) {
        this.organiserName = organiserName != null ? organiserName : "";
    }

    public String getLastMessage() {
        return lastMessage != null ? lastMessage : "";
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage != null ? lastMessage : "";
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }
}
