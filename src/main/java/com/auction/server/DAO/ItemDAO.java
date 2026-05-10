package com.auction.server.dao;

import com.auction.server.database.DatabaseManager;
import com.auction.shared.model.Art;
import com.auction.shared.model.Electronic;
import com.auction.shared.model.Item;
import com.auction.shared.model.Vehicle;
import com.auction.shared.model.user.Admin;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    // 1. THÊM TÀI SẢN MỚI
    public boolean insertItem(Item item) {
        // Đã canh chỉnh lại cho đủ 9 cột và 9 dấu ?
        String sql = "INSERT INTO items (item_id, item_name, start_price, current_price, step_price, lastbidder_id, status, type, productImageURL) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getId());
            pstmt.setString(2, item.getName());
            pstmt.setDouble(3, item.getStartingPrice());
            pstmt.setDouble(4, item.getCurrentPrice());
            pstmt.setDouble(5, item.getMinIncrement());
            pstmt.setString(6, item.getLastBidderId());
            pstmt.setString(7, item.getStatus());

            // Chú ý: Nếu class Item chưa có hàm getType(), cưng có thể truyền thẳng chuỗi "ELECTRONIC" / "VEHICLE" tạm vào đây
            pstmt.setString(8, item.getClass().getSimpleName().toUpperCase()); // Lấy luôn tên class làm Type (ART/VEHICLE/ELECTRONIC)
            pstmt.setString(9, item.getProductImageURL());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi khi thêm tài sản mới: " + e.getMessage());
            return false;
        }
    }

    // 2. LẤY DANH SÁCH TÀI SẢN ĐANG ĐẤU GIÁ
    public List<Item> getActiveItems() {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE status = 'ACTIVE'";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql);
             ResultSet rs = preparedStatement.executeQuery()) {

            while (rs.next()) {
                Item foundItem = createItemByType(rs.getString("type"));

                if (foundItem != null) {
                    mapResultSetToItem(rs, foundItem);
                    itemList.add(foundItem);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy danh sách ACTIVE: " + e.getMessage());
        }
        return itemList;
    }

    // 3. CẬP NHẬT GIÁ TÀI SẢN
    // Tui đổi int itemId thành String itemId vì trong Item.java id đang là kiểu String nha
    public boolean updateCurrentPrice(String itemId, double newPrice) {
        String sql = "UPDATE items SET current_price = ? WHERE item_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, newPrice);
            pstmt.setString(2, itemId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật giá: " + e.getMessage());
            return false;
        }
    }

    // 4. LẤY DANH SÁCH TÀI SẢN CHUẨN BỊ ĐẤU GIÁ
    public List<Item> getPreparedItems() {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE status = 'PREPARED'";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql);
             ResultSet rs = preparedStatement.executeQuery()) {

            while (rs.next()) {
                Item foundItem = createItemByType(rs.getString("type"));
                if (foundItem != null) {
                    mapResultSetToItem(rs, foundItem);
                    itemList.add(foundItem);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy danh sách PREPARED: " + e.getMessage());
        }
        return itemList;
    }

    // 5. LẤY DANH SÁCH TÀI SẢN ĐÃ ĐẤU GIÁ XONG
    public List<Item> getSoldItems() {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE status = 'SOLD'";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql);
             ResultSet rs = preparedStatement.executeQuery()) {

            while (rs.next()) {
                Item foundItem = createItemByType(rs.getString("type"));
                if (foundItem != null) {
                    mapResultSetToItem(rs, foundItem);
                    itemList.add(foundItem);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy danh sách SOLD: " + e.getMessage());
        }
        return itemList;
    }

    // 6. CẬP NHẬT TRẠNG THÁI TỪ PREPARED SANG ACTIVE KHI ĐẾN GIỜ
    public void updateToActive() {
        String sql = "UPDATE items SET status = 'ACTIVE' WHERE status='PREPARED' AND startTime<=? AND ?<=endTime";
        Timestamp now = new Timestamp(System.currentTimeMillis());

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {

            preparedStatement.setTimestamp(1, now);
            preparedStatement.setTimestamp(2, now);
            preparedStatement.executeUpdate(); // Đã thêm lệnh execute để chạy SQL

        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật status thành ACTIVE: " + e.getMessage());
        }
    }

    // 7. CẬP NHẬT TRẠNG THÁI THÀNH SOLD KHI HẾT GIỜ
    public void updateToSold() {
        String sql = "UPDATE items SET status = 'SOLD' WHERE status='ACTIVE' AND ?>=endTime";
        Timestamp now = new Timestamp(System.currentTimeMillis());

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {

            preparedStatement.setTimestamp(1, now);
            preparedStatement.executeUpdate(); // Đã thêm lệnh execute để chạy SQL

        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật status thành SOLD: " + e.getMessage());
        }
    }

    // =========================================================================
    // CÁC HÀM TIỆN ÍCH (HELPER METHODS) ĐỂ TRÁNH LẶP CODE
    // =========================================================================

    // Hàm sinh Object theo Type
    private Item createItemByType(String type) {
        if (type == null) return new Art(); // Default
        switch (type.toUpperCase()) {
            case "ELECTRONIC":
                return new Electronic();
            case "VEHICLE":
                return new Vehicle();
            case "ART":
            default:
                return new Art();
        }
    }

    // Hàm gắn dữ liệu từ ResultSet vào Object Item
    private void mapResultSetToItem(ResultSet rs, Item item) throws SQLException {
        item.setId(rs.getString("item_id"));
        item.setName(rs.getString("item_name"));
        item.setStartingPrice(rs.getDouble("starting_price"));
        item.setCurrentPrice(rs.getDouble("current_price"));
        item.setMinIncrement(rs.getDouble("step_price"));
        item.setLastBidderId(rs.getString("lastbidder_id"));
        item.setEndTime(rs.getDate("EndTime"));
        item.setStatus(rs.getString("status"));
        item.setStartTime(rs.getDate("StartTime"));
        item.setProductImageURL(rs.getString("productImageURL"));
    }
}