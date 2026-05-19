package com.example.hobbycircle.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey
    @NonNull
    private String id;

    private String name;
    private String email;
    private String role;
    private String location;
    private String selectedHobbiesCsv;
    private String photoUrl;
    private long updatedAtMillis;

    public UserEntity() {
        this.id = "";
        this.name = "";
        this.email = "";
        this.role = "";
        this.location = "";
        this.selectedHobbiesCsv = "";
        this.photoUrl = "";
        this.updatedAtMillis = 0L;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id != null ? id : "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email : "";
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role != null ? role : "";
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location != null ? location : "";
    }

    public String getSelectedHobbiesCsv() {
        return selectedHobbiesCsv;
    }

    public void setSelectedHobbiesCsv(String selectedHobbiesCsv) {
        this.selectedHobbiesCsv = selectedHobbiesCsv != null ? selectedHobbiesCsv : "";
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl != null ? photoUrl : "";
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    public void setUpdatedAtMillis(long updatedAtMillis) {
        this.updatedAtMillis = Math.max(0L, updatedAtMillis);
    }
}
