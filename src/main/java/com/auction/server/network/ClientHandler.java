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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import com.auction.server.controller.AntiSnipingPolicy;

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
     * ĐÃ THÊM FLUSH() ĐỂ ÉP ĐẨY DỮ LIỆU QUA MẠNG NGAY LẬP TỨC
     */
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
            out.flush(); // Cứu tinh chống Timeout là đây
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

                        sendMessage(RegisterHandler.processRegister(username, password, email, phone, role));
                    }

                    // ==============================================================
                    // XỬ LÝ YÊU CẦU ĐĂNG NHẬP
                    // ==============================================================
                    case "LOGIN" -> {
                        try {
                            String username = parts[1];
                            String password = parts[2];
                            String role = parts[3];

                            User loggedInUser = UserDAO.Login(username, password, role);

                            if (loggedInUser != null) {
                                if ("BANNED".equalsIgnoreCase(loggedInUser.getStatus())) {
                                    sendMessage("LOGIN_BANNED");
                                } else {
                                    sendMessage("LOGIN_SUCCESS|" + loggedInUser.getRole());
                                }
                            } else {
                                sendMessage("LOGIN_FAIL");
                            }
                        } catch (Exception e) {
                            System.err.println("Lỗi xử lý LOGIN trên Server: " + e.getMessage());
                            sendMessage("ERROR|Lỗi hệ thống khi đăng nhập");
                        }
                    }

                    // 3a. BID:
                    case "BID" -> {
                        String user = parts[1];
                        double price = Double.parseDouble(parts[2]);
                        String itemId = parts[3].trim();
                        System.out.println("===> CHECK SERVER NHẬN ĐƯỢC:");
                        System.out.println("User: " + parts[1] + " | Price: " + parts[2] + " | ItemID: '" + itemId + "'");

                        Item targetItem = AuctionServer.itemCache.get(itemId);
                        if (targetItem == null) {
                            targetItem = itemDAO.getItemById(itemId);
                            if (targetItem != null) {
                                AuctionServer.itemCache.put(itemId, targetItem);
                            }
                        }else {
                            // [FIX LỖI BÓNG MA CACHE]
                            Item freshData = itemDAO.getItemById(itemId);
                            if (freshData != null) {
                                targetItem.setStartTime(freshData.getStartTime());
                                targetItem.setEndTime(freshData.getEndTime());
                            }
                        }

                        if (targetItem != null) {
                            String result = biddingService.placeBid(targetItem, price, user);
                            sendMessage(result);

                            if (result.startsWith("THÀNH CÔNG")) {
                                double finalPrice = targetItem.getCurrentPrice();
                                String finalWinner = targetItem.getLastBidderId();

                                BidTransaction tx = new BidTransaction(user, itemId, finalPrice);
                                bidDAO.save(tx);

                                AuctionServer.broadcast(
                                        "BID_UPDATE|" + itemId + "|" + finalPrice + "|" + finalWinner
                                );
                            }
                        } else {
                            sendMessage("BID_FAIL|Sản phẩm không tồn tại!");
                        }
                    }

                    // 3b. AUTO-BID
                    case "AUTO_BID" -> {
                        String bidderId  = parts[1];
                        double maxBid    = Double.parseDouble(parts[2]);
                        double increment = Double.parseDouble(parts[3]);
                        String itemId    = parts[4];

                        Item targetItem = AuctionServer.itemCache.computeIfAbsent(
                                itemId,
                                id -> itemDAO.getItemById(id)
                        );

                        if (targetItem != null) {
                            String result = biddingService.registerAutoBid(
                                    targetItem, bidderId, maxBid, increment);
                            sendMessage(result);

                            if (result.startsWith("AutoBid") || result.startsWith("THÀNH CÔNG")) {
                                // Nếu nhóm có hàm saveAutoBid trong DB thì dùng, nếu không có thể bỏ qua dòng saveAutoBid
                                try {
                                    bidDAO.saveAutoBid(bidderId, itemId, maxBid, increment);
                                } catch (Exception ignored) {}

                                double finalPrice = targetItem.getCurrentPrice();
                                String finalWinner = targetItem.getLastBidderId();
                                itemDAO.updatePrice(targetItem);
                                BidTransaction tx = new BidTransaction(finalWinner, itemId, finalPrice);
                                bidDAO.save(tx);
                                AuctionServer.broadcast(
                                        "BID_UPDATE|" + itemId + "|" + finalPrice + "|" + finalWinner
                                );
                            }
                        } else {
                            sendMessage("AUTO_BID_FAIL|Sản phẩm không tồn tại!");
                        }
                    }

                    // [Gộp từ bản cũ] Xử lý việc UI muốn kiểm tra xem có lưu AutoBid dưới DB không
                    case "CHECK_MY_AUTOBID" -> {
                        try {
                            String username = parts[1];
                            String itemId = parts[2];
                            double maxBid = bidDAO.getAutoBidValue(username, itemId);
                            if (maxBid > 0) {
                                sendMessage("YES|" + maxBid);
                            } else {
                                sendMessage("NO");
                            }
                        } catch (Exception e) {
                            sendMessage("NO");
                        }
                    }

                    // 4. GET PREPARED ITEMS
                    case "GET_PREPARED_ITEMS" -> {
                        List<Item> preparedList = itemDAO.getPreparedItems();
                        String json = new Gson().toJson(preparedList);
                        sendMessage(json);
                    }

                    // 5. GET ACTIVE ITEMS
                    case "GET_ACTIVE_ITEMS" -> {
                        List<Item> activeList = itemDAO.getActiveItems();
                        String json = new Gson().toJson(activeList);
                        sendMessage(json);
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
                                sendMessage("UPDATE_SUCCESS|Cập nhật thông tin thành công rồi cưng!");
                                break;
                            case "ERR_USERNAME":
                                sendMessage("UPDATE_FAIL|Tên đăng nhập mới này đã có người sử dụng!");
                                break;
                            case "ERR_EMAIL":
                                sendMessage("UPDATE_FAIL|Email mới này đã có người sử dụng!");
                                break;
                            case "ERR_PHONE":
                                sendMessage("UPDATE_FAIL|Số điện thoại mới này đã có người sử dụng!");
                                break;
                            default:
                                sendMessage("UPDATE_FAIL|Lỗi hệ thống Database, không thể cập nhật thông tin.");
                                break;
                        }
                    }

                    // 7. XỬ LÝ ĐỔI MẬT KHẨU
                    case "CHANGE_PASS" -> {
                        String username = parts[1];
                        String oldPassword = parts[2];
                        String newPassword = parts[3];

                        String result = userDAO.changePassword(username, oldPassword, newPassword);
                        sendMessage(result);
                    }

                    // 8. XỬ LÝ NẠP TIỀN
                    case "DEPOSIT" -> {
                        String username = parts[1];
                        double amount = Double.parseDouble(parts[2]);
                        double currentBalance = userDAO.getBalance(username);

                        if (currentBalance + amount > 1000000.0) {
                            sendMessage("DEPOSIT_FAIL|Nạp thất bại! Hạn mức ví tối đa chỉ là 1,000,000$.");
                        } else {
                            boolean isSuccess = userDAO.deposit(username, amount);
                            if (isSuccess) {
                                double newBalance = userDAO.getBalance(username);
                                sendMessage("DEPOSIT_SUCCESS|" + newBalance);
                            } else {
                                sendMessage("DEPOSIT_FAIL|Lỗi hệ thống Database khi nạp tiền.");
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
                            sendMessage("WITHDRAW_SUCCESS|" + newBalance);
                        } else {
                            sendMessage("WITHDRAW_FAIL|Số dư không đủ hoặc lỗi Database!");
                        }
                    }

                    // 10. XỬ LÝ LẤY SỐ DƯ TÀI KHOẢN
                    case "GET_BALANCE" -> {
                        String username = parts[1];
                        double currentBalance = userDAO.getBalance(username);
                        sendMessage("BALANCE_SUCCESS|" + currentBalance);
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
                                    sendMessage("GET_SELLER_ITEMS_SUCCESS|" + json);
                                } else {
                                    sendMessage("GET_SELLER_ITEMS_SUCCESS|[]");
                                }
                            } else {
                                sendMessage("GET_SELLER_ITEMS_SUCCESS|[]");
                            }

                        } catch (Exception e) {
                            System.out.println("[SEVERE] Lỗi khi xử lý GET_ITEMS_BY_SELLER: " + e.getMessage());
                            e.printStackTrace();
                            sendMessage("GET_SELLER_ITEMS_FAIL|Lỗi server!");
                        }
                    }

                    case "CREATE_ITEM" -> {
                        try {
                            String[] dataParts = message.split("\\|", 2);
                            if (dataParts.length < 2) {
                                sendMessage("ERROR|Thiếu dữ liệu JSON");
                            } else {
                                String jsonString = dataParts[1];

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

                                Item newItem = gson.fromJson(jsonString, Item.class);
                                ItemDAO itemDAO = new ItemDAO();
                                boolean isInserted = itemDAO.createItem(newItem);

                                if (isInserted) {
                                    sendMessage("CREATE_ITEM_SUCCESS");
                                } else {
                                    sendMessage("ERROR|Không lưu được vào Database");
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Lỗi khi thêm sản phẩm: " + e.getMessage());
                            e.printStackTrace();
                            sendMessage("ERROR|Lỗi dữ liệu Server");
                        }
                    }

                    // ==============================================================
                    // [ĐÃ GỘP] LISTEN_ONLY - Xử lý ngầm bắt giá Realtime của Nhung
                    // ==============================================================
                    case "LISTEN_ONLY" -> {
                        System.out.println("[SERVER] Một Client vừa đăng ký kênh nhận giá Realtime (LISTEN_ONLY)");
                        AuctionServer.activeListeners.add(this.out);
                        try {
                            String ping;
                            while ((ping = in.readLine()) != null) {
                                // Vòng lặp này giúp luồng không bị ngắt, giữ kết nối Socket sống
                            }
                        } catch (IOException e) {
                            System.out.println("[SERVER] Client ngắt kết nối kênh Realtime.");
                        } finally {
                            AuctionServer.activeListeners.remove(this.out); // Rút ống thở an toàn
                        }
                    }

                    case "LOGOUT" -> {
                        System.out.println("Một Client đã yêu cầu đăng xuất và ngắt kết nối.");
                        sendMessage("LOGOUT_SUCCESS");
                        return;
                    }

                    case "GET_USER_INFO" -> {
                        try {
                            String targetUsername = parts[1];
                            UserDAO userDAO = new UserDAO();
                            User user = userDAO.getUserByUsername(targetUsername);

                            if (user != null) {
                                String jsonString = new Gson().toJson(user);
                                sendMessage("USER_INFO_SUCCESS|" + jsonString);
                            } else {
                                sendMessage("ERROR|Không tìm thấy người dùng");
                            }
                        } catch (Exception e) {
                            sendMessage("ERROR|Lỗi server khi lấy thông tin");
                            e.printStackTrace();
                        }
                    }

                    // ==============================================================
                    // TÍNH NĂNG ADMIN: QUẢN LÝ SẢN PHẨM & NGƯỜI DÙNG
                    // ==============================================================
                    case "GET_WAITING_ITEMS" -> {
                        try {
                            ItemDAO itemDAO = new ItemDAO();
                            List<Item> waitingList = itemDAO.takeWaitingItems();
                            String jsonResponse = new Gson().toJson(waitingList);
                            sendMessage("WAITING_ITEMS_SUCCESS|" + jsonResponse);
                        } catch (Exception e) {
                            sendMessage("ERROR|Lỗi khi lấy danh sách chờ duyệt");
                            e.printStackTrace();
                        }
                    }

                    case "APPROVE_ITEM" -> {
                        try {
                            String itemId = parts[1];
                            boolean success = ItemDAO.approveItemWithTimeCheck(itemId);
                            if (success) {
                                sendMessage("APPROVE_SUCCESS");
                            } else {
                                sendMessage("ERROR|Không thể duyệt sản phẩm này");
                            }
                        } catch (Exception e) {
                            sendMessage("ERROR|Lỗi server khi duyệt sản phẩm");
                            e.printStackTrace();
                        }
                    }

                    case "REJECT_ITEM" -> {
                        try {
                            String itemId = parts[1];
                            boolean success = ItemDAO.deleteItem(itemId);
                            if (success) {
                                sendMessage("REJECT_SUCCESS");
                            } else {
                                sendMessage("ERROR|Không thể xóa sản phẩm này");
                            }
                        } catch (Exception e) {
                            sendMessage("ERROR|Lỗi server khi từ chối sản phẩm");
                            e.printStackTrace();
                        }
                    }

                    case "GET_ALL_USERS" -> {
                        try {
                            UserDAO userDAO = new UserDAO();
                            List<User> userList = userDAO.getAllUsers();
                            String jsonResponse = new Gson().toJson(userList);
                            sendMessage("ALL_USERS_SUCCESS|" + jsonResponse);
                        } catch (Exception e) {
                            sendMessage("ERROR|Lỗi khi lấy danh sách người dùng");
                            e.printStackTrace();
                        }
                    }

                    case "BAN_USER" -> {
                        try {
                            String userId = parts[1];
                            boolean success = UserDAO.updateUserStatus(userId, "BANNED");
                            sendMessage(success ? "BAN_SUCCESS" : "ERROR|Không thể khóa user");
                        } catch (Exception e) {
                            sendMessage("ERROR|Lỗi server khi khóa user");
                        }
                    }

                    case "UNBAN_USER" -> {
                        try {
                            String userId = parts[1];
                            boolean success = UserDAO.updateUserStatus(userId, "ACTIVE");
                            sendMessage(success ? "UNBAN_SUCCESS" : "ERROR|Không thể mở khóa user");
                        } catch (Exception e) {
                            sendMessage("ERROR|Lỗi server khi mở khóa user");
                        }
                    }

                    case "DELETE_ITEM" -> {
                        String itemIdToDelete = parts[1];
                        System.out.println("Server: Đang xử lý yêu cầu xóa Item [" + itemIdToDelete + "]");
                        Item targetItem = itemDAO.getItemById(itemIdToDelete);

                        if (targetItem == null) {
                            sendMessage("DELETE_FAIL|Sản phẩm không tồn tại hoặc đã bị xóa trước đó.");
                        } else {
                            Date currentTime = new Date();
                            if (targetItem.getStartTime() != null && targetItem.getStartTime().before(currentTime)) {
                                sendMessage("DELETE_FAIL|Thất bại! Sản phẩm này đã bắt đầu lên sàn đấu giá.");
                                System.out.println("Server: Từ chối xóa vì item " + itemIdToDelete + " đã diễn ra.");
                            } else {
                                boolean isItemDeleted = itemDAO.deleteItem(itemIdToDelete);
                                if (isItemDeleted) {
                                    sendMessage("DELETE_SUCCESS|Đã rút sản phẩm thành công.");
                                    System.out.println("Server: Xóa thành công sản phẩm ID: " + itemIdToDelete);
                                } else {
                                    sendMessage("DELETE_FAIL|Lỗi hệ thống Database khi xóa.");
                                }
                            }
                        }
                    }

                    case "DELETE_USER" -> {
                        try {
                            String targetUsername = parts[1];
                            System.out.println("Server: Đang xử lý yêu cầu xóa User [" + targetUsername + "]");
                            boolean isDeleted = userDAO.deleteUser(targetUsername);

                            if (isDeleted) {
                                sendMessage("DELETE_SUCCESS|Đã xóa người dùng thành công!");
                                System.out.println("Server: Đã xóa xong User " + targetUsername);
                            } else {
                                sendMessage("ERROR|Không tìm thấy tài khoản hoặc không thể xóa!");
                            }
                        } catch (Exception e) {
                            sendMessage("ERROR|Lỗi server khi xóa tài khoản");
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Lỗi mạng! Client đã ngắt kết nối.");
        }
    }
}