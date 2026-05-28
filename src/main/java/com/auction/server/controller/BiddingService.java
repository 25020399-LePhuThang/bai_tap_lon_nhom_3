package com.auction.server.controller;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.auction.server.dao.ItemDAO;
import com.auction.server.network.AuctionServer;
import com.auction.shared.model.AutoBid;
import com.auction.shared.model.item.Item;

public class BiddingService {

    // Mỗi phiên (itemId) có một AuctionAutoBid riêng — thread-safe map
    private final Map<String, AuctionAutoBid> autoBidMap = new ConcurrentHashMap<>();

    // Dùng AntiSnipingPolicy thay vì viết logic trực tiếp
    private final AntiSnipingPolicy antiSnipingPolicy;

    // Trong BiddingService, đổi field:
    private ItemDAO itemDao; // bỏ final

    // Tạo hàm getItemDao():
    private ItemDAO getItemDao() {
        if (itemDao == null) {
            itemDao = new ItemDAO();
        }
        return itemDao;
    }


    // Constructor mặc định — dùng AntiSnipingPolicy với 30s / 60s
    public BiddingService() {
        this.antiSnipingPolicy = new AntiSnipingPolicy();
    }

    // Constructor cho phép tùy chỉnh policy
    public BiddingService(AntiSnipingPolicy policy) {
        this.antiSnipingPolicy = policy;
    }

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
            // Kiểm tra số dư
            com.auction.server.dao.UserDAO userDAO = new com.auction.server.dao.UserDAO();
            double balance = userDAO.getBalance(userId);
            if (balance < amount) {
                return "Số dư không đủ! Số dư hiện tại: " + balance + " $";
            }
            if (amount < minRequired) {
                return "Giá đặt không hợp lệ. Bạn cần đặt tối thiểu " + minRequired + "đ";
            }

            // 5. Cập nhật giá và winner
            try {
                item.setCurrentPrice(amount);
                item.setLastBidderId(userId);
                System.out.println(">>> SET lastBidderId: " + userId + " cho item: " + item.getId());
                getItemDao().updatePrice(item);

                // Kiểm tra gia hạn tự động ở giây cuối (Anti-Sniping)
                if (antiSnipingPolicy.apply(item)) {
                    getItemDao().updateEndTime(item);
                    AuctionServer.broadcast(
                            "TIME_UPDATE|" + item.getId() + "|" + item.getEndTime().getTime()
                    );
                }

                // 6. Kích hoạt auto-bid phản ứng (nếu có người đã đăng ký)
                AuctionAutoBid autoBid = autoBidMap.get(item.getId());
                if (autoBid != null) {
                    AuctionAutoBid.AutoBidResult result = autoBid.onManualBidPlaced(amount, userId);
                    if (result.priceChanged) {
                        item.setCurrentPrice(result.finalPrice);
                        item.setLastBidderId(result.finalWinnerId);
                        getItemDao().updatePrice(item);
                        // Auto-bid đã phản ứng — trả về kết quả cuối cùng
                        return "THÀNH CÔNG: Giá hiện tại " + result.finalPrice
                                + "đ — dẫn đầu bởi " + result.finalWinnerId
                                + " (auto-bid)";
                    }
                }

                return "THÀNH CÔNG: Bạn đang là người dẫn đầu với mức giá " + amount + "$";
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
                    : "Auto-bid đã được đăng ký. Giá hiện tại: " + item.getCurrentPrice() + "$";
        }
    }
}
