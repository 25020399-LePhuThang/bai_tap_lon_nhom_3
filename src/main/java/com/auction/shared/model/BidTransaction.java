package com.auction.shared.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class BidTransaction implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final AtomicInteger counter = new AtomicInteger(0);
    private String transactionId;
    private String bidderId;
    private String itemId;
    private double bidAmount;
    private LocalDateTime timestamp;
    private String status;

    public BidTransaction(String bidderId, String itemId, double bidAmount) {
        this.bidderId = bidderId;
        this.itemId = itemId;
        transactionId = String.valueOf(counter.incrementAndGet());
        this.status = "ACTIVE"; // Cho nó hoạt động luôn
        this.bidAmount = bidAmount;
        this.timestamp = LocalDateTime.now();
    }

    public BidTransaction() {
    }

    /**
     * Kiểm tra xem mức giá người dùng vừa đặt có hợp lệ không (lớn hơn giá hiện tại + bước giá min)
     *
     * @param currentHighestPrice Giá cao nhất hiện tại
     * @param minIncrement        Bước giá tối thiểu
     * @return true / false
     */
    public boolean isValidBid(double currentHighestPrice, double minIncrement) {
        return this.bidAmount >= (currentHighestPrice + minIncrement);
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "BidTransaction{" +
                "id='" + transactionId + '\'' +
                ", bidderId='" + bidderId + '\'' +
                ", itemId='" + itemId + '\'' +
                ", amount=" + bidAmount +
                ", time=" + timestamp +
                ", status='" + status + '\'' +
                '}';
    }

}