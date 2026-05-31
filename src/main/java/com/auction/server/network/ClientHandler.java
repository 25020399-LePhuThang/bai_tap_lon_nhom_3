package com.auction.server.network;

import com.auction.server.controller.BiddingService;
import com.auction.server.controller.RegisterHandler;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.UserDAO;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.item.Art;
import com.auction.shared.model.item.Electronic;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.Vehicle;
import com.auction.shared.model.user.User;
import com.google.gson.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.Date;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final BiddingService biddingService;

    private final UserDAO userDAO = new UserDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final BidDAO bidDAO = new BidDAO();

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

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
            out.flush(); // Ép đẩy dữ liệu qua mạng ngay lập tức
        }
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                String[] parts = message.split("\\|", -1);
                String action = parts[0];

                // Bộ định tuyến cực kỳ gọn gàng
                switch (action) {
                    case "REGISTER" -> handleRegister(parts);
                    case "LOGIN" -> handleLogin(parts);
                    case "BID" -> handleBid(parts);
                    case "AUTO_BID" -> handleAutoBid(parts);
                    case "CHECK_MY_AUTOBID" -> handleCheckAutoBid(parts);
                    case "GET_PREPARED_ITEMS" -> handleGetPreparedItems();
                    case "GET_ACTIVE_ITEMS" -> handleGetActiveItems();
                    case "UPDATE_INFO" -> handleUpdateInfo(parts);
                    case "CHANGE_PASS" -> handleChangePass(parts);
                    case "DEPOSIT" -> handleDeposit(parts);
                    case "WITHDRAW" -> handleWithdraw(parts);
                    case "GET_BALANCE" -> handleGetBalance(parts);
                    case "GET_ITEMS_BY_SELLER" -> handleGetItemsBySeller(parts);
                    case "CREATE_ITEM" -> handleCreateItem(message);
                    case "LISTEN_ONLY" -> handleListenOnly();
                    case "LOGOUT" -> handleLogout();
                    case "GET_USER_INFO" -> handleGetUserInfo(parts);
                    case "DEDUCT_MONEY" -> handleDeductMoney(parts);

                    // --- Chức năng Admin ---
                    case "GET_WAITING_ITEMS" -> handleGetWaitingItems();
                    case "APPROVE_ITEM" -> handleApproveItem(parts);
                    case "REJECT_ITEM" -> handleRejectItem(parts);
                    case "GET_ALL_USERS" -> handleGetAllUsers();
                    case "BAN_USER" -> handleBanUser(parts);
                    case "UNBAN_USER" -> handleUnbanUser(parts);
                    case "DELETE_ITEM" -> handleDeleteItem(parts);
                    case "DELETE_USER" -> handleDeleteUser(parts);
                    default -> System.out.println("Unknown action: " + action);
                }
            }
        } catch (IOException e) {
            System.out.println("Lỗi mạng! Client đã ngắt kết nối.");
        }
    }

    // ====================================================================================
    // CÁC HÀM XỬ LÝ LOGIC CHI TIẾT (ĐƯỢC TÁCH RA ĐỂ CODE SẠCH SẼ & DỄ BẢO TRÌ HƠN)
    // ====================================================================================

    private void handleRegister(String[] parts) {
        String username = parts[1];
        String password = parts[2];
        String email = parts[3];
        String phone = parts[4];
        String role = parts[5];
        sendMessage(RegisterHandler.processRegister(username, password, email, phone, role));
    }

    private void handleLogin(String[] parts) {
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

    private void handleBid(String[] parts) {
        String user = parts[1];
        double price = Double.parseDouble(parts[2]);
        String itemId = parts[3].trim();
        System.out.println("===> CHECK SERVER NHẬN ĐƯỢC: User: " + user + " | Price: " + price + " | ItemID: '" + itemId + "'");

        Item targetItem = AuctionServer.itemCache.get(itemId);
        if (targetItem == null) {
            targetItem = itemDAO.getItemById(itemId);
            if (targetItem != null) AuctionServer.itemCache.put(itemId, targetItem);
        } else {
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
                bidDAO.save(new BidTransaction(user, itemId, finalPrice));
                AuctionServer.broadcast("BID_UPDATE|" + itemId + "|" + finalPrice + "|" + finalWinner);
            }
        } else {
            sendMessage("BID_FAIL|Sản phẩm không tồn tại!");
        }
    }

    private void handleAutoBid(String[] parts) {
        String bidderId = parts[1];
        double maxBid = Double.parseDouble(parts[2]);
        double increment = Double.parseDouble(parts[3]);
        String itemId = parts[4];

        Item targetItem = AuctionServer.itemCache.computeIfAbsent(itemId, id -> itemDAO.getItemById(id));

        if (targetItem != null) {
            String result = biddingService.registerAutoBid(targetItem, bidderId, maxBid, increment);
            sendMessage(result);

            if (result.startsWith("AutoBid") || result.startsWith("THÀNH CÔNG")) {
                try { bidDAO.saveAutoBid(bidderId, itemId, maxBid, increment); } catch (Exception ignored) {}
                double finalPrice = targetItem.getCurrentPrice();
                String finalWinner = targetItem.getLastBidderId();
                itemDAO.updatePrice(targetItem);
                bidDAO.save(new BidTransaction(finalWinner, itemId, finalPrice));
                AuctionServer.broadcast("BID_UPDATE|" + itemId + "|" + finalPrice + "|" + finalWinner);
            }
        } else {
            sendMessage("AUTO_BID_FAIL|Sản phẩm không tồn tại!");
        }
    }

    private void handleCheckAutoBid(String[] parts) {
        try {
            double maxBid = bidDAO.getAutoBidValue(parts[1], parts[2]);
            sendMessage(maxBid > 0 ? "YES|" + maxBid : "NO");
        } catch (Exception e) {
            sendMessage("NO");
        }
    }

    private void handleGetPreparedItems() {
        sendMessage(new Gson().toJson(itemDAO.getPreparedItems()));
    }

    private void handleGetActiveItems() {
        sendMessage(new Gson().toJson(itemDAO.getActiveItems()));
    }

    private void handleUpdateInfo(String[] parts) {
        String result = userDAO.updateUserInfo(parts[1], parts[2], parts[3], parts[4], parts[5]);
        switch (result) {
            case "SUCCESS" -> sendMessage("UPDATE_SUCCESS|Cập nhật thông tin thành công rồi cưng!");
            case "ERR_USERNAME" -> sendMessage("UPDATE_FAIL|Tên đăng nhập mới này đã có người sử dụng!");
            case "ERR_EMAIL" -> sendMessage("UPDATE_FAIL|Email mới này đã có người sử dụng!");
            case "ERR_PHONE" -> sendMessage("UPDATE_FAIL|Số điện thoại mới này đã có người sử dụng!");
            default -> sendMessage("UPDATE_FAIL|Lỗi hệ thống Database, không thể cập nhật thông tin.");
        }
    }

    private void handleChangePass(String[] parts) {
        sendMessage(userDAO.changePassword(parts[1], parts[2], parts[3]));
    }

    private void handleDeposit(String[] parts) {
        String username = parts[1];
        double amount = Double.parseDouble(parts[2]);
        if (userDAO.getBalance(username) + amount > 1000000.0) {
            sendMessage("DEPOSIT_FAIL|Nạp thất bại! Hạn mức ví tối đa chỉ là 1,000,000$.");
        } else {
            sendMessage(userDAO.deposit(username, amount) ? "DEPOSIT_SUCCESS|" + userDAO.getBalance(username) : "DEPOSIT_FAIL|Lỗi hệ thống Database khi nạp tiền.");
        }
    }

    private void handleWithdraw(String[] parts) {
        String username = parts[1];
        sendMessage("SUCCESS".equals(userDAO.withdraw(username, Double.parseDouble(parts[2])))
                ? "WITHDRAW_SUCCESS|" + userDAO.getBalance(username)
                : "WITHDRAW_FAIL|Số dư không đủ hoặc lỗi Database!");
    }

    private void handleGetBalance(String[] parts) {
        sendMessage("BALANCE_SUCCESS|" + userDAO.getBalance(parts[1]));
    }

    private void handleGetItemsBySeller(String[] parts) {
        try {
            int sellerId = userDAO.getIdByUsername(parts[1]);
            if (sellerId != -1) {
                List<Item> sellerItems = itemDAO.getItemsBySellerID(sellerId);
                sendMessage("GET_SELLER_ITEMS_SUCCESS|" + (sellerItems != null && !sellerItems.isEmpty() ? new Gson().toJson(sellerItems) : "[]"));
            } else {
                sendMessage("GET_SELLER_ITEMS_SUCCESS|[]");
            }
        } catch (Exception e) {
            sendMessage("GET_SELLER_ITEMS_FAIL|Lỗi server!");
        }
    }

    private void handleCreateItem(String message) {
        try {
            String[] dataParts = message.split("\\|", 2);
            if (dataParts.length < 2) {
                sendMessage("ERROR|Thiếu dữ liệu JSON");
                return;
            }
            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").registerTypeAdapter(Item.class, new JsonDeserializer<Item>() {
                @Override
                public Item deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    String itemType = json.getAsJsonObject().has("type") ? json.getAsJsonObject().get("type").getAsString() : "";
                    if (itemType.equalsIgnoreCase("Art")) return context.deserialize(json, Art.class);
                    if (itemType.equalsIgnoreCase("Vehicle")) return context.deserialize(json, Vehicle.class);
                    if (itemType.equalsIgnoreCase("Electronic")) return context.deserialize(json, Electronic.class);
                    throw new JsonParseException("Không nhận diện được: " + itemType);
                }
            }).create();

            boolean isInserted = itemDAO.createItem(gson.fromJson(dataParts[1], Item.class));
            sendMessage(isInserted ? "CREATE_ITEM_SUCCESS" : "ERROR|Không lưu được vào Database");
        } catch (Exception e) {
            sendMessage("ERROR|Lỗi dữ liệu Server");
        }
    }

    private void handleListenOnly() {
        System.out.println("[SERVER] Một Client vừa đăng ký kênh nhận giá Realtime (LISTEN_ONLY)");
        AuctionServer.activeListeners.add(this.out);
        try {
            while (in.readLine() != null) { /* Giữ kết nối Socket sống */ }
        } catch (IOException e) {
            System.out.println("[SERVER] Client ngắt kết nối kênh Realtime.");
        } finally {
            AuctionServer.activeListeners.remove(this.out);
        }
    }

    private void handleLogout() {
        System.out.println("Một Client đã yêu cầu đăng xuất và ngắt kết nối.");
        sendMessage("LOGOUT_SUCCESS");
    }

    private void handleGetUserInfo(String[] parts) {
        try {
            User user = userDAO.getUserByUsername(parts[1]);
            sendMessage(user != null ? "USER_INFO_SUCCESS|" + new Gson().toJson(user) : "ERROR|Không tìm thấy người dùng");
        } catch (Exception e) {
            sendMessage("ERROR|Lỗi server khi lấy thông tin");
        }
    }

    private void handleDeductMoney(String[] parts) {
        try {
            String winnerUsername = parts[1];
            double finalPrice = Double.parseDouble(parts[2]);
            String itemId = parts[3].trim();
            System.out.println("[SERVER - THANH TOÁN] Đang xử lý cho: " + winnerUsername + " | SP: " + itemId + " | Giá: " + finalPrice);

            Item checkItem = itemDAO.getItemById(itemId);

            // 1. Kiểm tra tồn tại và trạng thái hợp lệ
            if (checkItem != null && "ACTIVE".equalsIgnoreCase(checkItem.getStatus())) {
                String sellerUsername = checkItem.getSeller_ID();

                // 2. Gọi hàm Transaction xịn xò của BidDAO (vừa trừ tiền, cộng tiền, vừa đổi trạng thái)
                boolean paymentSuccess = bidDAO.processAuctionPayment(itemId, winnerUsername, sellerUsername, finalPrice);

                if (paymentSuccess) {
                    // 3. Bắn loa thông báo cho người bán
                    AuctionServer.broadcast("NOTIFY_SELLER|" + sellerUsername + "|" + itemId + "|" + checkItem.getName() + "|" + finalPrice);

                    // ==========================================================
                    // 4. (SỬA LỖI BÓNG MA): ÉP CLIENT CỦA NGƯỜI BÁN TẢI LẠI TIỀN
                    // ==========================================================
                    AuctionServer.broadcast("SERVER_SIGNAL_REFRESH");

                    System.out.println("[SERVER - THANH TOÁN] HOÀN TẤT: Đã chốt đơn SP " + itemId);
                    sendMessage("THÀNH CÔNG|Giao dịch hoàn tất!");
                } else {
                    sendMessage("LỖI|Thanh toán thất bại (Không đủ số dư hoặc lỗi Database)!");
                }
            } else {
                sendMessage("THÀNH CÔNG|Sản phẩm đã được thanh toán trước đó.");
            }
        } catch (Exception e) {
            System.err.println("[SERVER] Sự cố ngoại lệ khi thanh toán: " + e.getMessage());
            sendMessage("LỖI|Hệ thống gặp sự cố trong quá trình trừ tiền!");
        }
    }
    private void handleGetWaitingItems() {
        try { sendMessage("WAITING_ITEMS_SUCCESS|" + new Gson().toJson(itemDAO.takeWaitingItems())); }
        catch (Exception e) { sendMessage("ERROR|Lỗi khi lấy danh sách chờ duyệt"); }
    }

    private void handleApproveItem(String[] parts) {
        try { sendMessage(ItemDAO.approveItemWithTimeCheck(parts[1]) ? "APPROVE_SUCCESS" : "ERROR|Không thể duyệt sản phẩm này"); }
        catch (Exception e) { sendMessage("ERROR|Lỗi server khi duyệt sản phẩm"); }
    }

    private void handleRejectItem(String[] parts) {
        try { sendMessage(ItemDAO.deleteItem(parts[1]) ? "REJECT_SUCCESS" : "ERROR|Không thể xóa sản phẩm này"); }
        catch (Exception e) { sendMessage("ERROR|Lỗi server khi từ chối sản phẩm"); }
    }

    private void handleGetAllUsers() {
        try { sendMessage("ALL_USERS_SUCCESS|" + new Gson().toJson(userDAO.getAllUsers())); }
        catch (Exception e) { sendMessage("ERROR|Lỗi khi lấy danh sách người dùng"); }
    }

    private void handleBanUser(String[] parts) {
        try { sendMessage(UserDAO.updateUserStatus(parts[1], "BANNED") ? "BAN_SUCCESS" : "ERROR|Không thể khóa user"); }
        catch (Exception e) { sendMessage("ERROR|Lỗi server khi khóa user"); }
    }

    private void handleUnbanUser(String[] parts) {
        try { sendMessage(UserDAO.updateUserStatus(parts[1], "ACTIVE") ? "UNBAN_SUCCESS" : "ERROR|Không thể mở khóa user"); }
        catch (Exception e) { sendMessage("ERROR|Lỗi server khi mở khóa user"); }
    }

    private void handleDeleteItem(String[] parts) {
        String itemIdToDelete = parts[1];
        Item targetItem = itemDAO.getItemById(itemIdToDelete);
        if (targetItem == null) {
            sendMessage("DELETE_FAIL|Sản phẩm không tồn tại hoặc đã bị xóa trước đó.");
        } else if (targetItem.getStartTime() != null && targetItem.getStartTime().before(new Date())) {
            sendMessage("DELETE_FAIL|Thất bại! Sản phẩm này đã bắt đầu lên sàn đấu giá.");
        } else {
            sendMessage(ItemDAO.deleteItem(itemIdToDelete) ? "DELETE_SUCCESS|Đã rút sản phẩm thành công." : "DELETE_FAIL|Lỗi hệ thống Database khi xóa.");
        }
    }

    private void handleDeleteUser(String[] parts) {
        try { sendMessage(userDAO.deleteUser(parts[1]) ? "DELETE_SUCCESS|Đã xóa người dùng thành công!" : "ERROR|Không tìm thấy tài khoản hoặc không thể xóa!"); }
        catch (Exception e) { sendMessage("ERROR|Lỗi server khi xóa tài khoản"); }
    }
}