package com.auction.client.controller;

import java.util.Date;
import com.auction.shared.model.Item;

public class BiddingService {
    private AuctionTimer auctionTimer;
    
    public BiddingService(AuctionTimer timer) {
        this.auctionTimer = timer;
    }
    public synchronized String placeBid(Item item, double amount, String userId) {
        Date now = new Date();

        // 1. Kiểm tra Dữ liệu đầu vào 
        if (item.getStartTime() == null || item.getEndTime() == null) {
            return "Sản phẩm này chưa được đăng kí thời gian đấu giá chính thức.";
        }

        // 2. Kiểm tra Trạng thái: Đã mở đấu giá chưa?
        if (now.before(item.getStartTime())) {
            return "Phiên đấu giá chưa mở. Hãy quay lại vào lúc " + item.getStartTime();
        }

        // 3. Kiểm tra Trạng thái: Đã hết giờ chưa?
        if (now.after(item.getEndTime())) {
            return "Phiên đấu giá đã kết thúc!";
        }

        // 4. Kiểm tra Tiền: Có đủ bước giá không?
        double minRequired = item.getCurrentPrice() + item.getMinIncrement();
        if (amount < minRequired) {
            return "Giá đặt không hợp lệ. Bạn cần đặt tối thiểu " + minRequired + "đ";
        }

        // 5. Cập nhật và Báo thành công 
        try {
            item.setCurrentPrice(amount);
            item.setLastBidderId(userId);
        
            // Kiểm tra gia hạn tự động ở giây cuối (Anti-Sniping)
            if (handleAntiSniping(item)) {
                // Gọi AuctionTimer để hủy lịch cũ, lập lịch mới
                auctionTimer.cancel();
                auctionTimer.start();
            }
            return "THÀNH CÔNG: Bạn đang là người dẫn đầu với mức giá " + amount + "đ";
        }
        catch (Exception e) {
            return "LỖI HỆ THỐNG: Không thể cập nhật giá lúc này. Vui lòng thử lại.";
        }
    }
    //Anti-Sniping
    private boolean handleAntiSniping(Item item) {
        // Tính thời gian còn lại (ms)
        long timeLeft = item.getEndTime().getTime() - System.currentTimeMillis();
    
        // Nếu còn dưới 30 giây thì gia hạn
        if (timeLeft > 0 && timeLeft < 30000) { 
            long newEndTime = item.getEndTime().getTime() + 60000; // Cộng thêm 60s
            item.setEndTime(new java.util.Date(newEndTime));
            System.out.println(">>> He thong: Phat hien Sniping! Gia han them 60s.");
            return true; // Trả về true để báo là có gia hạn
        }
        return false; // Không cần gia hạn
    }  
}