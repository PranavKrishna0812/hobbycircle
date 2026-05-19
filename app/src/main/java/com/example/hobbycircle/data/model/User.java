package com.example.hobbycircle.data.model;

import java.util.ArrayList;
import java.util.List;

public class User {

    private String id;
    private String name;
    private String email;
    private String role;
    private String location;
    private String photoUrl;
    private long updatedAtMillis;
    private List<String> selectedHobbies;

    public User() {
        this.selectedHobbies = new ArrayList<>();
        this.role = "";
        this.location = "";
        this.photoUrl = "";
        this.updatedAtMillis = 0L;
    }

    public User(String id, String name, String email, List<String> selectedHobbies) {
        this(id, name, email, "", "", "", selectedHobbies);
    }

    public User(String id, String name, String email, String role, List<String> selectedHobbies) {
        this(id, name, email, role, "", "", selectedHobbies);
    }

    public User(String id, String name, String email, String role, String location,
                String photoUrl, List<String> selectedHobbies) {
        this.id = id != null ? id : "";
        this.name = name != null ? name : "";
        this.email = email != null ? email : "";
        this.role = role != null ? role : "";
        this.location = location != null ? location : "";
        this.photoUrl = photoUrl != null ? photoUrl : "";
        this.selectedHobbies = selectedHobbies != null ? selectedHobbies : new ArrayList<>();
        this.updatedAtMillis = 0L;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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
        return role != null ? role : "";
    }

    public void setRole(String role) {
        this.role = role != null ? role : "";
    }

    public String getLocation() {
        return location != null ? location : "";
    }

    public void setLocation(String location) {
        this.location = location != null ? location : "";
    }

    public String getPhotoUrl() {
        return photoUrl != null ? photoUrl : "";
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl != null ? photoUrl : "";
    }

    public long getUpdatedAtMillis() {
        return Math.max(0L, updatedAtMillis);
    }

    public void setUpdatedAtMillis(long updatedAtMillis) {
        this.updatedAtMillis = Math.max(0L, updatedAtMillis);
    }

    public List<String> getSelectedHobbies() {
        if (selectedHobbies == null) {
            selectedHobbies = new ArrayList<>();
        }
        return selectedHobbies;
    }

    public void setSelectedHobbies(List<String> selectedHobbies) {
        this.selectedHobbies = selectedHobbies != null ? selectedHobbies : new ArrayList<>();
    }
}