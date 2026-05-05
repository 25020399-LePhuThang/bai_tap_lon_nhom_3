package com.auction.shared.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class BidTransaction implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private AtomicInteger counter= new AtomicInteger(0);
    private  String transactionId;
    private String lastBidderId;
    private String itemId;
    private double bidAmount;
    private LocalDateTime timestamp;
    private String status;

    public BidTransaction(String name,double bidAmount) {
        transactionId= String.valueOf(counter.incrementAndGet());
        this.status = "ACTIVE"; // Cho nó hoạt động luôn
        this.bidAmount = bidAmount;
        this.timestamp = LocalDateTime.now();
    }

    /** Kiểm tra xem mức giá người dùng vừa đặt có hợp lệ không (lớn hơn giá hiện tại + bước giá min)
     *
     * @param currentHighestPrice
     * @param minIncrement
     * @return true / false
     */
    public boolean isValidBid(double currentHighestPrice, double minIncrement) {
        return this.bidAmount >= (currentHighestPrice + minIncrement);
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) {this.transactionId = transactionId;}

    public String getLastBidderId() { return  lastBidderId; }
    public void setLastBidderId(String lastBidderId) { this.lastBidderId = lastBidderId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public double getBidAmount() { return bidAmount; }
    public void setBidAmount(double bidAmount) { this.bidAmount = bidAmount; }

    public LocalDateTime getTimestamp() { return timestamp; }

}