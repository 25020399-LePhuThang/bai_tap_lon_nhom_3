package com.auction.shared.model;

public interface Observer {
    void update(String auctionId, double newPrice, String bidder);
}