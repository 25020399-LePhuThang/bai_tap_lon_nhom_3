package com.auction.server.dao;

import com.auction.server.database.DatabaseManager;
import com.auction.shared.model.item.Art;
import com.auction.shared.model.item.Electronic;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.Vehicle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    // 1. THÊM TÀI SẢN MỚI
    public boolean insertItem(Item item) {
        String sql = "INSERT INTO items (item_name, start_price, current_price, step_price, last_bidder_id, status, type, productImageURL) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getName());
            pstmt.setDouble(2, item.getStartingPrice());
            pstmt.setDouble(3, item.getCurrentPrice());
            pstmt.setDouble(4, item.getMinIncrement());
            pstmt.setString(5, item.getLastBidderId());
            pstmt.setString(6, item.getStatus());
            pstmt.setString(7, item.getType());
            pstmt.setString(8, item.getProductImageURL());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi khi thêm tài sản mới: " + e.getMessage());
            return false;
        }
    }

    // 2. LẤY DANH SÁCH TÀI SẢN ĐANG ĐẤU GIÁ
    public List<Item> getActiveItems() {
        return getItemsByStatus("ACTIVE");
    }

    // 3. LẤY DANH SÁCH TÀI SẢN CHUẨN BỊ ĐẤU GIÁ
    public List<Item> getPreparedItems() {
        return getItemsByStatus("PREPARED");
    }

    // 4. LẤY DANH SÁCH TÀI SẢN ĐÃ BÁN
    public List<Item> getSoldItems() {
        return getItemsByStatus("SOLD");
    }

    // HÀM DÙNG CHUNG ĐỂ LẤY DỮ LIỆU (Tối ưu code, tránh lặp lại)
    private List<Item> getItemsByStatus(String status) {
        List<Item> itemList = new ArrayList<>();

        String sql = "SELECT * FROM items WHERE status = ?";
        if ("ACTIVE".equalsIgnoreCase(status)) {
            sql = "SELECT * FROM items WHERE status = ? AND EndTime > CURRENT_TIMESTAMP";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("type");
                    Item foundItem = null;

                    if (type != null) {
                        switch (type.toUpperCase()) {
                            case "ELECTRONIC":
                                Electronic el = new Electronic();
                                // Đọc thêm thuộc tính riêng của đồ điện tử từ DB
                                el.setWarrantyPeriod(rs.getInt("warranty_period"));
                                el.setBrand(rs.getString("brand"));
                                foundItem = el;
                                break;

                            case "VEHICLE":
                                Vehicle v = new Vehicle();
                                v.setWarrantyPeriod(rs.getInt("warranty_period"));
                                v.setBrand(rs.getString("brand"));
                                v.setEngineCapacity(rs.getString("engine_capacity"));
                                v.setFuelType(rs.getString("fuel_type"));
                                foundItem = v;
                                break;

                            case "ART":
                                Art a = new Art();
                                a.setAuthor(rs.getString("author"));
                                a.setCreationYear(rs.getInt("creation_year"));
                                foundItem = a;
                                break;
                        }
                    }

                    if (foundItem != null) {
                        foundItem.setId(rs.getString("item_id"));
                        foundItem.setName(rs.getString("item_name"));
                        foundItem.setStartingPrice(rs.getDouble("start_price"));
                        foundItem.setCurrentPrice(rs.getDouble("current_price"));
                        foundItem.setMinIncrement(rs.getDouble("step_price"));
                        foundItem.setLastBidderId(rs.getString("last_bidder_id"));

                        foundItem.setType(type);

                        foundItem.setStatus(rs.getString("status"));
                        foundItem.setProductImageURL(rs.getString("productImageURL"));

                        foundItem.setStartTime(rs.getTimestamp("StartTime"));
                        foundItem.setEndTime(rs.getTimestamp("EndTime"));
                        foundItem.setSeller_ID(rs.getString("seller_id"));

                        itemList.add(foundItem);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy danh sách tài sản (" + status + "): " + e.getMessage());
        }
        return itemList;
    }

    // 5. CẬP NHẬT GIÁ TÀI SẢN
    public boolean updateCurrentPrice(int itemId, double newPrice) {
        String sql = "UPDATE items SET current_price = ? WHERE item_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, newPrice);
            pstmt.setInt(2, itemId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật giá: " + e.getMessage());
            return false;
        }
    }

    // HÀM MỚI: Cập nhật giá và người dẫn đầu sau mỗi lần bid thành công
    public boolean updatePrice(Item item) {
        String sql = "UPDATE items SET current_price = ?, last_bidder_id = ? WHERE item_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, item.getCurrentPrice());
            pstmt.setString(2, item.getLastBidderId());
            pstmt.setString(3, item.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật giá sau bid: " + e.getMessage());
            return false;
        }
    }

    // HÀM MỚI: Cập nhật EndTime sau khi Anti-Sniping gia hạn
    public boolean updateEndTime(Item item) {
        String sql = "UPDATE items SET EndTime = ? WHERE item_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, new java.sql.Timestamp(item.getEndTime().getTime()));
            pstmt.setString(2, item.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật EndTime: " + e.getMessage());
            return false;
        }
    }

    // 6. CẬP NHẬT TRẠNG THÁI SANG ACTIVE
    public void updateToActive() {
        String sql = "UPDATE items SET status = 'ACTIVE' WHERE status = 'PREPARED' AND StartTime <= ? AND ? <= EndTime";
        Timestamp now = new Timestamp(System.currentTimeMillis());

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, now);
            pstmt.setTimestamp(2, now);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Lỗi khi updateToActive: " + e.getMessage());
        }
    }

    public List<Item> getItemsBySellerID(Integer sellerId) {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE seller_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 2. TRUYỀN BIẾN: Nhét sellerId vào dấu ?
            pstmt.setInt(1, sellerId);

            try (ResultSet rs = pstmt.executeQuery()) {
                // [Tâm] Khai báo bộ định dạng thời gian ở ngoài vòng lặp để tối ưu hiệu năng
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                while (rs.next()) {
                    String type = rs.getString("type");
                    Item foundItem = null;

                    if (type != null) {
                        switch (type.toUpperCase()) {
                            case "ELECTRONIC":
                                Electronic el = new Electronic();
                                el.setWarrantyPeriod(rs.getInt("warranty_period"));
                                el.setBrand(rs.getString("brand"));
                                foundItem = el;
                                break;

                            case "VEHICLE":
                                Vehicle v = new Vehicle();
                                v.setWarrantyPeriod(rs.getInt("warranty_period"));
                                v.setBrand(rs.getString("brand"));
                                v.setEngineCapacity(rs.getString("engine_capacity"));
                                v.setFuelType(rs.getString("fuel_type"));
                                foundItem = v;
                                break;

                            case "ART":
                                Art a = new Art();
                                a.setAuthor(rs.getString("author"));
                                a.setCreationYear(rs.getInt("creation_year"));
                                foundItem = a;
                                break;
                        }
                    }

                    if (foundItem != null) {
                        foundItem.setId(rs.getString("item_id"));
                        foundItem.setName(rs.getString("item_name"));
                        foundItem.setStartingPrice(rs.getDouble("start_price"));
                        foundItem.setCurrentPrice(rs.getDouble("current_price"));
                        foundItem.setMinIncrement(rs.getDouble("step_price"));
                        foundItem.setLastBidderId(rs.getString("last_bidder_id"));
                        foundItem.setType(type);
                        foundItem.setStatus(rs.getString("status"));
                        foundItem.setProductImageURL(rs.getString("productImageURL"));
                        foundItem.setSeller_ID(rs.getString("seller_id"));

                        // [Tâm] Xử lý an toàn thời gian bằng String để chống lỗi parse trên DB
                        try {
                            String startStr = rs.getString("StartTime");
                            String endStr = rs.getString("EndTime");

                            if (startStr != null && !startStr.isEmpty()) {
                                foundItem.setStartTime(new java.sql.Timestamp(sdf.parse(startStr).getTime()));
                            }
                            if (endStr != null && !endStr.isEmpty()) {
                                foundItem.setEndTime(new java.sql.Timestamp(sdf.parse(endStr).getTime()));
                            }
                        } catch (java.text.ParseException pe) {
                            System.err.println("Lỗi parse time cho sản phẩm " + foundItem.getId() + ": " + pe.getMessage());
                        }

                        itemList.add(foundItem);
                    }
                }
            }
        } catch (SQLException e) {
            // 3. ĐỔI LỜI BÁO LỖI cho dễ debug
            System.err.println("Lỗi khi lấy danh sách tài sản của Seller (" + sellerId + "): " + e.getMessage());
        }

        return itemList;
    }

    public boolean createItem(Item item) {
        String sql = "INSERT INTO Items (item_name, type, start_price, current_price, step_price, " +
                "StartTime, EndTime, status, seller_id, productImageURL, " +
                "warranty_period, brand, engine_capacity, fuel_type, author, creation_year) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, (SELECT user_id FROM Users WHERE username = ?), ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getType());
            pstmt.setDouble(3, item.getStartingPrice());
            pstmt.setDouble(4, item.getCurrentPrice());
            pstmt.setDouble(5, item.getMinIncrement());

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            pstmt.setString(6, item.getStartTime() != null ? sdf.format(item.getStartTime()) : null);
            pstmt.setString(7, item.getEndTime() != null ? sdf.format(item.getEndTime()) : null);

            pstmt.setString(8, item.getStatus());

            pstmt.setString(9, item.getSeller_ID());
            pstmt.setString(10, item.getProductImageURL());

            if (item instanceof Electronic el) {
                pstmt.setInt(11, el.getWarrantyPeriod());
                pstmt.setString(12, el.getBrand());
                pstmt.setNull(13, java.sql.Types.VARCHAR);
                pstmt.setNull(14, java.sql.Types.VARCHAR);
                pstmt.setNull(15, java.sql.Types.VARCHAR);
                pstmt.setNull(16, java.sql.Types.INTEGER);

            } else if (item instanceof Vehicle v) {
                pstmt.setInt(11, v.getWarrantyPeriod());
                pstmt.setString(12, v.getBrand());
                pstmt.setString(13, v.getEngineCapacity());
                pstmt.setString(14, v.getFuelType());
                pstmt.setNull(15, java.sql.Types.VARCHAR);
                pstmt.setNull(16, java.sql.Types.INTEGER);

            } else if (item instanceof Art a) {
                pstmt.setNull(11, java.sql.Types.INTEGER);
                pstmt.setNull(12, java.sql.Types.VARCHAR);
                pstmt.setNull(13, java.sql.Types.VARCHAR);
                pstmt.setNull(14, java.sql.Types.VARCHAR);
                pstmt.setString(15, a.getAuthor());
                pstmt.setInt(16, a.getCreationYear());
            }

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi Insert DB: " + e.getMessage());
            return false;
        }
    }

    public List<Item> takeWaitingItems() {
        List<Item> waitingItems = new ArrayList<>();

        String sql = "SELECT * FROM Items WHERE status = 'WAITING'";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String type = rs.getString("type");
                Item item = null;
                if (type != null) {
                    if (type.equalsIgnoreCase("Electronic")) {
                        item = new Electronic();
                    } else if (type.equalsIgnoreCase("Art")) {
                        item = new Art();
                    } else if (type.equalsIgnoreCase("Vehicle")) {
                        item = new Vehicle();
                    } else {
                        // Nếu gặp loại lạ (do rác trong DB), in ra log và bỏ qua dòng này
                        System.err.println("Bỏ qua sản phẩm không rõ loại: " + type);
                        continue;
                    }
                } else {
                    continue;
                }

                item.setId(rs.getString("item_id"));
                item.setName(rs.getString("item_name"));
                item.setType(type);
                item.setStartingPrice(rs.getDouble("start_price"));
                item.setCurrentPrice(rs.getDouble("current_price"));
                item.setSeller_ID(rs.getString("seller_id"));
                item.setStatus(rs.getString("status"));

                String imageUrl = rs.getString("productImageURL");
                if (imageUrl != null) item.setProductImageURL(imageUrl);
                waitingItems.add(item);
            }

        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách sản phẩm chờ duyệt: " + e.getMessage());
            e.printStackTrace();
        }

        return waitingItems;
    }


    public static boolean approveItemWithTimeCheck(String itemId) {

        String queryTime = "SELECT StartTime FROM Items WHERE item_id = ?";
        String updateStatus = "UPDATE Items SET status = ? WHERE item_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmtQuery = conn.prepareStatement(queryTime);
             PreparedStatement pstmtUpdate = conn.prepareStatement(updateStatus)) {

            pstmtQuery.setString(1, itemId);
            ResultSet rs = pstmtQuery.executeQuery();

            if (rs.next()) {

                String newStatus = "ACTIVE";

                java.sql.Timestamp startTime = rs.getTimestamp("StartTime");

                if (startTime != null && startTime.after(new java.sql.Timestamp(System.currentTimeMillis()))) {
                    newStatus = "PREPARED";
                }

                // 2. Chốt hạ trạng thái mới vào Database
                pstmtUpdate.setString(1, newStatus);
                pstmtUpdate.setString(2, itemId);

                return pstmtUpdate.executeUpdate() > 0;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Lỗi khi duyệt có điều kiện thời gian: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteItem(String itemId) {
        String sql = "DELETE FROM Items WHERE item_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, itemId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (Exception e) {
            System.err.println("Lỗi khi xóa Item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public Item getItemById(String id) {
        String sql = "SELECT * FROM Items WHERE item_id = ?";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String type = rs.getString("type");
                Item item = null;

                if (type != null) {
                    switch (type.toUpperCase()) {
                        case "ART":
                            Art a = new Art();
                            a.setAuthor(rs.getString("author"));
                            a.setCreationYear(rs.getInt("creation_year"));
                            item = a;
                            break;
                        case "VEHICLE":
                            Vehicle v = new Vehicle();
                            v.setBrand(rs.getString("brand"));
                            v.setEngineCapacity(rs.getString("engine_capacity"));
                            v.setFuelType(rs.getString("fuel_type"));
                            v.setWarrantyPeriod(rs.getInt("warranty_period"));
                            item = v;
                            break;
                        case "ELECTRONIC":
                            Electronic el = new Electronic();
                            el.setBrand(rs.getString("brand"));
                            el.setWarrantyPeriod(rs.getInt("warranty_period"));
                            item = el;
                            break;
                        default:
                            System.out.println("Cảnh báo: Loại sản phẩm không xác định: " + type);
                            break;
                    }
                }


                if (item != null) {
                    item.setId(rs.getString("item_id"));
                    item.setName(rs.getString("item_name"));
                    item.setType(type);
                    item.setStartingPrice(rs.getDouble("start_price"));
                    item.setCurrentPrice(rs.getDouble("current_price"));
                    item.setMinIncrement(rs.getDouble("step_price"));
                    item.setLastBidderId(rs.getString("last_bidder_id"));
                    item.setStatus(rs.getString("status"));
                    item.setProductImageURL(rs.getString("productImageURL"));
                    item.setStartTime(rs.getTimestamp("StartTime"));
                    item.setEndTime(rs.getTimestamp("EndTime"));
                    item.setSeller_ID(rs.getString("seller_id"));

                    // Db lưu dạng TEXT "yyyy-MM-dd HH:mm:ss" - phải parse thủ công
                    try {
                        String startStr = rs.getString("StartTime");
                        String endStr = rs.getString("EndTime");
                        if (startStr != null) item.setStartTime(new java.sql.Timestamp(sdf.parse(startStr).getTime()));
                        if (startStr != null) item.setEndTime(new java.sql.Timestamp(sdf.parse(endStr).getTime()));
                    } catch (java.text.ParseException pe) {
                        System.out.println("Lỗi parse StartTime/EndTime:" + pe.getMessage());
                    }

                    return item;
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi khi tìm Item theo ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateStatusToSold(String itemId) {
        String sql = "UPDATE items SET status = 'SOLD' WHERE item_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi update status SOLD: " + e.getMessage());
            return false;
        }
    }
}