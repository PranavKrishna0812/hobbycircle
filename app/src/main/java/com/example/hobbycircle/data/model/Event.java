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
    private long eventTimeMillis;
    private String createdByUserId;
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
        this.eventTimeMillis = 0L;
        this.createdByUserId = "";
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
                 long eventTimeMillis,
                 String createdByUserId,
                 List<String> joinedUserIds,
                 String imageUrl) {
        this.id = id != null ? id : "";
        this.title = title != null ? title : "";
        this.description = description != null ? description : "";
        this.hobbyId = hobbyId != null ? hobbyId : "";
        this.location = location != null ? location : "";
        this.mapQuery = mapQuery != null ? mapQuery : "";
        this.eventTimeMillis = Math.max(0L, eventTimeMillis);
        this.createdByUserId = createdByUserId != null ? createdByUserId : "";
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

    public long getEventTimeMillis() {
        return Math.max(0L, eventTimeMillis);
    }

    public void setEventTimeMillis(long eventTimeMillis) {
        this.eventTimeMillis = Math.max(0L, eventTimeMillis);
    }

    public String getCreatedByUserId() {
        return createdByUserId != null ? createdByUserId : "";
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId != null ? createdByUserId : "";
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