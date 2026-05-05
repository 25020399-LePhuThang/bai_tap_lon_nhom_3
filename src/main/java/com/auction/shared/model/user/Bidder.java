package com.auction.shared.model.user;
public class Bidder extends User {
    String name;
    protected float balance=0;
    protected

    public Bidder(String name) {
        this.name = name;
    }

    public String deposit(float amount){
        if(amount <=10000) {
            if (Float.isInfinite(balance+amount)){
                return "BALANCE LIMIT REACHED";
            }
            else{
                balance+=amount;
                return "Đã nạp " + amount + " USD";
            }
        }
        else { return "Không thể nạp quá 10,000 USD 1 lần"; }
    }

    @Override
    public void printInfo() {
        System.out.println(name);
    }
}