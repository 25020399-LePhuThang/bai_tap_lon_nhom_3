package com.auction.shared.model.user;

import com.auction.shared.model.Item;

import java.util.ArrayList;
import java.util.List;
import java.io.Serial;

public class Seller extends User{
    @Serial
    private static final long serialVersionUID = 1L;

    private double rating=0.0;
    protected float balance=0;

    public Seller(String id, String name,String password,String email,long phoneNumber,String status){
        super(id,name,password,email,phoneNumber,status);
    }

    public Seller() {super();}

    public double  getRating() {
        return rating;
    }
    public void setRating(double rating) {this.rating=rating;}

    public float getBalance() { return balance; }
    public void setBalance(float balance) {}

    @Override
    public String getRole(){ return "seller"; }

    @Override
    public String toString() {
        return "Seller{" + super.toString()+'\''+
                "rating=" + rating +'\''+
                ", balance=" + balance +'\''+
                "} ";
    }
}