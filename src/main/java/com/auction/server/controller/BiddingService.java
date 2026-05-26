package com.auction.server.controller;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.auction.shared.model.AutoBid;
import com.auction.shared.model.item.Item;

public class BiddingService {

    // Mỗi phiên (itemId) có một AuctionAutoBid riêng — thread-safe map
    private final Map<String, AuctionAutoBid> autoBidMap = new ConcurrentHashMap<>();

    //   private AuctionTimer auctionTimer;
    //   public BiddingService(AuctionTimer timer) { this.auctionTimer = timer; }

    /**
     * Đặt giá thủ công. Sau khi bid thành công, kích hoạt auto-bid
     * để những người đã đăng ký auto-bid có thể phản ứng lại.
     */
    public String placeBid(Item item, double amount, String userId) {
        // Lock trên item được share giữa các thread
        synchronized (item) {
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

            // 5. Cập nhật giá và winner
            try {
                item.setCurrentPrice(amount);
                item.setLastBidderId(userId);

                // Kiểm tra gia hạn tự động ở giây cuối (Anti-Sniping)
                if (handleAntiSniping(item)) {
                    // Gọi AuctionTimer để hủy lịch cũ, lập lịch mới
                    // auctionTimer.reschedule();
                }

                // 6. Kích hoạt auto-bid phản ứng (nếu có người đã đăng ký)
                AuctionAutoBid autoBid = autoBidMap.get(item.getId());
                if (autoBid != null) {
                    AuctionAutoBid.AutoBidResult result = autoBid.onManualBidPlaced(amount, userId);
                    if (result.priceChanged) {
                        item.setCurrentPrice(result.finalPrice);
                        item.setLastBidderId(result.finalWinnerId);
                        // Auto-bid đã phản ứng — trả về kết quả cuối cùng
                        return "THÀNH CÔNG: Giá hiện tại " + result.finalPrice
                                + "đ — dẫn đầu bởi " + result.finalWinnerId
                                + " (auto-bid)";
                    }
                }

                return "THÀNH CÔNG: Bạn đang là người dẫn đầu với mức giá " + amount + "đ";
            } catch (Exception e) {
                return "LỖI HỆ THỐNG: Không thể cập nhật giá lúc này. Vui lòng thử lại.";
            }
        }
    }

    /**
     * Bidder đăng ký auto-bid cho một phiên đấu giá.
     * Ngay lập tức kích hoạt để xem có thể vượt giá hiện tại không.
     *
     * @param item      sản phẩm đang đấu giá
     * @param bidderId  người đăng ký
     * @param maxBid    giá tối đa sẵn sàng trả
     * @param increment bước tăng giá mỗi lần
     * @return thông báo kết quả
     */
    public String registerAutoBid(Item item, String bidderId, double maxBid, double increment) {
        synchronized (item) {
            Date now = new Date();

            // Kiểm tra phiên còn hoạt động không
            if (item.getStartTime() == null || item.getEndTime() == null) {
                return "Sản phẩm này chưa được đăng kí thời gian đấu giá chính thức.";
            }
            if (now.before(item.getStartTime())) {
                return "Phiên đấu giá chưa mở.";
            }
            if (now.after(item.getEndTime())) {
                return "Phiên đấu giá đã kết thúc!";
            }

            // Lấy hoặc tạo AuctionAutoBid cho phiên này
            AuctionAutoBid autoBid = autoBidMap.computeIfAbsent(
                    item.getId(),
                    id -> new AuctionAutoBid(item.getCurrentPrice(), item.getLastBidderId())
            );

            AutoBid bid = new AutoBid(bidderId, maxBid, increment);
            AuctionAutoBid.AutoBidResult result = autoBid.registerAutoBid(bid);

            // Cập nhật lại giá và winner trên Item nếu auto-bid thay đổi
            if (result.priceChanged) {
                item.setCurrentPrice(result.finalPrice);
                item.setLastBidderId(result.finalWinnerId);
            }

            return result.message != null ? result.message
                    : "Auto-bid đã được đăng ký. Giá hiện tại: " + item.getCurrentPrice() + "đ";
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