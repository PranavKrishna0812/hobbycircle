package com.example.hobbycircle.data.model;

public class Hobby {

    private String id;
    private String name;
    private String description;

    public Hobby() {
        // Required empty constructor for Firestore and serialization
    }

    public Hobby(String id, String name, String description) {
        this.id = id != null ? id : "";
        this.name = name != null ? name : "";
        this.description = description != null ? description : "";
    }

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id != null ? id : "";
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }
}