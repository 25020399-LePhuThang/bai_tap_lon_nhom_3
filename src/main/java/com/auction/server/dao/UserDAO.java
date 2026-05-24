package com.auction.server.dao;

import com.auction.server.database.DatabaseManager;
import com.auction.shared.model.user.Admin;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;
import java.sql.*;

public class UserDAO {
    //Hàm đăng ký
    public boolean register(User user) {
        String sql = "INSERT INTO Users(username,password,email,phone) VALUES (?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, user.getName());
            preparedStatement.setString(2, user.getPassword());
            preparedStatement.setString(3, user.getEmail());
            preparedStatement.setString(4, user.getPhoneNumber());
            int rows = preparedStatement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi DAO");
            return false;
        }
    }

    //Hàm đăng nhập
    public static User Login(String name, String pass) {

        name = name.trim();
        pass = pass.trim();

        // 2. MÁY QUÉT: In ra Console để làm bằng chứng trước tòa
        System.out.println("SERVER ĐANG XỬ LÝ ĐĂNG NHẬP -> Tên: [" + name + "] | Mật khẩu: [" + pass + "]");

        // ... Từ dòng String sql = ... trở xuống bạn giữ nguyên ...
        String sql = "SELECT * FROM Users WHERE username=? AND password=?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, pass);

            try (ResultSet result = preparedStatement.executeQuery()) {
                if (result.next()) {
                    User foundUser = null;
                    String role = result.getString("role");

                    if (role == null || role.trim().isEmpty()) {
                        role = "BIDDER"; // Cứu nguy, mặc định cho làm người đấu giá
                    }
                        switch (role.toUpperCase()) {
                            case "ADMIN":
                                foundUser = new Admin();
                                break;
                            case "SELLER":
                                foundUser = new Seller();
                                break;
                            case "BIDDER":
                            default:
                                foundUser = new Bidder();
                                break;
                    }
                    if (foundUser != null) {
                        foundUser.setId(result.getString("user_id"));
                        foundUser.setName(result.getString("username"));
                        foundUser.setPassword(result.getString("password"));
                        foundUser.setEmail(result.getString("email"));
                        foundUser.setPhoneNumber(result.getString("phone"));
                        foundUser.setStatus(result.getString("status"));
                        foundUser.setRole(result.getString("role"));
                    }
                    return foundUser;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi DAO");
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
}