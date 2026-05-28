package com.auction.shared.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public class Auction implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // --- CÁC THUỘC TÍNH DỮ LIỆU CỐT LÕI (MODEL) ---
    private String auctionId;
    private String itemId;
    private String sellerId;

    private double startPrice;
    private double currentHighestPrice;
    private double minIncrement;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /**
     * Trạng thái hiện tại của phiên đấu giá.
     * Các giá trị hợp lệ bao gồm:
     * <ul>
     * <li>{@code "PENDING"}   : Phiên đấu giá mới tạo, đang chờ Admin phê duyệt.</li>
     * <li>{@code "ACTIVE"}    : Phiên đấu giá đang diễn ra, cho phép Bidder đặt giá.</li>
     * <li>{@code "COMPLETED"} : Phiên đấu giá đã kết thúc.</li>
     * <li>{@code "CANCELED"}  : Phiên đấu giá bị hủy do vi phạm hoặc không có ai mua.</li>
     * </ul>
     */
    private String status;
    private String currentWinnerId;

    // private transient List<Observer> observers = new ArrayList<>();

    // --- CONSTRUCTOR RỖNG (Cho Serialize/Database) ---
    public Auction() {
    }

    // --- CONSTRUCTOR ĐẦY ĐỦ ---
    public Auction(String auctionId, String itemId, String sellerId, double startPrice, double minIncrement, LocalDateTime endTime) {
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.startPrice = startPrice;
        this.currentHighestPrice = startPrice; // Mới tạo thì giá cao nhất = giá khởi điểm
        this.minIncrement = minIncrement;
        this.startTime = LocalDateTime.now();
        this.endTime = endTime;
        this.status = "PENDING";
        this.currentWinnerId = null; // Chưa ai mua
    }


    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public double getStartPrice() { return startPrice; }
    public void setStartPrice(double startPrice) { this.startPrice = startPrice; }

    public double getCurrentHighestPrice() { return currentHighestPrice; }
    public void setCurrentHighestPrice(double currentHighestPrice) { this.currentHighestPrice = currentHighestPrice; }

    public double getMinIncrement() { return minIncrement; }
    public void setMinIncrement(double minIncrement) { this.minIncrement = minIncrement; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrentWinnerId() { return currentWinnerId; }
    public void setCurrentWinnerId(String currentWinnerId) { this.currentWinnerId = currentWinnerId; }

    @Override
    public String toString() {
        return "Auction{" +
                "id='" + auctionId + '\'' +
                ", item='" + itemId + '\'' +
                ", currentPrice=" + currentHighestPrice +
                ", winner='" + currentWinnerId + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}