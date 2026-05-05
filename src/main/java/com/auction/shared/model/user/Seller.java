package com.auction.shared.model.user;

import com.auction.shared.model.Item;

import java.util.ArrayList;
import java.util.List;

public class Seller extends User{
    private double rating=0.0;
    private List<Item> addedItems;

    public Seller(String id, String name,String password,String email,long phoneNumber,String status){
        super(id,name,password,email,phoneNumber,status);
        addedItems = new ArrayList<>();
    }

    public void addItem(Item item){
        addedItems.add(item);
    }

    public double  getRating() {
        return rating;
    }
    public void setRating(double rating) {this.rating=rating;}

    public List<Item> getAddedItems() { return  addedItems; }
    public void setAddedItems(List<Item> addedItems) { this.addedItems = addedItems; }

}