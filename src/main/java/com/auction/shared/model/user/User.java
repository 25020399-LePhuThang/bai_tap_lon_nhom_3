package com.auction.shared.model.user;

import com.auction.shared.model.Entity;
import com.google.gson.annotations.SerializedName;

import java.io.Serial;

public abstract class User implements Entity {
    @Serial
    private static final long serialVersionUID = 1L;
    @SerializedName("user_id")
    protected String id;
    @SerializedName("username")
    protected String username;
    @SerializedName("password")
    protected String password;
    @SerializedName("email")
    protected String email;
    @SerializedName("phone")
    protected String phoneNumber;
    @SerializedName("status")
    protected String status;
    @SerializedName("role")
    protected String role;


    public User(String id, String name, String password, String email, String phoneNumber, String status, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.role = role;
    }

    public User() {}

    public String getId() { return id; }
    @Override
    public void setId(String id) { this.id = id; }

    public String getName() { return username; }
    public void setName(String name) { this.username = name; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }


    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }


    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @Override
    public void printInfo() {
        System.out.println(username + " | " + id + " | Role: " + role);
    }
}