package com.example.hobbycircle.data.model;

public class Warning {

    private String id;
    private String eventId;
    private String eventTitle;
    private String creatorId;
    private String message;
    private long timestamp;
    private boolean read;

    public Warning() {
        this.id = "";
        this.eventId = "";
        this.eventTitle = "";
        this.creatorId = "";
        this.message = "";
        this.timestamp = 0L;
        this.read = false;
    }

    public Warning(String id, String eventId, String eventTitle, String creatorId, String message, long timestamp, boolean read) {
        this.id = id != null ? id : "";
        this.eventId = eventId != null ? eventId : "";
        this.eventTitle = eventTitle != null ? eventTitle : "";
        this.creatorId = creatorId != null ? creatorId : "";
        this.message = message != null ? message : "";
        this.timestamp = timestamp;
        this.read = read;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id != null ? id : "";
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId != null ? eventId : "";
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle != null ? eventTitle : "";
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId != null ? creatorId : "";
    }

    public String getMessage() {
        return message;
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

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
