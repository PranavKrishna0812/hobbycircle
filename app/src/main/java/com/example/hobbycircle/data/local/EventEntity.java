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
    private long dateTime; // long timestamp
    private String creatorId;
    private String creatorName;
    private String joinedUserIdsCsv;
    private long updatedAtMillis;
    private String imageUrl;
    private String ratingsCsv;

    public EventEntity() {
        this.id = "";
        this.title = "";
        this.description = "";
        this.hobbyId = "";
        this.location = "";
        this.mapQuery = "";
        this.dateTime = 0L;
        this.creatorId = "";
        this.creatorName = "";
        this.joinedUserIdsCsv = "";
        this.updatedAtMillis = 0L;
        this.imageUrl = "";
        this.ratingsCsv = "";
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

    // New Fields Getters & Setters
    public long getDateTime() {
        return dateTime;
    }

    public void setDateTime(long dateTime) {
        this.dateTime = Math.max(0L, dateTime);
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId != null ? creatorId : "";
    }

    public String getCreatorName() {
        return creatorName;
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

    public String getRatingsCsv() {
        return ratingsCsv != null ? ratingsCsv : "";
    }

    public void setRatingsCsv(String ratingsCsv) {
        this.ratingsCsv = ratingsCsv != null ? ratingsCsv : "";
    }
}