package com.auction.client.network;

import com.auction.shared.model.item.Item;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.ArrayList;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class NetworkClient {
    private static NetworkClient instance;
    private static Socket socket;
    private static BufferedReader in;
    private static PrintWriter out;

    private NetworkClient() {
        try {
            socket = new Socket("127.0.0.1", 5000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Client: Đã nối ống nước tới Server thành công!");
        } catch (IOException e) {
            System.out.println("Client: Toang rồi, không kết nối được Server!");
            e.printStackTrace();
        }
    }

    public static NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    public static String sendAndReceive(String message) {
        try {
            out.println(message);
            return in.readLine();
        } catch (IOException e) {
            return "ERROR|Mất mạng rồi má ơi";
        }
    }

    public static boolean sendRegisterRequest(String name, String password, String email, String phone) {
        final String host = "127.0.0.1";
        final int port = 5000;

        // 1. Đảm bảo socket đã kết nối
        if (!isConnected()) {
            if (!connect(host, port)) {
                System.err.println("Client: Không thể kết nối server.");
                return false;
            }
        }

        try {
            // 2. Gửi request
            String msg = "REGISTER|" + safe(name) + "|" + safe(password) + "|" + safe(email) + "|" + safe(phone);
            out.println(msg);

            // 3. Chờ phản hồi
            socket.setSoTimeout(3000);
            String response = in.readLine();

            if (response == null) {
                System.err.println("Client: Server trả về null.");
                closeAll();
                return false;
            }

            System.out.println("Client: Response -> " + response);
            return response.startsWith("REGISTER_SUCCESS") || response.startsWith("OK");

        } catch (Exception e) {
            System.err.println("Client: Lỗi khi gửi request: " + e.getMessage());
            closeAll();
            return false;
        }
    }


    private static boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }


    private static boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Client: Kết nối thành công.");
            return true;
        } catch (Exception e) {
            System.err.println("Client: Không thể kết nối: " + e.getMessage());
            return false;
        }
    }


    private static void closeAll() {
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        in = null;
        out = null;
        socket = null;
    }


    private static String safe(String s) {
        return s == null ? "" : s.replace("\n", "").replace("\r", "").replace("|", "");
    }

    public static List<Item> getActiveItems() {
        String response = sendAndReceive("GET_ACTIVE_ITEMS");
        return parseItems(response);
    }

    public static List<Item> parseItems(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isEmpty()) return new ArrayList<>();

        Gson gson = new Gson();

        // Sửa chỗ này nè: Thay Electronic bằng Item
        Type listType = new TypeToken<ArrayList<Item>>(){}.getType();

        // BÙM! Bây giờ nó sẽ map đúng vào list Item, có đầy đủ getter/setter cưng mới thêm
        List<Item> items = gson.fromJson(jsonResponse, listType);

        return items;
    }


    // 1. Lấy đồ đang đấu giá (đổi getActiveItems thành takeActiveItem)
    public static List<Item> takeActiveItems() {
        String response = sendAndReceive("GET_ACTIVE_ITEMS");
        return parseItems(response);
    }

    // 2. Lấy đồ chuẩn bị lên sàn (đổi takePreparedItems thành takePreparedItem)
    public static List<Item> takePreparedItems() {
        String response = sendAndReceive("GET_PREPARED_ITEMS");
        return parseItems(response);
    }
}