package com.auction.client.controller;

import com.auction.shared.model.AutoBid;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionAutoBid {

    private double currentPrice = 0;
    private String currentWinner = null;

    // Thread-safe
    private final ReentrantLock lock = new ReentrantLock();

    // danh sách auto bid
    private final List<AutoBid> autoBids = new ArrayList<>();

    // Đăng ký auto bid
    public void registerAutoBid(AutoBid bid) {
        lock.lock();
        try {
            autoBids.add(bid);
            processAutoBidding();
        } finally {
            lock.unlock();
        }
    }

    // CORE LOGIC
    private void processAutoBidding() {

        if (autoBids.isEmpty()) return;

        // sort theo:
        // 1. maxBid giảm dần
        // 2. timestamp tăng dần
        autoBids.sort((a, b) -> {
            if (Double.compare(b.getMaxBid(), a.getMaxBid()) == 0) {
                return Long.compare(a.getTimestamp(), b.getTimestamp());
            }
            return Double.compare(b.getMaxBid(), a.getMaxBid());
        });

        boolean changed;

        do {
            changed = false;

            for (AutoBid bid : autoBids) {

                // nếu chưa vượt maxBid thì tăng giá
                if (currentPrice + bid.getIncrement() <= bid.getMaxBid()) {
                    currentPrice += bid.getIncrement();
                    currentWinner = bid.getBidderId();
                    changed = true;

                    System.out.println(
                        "AutoBid: " + currentWinner + " -> " + currentPrice
                    );
                }
            }

        } while (changed); // lặp đến khi không ai tăng được nữa
    }

    // Getter
    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getCurrentWinner() {
        return currentWinner;
    }
}