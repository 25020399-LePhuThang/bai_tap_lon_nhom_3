package com.auction.server.network;

import com.auction.server.controller.AuctionTimer;
import com.auction.server.controller.BiddingService;
import com.auction.server.database.DatabaseManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import com.auction.shared.model.item.Item;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



public class AuctionServer {
    // 1. Khai báo danh sách
    public static ArrayList<ClientHandler> clients = new ArrayList<>();
    public static Map<String, Item> itemCache = new ConcurrentHashMap<>();
    public static Map<String, com.auction.server.controller.AuctionAutoBid> autoBidManagers = new ConcurrentHashMap<>();
    public static Map<String, AuctionTimer> auctionTimers = new ConcurrentHashMap<>();
    public static ArrayList<java.io.PrintWriter> activeListeners = new ArrayList<>();

    public static void main(String[] args) {
        DatabaseManager.initDB();
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        System.out.println("Server đã khởi động với múi giờ: " + java.time.ZoneId.systemDefault());
        try {
            // 2. Mở cổng - đọc PORT từ Railway
            int port = System.getenv("PORT") != null
                    ? Integer.parseInt(System.getenv("PORT"))
                    : 5000;
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server đang chạy trên cổng: " + port);



            BiddingService sharedBiddingService = new BiddingService();
            // Khởi động timer cho tất cả item ACTIVE
            com.auction.server.dao.ItemDAO itemDAO = new com.auction.server.dao.ItemDAO();
            java.util.List<Item> activeItems = itemDAO.getActiveItems();
            for (Item item : activeItems) {
                itemCache.put(item.getId(), item);
                AuctionTimer timer = new AuctionTimer(item);
                timer.start();
                auctionTimers.put(item.getId(), timer);
                System.out.println("Timer started for: " + item.getName());
            }


            java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    // 1. Quét và đổi trạng thái PREPARED -> ACTIVE trong Database (Ổ cứng)
                    itemDAO.updateToActive();

                    // 2. Kéo danh sách đang ACTIVE dưới Database lên để đối chiếu với RAM
                    java.util.List<Item> currentActiveItems = itemDAO.getActiveItems();
                    boolean hasNewItems = false;

                    for (Item item : currentActiveItems) {
                        // Nếu RAM (itemCache) chưa có mặt SP này -> Nó là hàng mới vừa được duyệt/đến giờ
                        if (!itemCache.containsKey(item.getId())) {

                            // Đưa vào RAM để ClientHandler tìm thấy
                            itemCache.put(item.getId(), item);

                            // Bật đồng hồ đếm ngược cho SP này
                            AuctionTimer newTimer = new AuctionTimer(item);
                            newTimer.start();
                            auctionTimers.put(item.getId(), newTimer);

                            System.out.println("[Server] Sản phẩm ID " + item.getId() + " - " + item.getName() + " đã tự động MỞ BÁN!");
                            hasNewItems = true; // Đánh dấu là có sự thay đổi
                        }
                    }

                    // 3. Chỉ khi nào có đồ mới lên kệ, mới hét lên cho các App Client F5 lại bảng
                    if (hasNewItems) {
                        for (ClientHandler client : clients) {
                            client.sendMessage("SERVER_SIGNAL_REFRESH");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi quét trạng thái: " + e.getMessage());
                }
            }, 0, 5, java.util.concurrent.TimeUnit.SECONDS);


            // 3. Vòng lặp chờ khách
            while (true) {
                // 4. Lệnh BLOCK (Chặn đứng)
                Socket socket = serverSocket.accept();

                // 5. Giao việc cho luồng mới
                ClientHandler clientHandler = new ClientHandler(socket,sharedBiddingService);
                clients.add(clientHandler);

                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized com.auction.server.controller.AuctionAutoBid getAutoBidManager(String itemId, double currentPrice, String currentWinnerId) {
        if (!autoBidManagers.containsKey(itemId)) {
            autoBidManagers.put(itemId, new com.auction.server.controller.AuctionAutoBid(currentPrice, currentWinnerId));
        }
        return autoBidManagers.get(itemId);
    }

    /**
     * Gửi một tin nhắn đến TẤT CẢ client đang kết nối.
     * Được gọi sau mỗi bid hợp lệ để cập nhật realtime cho mọi người xem.
     *
     * @param message chuỗi tin nhắn (ví dụ: "BID_UPDATE|IPHONE_15|21000000|user1")
     */
    public static void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    /**
     * Kiểm tra xem một người dùng cụ thể đã đăng ký Auto-bid cho sản phẩm này chưa.
     * Nếu rồi, trả về mức giá trần (maxBid) của họ. Nếu chưa, trả về -1.
     */
    public static double getUserMaxBid(String itemId, String bidderId) {
        var autoManager = autoBidManagers.get(itemId);
        if (autoManager == null) return -1;

        // Sử dụng Reflection hoặc một hàm getter trong AuctionAutoBid để lấy danh sách autoBids
        // Giả sử bạn thêm hàm getAutoBids() trả về List<AutoBid> trong class AuctionAutoBid:
        for (com.auction.shared.model.AutoBid bid : autoManager.getAutoBids()) {
            if (bid.getBidderId().equals(bidderId)) {
                return bid.getMaxBid();
            }
        }
        return -1;
    }
}
