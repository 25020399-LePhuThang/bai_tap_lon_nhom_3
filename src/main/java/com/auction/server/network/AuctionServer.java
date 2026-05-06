package com.auction.server.network;

import com.auction.server.controller.AuctionTimer;
import com.auction.server.controller.BiddingService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class AuctionServer {
    // 1. Khai báo danh sách
    public static ArrayList<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        try {
            // 2. Mở cổng
            ServerSocket serverSocket = new ServerSocket(5000);

            AuctionTimer timer = new AuctionTimer(); // (Nhớ kêu Tâmi đưa class này cho cưng)
            BiddingService sharedBiddingService = new BiddingService(timer);

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
}
