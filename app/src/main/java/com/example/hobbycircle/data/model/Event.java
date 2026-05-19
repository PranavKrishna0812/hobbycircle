package com.example.hobbycircle.data.model;

import java.util.ArrayList;
import java.util.List;

public class Event {

    private String id;
    private String title;
    private String description;
    private String hobbyId;
    private String location;
    private String mapQuery;
    private long dateTime; // long timestamp
    private String creatorId;
    private String creatorName;
    private List<String> joinedUserIds;
    private String imageUrl;
    private long updatedAtMillis;

    public Event() {
        this.id = "";
        this.title = "";
        this.description = "";
        this.hobbyId = "";
        this.location = "";
        this.mapQuery = "";
        this.dateTime = 0L;
        this.creatorId = "";
        this.creatorName = "";
        this.joinedUserIds = new ArrayList<>();
        this.imageUrl = "";
        this.updatedAtMillis = 0L;
    }

    public Event(String id,
                 String title,
                 String description,
                 String hobbyId,
                 String location,
                 String mapQuery,
                 long dateTime,
                 String creatorId,
                 String creatorName,
                 List<String> joinedUserIds,
                 String imageUrl) {
        this.id = id != null ? id : "";
        this.title = title != null ? title : "";
        this.description = description != null ? description : "";
        this.hobbyId = hobbyId != null ? hobbyId : "";
        this.location = location != null ? location : "";
        this.mapQuery = mapQuery != null ? mapQuery : "";
        this.dateTime = Math.max(0L, dateTime);
        this.creatorId = creatorId != null ? creatorId : "";
        this.creatorName = creatorName != null ? creatorName : "";
        this.joinedUserIds = joinedUserIds != null ? joinedUserIds : new ArrayList<>();
        this.imageUrl = imageUrl != null ? imageUrl : "";
        this.updatedAtMillis = 0L;
    }

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id != null ? id : "";
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public void setTitle(String title) {
        this.title = title != null ? title : "";
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public String getHobbyId() {
        return hobbyId != null ? hobbyId : "";
    }

    public void setHobbyId(String hobbyId) {
        this.hobbyId = hobbyId != null ? hobbyId : "";
    }

    public String getLocation() {
        return location != null ? location : "";
    }

    public void setLocation(String location) {
        this.location = location != null ? location : "";
    }

    public String getMapQuery() {
        return mapQuery != null ? mapQuery : "";
    }

    public void setMapQuery(String mapQuery) {
        this.mapQuery = mapQuery != null ? mapQuery : "";
    }

    // New Fields Getters & Setters
    public long getDateTime() {
        return Math.max(0L, dateTime);
    }

    public void setDateTime(long dateTime) {
        this.dateTime = Math.max(0L, dateTime);
    }

    public String getCreatorId() {
        return creatorId != null ? creatorId : "";
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId != null ? creatorId : "";
    }

    public String getCreatorName() {
        return creatorName != null ? creatorName : "";
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName != null ? creatorName : "";
    }

    // Backward-compatible delegates for existing usages
    public long getEventTimeMillis() {
        return getDateTime();
    }

    public void setEventTimeMillis(long eventTimeMillis) {
        setDateTime(eventTimeMillis);
    }

    public String getCreatedByUserId() {
        return getCreatorId();
    }

    public void setCreatedByUserId(String createdByUserId) {
        setCreatorId(createdByUserId);
    }

    public List<String> getJoinedUserIds() {
        if (joinedUserIds == null) {
            joinedUserIds = new ArrayList<>();
        }
        return joinedUserIds;
    }

    public void setJoinedUserIds(List<String> joinedUserIds) {
        this.joinedUserIds = joinedUserIds != null ? joinedUserIds : new ArrayList<>();
    }

    public String getImageUrl() {
        return imageUrl != null ? imageUrl : "";
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl != null ? imageUrl : "";
    }

    public long getUpdatedAtMillis() {
        return Math.max(0L, updatedAtMillis);
    }

    public void setUpdatedAtMillis(long updatedAtMillis) {
        this.updatedAtMillis = Math.max(0L, updatedAtMillis);
    }
}