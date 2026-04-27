package com.auction.client.controller;

import javal.util.Date;
import com.auction.shared.model.Item;

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
        
        return "THÀNH CÔNG: Bạn đang là người dẫn đầu với mức giá " + amount + "đ";
    } catch (Exception e) {
        return "LỖI HỆ THỐNG: Không thể cập nhật giá lúc này. Vui lòng thử lại.";
    }
}