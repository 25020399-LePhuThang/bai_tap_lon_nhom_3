package com.auction.server.controller;

import com.auction.server.dao.UserDAO;
import com.auction.shared.model.user.User;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;

public class RegisterHandler {

    public static String processRegister(String username, String password, String email, String phone, String role) {
        UserDAO userDAO = new UserDAO();


        if (userDAO.Exist(username, email, phone)) {
            return "REGISTER_FAIL|Tài khoản, Email hoặc Số điện thoại đã được sử dụng nha má!";
        }


        User newUser;
        if ("SELLER".equalsIgnoreCase(role)) {
            newUser = new Seller();
            newUser.setRole("SELLER");
        } else {
            newUser = new Bidder();
            newUser.setRole("BIDDER");
        }

        newUser.setName(username);
        newUser.setPassword(password);
        newUser.setEmail(email);
        newUser.setPhoneNumber(phone);


        boolean isSaved = userDAO.register(newUser);

        if (isSaved) {
            return "REGISTER_SUCCESS|Chào mừng người chơi hệ đấu giá!";
        } else {
            return "REGISTER_FAIL|Lỗi hệ thống khi lưu dữ liệu.";
        }
    }
}