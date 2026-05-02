package com.auction.shared.model.user;

import com.auction.shared.model.Entity;

import java.io.Serial;

public abstract class User implements Entity {
    @Serial
    private static final long serialVersionUID = 1L;

    protected String name;
    protected String password;
    protected String email;
    protected long phoneNumber;
    protected String status;
    protected String id;
    public User(String id, String name,String password,String email,long phoneNumber,String status) {
        this.name=name;
        this.password=password;
        this.email=email;
        this.phoneNumber=phoneNumber;
        this.status=status;
        this.id=id;
    }
    public User(){};

    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setPhonenumber(long phonenumber) {
        this.phoneNumber = phonenumber;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getPhonenumber() {
        return phoneNumber;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public void setId(String ID){ this.id=ID; }

    @Override
    public void printInfo(){ System.out.println(name+" | "+ id); }
}