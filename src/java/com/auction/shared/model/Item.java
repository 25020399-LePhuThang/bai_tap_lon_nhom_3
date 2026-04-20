package com.auction.shared.model;

import java.util.Date;
import java.io.Serializable;

public abstract class Item implements Entity, Serializable {
    private double currentPrice;
    private double minIncrement;
    private Date startTime;
    private Date endTime;
    private String lastBidderId;

    public Item() {
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
}