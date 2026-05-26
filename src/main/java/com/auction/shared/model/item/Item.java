package com.auction.shared.model.item;

import com.auction.shared.model.Entity;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.io.Serial;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Item implements Entity, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    protected static AtomicInteger counter = new AtomicInteger(0);

    // --- Ánh xạ các trường khớp 100% với các cột trong SQLite ---
    @SerializedName("item_id")
    protected String itemID;

    @SerializedName("item_name")
    protected String name;

    @SerializedName("start_price")
    protected double startingPrice;

    @SerializedName("current_price")
    protected double currentPrice;

    @SerializedName("step_price")
    protected double minIncrement;

    @SerializedName("lastbidder_id")
    protected String lastBidderId;

    @SerializedName("seller_id")
    protected String seller_ID;

    @SerializedName("status")
    protected String status;

    @SerializedName("type")
    protected String type;

    @SerializedName("StartTime")
    protected Date startTime;

    @SerializedName("EndTime")
    protected Date endTime;

    @SerializedName("productImageURL")
    protected String productImageURL;

    public Item() {
    }

    public Item(String name, double startingPrice, double minIncrement) {
        this.itemID = String.valueOf(counter.incrementAndGet());
        this.name = name;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.minIncrement = minIncrement;
        this.status = "ACTIVE";
        this.startTime = new Date();
        // Kết thúc sau 2 phút để test phiên đấu giá
        this.endTime = new Date(System.currentTimeMillis() + 120000);
    }

    // =========================================================================
    // VÙNG SỬA LỖI: Giữ cả 2 cặp hàm để vừa lòng cả Entity lẫn JavaFX TableView
    // =========================================================================

    // 1. Cặp hàm bắt buộc tuân thủ theo hợp đồng interface Entity -> Giúp class con (Vehicle, Art, Electronic) HẾT ĐỎ
    @Override
    public String getId() {
        return itemID;
    }
    @Override
    public void setId(String newID) {
        this.itemID = newID;
    }

    // 2. Cặp hàm viết theo chuẩn camelCase -> Giúp JavaFX PropertyValueFactory tìm thấy để ĐỔ DỮ LIỆU LÊN CỘT ID
    public String getItemID() {
        return itemID;
    }
    public void setItemID(String itemID) {
        this.itemID = itemID;
    }

    // =========================================================================
    // CÁC HÀM GETTER & SETTER CÒN LẠI (GIỮ NGUYÊN)
    // =========================================================================

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double newStartingPrice) { this.startingPrice = newStartingPrice; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getMinIncrement() { return minIncrement; }
    public void setMinIncrement(double minIncrement) { this.minIncrement = minIncrement; }

    public String getLastBidderId() { return lastBidderId; }
    public void setLastBidderId(String lastBidderId) { this.lastBidderId = lastBidderId; }

    public String getSeller_ID() { return seller_ID; }
    public void setSeller_ID(String seller_ID) { this.seller_ID = seller_ID; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public String getProductImageURL() { return productImageURL; }
    public void setProductImageURL(String productImageURL) { this.productImageURL = productImageURL; }

    /** Kiểm tra xem phiên đấu giá còn hoạt động không?
     * @return true / false
     */
    public boolean isActive() {
        long currentTime = System.currentTimeMillis();
        return status.equals("ACTIVE") && currentTime >= startTime.getTime() && currentTime <= endTime.getTime();
    }
}