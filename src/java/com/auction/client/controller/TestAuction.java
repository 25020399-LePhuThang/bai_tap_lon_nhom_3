package com.auction.client.controller;

import com.auction.shared.model.Item;
import java.util.Date;

public class TestAuction {
    public static void main(String[] args) {
        try {
            // 1. Tạo món hàng giả: Laptop, giá 1000, bước giá 100
            Item item = new Item("Laptop Gaming", 1000, 100);
            
            // 2. Cho phiên đấu giá còn đúng 10 giây (để kích hoạt Anti-Sniping < 30s)
            long endTime = System.currentTimeMillis() + 10000;
            item.setEndTime(new Date(endTime));
            item.setStatus("ACTIVE");

            // 3. Khởi tạo các module 
            AuctionTimer timer = new AuctionTimer(item);
            BiddingService service = new BiddingService(timer);
            
            timer.start(); // Bắt đầu đếm ngược
            
            System.out.println("--- BAT DAU TEST ---");
            System.out.println("Thoi gian ket thuc ban dau: " + item.getEndTime());

            // 4. Giả lập đặt giá 1500đ
            Thread.sleep(2000); // Đợi 2 giây
            System.out.println("\nDang dat gia 1500...");
            service.placeBid(item, 1500, "user");

            // 5. Kiểm tra xem giờ có nhảy thêm 60 giây không
            System.out.println("Thoi gian ket thuc SAU KHI GIA HAN: " + item.getEndTime());
            System.out.println("\nHay doi 8 giay nua xem thong bao ket thuc...");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}