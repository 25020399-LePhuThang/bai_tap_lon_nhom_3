package com.auction.shared.model.user;

import java.io.Serial;

public class Bidder extends User {
    @Serial
    private static final long serialVersionUID = 1L;

    String name;
    protected float balance=0;
    protected float maxAutoBidLimit;

    public Bidder(String name) {
        this.name = name;
        this.maxAutoBidLimit = 0;
    }

    public float  getBalance(){ return balance; }
    public void setBalance(float balance){ this.balance=balance; }

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