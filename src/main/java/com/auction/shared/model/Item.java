package com.auction.shared.model;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Item implements Entity {
    private static AtomicInteger counter = new AtomicInteger(0);
    private double currentPrice;
    private double minIncrement;
    private Date startTime;
    private Date endTime;
    private String lastBidderId;
    private String name;
    private String status;
    private String itemID;

    public Item() {
    }

    public Item(String name, double currentPrice, double minIncrement) {
        itemID= String.valueOf(counter.incrementAndGet());
        this.name = name;
        this.currentPrice = currentPrice;
        this.minIncrement = minIncrement;
        this.status = "ACTIVE"; // Cho nó hoạt động luôn
        this.startTime = new Date();
        // Cho kết thúc sau 2 phút để test Anti-Sniping
        this.endTime = new Date(System.currentTimeMillis() + 120000);
    }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getMinIncrement() { return minIncrement; }
    public void setMinIncrement(double minIncrement) { this.minIncrement = minIncrement; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public String getLastBidderId() { return lastBidderId; }
    public void setLastBidderId(String lastBidderId) { this.lastBidderId = lastBidderId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getId(){return itemID;}
    public void setId(String newID){itemID=newID;}
    
}