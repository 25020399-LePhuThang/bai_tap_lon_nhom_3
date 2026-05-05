package com.auction.shared.model.user;

import java.io.Serial;
import java.util.concurrent.atomic.AtomicInteger;

public class Admin extends User{
    @Serial
    private static final long serialVersionUID = 1L;

    protected static AtomicInteger counter = new AtomicInteger(0);
    private String adminLevel;
    private String employeeId;

    public Admin(String id, String name,String password,String email,String phoneNumber,String status){
        super(id, name, password, email, phoneNumber, status, "ADMIN");
        adminLevel="MODERATOR";
        employeeId=String.valueOf(counter.getAndIncrement());
    }
    public Admin(){}

    public Admin() { super(); }

    public String getAdminLevel() { return adminLevel; }
    public void setAdminLevel(String adminLevel) { this.adminLevel = adminLevel; }

    public String getEmployeeId(){ return employeeId; }
    public void setEmployeeId(String employeeId){ this.employeeId = employeeId; }

    @Override
    public String getRole(){ return "admin";}

    @Override
    public String toString() {
        return "Admin{" +
                "id='" + getId() + '\'' +
                ",employee id="+ employeeId +'\'' +
                ", username='" + getName() + '\'' +
                ", level='" + adminLevel + '\'' +
                '}';
    }



}