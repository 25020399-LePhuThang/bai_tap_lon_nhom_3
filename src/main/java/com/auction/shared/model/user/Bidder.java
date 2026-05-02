package com.auction.shared.model.user;
public class Bidder extends User {
    String name;

    public Bidder(String name) {
        this.name = name;
    }

    @Override
    public void printInfo() {
        System.out.println(name);
    }
}