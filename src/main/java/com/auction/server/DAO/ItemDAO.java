//package com.auction.server.DAO;

//import com.auction.client.database.DatabaseManager;
//import com.auction.shared.model.Item; // Đảm bảo bạn đã có class Item trong thư mục shared/model
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.List;
//
//public class ItemDAO {
//
//    // 1. THÊM TÀI SẢN MỚI (Dùng khi Seller đăng bán một món hàng)
//    public boolean insertItem(Item item) {
//        // Giả sử bảng items không cần chèn id (vì AUTOINCREMENT) và status mặc định là 'ACTIVE'
//        String sql = "INSERT INTO items (auction_id, item_name,description, start_price, step_price, seller_id, status) VALUES (?, ?, ?, ?, ?, ?)";
//
//        try (Connection conn = DatabaseManager.getConnection();
//             PreparedStatement pstmt = conn.prepareStatement(sql)) {
//
//            pstmt.setString(1, item.getId());
//            pstmt.setString(2, item.getName());
//            pstmt.setDouble(3, item.g);
//            pstmt.setDouble(4, item.getStartingPrice()); // Vừa đăng lên thì giá hiện tại = giá khởi điểm
//            pstmt.setInt(5, item.getSellerId());
//            pstmt.setString(6, item.getEndTime()); // Lưu dưới dạng chuỗi (String) hoặc Timestamp tùy Database
//
//            int rowsAffected = pstmt.executeUpdate();
//            return rowsAffected > 0;
//
//        } catch (SQLException e) {
//            System.err.println("Lỗi khi thêm tài sản mới: " + e.getMessage());
//            return false;
//        }
//    }
//
//    // 2. LẤY DANH SÁCH TÀI SẢN ĐANG ĐẤU GIÁ (Dùng để hiển thị lên giao diện cho Bidder xem)
//    public List<Item> getActiveItems() {
//        List<Item> itemList = new ArrayList<>();
//        // Chỉ lấy những tài sản đang trong trạng thái mở đấu giá
//        String sql = "SELECT * FROM items WHERE status = 'ACTIVE'";
//
//        try (Connection conn = DatabaseManager.getConnection();
//             PreparedStatement pstmt = conn.prepareStatement(sql);
//             ResultSet rs = pstmt.executeQuery()) {
//
//            while (rs.next()) {
//                Item item = new Item();
//                item.setId(rs.getInt("id"));
//                item.setName(rs.getString("name"));
//                item.setDescription(rs.getString("description"));
//                item.setStartingPrice(rs.getDouble("starting_price"));
//                item.setCurrentPrice(rs.getDouble("current_price"));
//                item.setSellerId(rs.getInt("seller_id"));
//                item.setEndTime(rs.getString("end_time"));
//                item.setStatus(rs.getString("status"));
//
//                itemList.add(item);
//            }
//        } catch (SQLException e) {
//            System.err.println("Lỗi khi lấy danh sách tài sản: " + e.getMessage());
//        }
//        return itemList;
//    }
//
//    // 3. CẬP NHẬT GIÁ TÀI SẢN (Dùng khi có người đặt giá thành công)
//    public boolean updateCurrentPrice(int itemId, double newPrice) {
//        String sql = "UPDATE items SET current_price = ? WHERE id = ?";
//
//        try (Connection conn = DatabaseManager.getConnection();
//             PreparedStatement pstmt = conn.prepareStatement(sql)) {
//
//            pstmt.setDouble(1, newPrice);
//            pstmt.setInt(2, itemId);
//
//            int rowsAffected = pstmt.executeUpdate();
//            return rowsAffected > 0;
//
//        } catch (SQLException e) {
//            System.err.println("Lỗi khi cập nhật giá: " + e.getMessage());
//            return false;
//        }
//    }
//}