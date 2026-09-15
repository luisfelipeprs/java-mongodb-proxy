package com.example.mongoproxy.model;

import org.bson.Document;

public class User {

    private String id;
    private String name;
    private String email;

    public User() {
    }

    public User(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public static User fromDocument(Document doc) {
        User user = new User();
        Object objectId = doc.get("_id");
        user.setId(objectId != null ? objectId.toString() : null);
        user.setName(doc.getString("name"));
        user.setEmail(doc.getString("email"));
        return user;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
