package com.auction.server.network;

import com.auction.server.controller.BiddingService;
import com.auction.server.controller.ProductManager;
import com.auction.server.controller.RegisterHandler;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.UserDAO;
import com.auction.shared.model.Item;
import com.auction.shared.model.user.User;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private BiddingService biddingService;

    private UserDAO userDAO = new UserDAO();
    private ItemDAO itemDAO = new ItemDAO(); // <--- Thêm dòng này vào đầu class

    public ClientHandler(Socket socket, BiddingService  biddingService) {
        this.socket = socket;
        this.biddingService=biddingService;
        try {
            // 1. Gắn ống dịch chữ
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // 2. Gắn ống đẩy chữ
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                String[] parts = message.split("\\|");
                String action = parts[0];

                switch (action) {
                    //1. ĐĂNG KÍ
                    case "REGISTER"-> {
                    String username = parts[1];
                    String password = parts[2];
                    String email = parts[3];
                    String phone = parts[4];

                    // Giao hết cho cái cục Thắng vừa tạo xử lý, xong hét vô ống nước trả về Client
                    out.println(RegisterHandler.processRegister(username, password, email, phone));
                }


                    //2.ĐĂNG NHẬP
                    case "LOGIN" -> {
                        String username = parts[1];
                        String password = parts[2];


                        User loggedInUser = UserDAO.Login(username, password);

                        if (loggedInUser != null) {
                            out.println("LOGIN_SUCCESS|" + loggedInUser.getRole());
                        } else {
                            out.println("LOGIN_FAIL|Sai tài khoản hoặc mật khẩu");
                        }
                    }
                    //3a. BID:
                    case "BID" -> {
                        String user = parts[1];
                        // Đổi sang parseDouble để khớp với kiểu double của Tâmi
                        double price = Double.parseDouble(parts[2]);
                        String itemId = parts[3];

                        // Tìm món đồ trong kho giả
                        Item targetItem = ProductManager.getItemById(itemId);

                        if (targetItem != null) {
                            // 👉 TRUYỀN ĐÚNG THỨ TỰ: Món đồ -> Tiền -> User
                            String result = biddingService.placeBid(targetItem, price, user);

                            out.println(result);

                            // (Nếu placeBid trả về chữ THÀNH CÔNG, cưng nhớ gọi AuctionServer.broadcast(giá mới)
                            // để loa phát thanh cho cả làng biết nha)
                        } else {
                            out.println("BID_FAIL|Sản phẩm không tồn tại!");
                        }
                    }

                    // 3b. AUTO-BID: AUTO_BID|bidderId|maxBid|increment|itemId
                    case "AUTO_BID" -> {
                        String bidderId  = parts[1];
                        double maxBid    = Double.parseDouble(parts[2]);
                        double increment = Double.parseDouble(parts[3]);
                        String itemId    = parts[4];

                        Item targetItem = ProductManager.getItemById(itemId);

                        if (targetItem != null) {
                            String result = biddingService.registerAutoBid(
                                    targetItem, bidderId, maxBid, increment);
                            out.println(result);
                        } else {
                            out.println("AUTO_BID_FAIL|Sản phẩm không tồn tại!");
                        }
                    }

                    // 4. NẾU KHÁCH MUỐN XEM DANH SÁCH SẢN PHẨM
                    case "GET_PREPARED_ITEMS" -> {
                        // 1. Gọi ItemDAO móc danh sách Prepared ra
                        List<Item> preparedList = itemDAO.getPreparedItems();

                        // 2. Ép danh sách đó thành JSON bằng Gson
                        String json = new Gson().toJson(preparedList);

                        // 3. Quăng cái chuỗi JSON đó trả lại cho Client
                        out.println(json);
                    }

                }
                // ... (Các tính năng khác tương tự) ...
            }
        } catch (IOException e) {
            System.out.println("Lỗi mạng!");
        }
    }
}

