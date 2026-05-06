package com.auction.server.controller;

import com.auction.server.DAO.UserDAO;
import com.auction.shared.model.user.User;
import com.auction.shared.model.user.Bidder; // Mặc định người mới đăng ký là Bidder

public class RegisterHandler {

    // Đổi tham số thành chuỗi luôn để Zưin bên ClientHandler dễ gọi
    public static String processRegister(String username, String password, String email, String phone) {
        UserDAO userDAO = new UserDAO();

        // Bước 1: Gọi hàm Exist thật của ông Thắng
        if (userDAO.Exist(username, email, phone)) { //
            // Nếu trùng, gửi phản hồi lỗi về Client
            return "REGISTER_FAIL|Tài khoản, Email hoặc Số điện thoại đã được sử dụng nha má!";
        }

        // Bước 2: Đóng gói thành object User
        User newUser = new Bidder(); // Khởi tạo Bidder vì User có thể là class cha
        newUser.setName(username);
        newUser.setPassword(password);
        newUser.setEmail(email);
        newUser.setPhoneNumber(phone);

        // Bước 3: Gọi hàm register thật của ông Thắng
        boolean isSaved = userDAO.register(newUser); //

        if (isSaved) {
            return "REGISTER_SUCCESS|Chào mừng người chơi hệ đấu giá!";
        } else {
            return "REGISTER_FAIL|Lỗi hệ thống khi lưu dữ liệu.";
        }
    }
}
