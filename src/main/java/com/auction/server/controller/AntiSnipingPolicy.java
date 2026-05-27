package com.auction.server.controller;

import com.auction.shared.model.item.Item;

public class AntiSnipingPolicy {

    public boolean apply(Item item) {
        // Tính thời gian còn lại (ms)
        long timeLeft = item.getEndTime().getTime() - System.currentTimeMillis();

        // Nếu còn dưới 30 giây thì gia hạn
        if (timeLeft > 0 && timeLeft < 30000) {
            long newEndTime = item.getEndTime().getTime() + 60000; // Cộng thêm 60s
            item.setEndTime(new java.util.Date(newEndTime));
            System.out.println(">>> Hệ thống: Phát hiện Sniping! Gia han thêm 60s.");
            return true; // Trả về true để báo là có gia hạn
        }
        return false; // Không cần gia hạn
    }
}