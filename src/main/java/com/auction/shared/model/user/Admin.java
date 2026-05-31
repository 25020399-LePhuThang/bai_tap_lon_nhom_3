package com.auction.shared.model.user;

import com.google.gson.annotations.SerializedName;

import java.io.Serial;

public class Admin extends User {
    @Serial
    private static final long serialVersionUID = 1L;

    @SerializedName("admin_level")
    private String adminLevel;

    public Admin(String id, String name, String password, String email, String phoneNumber, String status) {
        super(id, name, password, email, phoneNumber, status, "ADMIN");
        adminLevel = "MODERATOR";
    }

    public Admin() {
        super();
    }

    public String getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(String adminLevel) {
        this.adminLevel = adminLevel;
    }

    public String getEmployeeId() {
        return "EMP" + super.getId();
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public String toString() {
        return "Admin{" +
                "id='" + getId() + '\'' +
                ",employee id=" + getEmployeeId() + '\'' +
                ", username='" + getName() + '\'' +
                ", level='" + adminLevel + '\'' +
                '}';
    }


}