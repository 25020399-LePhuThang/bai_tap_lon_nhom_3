package com.auction.shared.model.user;

import com.auction.shared.model.Entity;
import java.io.Serial;

public abstract class User implements Entity {
    @Serial
    private static final long serialVersionUID = 1L;

    protected String id;
    protected String name;
    protected String password;
    protected String email;
    protected String phoneNumber;
    protected String status;
    protected String role;


    public User(String id, String name, String password, String email, String phoneNumber, String status, String role) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.role = role; // Gán giá trị role
    }

    public User() {}

    public String getId() { return id; }
    @Override
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

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
        System.out.println(name + " | " + id + " | Role: " + role);
    }
}