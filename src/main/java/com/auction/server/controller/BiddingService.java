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
    private final AntiSnipingPolicy antiSnipingPolicy;
    private ItemDAO itemDao;

    private ItemDAO getItemDao() {
        if (itemDao == null) {
            itemDao = new ItemDAO();
        }
        return itemDao;
    }

    public BiddingService() {
        this.antiSnipingPolicy = new AntiSnipingPolicy();
    }

    public BiddingService(AntiSnipingPolicy policy) {
        this.antiSnipingPolicy = policy;
    }

    /**
     * Đặt giá thủ công. Sau khi bid thành công, kích hoạt auto-bid
     * để những người đã đăng ký auto-bid có thể phản ứng lại.
     */
    public String placeBid(Item item, double amount, String userId) {
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
            // [Tâm] Cho phép trễ 1 giây để bù trừ độ trễ mạng
            long timeLeft = item.getEndTime().getTime() - now.getTime();
            if (timeLeft < -1000) {
                return "Phiên đấu giá đã kết thúc!";
            }

            // 4. Kiểm tra Tiền: Có đủ bước giá không?
            com.auction.server.dao.UserDAO userDAO = new com.auction.server.dao.UserDAO();
            double balance = userDAO.getBalance(userId);
            if (balance < amount) {
                return "Số dư không đủ! Số dư hiện tại: " + balance + " $";
            }
            double minRequired = item.getCurrentPrice() + item.getMinIncrement();
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

                    // [Tâm] Reschedule timer với endTime mới cho Server
                    // Kiểm tra gia hạn tự động ở giây cuối (Anti-Sniping)
                    if (antiSnipingPolicy.apply(item)) {
                        getItemDao().updateEndTime(item);
                        AuctionServer.broadcast(
                                "TIME_UPDATE|" + item.getId() + "|" + item.getEndTime().getTime()
                        );

                        //Tạm thời đóng phần Reschedule Timer vì dự án chưa có class AuctionTimer
                        AuctionTimer timer = AuctionServer.auctionTimers.get(item.getId());
                        if (timer != null) {
                            timer.reschedule();
                            System.out.println(">>> Timer đã được gia hạn thêm 60s cho: " + item.getId());
                        }
                    }}

                // 6. Kích hoạt auto-bid phản ứng (nếu có người đã đăng ký)
                AuctionAutoBid autoBid = autoBidMap.get(item.getId());

                // [Bản 2] Kích hoạt đầy đủ bộ xử lý AutoBid và thông báo Socket
                if (autoBid != null) {
                    // ─── ĐỒNG BỘ RAM THEO DATABASE TRƯỚC KHI CHẠY ───
                    com.auction.server.dao.BidDAO bidDAO = new com.auction.server.dao.BidDAO();
                    autoBid.getAutoBids().removeIf(bid -> {
                        double dbMaxBid = bidDAO.getAutoBidValue(bid.getBidderId(), item.getId());
                        return dbMaxBid <= 0;
                    });

                    // 🛠️ SỬA TRỰC TIẾP: Lưu lại người đang giữ Winner trước khi AutoBid chạy đè lên
                    String oldWinnerId = item.getLastBidderId();
                    String manualBidderId = userId;

                    AuctionAutoBid.AutoBidResult result = autoBid.onManualBidPlaced(amount, userId);
                    if (result.priceChanged) {
                        item.setCurrentPrice(result.finalPrice);
                        item.setLastBidderId(result.finalWinnerId);
                        getItemDao().updatePrice(item);

                        // 🛠️ SỬA TRỰC TIẾP: Gửi kèm oldWinnerId ở vị trí tham số thứ 7 để UI đọc Alert
                        AuctionServer.broadcast(
                                "BID_UPDATE|" + item.getId() + "|" + result.finalPrice + "|" + result.finalWinnerId +
                                        "|AUTOBID_TRIGGERED|" + manualBidderId + "|" + oldWinnerId + "\n"
                        );

                        // Gửi kèm winnerId và giá mới để client hiển thị thông báo chi tiết
                        return "SERVER_PROCESSED_AUTOBID|" + result.finalWinnerId + "|" + result.finalPrice;
                    }
                }

                return "THÀNH CÔNG: Bạn đang là người dẫn đầu với mức giá " + amount + "$";
            } catch (Exception e) {
                return "LỖI HỆ THỐNG: Không thể cập nhật giá lúc này. Vui lòng thử lại.";
            }
        }
    }

    public String registerAutoBid(Item item, String bidderId, double maxBid, double increment) {
        synchronized (item) {
            Date now = new Date();

            if (item.getStartTime() == null || item.getEndTime() == null) {
                return "Sản phẩm này chưa được đăng kí thời gian đấu giá chính thức.";
            }
            if (now.before(item.getStartTime())) {
                return "Phiên đấu giá chưa mở.";
            }
            // [Tâm] Bù trừ độ trễ mạng 1 giây giống bên placeBid
            long timeLeft = item.getEndTime().getTime() - now.getTime();
            if (timeLeft < -1000) {
                return "Phiên đấu giá đã kết thúc!";
            }

            AuctionAutoBid autoBid = autoBidMap.computeIfAbsent(
                    item.getId(),
                    id -> new AuctionAutoBid(item.getCurrentPrice(), item.getLastBidderId())
            );

            // [Bản 2] Đồng bộ loại bỏ các cài đặt AutoBid đã hủy dưới DB
            com.auction.server.dao.BidDAO bidDAO = new com.auction.server.dao.BidDAO();
            autoBid.getAutoBids().removeIf(b -> {
                double dbMaxBid = bidDAO.getAutoBidValue(b.getBidderId(), item.getId());
                return dbMaxBid <= 0;
            });

            // 🛠️ SỬA TẠI ĐÂY: Lưu lại người thắng cũ trước khi guồng máy AutoBid chạy đè lên
            String oldWinnerId = item.getLastBidderId();

            AutoBid bid = new AutoBid(bidderId, maxBid, increment);
            AuctionAutoBid.AutoBidResult result = autoBid.registerAutoBid(bid);

            // 🛠️ SỬA TẠI ĐÂY: Nếu đăng ký AutoBid làm thay đổi giá (giật lại lượt dẫn đầu)
            if (result.priceChanged) {
                try {
                    // 1. Cập nhật RAM
                    item.setCurrentPrice(result.finalPrice);
                    item.setLastBidderId(result.finalWinnerId);

                    // 2. Cập nhật cứng xuống Database (Code cũ của bạn bị thiếu dòng này nên DB không đổi)
                    getItemDao().updatePrice(item);

                    // 3. Kiểm tra gia hạn tự động ở giây cuối (Anti-Sniping)
                    if (antiSnipingPolicy.apply(item)) {
                        getItemDao().updateEndTime(item);
                        AuctionServer.broadcast(
                                "TIME_UPDATE|" + item.getId() + "|" + item.getEndTime().getTime()
                        );

                        AuctionTimer timer = AuctionServer.auctionTimers.get(item.getId());
                        if (timer != null) {
                            timer.reschedule();
                            System.out.println(">>> Timer đã được gia hạn thêm 60s (từ AutoBid) cho: " + item.getId());
                        }
                    }

                    // 4. Bắn Socket thông báo cho toàn bộ các Client đang mở UI cập nhật ngay lập tức
                    AuctionServer.broadcast(
                            "BID_UPDATE|" + item.getId() + "|" + result.finalPrice + "|" + result.finalWinnerId +
                                    "|AUTOBID_TRIGGERED|" + bidderId + "|" + oldWinnerId + "\n"
                    );

                    return "SERVER_PROCESSED_AUTOBID|" + result.finalWinnerId + "|" + result.finalPrice;
                } catch (Exception e) {
                    return "LỖI HỆ THỐNG: Không thể cập nhật giá AutoBid. Vui lòng thử lại.";
                }
            }

            return result.message != null ? result.message
                    : "Auto-bid đã được đăng ký. Giá hiện tại: " + item.getCurrentPrice() + "$";
        }
    }
}