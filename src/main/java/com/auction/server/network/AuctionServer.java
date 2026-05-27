package com.auction.server.network;

import com.auction.server.controller.AuctionTimer;
import com.auction.server.controller.BiddingService;
import com.auction.server.database.DatabaseManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class AuctionServer {
    // 1. Khai báo danh sách
    public static ArrayList<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        DatabaseManager.initDB();
        try {
            // 2. Mở cổng - đọc PORT từ Railway
            int port = System.getenv("PORT") != null
                     ? Integer.parseInt(System.getenv("PORT"))
                     : 5000;
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server đang chạy trên cổng: " + port);
            
            AuctionTimer timer = new AuctionTimer(); 
            BiddingService sharedBiddingService = new BiddingService();

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
}
