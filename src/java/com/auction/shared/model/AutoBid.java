package com.auction.shared.model;

public class AutoBid {
    private String bidderId;
    private double maxBid;
    private double increment;
    private long timestamp; // để ưu tiên

    public AutoBid(String bidderId, double maxBid, double increment) {
        this.bidderId = bidderId;
        this.maxBid = maxBid;
        this.increment = increment;
        this.timestamp = System.currentTimeMillis();
    }

    public String getBidderId() { return bidderId; }
    public double getMaxBid() { return maxBid; }
    public double getIncrement() { return increment; }
    public long getTimestamp() { return timestamp; }
}