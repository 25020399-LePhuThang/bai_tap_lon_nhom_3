package com.auction.client.network;

import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.user.Admin;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;
import com.auction.shared.model.item.Art;
import com.auction.shared.model.item.Vehicle;
import com.auction.shared.model.item.Electronic;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class NetworkClient {
    private static NetworkClient instance;
    private static Socket socket;
    private static BufferedReader in;
    private static PrintWriter out;

    private static final String SERVER_IP ="zephyr.proxy.rlwy.net";
    private static final int SERVER_PORT = 37065;

    // Callback được gọi mỗi khi nhận được BID_UPDATE từ server
    private Consumer<String> bidUpdateListener;

    // Thread lắng nghe server push tin về
    private Thread listenerThread;
    private static Socket listenerSocket;
    private static BufferedReader listenerIn;

    private NetworkClient() {
        connect(SERVER_IP, SERVER_PORT);
    }

    public static NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }
    private static boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(5000); // Khiên bảo vệ 5s
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Client: Nối ống nước thành công, đã bật khiên bảo vệ 5s!");
            return true;
        } catch (Exception e) {
            System.err.println("Client: Lỗi kết nối Server: " + e.getMessage());
            return false;
        }
    }

    private static boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    private static void closeAll() {
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        in = null; out = null; socket = null;
    }

    // =========================================================================
    // HÀM GỬI NHẬN ĐỒNG BỘ CHUNG (SYNCHRONIZED)
    // =========================================================================
    public static synchronized String sendAndReceive(String message) {
        if (!isConnected()) {
            System.out.println("Client: Mất kết nối, đang thử nối lại...");
            if (!connect(SERVER_IP, SERVER_PORT)) {
                return "ERROR|Không thể kết nối đến Server.";
            }
        }
        if (out == null) return "ERROR|Lỗi cấu hình Socket nội bộ";
        try {
            out.println(message);

            String response;
            while ((response = in.readLine()) != null) {
                // BỘ LỌC RÁC: Nếu Server ném nhầm BID_UPDATE vào ống chính, vứt nó đi!
                // Vì luồng Listener (ống số 2) đã lo việc bắt giá live rồi.
                if (response.startsWith("BID_UPDATE|")) {
                    continue;
                }
                return response; // Trả về đúng dữ liệu mình cần
            }
            return "ERROR|Kết nối bị đóng bởi Server";

        } catch (SocketTimeoutException timeout) {
            // ✅ ĐÃ FIX: Bắt buộc phải đóng ống nước để chống kẹt dữ liệu!
            closeAll();
            return "ERROR|Server phản hồi quá chậm! Đã reset luồng mạng.";
        } catch (IOException e) {
            closeAll();
            return "ERROR|Đứt cáp mạng giữa chừng!";
        }
    }
    private static String safe(String s) {
        return s == null ? "" : s.replace("\n", "").replace("\r", "").replace("|", "");
    }

    public static String login(String username, String password, String role) {
        String msg = "LOGIN|" + safe(username) + "|" + safe(password) + "|" + safe(role);
        return sendAndReceive(msg);
    }

    public static boolean sendRegisterRequest(String name, String password, String email, String phone, String role) {
        String msg = "REGISTER|" + safe(name) + "|" + safe(password) + "|" + safe(email) + "|" + safe(phone) + "|" + safe(role);
        String response = sendAndReceive(msg);
        System.out.println("Client: Phản hồi Đăng ký -> " + response);
        return response != null && (response.startsWith("REGISTER_SUCCESS") || response.startsWith("OK"));
    }

    public static String sendUpdateInfoRequest(String currentUsername, String newUsername, String newEmail, String newPhone, String newAddress) {
        String msg = "UPDATE_INFO|" + safe(currentUsername) + "|" + safe(newUsername) + "|" + safe(newEmail) + "|" + safe(newPhone) + "|" + safe(newAddress);
        return sendAndReceive(msg);
    }

    public static String sendChangePasswordRequest(String currentUsername, String oldPass, String newPass) {
        String msg = "CHANGE_PASS|" + safe(currentUsername) + "|" + safe(oldPass) + "|" + safe(newPass);
        return sendAndReceive(msg);
    }

    // Thêm String username vào trong ngoặc
    public static void disconnect(String username) {
        try {
            String userToLogout = (username != null) ? username : "UNKNOWN";
            out.println("LOGOUT|" + userToLogout);
            closeAll();
            System.out.println("Client: Đã ngắt kết nối an toàn khỏi Server!");
        } catch (Exception e) {
            System.err.println("Lỗi khi ngắt kết nối: " + e.getMessage());
        }
    }

    public static User getUserInfo(String username) {
        try {
            // ĐÃ ĐƯỢC ĐƯA VÀO LUỒNG AN TOÀN SYNCHRONIZED
            String response = sendAndReceive("GET_USER_INFO|" + username);

            if (response != null && response.startsWith("USER_INFO_SUCCESS|")) {
                String jsonString = response.split("\\|", 2)[1];

                Gson gson = new GsonBuilder()

                        .registerTypeAdapter(User.class, new com.google.gson.JsonDeserializer<User>() {
                            @Override
                            public User deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfT, com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {
                                com.google.gson.JsonObject jsonObject = json.getAsJsonObject();
                                String role = jsonObject.has("role") ? jsonObject.get("role").getAsString().toUpperCase() : "";

                                if (role.equals("BIDDER")) return context.deserialize(json, Bidder.class);
                                else if (role.equals("SELLER")) return context.deserialize(json, Seller.class);
                                else if (role.equals("ADMIN")) return context.deserialize(json, Admin.class);

                                throw new com.google.gson.JsonParseException("Không nhận diện được vai trò: " + role);
                            }
                        })

                        .registerTypeAdapter(java.time.LocalDateTime.class, new com.google.gson.JsonDeserializer<java.time.LocalDateTime>() {
                            @Override
                            public java.time.LocalDateTime deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfT, com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {
                                return java.time.LocalDateTime.parse(json.getAsString(), java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            }
                        })

                        // 3. Dạy Gson cách ghi LocalDateTime (Dùng khi Client của cậu gửi gói tin lên Server)
                        .registerTypeAdapter(java.time.LocalDateTime.class, new com.google.gson.JsonSerializer<java.time.LocalDateTime>() {
                            @Override
                            public com.google.gson.JsonElement serialize(java.time.LocalDateTime src, java.lang.reflect.Type typeOfSrc, com.google.gson.JsonSerializationContext context) {
                                return new com.google.gson.JsonPrimitive(src.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                            }
                        })

                        .create(); // Chốt sổ tạo ra cục Gson hoàn hảo
                return gson.fromJson(jsonString, User.class);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy thông tin User: " + e.getMessage());
        }
        return null;
    }
    public static String getBalanceRequest(String username) {
        return sendAndReceive("GET_BALANCE|" + safe(username));
    }

    public static String sendDepositRequest(String username, double amount) {
        return sendAndReceive("DEPOSIT|" + safe(username) + "|" + amount);
    }

    public static String sendWithdrawRequest(String username, double amount) {
        return sendAndReceive("WITHDRAW|" + safe(username) + "|" + amount);
    }

    // =========================================================================
    // CÁC HÀM XỬ LÝ HÀNG HÓA VÀ ĐẤU GIÁ
    // =========================================================================

    public static List<Item> takeActiveItems() {
        return parseItems(sendAndReceive("GET_ACTIVE_ITEMS"));
    }

    public static List<Item> getAllActiveItemsRequest() {
        return takeActiveItems();
    }

    public static List<Item> takePreparedItems() {
        return parseItems(sendAndReceive("GET_PREPARED_ITEMS"));
    }

    public static List<Item> getItemsBySellerIdRequest(String username) {
        try {
            out.println("GET_ITEMS_BY_SELLER|" + username);
            String response = in.readLine();

            if (response != null && response.startsWith("GET_SELLER_ITEMS_SUCCESS|")) {
                String json = response.substring(response.indexOf("|") + 1).trim();
                if (json.equals("[]")) return new ArrayList<>();

                List<Item> resultList = new ArrayList<>();
                Gson gson = new Gson();
                JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();

                for (JsonElement element : jsonArray) {
                    JsonObject obj = element.getAsJsonObject();
                    String type = obj.get("type").getAsString().toUpperCase();
                    switch (type) {
                        case "ELECTRONIC" -> resultList.add(gson.fromJson(obj, Electronic.class));
                        case "VEHICLE" -> resultList.add(gson.fromJson(obj, Vehicle.class));
                        case "ART" -> resultList.add(gson.fromJson(obj, Art.class));
                    }
                }
                return resultList;
            }
        } catch (Exception e) {
            System.err.println("Lỗi bóc tách JSON mảng: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    public static List<Item> parseItems(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isEmpty() || jsonResponse.startsWith("ERROR"))
            return new ArrayList<>();

        try {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Item.class, new JsonDeserializer<Item>() {
                        @Override
                        public Item deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                            JsonObject jsonObject = json.getAsJsonObject();
                            String itemType = jsonObject.has("type") ? jsonObject.get("type").getAsString() : "";

                            if (itemType.equalsIgnoreCase("Art")) return context.deserialize(json, Art.class);
                            else if (itemType.equalsIgnoreCase("Vehicle")) return context.deserialize(json, Vehicle.class);
                            else if (itemType.equalsIgnoreCase("Electronic")) return context.deserialize(json, Electronic.class);

                            throw new JsonParseException("Không nhận diện được loại Item: " + itemType);
                        }
                    }).create();

            Type listType = new TypeToken<ArrayList<Item>>() {}.getType();
            return gson.fromJson(jsonResponse, listType);
        } catch (Exception e) {
            System.err.println("Lỗi bóc tách JSON Item: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static boolean createItemRequest(Item item) {
        try {
            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
            String jsonItem = gson.toJson(item);
            String response = sendAndReceive("CREATE_ITEM|" + jsonItem);
            System.out.println("Client: Server phản hồi thêm sản phẩm -> " + response);
            return response != null && response.startsWith("CREATE_ITEM_SUCCESS");
        } catch (Exception e) {
            System.err.println("Lỗi khi đóng gói gửi sản phẩm: " + e.getMessage());
            return false;
        }
    }

    public static List<User> getAllUsers() {
        try {
            out.println("GET_ALL_USERS");
            String response = in.readLine();

            if (response != null && response.startsWith("ALL_USERS_SUCCESS|")) {
                String jsonStr = response.substring("ALL_USERS_SUCCESS|".length());
                com.google.gson.Gson customGson = new com.google.gson.GsonBuilder()
                        .registerTypeAdapter(User.class, new com.google.gson.JsonDeserializer<User>() {
                            @Override
                            public User deserialize(com.google.gson.JsonElement json, Type typeOfT, com.google.gson.JsonDeserializationContext context) {
                                com.google.gson.JsonObject jsonObject = json.getAsJsonObject();
                                String role = jsonObject.has("role") ? jsonObject.get("role").getAsString().toUpperCase() : "";

                                if (role.equals("BIDDER")) return context.deserialize(json, Bidder.class);
                                if (role.equals("SELLER")) return context.deserialize(json, Seller.class);
                                if (role.equals("ADMIN")) return context.deserialize(json, Admin.class);
                                return null;
                            }
                        }).create();

                Type listType = new TypeToken<ArrayList<User>>(){}.getType();
                return customGson.fromJson(jsonStr, listType);
            }
        } catch (Exception e) {
            System.err.println("Lỗi kết nối khi lấy danh sách User: " + e.getMessage());
        }
        return new ArrayList<>();
    }


    public static boolean approveItem(String itemId) {
        String response = sendAndReceive("APPROVE_ITEM|" + itemId);
        return response != null && response.equals("APPROVE_SUCCESS");
    }

    public static boolean rejectItem(String itemId) {
        try {
            out.println("REJECT_ITEM|" + itemId);
            String response = in.readLine();
            return response != null && response.equals("REJECT_SUCCESS");
        } catch (Exception e) {
            System.err.println("Lỗi kết nối khi xóa SP: " + e.getMessage());
            return false;
        }
    }

    public static boolean banUser(String userId) {
        try {
            out.println("BAN_USER|" + userId);
            String response = in.readLine();
            return response != null && response.equals("BAN_SUCCESS");
        } catch (Exception e) {
            System.err.println("Lỗi kết nối khi khóa User: " + e.getMessage());
            return false;
        }
    }

    public static boolean unbanUser(String userId) {
        try {
            out.println("UNBAN_USER|" + userId);
            String response = in.readLine();
            return response != null && response.equals("UNBAN_SUCCESS");
        } catch (Exception e) {
            System.err.println("Lỗi kết nối khi mở khóa User: " + e.getMessage());
            return false;
        }
    }

    public static List<Item> takeWaitingItemRequest() {
        try {
            out.println("GET_WAITING_ITEMS");
            String response = in.readLine();

            if (response != null && response.startsWith("WAITING_ITEMS_SUCCESS|")) {
                String jsonStr = response.substring("WAITING_ITEMS_SUCCESS|".length());
                com.google.gson.Gson customGson = new com.google.gson.GsonBuilder()
                        .registerTypeAdapter(Item.class, new com.google.gson.JsonDeserializer<Item>() {
                            @Override
                            public Item deserialize(com.google.gson.JsonElement json, Type typeOfT, com.google.gson.JsonDeserializationContext context) {
                                com.google.gson.JsonObject jsonObject = json.getAsJsonObject();
                                String type = jsonObject.has("type") ? jsonObject.get("type").getAsString() : "";

                                if (type.equalsIgnoreCase("Electronic")) return context.deserialize(json, Electronic.class);
                                if (type.equalsIgnoreCase("Art")) return context.deserialize(json, Art.class);
                                if (type.equalsIgnoreCase("Vehicle")) return context.deserialize(json, Vehicle.class);
                                return null;
                            }
                        }).create();

                Type listType = new TypeToken<ArrayList<Item>>(){}.getType();
                return customGson.fromJson(jsonStr, listType);
            }
        } catch (Exception e) {
            System.err.println("Lỗi lấy danh sách SP chờ duyệt: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Lấy lịch sử bid của một sản phẩm (sắp xếp tăng dần) để vẽ LineChart.
     * Gửi GET_BID_HISTORY|itemId → server trả về JSON array.
     */
    public static List<BidTransaction> getBidHistory(String itemId) {
        String response = sendAndReceive("GET_BID_HISTORY|" + itemId);
        if (response == null || response.isEmpty()) return new ArrayList<>();
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<BidTransaction>>(){}.getType();
        return gson.fromJson(response, listType);
    }

    // Trong file NetworkClient.java

    // Bỏ hàm stopListening cũ đi, sửa lại cấu trúc như sau:
    public void startListening(Consumer<String> listener) {
        this.bidUpdateListener = listener; // Chỉ thay đổi hàm xử lý giao diện

        // Nếu Thread lắng nghe đã chạy rồi thì KHÔNG tạo mới Socket nữa, cứ để nó chạy ngầm
        if (listenerThread != null && listenerThread.isAlive()) {
            System.out.println("Client: Sử dụng lại ống nước Realtime đang có.");
            return;
        }

        // Chỉ kết nối lần đầu tiên duy nhất
        listenerThread = new Thread(() -> {
            try {
                System.out.println("Client: Đang thiết lập ống Realtime vĩnh viễn lên Cloud...");
                listenerSocket = new Socket(SERVER_IP, SERVER_PORT);
                listenerIn = new BufferedReader(new InputStreamReader(listenerSocket.getInputStream()));
                PrintWriter listenerOut = new PrintWriter(listenerSocket.getOutputStream(), true);

                listenerOut.println("LISTEN_ONLY");

                String line;
                while ((line = listenerIn.readLine()) != null) {
                    if (bidUpdateListener != null) {
                        bidUpdateListener.accept(line); // Đẩy data về màn hình đang active
                    }
                }
            } catch (IOException e) {
                System.err.println("Listener bị ngắt kết nối: " + e.getMessage());
            }
        }, "bid-listener-thread");

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    // Hàm này bây giờ chỉ làm nhiệm vụ ngắt gắn kết giao diện, giữ nguyên kết nối mạng
    public void detachListener() {
        this.bidUpdateListener = null;
    }
    /**
     * Gửi yêu cầu xóa tài khoản lên Server
     * Trả về nguyên chuỗi phản hồi từ Server (vd: "DELETE_SUCCESS|..." hoặc "DELETE_FAIL|...")
     */
    public static String deleteUser(String username) {
        try {
            out.println("DELETE_USER|" + username);

            String responseStr = in.readLine();
            System.out.println("Client nhận phản hồi xóa: " + responseStr);
            if (responseStr != null) {
                return responseStr;
            } else {
                return "DELETE_FAIL|Lỗi: Không nhận được phản hồi từ Server.";
            }
        } catch (Exception e) {
            System.out.println("Lỗi mất kết nối khi xóa User: " + e.getMessage());
            e.printStackTrace();
            return "DELETE_FAIL|Lỗi mạng: Mất kết nối tới Server.";
        }
    }

    /**
     * Gửi yêu cầu rút/xóa sản phẩm (Item) lên Server dựa vào ID
     * Trả về nguyên chuỗi phản hồi từ Server (vd: "DELETE_SUCCESS|..." hoặc "DELETE_FAIL|...")
     */
    public static String deleteItem(String itemId) {
        String responseStr = sendAndReceive("DELETE_ITEM|" + itemId);
        if (responseStr != null && !responseStr.startsWith("ERROR")) {
            return responseStr;
        } else {
            return "DELETE_FAIL|Lỗi: Không nhận được phản hồi từ Server.";
        }
    }

    // Nằm trong file NetworkClient.java

    /**
     * Gỡ bỏ hoàn toàn bộ lắng nghe của giao diện hiện tại,
     * nhưng giữ nguyên kết nối Socket ngầm lên Cloud.
     */
}