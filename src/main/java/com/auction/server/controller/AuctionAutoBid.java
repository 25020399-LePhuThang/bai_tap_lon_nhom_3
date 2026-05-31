package com.auction.server.controller;

import com.auction.shared.model.AutoBid;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Quản lý Auto-Bidding cho một phiên đấu giá.
 */
public class AuctionAutoBid {

    private double currentPrice;
    private String currentWinnerId;
    private final ReentrantLock lock = new ReentrantLock();
    private final List<AutoBid> autoBids = new ArrayList<>();

    public AuctionAutoBid(double currentPrice, String currentWinnerId) {
        this.currentPrice = currentPrice;
        this.currentWinnerId = currentWinnerId;
    }


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

    public AutoBidResult registerAutoBid(AutoBid newBid) {
        lock.lock();
        try {
            // 🛠️ XÓA ĐOẠN CHECK KHÓA: Bỏ đoạn check "if (newBid.getBidderId().equals(currentWinnerId))" cũ
            // để cho phép người đang thắng vẫn được quyền nâng/thay đổi mức giá trần của họ.

            if (newBid.getMaxBid() <= currentPrice) {
                return new AutoBidResult(currentPrice, currentWinnerId, false,
                        "maxBid phải lớn hơn giá hiện tại (" + currentPrice + ").");
            }

            // Ghi đè hoặc thêm mới cấu hình AutoBid của User vào danh sách quản lý trên RAM
            autoBids.removeIf(b -> b.getBidderId().equals(newBid.getBidderId()));
            autoBids.add(newBid);

            // Kích hoạt guồng máy so kè giá
            return processAutoBidding();
        } finally {
            lock.unlock();
        }
    }

    private AutoBidResult processAutoBidding() {
        boolean priceUpdated;
        boolean actualPriceChanged = false;

        // Bước giá mặc định (Bạn có thể tinh chỉnh hoặc truyền item.getMinIncrement() từ Service vào đây)
        double increment = 10.0;

        // Vòng lặp mô phỏng cuộc chiến nâng giá tự động
        do {
            priceUpdated = false;
            AutoBid bestChallenger = null;

            // Mức giá tối thiểu mà một đối thủ cần phải đáp ứng để được quyền nâng giá
            double nextRequiredBid = currentPrice + increment;

            // Lọc tìm người thách đấu có mức giá trần hợp lý và cao nhất lúc này
            for (AutoBid bid : autoBids) {
                // Người đang dẫn đầu phiên hiện tại không tự nâng giá chống lại chính mình
                if (bid.getBidderId().equals(currentWinnerId)) {
                    continue;
                }

                // Nếu giá trần của đối thủ này đủ để trả cho bước giá tiếp theo
                if (bid.getMaxBid() >= nextRequiredBid) {
                    if (bestChallenger == null) {
                        bestChallenger = bid;
                    } else if (bid.getMaxBid() > bestChallenger.getMaxBid()) {
                        bestChallenger = bid; // Ai đặt giá trần cao hơn thì ưu tiên người đó nâng trước
                    } else if (bid.getMaxBid() == bestChallenger.getMaxBid()
                            && bid.getTimestamp() < bestChallenger.getTimestamp()) {
                        bestChallenger = bid; // Nếu cùng giá trần, ai đặt trước xếp trước
                    }
                }
            }

            // Nếu tìm thấy một đối thủ hợp lệ để đè giá người hiện tại
            if (bestChallenger != null) {
                currentPrice = nextRequiredBid;
                currentWinnerId = bestChallenger.getBidderId();
                priceUpdated = true;
                actualPriceChanged = true;

                System.out.printf("[AutoBid] Hệ thống tự động đẩy giá lên: %.0f$ cho User: %s%n", currentPrice, currentWinnerId);
            }

        } while (priceUpdated); // Vòng lặp chạy liên tục cho đến khi tất cả các bên cạn giá trần

        String msg = String.format("AutoBid: %s dẫn đầu với %.0f$", currentWinnerId, currentPrice);
        return new AutoBidResult(currentPrice, currentWinnerId, actualPriceChanged, msg);
    }


    public void cancelAutoBid(String bidderId) {
        lock.lock();
        try {
            autoBids.removeIf(b -> b.getBidderId().equals(bidderId));
        } finally {
            lock.unlock();
        }
    }

    public double getCurrentPrice() {
        lock.lock();
        try { return currentPrice; } finally { lock.unlock(); }
    }

    public String getCurrentWinnerId() {
        lock.lock();
        try { return currentWinnerId; } finally { lock.unlock(); }
    }

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

    public List<AutoBid> getAutoBids() {
        return this.autoBids;
    }
}