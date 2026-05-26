package com.auction.server.dao;

import com.auction.server.database.DatabaseManager;
import com.auction.shared.model.item.Electronic;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.user.Admin;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;
import java.sql.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.out;

public class UserDAO {
    //Hàm đăng ký
    public boolean register(User user) {
        String sql = "INSERT INTO Users(username,password,email,phone,role) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, user.getName());
            preparedStatement.setString(2, user.getPassword());
            preparedStatement.setString(3, user.getEmail());
            preparedStatement.setString(4, user.getPhoneNumber());
            preparedStatement.setString(5,user.getRole().toUpperCase());
            int rows = preparedStatement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi DAO");
            return false;
        }
    }

    // 2. HÀM ĐĂNG NHẬP (Đã bổ sung xử lý BANNED và fix lỗi nhỏ ở Seller)
    public static User Login(String name, String pass, String selectedRole) {

        name = name.trim();
        pass = pass.trim();
        if (selectedRole == null) selectedRole = "BIDDER";
        selectedRole = selectedRole.trim().toUpperCase();

        System.out.println("SERVER ĐANG XỬ LÝ ĐĂNG NHẬP -> Tên: [" + name + "] | MK: [" + pass + "] | Vai trò: [" + selectedRole + "]");

        String sql = "SELECT * FROM Users WHERE username=? AND password=? AND role=?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, name);
            preparedStatement.setString(2, pass);
            preparedStatement.setString(3, selectedRole);

            try (ResultSet result = preparedStatement.executeQuery()) {
                if (result.next()) {

                    // 🌟 1. CHẶN NGAY TỪ CỬA NẾU TÀI KHOẢN BỊ KHÓA
                    String status = result.getString("status");
                    if ("BANNED".equalsIgnoreCase(status)) {
                        User bannedUser = new Bidder(); // Mượn tạm 1 class con để chứa dữ liệu
                        bannedUser.setStatus("BANNED");
                        return bannedUser; // Trả về object bị đánh dấu BANNED
                    }

                    User foundUser = null;
                    String role = result.getString("role");
                    if (role == null || role.trim().isEmpty()) {
                        role = "BIDDER";
                    }

                    // PHÂN LOẠI VÀ LẤY THUỘC TÍNH PHỤ CỦA CLASS CON TỪ DATABASE
                    switch (role.toUpperCase()) {
                        case "ADMIN":
                            Admin admin = new Admin();
                            admin.setAdminLevel(result.getString("admin_level"));
                            foundUser = admin;
                            break;

                        case "SELLER":
                            Seller seller = new Seller();
                            seller.setBalance(result.getFloat("balance"));
                            seller.setRating(result.getDouble("rating"));

                            String addedItemJson = result.getString("added_item");

                            if (addedItemJson != null && !addedItemJson.isEmpty()) {
                                Gson gson = new Gson();
                                // 1. Lấy danh sách các chuỗi (thường là ID sản phẩm) từ DB
                                Type listType = new TypeToken<ArrayList<String>>() {}.getType();
                                List<String> productIds = gson.fromJson(addedItemJson, listType);

                                // 2. Tạo một danh sách Item đúng chuẩn để nạp vào Seller
                                List<Item> itemsToSet = new ArrayList<>();
                                for (String id : productIds) {
                                    // Mượn tạm lớp con Electronic để khởi tạo (vì Item là Abstract)
                                    Electronic tempItem = new Electronic();
                                    tempItem.setId(id); // Nhét cái ID vào
                                    itemsToSet.add(tempItem);
                                }

                                // 3. Set vào Seller (Bây giờ đã chuẩn List<Item> 100%)
                                seller.setAddedItems(itemsToSet);
                            } else {
                                seller.setAddedItems(new ArrayList<>());
                            }

                            foundUser = seller;
                            break;

                        case "BIDDER":
                        default:
                            Bidder bidder = new Bidder();
                            bidder.setBalance(result.getFloat("balance"));
                            bidder.setName(result.getString("name"));
                            bidder.setMaxAutoBidLimit(result.getFloat("max_autobid_limit"));
                            bidder.setShippingAddress(result.getString("shipping_address"));
                            foundUser = bidder;
                            break;
                    }

                    // LẤY CÁC THUỘC TÍNH CHUNG CỦA LỚP CHA (USER)
                    if (foundUser != null) {
                        foundUser.setId(result.getString("user_id"));
                        foundUser.setName(result.getString("username"));
                        foundUser.setPassword(result.getString("password"));
                        foundUser.setEmail(result.getString("email"));
                        foundUser.setPhoneNumber(result.getString("phone"));
                        foundUser.setStatus(status);
                        foundUser.setRole(role.toUpperCase());
                    }
                    return foundUser;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi DAO khi đăng nhập: " + e.getMessage());
        }
        return null;
    }

    //Hàm kiểm tra xem người dùng đã có tài khoản chưa
    public boolean Exist(String name, String email, String phone) {
        String sql = "SELECT * FROM Users WHERE username=? OR email=? OR phone=?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, email);
            preparedStatement.setString(3, phone);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deposit(String username, double amount) {
        if (amount <= 0) return false;

        String sql = "UPDATE Users SET balance = balance + ? WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, amount);
            pstmt.setString(2, username);

            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi DAO khi nạp tiền: " + e.getMessage());
            return false;
        }
    }

    public String withdraw(String username, double amount) {
        // 1. Kiểm tra số tiền hợp lệ (Trả về chuỗi thay vì false)
        if (amount <= 0) return "INVALID_AMOUNT";

        // SQL cực kỳ thông minh: Chỉ cho phép trừ nếu balance >= số tiền rút
        String sql = "UPDATE Users SET balance = balance - ? WHERE username = ? AND balance >= ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, amount);
            pstmt.setString(2, username);
            pstmt.setDouble(3, amount);

            int rows = pstmt.executeUpdate();

            // 2. Nếu rows > 0 tức là trừ tiền thành công.
            // Nếu = 0 tức là không đủ tiền (hoặc sai user), trả về FAIL
            return rows > 0 ? "SUCCESS" : "FAIL";

        } catch (SQLException e) {
            System.err.println("Lỗi DAO khi rút tiền: " + e.getMessage());
            // 3. Trả về mã lỗi DB
            return "DB_ERROR";
        }
    }

    public String updateUserInfo(String currentUsername, String newUsername, String newEmail, String newPhone, String newAddress) {

        try (Connection conn = DatabaseManager.getConnection()) {


            if (!newUsername.isEmpty()) {
                String sqlCheck = "SELECT 1 FROM Users WHERE username = ? AND username != ?";
                try (PreparedStatement pst = conn.prepareStatement(sqlCheck)) {
                    pst.setString(1, newUsername);
                    pst.setString(2, currentUsername);
                    if (pst.executeQuery().next()) return "ERR_USERNAME";
                }
            }

            if (!newEmail.isEmpty()) {
                String sqlCheck = "SELECT 1 FROM Users WHERE email = ? AND username != ?";
                try (PreparedStatement pst = conn.prepareStatement(sqlCheck)) {
                    pst.setString(1, newEmail);
                    pst.setString(2, currentUsername);
                    if (pst.executeQuery().next()) return "ERR_EMAIL";
                }
            }

            if (!newPhone.isEmpty()) {
                String sqlCheck = "SELECT 1 FROM Users WHERE phone = ? AND username != ?";
                try (PreparedStatement pst = conn.prepareStatement(sqlCheck)) {
                    pst.setString(1, newPhone);
                    pst.setString(2, currentUsername);
                    if (pst.executeQuery().next()) return "ERR_PHONE";
                }
            }



            String sql = "UPDATE Users SET "
                    + "username = COALESCE(NULLIF(?, ''), username), "
                    + "email = COALESCE(NULLIF(?, ''), email), "
                    + "phone = COALESCE(NULLIF(?, ''), phone), "
                    + "shipping_address = COALESCE(NULLIF(?, ''), shipping_address) "
                    + "WHERE username = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newUsername);
                pstmt.setString(2, newEmail);
                pstmt.setString(3, newPhone);
                pstmt.setString(4, newAddress);
                pstmt.setString(5, currentUsername);

                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0 ? "SUCCESS" : "FAIL";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "DB_ERROR";
        }
    }

    // Hàm Đổi mật khẩu
    public String changePassword(String username, String oldPassword, String newPassword) {
        try (Connection conn = DatabaseManager.getConnection()) {

            // Bước 1: Kiểm tra mật khẩu cũ có chính xác không
            String sqlCheck = "SELECT 1 FROM Users WHERE username = ? AND password = ?";
            try (PreparedStatement pstCheck = conn.prepareStatement(sqlCheck)) {
                pstCheck.setString(1, username);
                pstCheck.setString(2, oldPassword);

                // Nếu tìm không thấy tài khoản nào khớp user + pass cũ -> Báo sai pass
                if (!pstCheck.executeQuery().next()) {
                    return "WRONG_PASS";
                }
            }

            // Bước 2: Cập nhật mật khẩu mới
            String sqlUpdate = "UPDATE Users SET password = ? WHERE username = ?";
            try (PreparedStatement pstUpdate = conn.prepareStatement(sqlUpdate)) {
                pstUpdate.setString(1, newPassword);
                pstUpdate.setString(2, username);

                int rowsAffected = pstUpdate.executeUpdate();
                return rowsAffected > 0 ? "SUCCESS" : "FAIL";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "DB_ERROR";
        }
    }

    /**
     * Hàm lấy số dư hiện tại của người dùng từ Database
     */
    public double getBalance(String username) {
        String sql = "SELECT balance FROM Users WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Móc đúng cột balance kiểu double trong bảng Users ra
                    return rs.getDouble("balance");
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi DAO khi lấy số dư của " + username + ": " + e.getMessage());
        }
        return 0.0; // Trả về 0.0 nếu không tìm thấy hoặc bị lỗi kết nối
    }

    // Hàm lấy ID từ Username
    public int getIdByUsername(String username) {
        int id = -1; // Mặc định là -1 (không tìm thấy)
        String query = "SELECT user_id FROM Users WHERE username = ?"; // Thay tên cột/bảng cho khớp DB của bạn

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                id = rs.getInt("user_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }
    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM Users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role").toUpperCase();
                User user = null; // Khai báo đối tượng cha abstract

                // 1. KHỞI TẠO ĐA HÌNH VÀ NẠP CÁC THUỘC TÍNH RIÊNG BIỆT (Đã tách address, rating)
                if (role.equals("BIDDER")) {
                    Bidder bidder = new Bidder();
                    bidder.setBalance(rs.getFloat("balance"));
                    bidder.setShippingAddress(rs.getString("shipping_address")); // Nạp riêng cho Bidder
                    user = bidder;

                } else if (role.equals("SELLER")) {
                    Seller seller = new Seller();
                    seller.setBalance(rs.getFloat("balance"));
                    seller.setRating(rs.getDouble("rating"));   // Nạp riêng cho Seller nếu cần
                    user = seller;

                } else if (role.equals("ADMIN")) {
                    user = new Admin();
                    // Admin hoàn toàn sạch sẽ, không có ví, không địa chỉ, không rating
                } else {
                    System.err.println("Lỗi: Vai trò người dùng không hợp lệ!");
                    return null;
                }

                // 2. NẠP CÁC THÔNG SỐ CHUNG NẰM Ở LỚP CHA USER
                user.setId(String.valueOf(rs.getInt("user_id")));
                user.setName(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setPhoneNumber(rs.getString("phone"));
                user.setRole(role);

                return user; // Trả về đối tượng con lồng dưới danh nghĩa lớp cha
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi truy vấn User đa hình: " + e.getMessage());
        }
        return null;
    }

    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();

        // Thường Admin chỉ cần quản lý danh sách Người Mua & Người Bán, lọc bỏ ADMIN ra cho sạch bảng
        String sql = "SELECT * FROM Users WHERE role != 'ADMIN'";

        try (Connection conn = DatabaseManager.getConnection(); // Thay bằng class kết nối DB của các cậu
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String role = rs.getString("role");
                if (role == null) continue;

                User user = null;
                role = role.toUpperCase();

                // CHÚ Ý ĐA HÌNH: Vì User là Abstract, bắt buộc phải dùng 'new' ở các lớp con cụ thể
                if (role.equals("BIDDER")) {
                    user = new Bidder();
                    // Nếu muốn lấy thêm thuộc tính riêng của Bidder lên bảng Admin thì nạp ở đây:
                    // ((Bidder) user).setBalance(rs.getDouble("balance"));
                } else if (role.equals("SELLER")) {
                    user = new Seller();
                } else {
                    continue; // Bỏ qua nếu dòng này chứa dữ liệu lạ
                }

                // NẠP CÁC TRƯỜNG CHUNG (Khai báo ở lớp cha Abstract User)
                // Fix triệt để lỗi ép kiểu chuỗi/số: user_id (int trong DB) -> setId (String trong Java)
                user.setId(String.valueOf(rs.getInt("user_id")));
                user.setName(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setPhoneNumber(rs.getString("phone"));
                user.setRole(role);
                user.setStatus(rs.getString("status")); // Thường lưu 'ACTIVE' hoặc 'BANNED'

                userList.add(user);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi quét danh sách User trong UserDAO: " + e.getMessage());
            e.printStackTrace();
        }

        return userList;
    }

    // ==========================================
    // 2. CẬP NHẬT TRẠNG THÁI (DÙNG ĐỂ BAN / UNBAN USER)
    // ==========================================
    public static boolean updateUserStatus(String userId, String newStatus) {
        // Kiểm tra lại tên cột ID của user trong SQLite của các cậu xem là 'user_id' hay 'id' nhé
        String sql = "UPDATE Users SET status = ? WHERE user_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus); // Truyền vào 'BANNED' hoặc 'ACTIVE'

            // ÉP KIỂU NGƯỢC LẠI: Chuyển chuỗi ID từ Client gửi xuống thành số nguyên int để update CSDL
            pstmt.setInt(2, Integer.parseInt(userId));

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0; // Trả về true nếu update thành công ít nhất 1 dòng

        } catch (Exception e) {
            System.err.println("Lỗi khi update status của User: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
