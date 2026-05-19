package com.example.hobbycircle.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class EventEntity {

    @PrimaryKey
    @NonNull
    private String id;

    private String title;
    private String description;
    private String hobbyId;
    private String location;
    private String mapQuery;
    private long eventTimeMillis;
    private String createdByUserId;
    private String joinedUserIdsCsv;
    private long updatedAtMillis;
    private String imageUrl;

    public EventEntity() {
        this.id = "";
        this.title = "";
        this.description = "";
        this.hobbyId = "";
        this.location = "";
        this.mapQuery = "";
        this.createdByUserId = "";
        this.joinedUserIdsCsv = "";
        this.updatedAtMillis = 0L;
        this.imageUrl = "";
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id != null ? id : "";
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title != null ? title : "";
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public String getHobbyId() {
        return hobbyId;
    }

    public void setHobbyId(String hobbyId) {
        this.hobbyId = hobbyId != null ? hobbyId : "";
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location != null ? location : "";
    }

    public String getMapQuery() {
        return mapQuery;
    }

    public void setMapQuery(String mapQuery) {
        this.mapQuery = mapQuery != null ? mapQuery : "";
    }

    public long getEventTimeMillis() {
        return eventTimeMillis;
    }

    public void setEventTimeMillis(long eventTimeMillis) {
        this.eventTimeMillis = Math.max(0L, eventTimeMillis);
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId != null ? createdByUserId : "";
    }

    public String getJoinedUserIdsCsv() {
        return joinedUserIdsCsv;
    }

    public void setJoinedUserIdsCsv(String joinedUserIdsCsv) {
        this.joinedUserIdsCsv = joinedUserIdsCsv != null ? joinedUserIdsCsv : "";
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    public void setUpdatedAtMillis(long updatedAtMillis) {
        this.updatedAtMillis = Math.max(0L, updatedAtMillis);
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl != null ? imageUrl : "";
    }
}