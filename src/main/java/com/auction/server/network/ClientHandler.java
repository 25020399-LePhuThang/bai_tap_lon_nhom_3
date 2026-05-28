package com.auction.server.network;

import com.auction.server.controller.BiddingService;
import com.auction.server.controller.ProductManager;
import com.auction.server.controller.RegisterHandler;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.dao.BidDAO;
import com.auction.shared.model.BidTransaction;
import com.auction.server.database.DatabaseManager;
import com.auction.shared.model.item.Art;
import com.auction.shared.model.item.Electronic;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.Vehicle;
import com.auction.shared.model.user.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.google.gson.*;
import java.lang.reflect.Type;
import com.auction.shared.model.item.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private BiddingService biddingService;

    private UserDAO userDAO = new UserDAO();
    private ItemDAO itemDAO = new ItemDAO();
    private BidDAO bidDAO = new BidDAO();

    public ClientHandler(Socket socket, BiddingService biddingService) {
        this.socket = socket;
        this.biddingService = biddingService;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Gửi một tin nhắn xuống client này.
     * Nhận vào chuỗi JSON của SocketMessageDTO đã đóng gói sẵn.
     */
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                String[] parts = message.split("\\|", -1);
                String action = parts[0];

                switch (action) {
                    // 1. ĐĂNG KÍ
                    case "REGISTER" -> {
                        String username = parts[1];
                        String password = parts[2];
                        String email = parts[3];
                        String phone = parts[4];
                        String role = parts[5];

                        out.println(RegisterHandler.processRegister(username, password, email, phone, role));
                    }

                    // ==============================================================
                    // XỬ LÝ YÊU CẦU ĐĂNG NHẬP (Nhận 3 tham số: user, pass, role)
                    // ==============================================================
                    case "LOGIN" -> {
                        try {
                            // Tách dữ liệu từ chuỗi "LOGIN|username|password|role"
                            String username = parts[1];
                            String password = parts[2];
                            String role = parts[3];

                            // Gọi hàm Login trong UserDAO mà Vương vừa sửa lúc nãy
                            User loggedInUser = UserDAO.Login(username, password, role);

                            if (loggedInUser != null) {
                                // Kiểm tra xem tài khoản có bị Admin khóa không
                                if ("BANNED".equalsIgnoreCase(loggedInUser.getStatus())) {
                                    out.println("LOGIN_BANNED");
                                } else {
                                    // Đăng nhập thành công, trả về kèm theo Role để Client xác nhận lại
                                    out.println("LOGIN_SUCCESS|" + loggedInUser.getRole());
                                }
                            } else {
                                // Trả về null tức là sai tài khoản hoặc mật khẩu
                                out.println("LOGIN_FAIL");
                            }
                        } catch (Exception e) {
                            System.err.println("Lỗi xử lý LOGIN trên Server: " + e.getMessage());
                            out.println("ERROR|Lỗi hệ thống khi đăng nhập");
                        }
                    }

                    //3a. BID:
                    case "BID" -> {
                        String user = parts[1];
                        double price = Double.parseDouble(parts[2]);
                        String itemId = parts[3].trim();
                        System.out.println("===> CHECK SERVER NHẬN ĐƯỢC:");
                        System.out.println("User: " + parts[1] + " | Price: " + parts[2] + " | ItemID: '" + itemId + "'");

                        Item targetItem = AuctionServer.itemCache.computeIfAbsent(
                                itemId,
                                id -> itemDAO.getItemById(id)
                        );

                        if (targetItem != null) {
                            String result = biddingService.placeBid(targetItem, price, user);

                            // Trả kết quả về đúng người vừa đặt giá
                            out.println(result);

                            // Nếu bid thành công → broadcast giá mới cho TẤT CẢ client đang xem
                            // Format: BID_UPDATE|itemId|giáMới|ngườiDẫnĐầu
                            if (result.startsWith("THÀNH CÔNG")) {
                                double finalPrice = targetItem.getCurrentPrice();
                                String finalWinner = targetItem.getLastBidderId();

                                // Lưu vào DB để Bid History Visualization có dữ liệu
                                BidTransaction tx = new BidTransaction(user, itemId, finalPrice);
                                bidDAO.save(tx);

                                // Broadcast realtime cho tất cả client
                                AuctionServer.broadcast(
                                        "BID_UPDATE|" + itemId + "|" + finalPrice + "|" + finalWinner
                                );

                                var autoManager = AuctionServer.getAutoBidManager(itemId, finalPrice, finalWinner);
                                var autoResult = autoManager.onManualBidPlaced(finalPrice, finalWinner);

                                if (autoResult.priceChanged) {
                                    targetItem.setCurrentPrice(autoResult.finalPrice);
                                    targetItem.setLastBidderId(autoResult.finalWinnerId);
                                    itemDAO.updatePrice(targetItem);

                                    bidDAO.save(new BidTransaction(autoResult.finalWinnerId, itemId, autoResult.finalPrice));
                                    // Broadcast đợt 2: Giá mới sau khi Bot giật lại vị trí dẫn đầu
                                    AuctionServer.broadcast("BID_UPDATE|" + itemId + "|" + autoResult.finalPrice + "|" + autoResult.finalWinnerId);
                                }
                            }
                        } else {
                            out.println("BID_FAIL|Sản phẩm không tồn tại!");
                        }
                    }

                    // 3b. AUTO-BID: AUTO_BID|bidderId|maxBid|increment|itemId
                    case "REGISTER_AUTOBID", "AUTO_BID" -> {
                        String bidderId  = parts[1];
                        double maxBid    = Double.parseDouble(parts[2]);
                        double increment = parts.length > 3 && !parts[3].isEmpty() ? Double.parseDouble(parts[3]) : 0;
                        String itemId    = parts[parts.length - 1].trim();

                        Item targetItem = AuctionServer.itemCache.computeIfAbsent(itemId, id -> itemDAO.getItemById(id));

                        if (targetItem != null) {
                            if (increment <= 0) increment = targetItem.getMinIncrement();

                            // Gọi bộ quản lý từ AuctionServer
                            var autoManager = AuctionServer.getAutoBidManager(itemId, targetItem.getCurrentPrice(), targetItem.getLastBidderId());

                            com.auction.shared.model.AutoBid newAuto = new com.auction.shared.model.AutoBid(
                                    bidderId, maxBid, increment
                            );
                            var result = autoManager.registerAutoBid(newAuto);

                            if (result.priceChanged) {
                                targetItem.setCurrentPrice(result.finalPrice);
                                targetItem.setLastBidderId(result.finalWinnerId);
                                itemDAO.updatePrice(targetItem);
                                bidDAO.save(new BidTransaction(result.finalWinnerId, itemId, result.finalPrice));

                                // Đẩy dữ liệu realtime xuống đồ thị Client
                                AuctionServer.broadcast("BID_UPDATE|" + itemId + "|" + result.finalPrice + "|" + result.finalWinnerId);
                                out.println("THÀNH CÔNG|" + result.message);
                            } else {
                                if (result.message != null && result.message.contains("dẫn đầu")) {
                                    out.println("THÀNH CÔNG|Đã ghi nhận mức giá trần tự động của bạn.");
                                } else {
                                    out.println("THẤT BẠI|" + result.message);
                                }
                            }
                        } else {
                            out.println("AUTO_BID_FAIL|Sản phẩm không tồn tại!");
                        }
                    }

                    // 4. GET PREPARED ITEMS
                    case "GET_PREPARED_ITEMS" -> {
                        List<Item> preparedList = itemDAO.getPreparedItems();
                        String json = new Gson().toJson(preparedList);
                        out.println(json);
                    }

                    // 5. GET ACTIVE ITEMS
                    case "GET_ACTIVE_ITEMS" -> {
                        List<Item> activeList = itemDAO.getActiveItems();
                        String json = new Gson().toJson(activeList);
                        out.println(json);
                    }

                    // 6. XỬ LÝ CẬP NHẬT THÔNG TIN
                    case "UPDATE_INFO" -> {
                        String currentUsername = parts[1];
                        String newUsername = parts[2];
                        String newEmail = parts[3];
                        String newPhone = parts[4];
                        String newAddress = parts[5];

                        String result = userDAO.updateUserInfo(currentUsername, newUsername, newEmail, newPhone, newAddress);

                        switch (result) {
                            case "SUCCESS":
                                out.println("UPDATE_SUCCESS|Cập nhật thông tin thành công rồi cưng!");
                                break;
                            case "ERR_USERNAME":
                                out.println("UPDATE_FAIL|Tên đăng nhập mới này đã có người sử dụng!");
                                break;
                            case "ERR_EMAIL":
                                out.println("UPDATE_FAIL|Email mới này đã có người sử dụng!");
                                break;
                            case "ERR_PHONE":
                                out.println("UPDATE_FAIL|Số điện thoại mới này đã có người sử dụng!");
                                break;
                            default:
                                out.println("UPDATE_FAIL|Lỗi hệ thống Database, không thể cập nhật thông tin.");
                                break;
                        }
                    }

                    // 7. XỬ LÝ ĐỔI MẬT KHẨU
                    case "CHANGE_PASS" -> {
                        String username = parts[1];
                        String oldPassword = parts[2];
                        String newPassword = parts[3];

                        String result = userDAO.changePassword(username, oldPassword, newPassword);
                        out.println(result);
                    }

                    // 8. XỬ LÝ NẠP TIỀN
                    case "DEPOSIT" -> {
                        String username = parts[1];
                        double amount = Double.parseDouble(parts[2]);

                        // 1. Kiểm tra ví hiện tại đang có bao nhiêu tiền
                        double currentBalance = userDAO.getBalance(username);

                        // 2. Cộng thử số tiền định nạp xem có lố 1 củ đô không (1,000,000)
                        if (currentBalance + amount > 1000000.0) {
                            // Trả về FAIL kèm theo lời nhắn giải thích
                            out.println("DEPOSIT_FAIL|Nạp thất bại! Hạn mức ví tối đa chỉ là 1,000,000$.");
                        } else {
                            // 3. Nếu an toàn thì mới tiến hành lưu vào Database
                            boolean isSuccess = userDAO.deposit(username, amount);

                            if (isSuccess) {
                                double newBalance = userDAO.getBalance(username);
                                out.println("DEPOSIT_SUCCESS|" + newBalance);
                            } else {
                                out.println("DEPOSIT_FAIL|Lỗi hệ thống Database khi nạp tiền.");
                            }
                        }
                    }
                    // 9. XỬ LÝ RÚT TIỀN
                    case "WITHDRAW" -> {
                        String username = parts[1];
                        double amount = Double.parseDouble(parts[2]);

                        String result = userDAO.withdraw(username, amount);

                        if (result.equals("SUCCESS")) {
                            double newBalance = userDAO.getBalance(username);
                            out.println("WITHDRAW_SUCCESS|" + newBalance);
                        } else {
                            out.println("WITHDRAW_FAIL|Số dư không đủ hoặc lỗi Database!");
                        }
                    }

                    // 10. XỬ LÝ LẤY SỐ DƯ TÀI KHOẢN
                    case "GET_BALANCE" -> {
                        String username = parts[1];
                        double currentBalance = userDAO.getBalance(username);
                        out.println("BALANCE_SUCCESS|" + currentBalance);
                    }

                    // 11. LẤY DANH SÁCH SẢN PHẨM CỦA NGƯỜI BÁN
                    case "GET_ITEMS_BY_SELLER" -> {
                        String username = parts[1];
                        System.out.println("[DEBUG] Đã nhận yêu cầu lấy danh sách của: " + username);

                        try {
                            int sellerId = userDAO.getIdByUsername(username);
                            System.out.println("[DEBUG] Tìm thấy SellerID: " + sellerId);

                            if (sellerId != -1) {
                                List<Item> sellerItems = itemDAO.getItemsBySellerID(sellerId);
                                System.out.println("[DEBUG] Số lượng sản phẩm tìm thấy: " + (sellerItems != null ? sellerItems.size() : 0));

                                if (sellerItems != null && !sellerItems.isEmpty()) {
                                    String json = new Gson().toJson(sellerItems);
                                    out.println("GET_SELLER_ITEMS_SUCCESS|" + json);
                                } else {
                                    out.println("GET_SELLER_ITEMS_SUCCESS|[]");
                                }
                            } else {
                                out.println("GET_SELLER_ITEMS_SUCCESS|[]");
                            }

                        } catch (Exception e) {
                            System.out.println("[SEVERE] Lỗi khi xử lý GET_ITEMS_BY_SELLER: " + e.getMessage());
                            e.printStackTrace();
                            out.println("GET_SELLER_ITEMS_FAIL|Lỗi server!");
                        }
                    }

                    case "CREATE_ITEM" -> {
                        try {
                            String[] dataParts = message.split("\\|", 2);
                            if (dataParts.length < 2) {
                                out.println("ERROR|Thiếu dữ liệu JSON");
                            } else {
                                String jsonString = dataParts[1];

                                // 1. Dạy Gson cách phân biệt các lớp conJsonParser.parseString(clientMessage).getAsJsonObject();
                                Gson gson = new GsonBuilder()
                                        .setDateFormat("yyyy-MM-dd HH:mm:ss")
                                        .registerTypeAdapter(Item.class, new JsonDeserializer<Item>() {
                                            @Override
                                            public Item deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                                                JsonObject jsonObject = json.getAsJsonObject();
                                                String itemType = jsonObject.has("type") ? jsonObject.get("type").getAsString() : "";

                                                if (itemType.equalsIgnoreCase("Art")) {
                                                    return context.deserialize(json, Art.class);
                                                } else if (itemType.equalsIgnoreCase("Vehicle")) {
                                                    return context.deserialize(json, Vehicle.class);
                                                } else if (itemType.equalsIgnoreCase("Electronic")) {
                                                    return context.deserialize(json, Electronic.class);
                                                }
                                                throw new JsonParseException("Không nhận diện được: " + itemType);
                                            }
                                        })
                                        .create();

                                // 2. Dịch JSON thành Object
                                Item newItem = gson.fromJson(jsonString, Item.class);

                                // 3. Gọi DAO lưu Database
                                ItemDAO itemDAO = new ItemDAO();
                                boolean isInserted = itemDAO.createItem(newItem);

                                // 4. Trả kết quả
                                if (isInserted) {
                                    out.println("CREATE_ITEM_SUCCESS");
                                } else {
                                    out.println("ERROR|Không lưu được vào Database");
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Lỗi khi thêm sản phẩm: " + e.getMessage());
                            e.printStackTrace();
                            out.println("ERROR|Lỗi dữ liệu Server");
                        }
                    }

                    case "LISTEN_ONLY" -> {
                        // Giữ kết nối để nhận broadcast, không làm gì thêm
                        System.out.println("Client đăng ký listen-only");
                    }
                    case "LOGOUT" -> {
                        System.out.println("Một Client đã yêu cầu đăng xuất và ngắt kết nối.");

                        out.println("LOGOUT_SUCCESS");

                        return;
                    }
                    case "GET_USER_INFO" -> {
                        try {
                            String targetUsername = parts[1]; // Lấy username Client gửi lên
                            UserDAO userDAO = new UserDAO();

                            // Trả về một đối tượng đa hình (Bidder/Seller) đã được fix lỗi ID
                            User user = userDAO.getUserByUsername(targetUsername);

                            if (user != null) {
                                // Gson sẽ tự động gom cả các trường riêng (như balance của Bidder) vào JSON
                                String jsonString = new Gson().toJson(user);
                                out.println("USER_INFO_SUCCESS|" + jsonString);
                            } else {
                                out.println("ERROR|Không tìm thấy người dùng");
                            }
                        } catch (Exception e) {
                            out.println("ERROR|Lỗi server khi lấy thông tin");
                            e.printStackTrace();
                        }
                    }
                    // ==============================================================
                    // TÍNH NĂNG ADMIN: QUẢN LÝ SẢN PHẨM
                    // ==============================================================

                    // 1. Lấy danh sách sản phẩm đang chờ duyệt (WAITING)
                    case "GET_WAITING_ITEMS" -> {
                        try {
                            ItemDAO itemDAO = new ItemDAO();
                            List<Item> waitingList = itemDAO.takeWaitingItems(); // Hàm cậu vừa code xong
                            String jsonResponse = new Gson().toJson(waitingList);
                            out.println("WAITING_ITEMS_SUCCESS|" + jsonResponse);
                        } catch (Exception e) {
                            out.println("ERROR|Lỗi khi lấy danh sách chờ duyệt");
                            e.printStackTrace();
                        }
                    }

                    // Admin duyệt sản phẩm (Chuyển WAITING -> ACTIVE hoặc PREPARED tùy thời gian)
                    case "APPROVE_ITEM" -> {
                        try {
                            String itemId = parts[1];

                            // GỌI HÀM KIỂM TRA THỜI GIAN THÔNG MINH
                            boolean success = ItemDAO.approveItemWithTimeCheck(itemId);

                            if (success) {
                                out.println("APPROVE_SUCCESS");
                            } else {
                                out.println("ERROR|Không thể duyệt sản phẩm này");
                            }
                        } catch (Exception e) {
                            out.println("ERROR|Lỗi server khi duyệt sản phẩm");
                            e.printStackTrace();
                        }
                    }

                    // 3. Admin từ chối sản phẩm (Xóa luôn khỏi Database)
                    case "REJECT_ITEM" -> {
                        try {
                            String itemId = parts[1];
                            // LƯU Ý: Vương cần viết thêm hàm deleteItem(id) trong ItemDAO
                            boolean success = ItemDAO.deleteItem(itemId);
                            if (success) {
                                out.println("REJECT_SUCCESS");
                            } else {
                                out.println("ERROR|Không thể xóa sản phẩm này");
                            }
                        } catch (Exception e) {
                            out.println("ERROR|Lỗi server khi từ chối sản phẩm");
                            e.printStackTrace();
                        }
                    }

                    // ==============================================================
                    // TÍNH NĂNG ADMIN: QUẢN LÝ NGƯỜI DÙNG (Cho Tab 2)
                    // ==============================================================

                    // 4. Lấy danh sách toàn bộ người dùng
                    case "GET_ALL_USERS" -> {
                        try {
                            UserDAO userDAO = new UserDAO();
                            // LƯU Ý: Vương cần viết hàm getAllUsers() trả về List<User>
                            List<User> userList = userDAO.getAllUsers();
                            String jsonResponse = new Gson().toJson(userList);
                            out.println("ALL_USERS_SUCCESS|" + jsonResponse);
                        } catch (Exception e) {
                            out.println("ERROR|Lỗi khi lấy danh sách người dùng");
                            e.printStackTrace();
                        }
                    }

                    // 5. Khóa tài khoản (BANNED)
                    case "BAN_USER" -> {
                        try {
                            String userId = parts[1]; // Gửi ID lên
                            // Vương cần viết hàm updateUserStatus(id, status)
                            boolean success = UserDAO.updateUserStatus(userId, "BANNED");
                            out.println(success ? "BAN_SUCCESS" : "ERROR|Không thể khóa user");
                        } catch (Exception e) {
                            out.println("ERROR|Lỗi server khi khóa user");
                        }
                    }

                    // 6. Mở khóa tài khoản (ACTIVE)
                    case "UNBAN_USER" -> {
                        try {
                            String userId = parts[1];
                            boolean success = UserDAO.updateUserStatus(userId, "ACTIVE");
                            out.println(success ? "UNBAN_SUCCESS" : "ERROR|Không thể mở khóa user");
                        } catch (Exception e) {
                            out.println("ERROR|Lỗi server khi mở khóa user");
                        }
                    }
                    case "DELETE_ITEM" -> {
                        String itemIdToDelete = parts[1];
                        System.out.println("Server: Đang xử lý yêu cầu xóa Item [" + itemIdToDelete + "]");

                        // 1. Lấy thông tin sản phẩm từ DB lên để kiểm tra
                        Item targetItem = itemDAO.getItemById(itemIdToDelete);

                        if (targetItem == null) {
                            out.println("DELETE_FAIL|Sản phẩm không tồn tại hoặc đã bị xóa trước đó.");
                        } else {
                            // 2. LỚP KHÓA 2: Kiểm tra thời gian bắt đầu
                            // (Giả sử model có hàm getStartTime trả về LocalDateTime)
                            Date currentTime = new Date();

                            if (targetItem.getStartTime() != null && targetItem.getStartTime().before(currentTime)) {
                                // Nếu thời gian hiện tại đã vượt qua thời gian bắt đầu -> Chặn
                                out.println("DELETE_FAIL|Thất bại! Sản phẩm này đã bắt đầu lên sàn đấu giá.");
                                System.out.println("Server: Từ chối xóa vì item " + itemIdToDelete + " đã diễn ra.");
                            } else {
                                // 3. Nếu an toàn (thời gian chưa tới) -> Thực hiện xóa
                                boolean isItemDeleted = itemDAO.deleteItem(itemIdToDelete);

                                if (isItemDeleted) {
                                    out.println("DELETE_SUCCESS|Đã rút sản phẩm thành công.");
                                    System.out.println("Server: Xóa thành công sản phẩm ID: " + itemIdToDelete);
                                } else {
                                    out.println("DELETE_FAIL|Lỗi hệ thống Database khi xóa.");
                                }
                            }
                        }
                    }
                    case "DELETE_USER" -> {
                        try {
                            String targetUsername = parts[1]; // Lấy username từ Client gửi lên
                            System.out.println("Server: Đang xử lý yêu cầu xóa User [" + targetUsername + "]");

                            // Gọi hàm xóa trong UserDAO
                            boolean isDeleted = userDAO.deleteUser(targetUsername);

                            if (isDeleted) {
                                out.println("DELETE_SUCCESS|Đã xóa người dùng thành công!");
                                System.out.println("Server: Đã xóa xong User " + targetUsername);
                            } else {
                                out.println("ERROR|Không tìm thấy tài khoản hoặc không thể xóa!");
                            }
                        } catch (Exception e) {
                            out.println("ERROR|Lỗi server khi xóa tài khoản");
                            e.printStackTrace();
                        }
                    }
                    }}
        } catch (IOException e) {
            System.out.println("Lỗi mạng! Client đã ngắt kết nối.");
        }
    }
}