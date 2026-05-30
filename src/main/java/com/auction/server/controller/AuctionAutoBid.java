package com.auction.server.controller;

import com.auction.shared.model.AutoBid;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Quản lý Auto-Bidding cho một phiên đấu giá.
 */
public class AuctionAutoBid {

    private double currentPrice;
    private String currentWinnerId;
    private final ReentrantLock lock = new ReentrantLock();
    private final List<AutoBid> autoBids = new ArrayList<>();

    public AuctionAutoBid(double currentPrice, String currentWinnerId) {
        this.currentPrice = currentPrice;
        this.currentWinnerId = currentWinnerId;
    }

    public AutoBidResult registerAutoBid(AutoBid newBid) {
        lock.lock();
        try {
            if (newBid.getBidderId().equals(currentWinnerId)) {
                return new AutoBidResult(currentPrice, currentWinnerId, false,
                        "Bạn đang là người dẫn đầu, không cần đăng ký auto-bid lúc này.");
            }

            if (newBid.getMaxBid() <= currentPrice) {
                return new AutoBidResult(currentPrice, currentWinnerId, false,
                        "maxBid phải lớn hơn giá hiện tại (" + currentPrice + ").");
            }

            autoBids.removeIf(b -> b.getBidderId().equals(newBid.getBidderId()));
            autoBids.add(newBid);
            return processAutoBidding();
        } finally {
            lock.unlock();
        }
    }

    public AutoBidResult onManualBidPlaced(double newPrice, String newWinner) {
        lock.lock();
        try {
            this.currentPrice = newPrice;
            this.currentWinnerId = newWinner;
            if (autoBids.isEmpty()) {
                return new AutoBidResult(currentPrice, currentWinnerId, false, null);
            }
            return processAutoBidding();
        } finally {
            lock.unlock();
        }
    }

    private AutoBidResult processAutoBidding() {
        boolean anyChange = true;
        boolean actualPriceChanged = false;

        while (anyChange) {
            anyChange = false;

            AutoBid challenger = findBestChallenger();
            if (challenger == null) break;

            double challengerBid = currentPrice + challenger.getIncrement();

            if (challengerBid > challenger.getMaxBid()) {
                break;
            }

            AutoBid currentWinnerBid = autoBids.stream()
                    .filter(b -> b.getBidderId().equals(currentWinnerId))
                    .findFirst().orElse(null);

            if (currentWinnerBid != null
                    && currentWinnerBid.getMaxBid() == challenger.getMaxBid()
                    && currentWinnerBid.getTimestamp() < challenger.getTimestamp()) {
                break;
            }

            if (currentWinnerBid != null) {
                if (currentWinnerBid.getMaxBid() >= challenger.getMaxBid()) {
                    double targetPrice = challenger.getMaxBid() + challenger.getIncrement();
                    if (targetPrice > currentWinnerBid.getMaxBid()) {
                        targetPrice = currentWinnerBid.getMaxBid();
                    }
                    if (targetPrice > currentPrice) {
                        currentPrice = targetPrice;
                        actualPriceChanged = true;
                    }
                    anyChange = false;
                } else {
                    double targetPrice = currentWinnerBid.getMaxBid() + challenger.getIncrement();
                    if (targetPrice > challenger.getMaxBid()) {
                        targetPrice = challenger.getMaxBid();
                    }
                    currentPrice = targetPrice;
                    currentWinnerId = challenger.getBidderId();
                    actualPriceChanged = true;
                    anyChange = true;
                }
            } else {
                currentPrice = challengerBid;
                currentWinnerId = challenger.getBidderId();
                actualPriceChanged = true;
                anyChange = true;
            }

            System.out.printf("[AutoBid] %s đặt %.0f (maxBid=%.0f)%n",
                    currentWinnerId, currentPrice, challenger.getMaxBid());
        }

        String msg = String.format("AutoBid: %s dẫn đầu với %.0f", currentWinnerId, currentPrice);
        return new AutoBidResult(currentPrice, currentWinnerId, actualPriceChanged, msg);
    }

    private AutoBid findBestChallenger() {
        AutoBid best = null;
        for (AutoBid bid : autoBids) {
            if (bid.getBidderId().equals(currentWinnerId)) continue;
            if (bid.getMaxBid() < currentPrice + bid.getIncrement()) continue;

            if (best == null) {
                best = bid;
            } else if (bid.getMaxBid() > best.getMaxBid()) {
                best = bid;
            } else if (bid.getMaxBid() == best.getMaxBid()
                    && bid.getTimestamp() < best.getTimestamp()) {
                best = bid;
            }
        }
        return best;
    }

    public void cancelAutoBid(String bidderId) {
        lock.lock();
        try {
            autoBids.removeIf(b -> b.getBidderId().equals(bidderId));
        } finally {
            lock.unlock();
        }
    }

    public double getCurrentPrice() {
        lock.lock();
        try { return currentPrice; } finally { lock.unlock(); }
    }

    public String getCurrentWinnerId() {
        lock.lock();
        try { return currentWinnerId; } finally { lock.unlock(); }
    }

    public static class AutoBidResult {
        public final double finalPrice;
        public final String finalWinnerId;
        public final boolean priceChanged;
        public final String message;

        public AutoBidResult(double finalPrice, String finalWinnerId,
                             boolean priceChanged, String message) {
            this.finalPrice = finalPrice;
            this.finalWinnerId = finalWinnerId;
            this.priceChanged = priceChanged;
            this.message = message;
        }
    }

    public List<AutoBid> getAutoBids() {
        return this.autoBids;
    }
}