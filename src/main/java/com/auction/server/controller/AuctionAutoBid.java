package com.auction.server.controller;

import com.auction.shared.model.AutoBid;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Quản lý Auto-Bidding cho một phiên đấu giá.
 *
 * Logic chuẩn (eBay Proxy Bidding):
 *  - Mỗi bidder đăng ký AutoBid với maxBid và increment riêng.
 *  - Khi có bid mới, hệ thống tìm challenger tốt nhất (maxBid cao nhất,
 *    đăng ký sớm nhất nếu hòa) và tăng giá lên currentPrice + increment,
 *    không vượt maxBid.
 *  - Lặp lại cho đến khi không ai thay đổi được nữa (hội tụ).
 *  - Người đang dẫn đầu không tự đấu với chính mình.
 */
public class AuctionAutoBid {

    // Giá hiện tại của phiên (phải được khởi tạo đúng từ Auction)
    private double currentPrice;

    // ID người đang dẫn đầu
    private String currentWinnerId;

    private final ReentrantLock lock = new ReentrantLock();

    // Danh sách tất cả auto-bid đang hoạt động trong phiên này
    private final List<AutoBid> autoBids = new ArrayList<>();

    /**
     * Khởi tạo với giá hiện tại và winner hiện tại của phiên đấu giá.
     */
    public AuctionAutoBid(double currentPrice, String currentWinnerId) {
        this.currentPrice = currentPrice;
        this.currentWinnerId = currentWinnerId;
    }

    /**
     * Bidder đăng ký auto-bid.
     * Sau khi đăng ký, ngay lập tức kích hoạt processAutoBidding()
     * để xem auto-bid mới có thể vượt ai không.
     */
    public AutoBidResult registerAutoBid(AutoBid newBid) {
        lock.lock();
        try {
            // Không cho phép tự đấu giá với chính mình
            if (newBid.getBidderId().equals(currentWinnerId)) {
                return new AutoBidResult(currentPrice, currentWinnerId, false,
                        "Bạn đang là người dẫn đầu, không cần đăng ký auto-bid lúc này.");
            }
            // Nếu maxBid thấp hơn hoặc bằng giá hiện tại → vô nghĩa
            if (newBid.getMaxBid() <= currentPrice) {
                return new AutoBidResult(currentPrice, currentWinnerId, false,
                        "maxBid phải lớn hơn giá hiện tại (" + currentPrice + ").");
            }
            autoBids.add(newBid);
            return processAutoBidding();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gọi khi có một bid thường (không phải auto-bid) vừa thành công.
     * Hệ thống kiểm tra xem có auto-bid nào cần phản ứng không.
     *
     * @param newPrice   giá vừa được đặt
     * @param newWinner  người vừa đặt giá
     */
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

    /**
     * CORE LOGIC — Thuật toán Proxy Bidding chuẩn:
     *
     * Bước 1: Tìm challenger tốt nhất (không phải winner hiện tại,
     *         có maxBid > currentPrice).
     *         Ưu tiên: maxBid cao → timestamp nhỏ (đăng ký sớm hơn).
     * Bước 2: Challenger đặt currentPrice + increment của họ,
     *         nhưng không vượt quá maxBid của họ.
     * Bước 3: Lặp lại — winner mới có thể bị phản ứng bởi auto-bid khác.
     * Bước 4: Dừng khi không còn challenger nào có thể vượt giá hiện tại.
     *
     * Trường hợp đặc biệt (hòa maxBid):
     *   Cả hai đều muốn maxBid như nhau → người đăng ký trước thắng.
     *   Người đó đặt mức giá = maxBid chung, người kia không thể vượt.
     */
    private AutoBidResult processAutoBidding() {
        boolean anyChange = true;

        while (anyChange) {
            anyChange = false;

            AutoBid challenger = findBestChallenger();
            if (challenger == null) break;

            // Mức giá challenger muốn đặt (tối thiểu để dẫn đầu)
            double challengerBid = currentPrice + challenger.getIncrement();

            if (challengerBid > challenger.getMaxBid()) {
                // Challenger không đủ khả năng tăng thêm một bước → dừng
                break;
            }

            // Challenger vượt được → cập nhật giá và winner
            currentPrice = challengerBid;
            currentWinnerId = challenger.getBidderId();
            anyChange = true;

            System.out.printf("[AutoBid] %s đặt %.0f (maxBid=%.0f)%n",
                    currentWinnerId, currentPrice, challenger.getMaxBid());
        }

        String msg = String.format("AutoBid: %s dẫn đầu với %.0f", currentWinnerId, currentPrice);
        return new AutoBidResult(currentPrice, currentWinnerId, true, msg);
    }

    /**
     * Tìm auto-bid tốt nhất trong số những người KHÔNG phải winner hiện tại
     * và có khả năng vượt giá hiện tại ít nhất một bước.
     *
     * Ưu tiên: maxBid cao hơn → đăng ký sớm hơn (timestamp nhỏ hơn).
     */
    private AutoBid findBestChallenger() {
        AutoBid best = null;
        for (AutoBid bid : autoBids) {
            if (bid.getBidderId().equals(currentWinnerId)) continue;
            if (bid.getMaxBid() <= currentPrice) continue;

            if (best == null) {
                best = bid;
            } else if (bid.getMaxBid() > best.getMaxBid()) {
                best = bid;
            } else if (bid.getMaxBid() == best.getMaxBid()
                    && bid.getTimestamp() < best.getTimestamp()) {
                // Hòa maxBid → ưu tiên người đăng ký trước
                best = bid;
            }
        }
        return best;
    }

    /**
     * Hủy đăng ký auto-bid (khi bidder rút lui hoặc phiên kết thúc).
     */
    public void cancelAutoBid(String bidderId) {
        lock.lock();
        try {
            autoBids.removeIf(b -> b.getBidderId().equals(bidderId));
        } finally {
            lock.unlock();
        }
    }

    // ========================
    // Getters
    // ========================

    public double getCurrentPrice() {
        lock.lock();
        try { return currentPrice; } finally { lock.unlock(); }
    }

    public String getCurrentWinnerId() {
        lock.lock();
        try { return currentWinnerId; } finally { lock.unlock(); }
    }

    // ========================
    // Inner class: kết quả trả về cho BiddingService / Auction
    // ========================

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
}
