package com.auction.shared.model.user;

import java.io.Serial;

public class Bidder extends User {
    @Serial
    private static final long serialVersionUID = 1L;

    String name;
    protected float balance=0;
    protected float maxAutoBidLimit;

    public Bidder(String id, String name,String password,String email,String phoneNumber,String status,String role) {
        super(id,name,password,email,phoneNumber, status,"Bidder");
        this.maxAutoBidLimit = 0;
    }

    // Constructor rỗng (Cần thiết cho Database/JSON)
    public Bidder() {
        super();
    }

    public float  getBalance(){ return balance; }
    public void setBalance(float balance){ this.balance=balance; }

    public float getMaxAutoBidLimit() { return maxAutoBidLimit; }
    public void setMaxAutoBidLimit(float maxAutoBidLimit) { this.maxAutoBidLimit = maxAutoBidLimit; }

    @Override
    public String getRole(){ return "bidder"; }

    @Override
    public void printInfo() {
        System.out.println(name);
    }

    @Override
    public String toString() {
        return "Bidder{" +
                "id='" + getId() + '\'' +
                ", username='" + getName() + '\'' +
                ", balance=" + balance + '\'' +
                '}';
    }
}