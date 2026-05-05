package com.auction.shared.model;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.Serial;

public abstract class Item implements Entity {
    @Serial
    private static final  long  serialVersionUID = 1L;

    protected static AtomicInteger counter = new AtomicInteger(0);
    protected double startingPrice;
    protected double currentPrice;
    protected double minIncrement;
    protected String sellerId;
    protected String lastBidderId;
    protected String name;
    /**
     * Trạng thái hiện tại của sản phẩm.
     * Các giá trị hợp lệ bao gồm:
     * <ul>
     * <li>{@code "ACTIVE"}    : Đang có thể đấu giá.</li>
     * <li>{@code "SOLD"} : Đã bán.</li>
     * <li>{@code "DELETED"}  : Sản phẩm bị hủy do vi phạm.</li>
     * </ul>
     */
    protected String status;
    protected String itemID;
    protected Date startTime;
    protected Date endTime;
    protected String productImageURL;

    public Item() {
    }

    public Item(String name,double startingPrice, double minIncrement) {
        itemID= String.valueOf(counter.incrementAndGet());
        this.name = name;
        this.startingPrice= startingPrice;
        this.currentPrice = startingPrice;
        this.minIncrement = minIncrement;
        this.status = "ACTIVE"; // Cho nó hoạt động luôn
        this.startTime = new Date();
        // Cho kết thúc sau 2 phút để test Anti-Sniping
        this.endTime = new Date(System.currentTimeMillis() + 120000);
    }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double newStartingPrice) { this.startingPrice = newStartingPrice; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getMinIncrement() { return minIncrement; }
    public void setMinIncrement(double minIncrement) { this.minIncrement = minIncrement; }

    public String getLastBidderId() { return lastBidderId; }
    public void setLastBidderId(String lastBidderId) { this.lastBidderId = lastBidderId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getId(){return itemID;}
    public void setId(String newID){itemID=newID;}

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public String  getProductImageURL() { return productImageURL; }
    public void setProductImageURL(String productImageURL) { this.productImageURL = productImageURL; }

    /** Kiểm tra xem phiên đấu giá còn hoạt động không?
     *
     * @return true / false
     */
    public boolean isActive() {
        long currentTime = System.currentTimeMillis();
        return status.equals("ACTIVE") && currentTime >= startTime.getTime() && currentTime <= endTime.getTime();
    }

    
}