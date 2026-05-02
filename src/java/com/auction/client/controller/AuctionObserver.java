package com.auction.server.controller;

import com.auction.shared.model.Observer;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionObservable {

    private final List<Observer> observers = new CopyOnWriteArrayList<>();

    private String auctionId;
    private double currentPrice = 0;

    public AuctionObservable(String auctionId) {
        this.auctionId = auctionId;
    }

    // =============================
    // SUBSCRIBE / UNSUBSCRIBE
    // =============================
    public void addObserver(Observer o) {
        observers.add(o);
    }

    public void removeObserver(Observer o) {
        observers.remove(o);
    }

    // =============================
    // PLACE BID (trigger realtime)
    // =============================
    public void placeBid(String bidder, double price) {

        if (price <= currentPrice) return;

        currentPrice = price;

        System.out.println("New bid: " + bidder + " -> " + price);

        notifyObservers(bidder);
    }

    // =============================
    // NOTIFY ALL CLIENTS
    // =============================
    private void notifyObservers(String bidder) {
        for (Observer o : observers) {
            o.update(auctionId, currentPrice, bidder);
        }
    }
}