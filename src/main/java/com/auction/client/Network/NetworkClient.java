package com.auction.client.Network;

import java.io.*;
import java.net.*;

public class NetworkClient {
    // Biến lưu trữ phiên bản duy nhất (Singleton)
    private static NetworkClient instance;

    private Socket socket;
    private static BufferedReader in;
    private static PrintWriter out;

    // Constructor để private -> Không cho bên ngoài dùng từ khóa 'new' tạo lung tung
    private NetworkClient() {
        try {
            // Nhớ đổi IP nếu chạy khác máy nha (127.0.0.1 là chạy cùng máy)
            socket = new Socket("127.0.0.1", 5000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Client: Đã nối ống nước tới Server thành công!");
        } catch (IOException e) {
            System.out.println("Client: Toang rồi, không kết nối được Server!");
            e.printStackTrace();
        }
    }

    // Hàm public duy nhất để lấy "ống nước" ra xài
    public static NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient(); // Chỉ tạo 1 lần duy nhất
        }
        return instance;
    }

    // Hàm "Gửi và Chờ Nhận" thiết kế riêng cho ông Thắng xài lúc Đăng nhập
    public static String sendAndReceive(String message) {
        try {
            out.println(message); // Quăng tin nhắn qua Server
            return in.readLine(); // Đứng chờ Server trả lời rồi quăng ngược lại cho Thắng
        } catch (IOException e) {
            return "ERROR|Mất mạng rồi má ơi";
        }
    }
}