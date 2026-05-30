package com.auction.server.dao;

import com.auction.server.database.DatabaseManager;
import com.auction.shared.model.BidTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

    /**
     * Lưu một lần đặt giá vào database.
     * Dùng field getBidAmount() khớp with BidTransaction hiện có.
     */
    public boolean save(BidTransaction bid) {
        String sql = "INSERT INTO BidTransactions (itemId, bidderId, amount, timestamp) " +
                "VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, bid.getItemId());
            stmt.setString(2, bid.getBidderId());
            stmt.setDouble(3, bid.getBidAmount());
            stmt.setString(4, bid.getTimestamp() != null ? bid.getTimestamp().toString() : "");

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("BidDAO.save() lỗi: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy lịch sử bid sắp xếp theo thời gian TĂNG DẦN — dùng cho LineChart.
     */
    public List<BidTransaction> findByItemIdAsc(String itemId) {
        List<BidTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM BidTransactions WHERE itemId = ? ORDER BY timestamp ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, itemId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                BidTransaction bid = new BidTransaction();
                bid.setItemId(rs.getString("itemId"));
                bid.setBidderId(rs.getString("bidderId"));
                bid.setBidAmount(rs.getDouble("amount"));
                list.add(bid);
            }

        } catch (SQLException e) {
            System.err.println("BidDAO.findByItemIdAsc() lỗi: " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy lịch sử bid sắp xếp mới nhất lên đầu — dùng cho TableView.
     */
    public List<BidTransaction> findByItemId(String itemId) {
        List<BidTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM BidTransactions WHERE itemId = ? ORDER BY timestamp DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, itemId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                BidTransaction bid = new BidTransaction();
                bid.setItemId(rs.getString("itemId"));
                bid.setBidderId(rs.getString("bidderId"));
                bid.setBidAmount(rs.getDouble("amount"));
                list.add(bid);
            }

        } catch (SQLException e) {
            System.err.println("BidDAO.findByItemId() lỗi: " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy bid cao nhất (mới nhất) của 1 sản phẩm.
     */
    public BidTransaction findLatestByItemId(String itemId) {
        String sql = "SELECT * FROM BidTransactions WHERE itemId = ? " +
                "ORDER BY amount DESC, timestamp DESC LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, itemId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                BidTransaction bid = new BidTransaction();
                bid.setItemId(rs.getString("itemId"));
                bid.setBidderId(rs.getString("bidderId"));
                bid.setBidAmount(rs.getDouble("amount"));
                return bid;
            }

        } catch (SQLException e) {
            System.err.println("BidDAO.findLatestByItemId() lỗi: " + e.getMessage());
        }
        return null;
    }

    /**
     * Đếm số lần bid của 1 sản phẩm.
     */
    public int countByItemId(String itemId) {
        String sql = "SELECT COUNT(*) FROM BidTransactions WHERE itemId = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, itemId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) return rs.getInt(1);

        } catch (SQLException e) {
            System.err.println("BidDAO.countByItemId() lỗi: " + e.getMessage());
        }
        return 0;
    }

    // Nằm trong file BidDAO.java phía Server
    public double getAutoBidValue(String username, String itemId) {
        // Thay đổi tên bảng và tên cột cho khớp với thiết kế CSDL của Vương
        String sql = "SELECT max_bid FROM autobids WHERE username = ? AND item_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username != null ? username.trim() : "");

            // Ép kiểu thông minh: Thử chuyển đổi itemId sang số nguyên nếu DB định nghĩa là số
            try {
                int idInt = Integer.parseInt(itemId.trim());
                pstmt.setInt(2, idInt);
            } catch (NumberFormatException nfe) {
                pstmt.setString(2, itemId != null ? itemId.trim() : "");
            }

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("max_bid"); // Lấy giá trần đã cài
            }
        } catch (Exception e) {
            System.err.println("Lỗi check AutoBid trong DB: " + e.getMessage());
            e.printStackTrace();
        }
        return 0.0; // Trả về 0 nghĩa là chưa cài đặt
    }

    /**
     * Lưu hoặc cập nhật cấu hình AutoBid của người dùng xuống Database.
     */
    public boolean saveAutoBid(String username, String itemId, double maxBid, double increment) {
        String deleteSql = "DELETE FROM autobids WHERE username = ? AND item_id = ?";
        String insertSql = "INSERT INTO autobids (username, item_id, max_bid, increment) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            // Tách biệt hoàn toàn luồng DELETE, bảo đảm không làm kẹt luồng chèn dữ liệu phía sau
            try (PreparedStatement delStmt = conn.prepareStatement(deleteSql)) {
                delStmt.setString(1, username != null ? username.trim() : "");
                try {
                    delStmt.setInt(2, Integer.parseInt(itemId.trim()));
                } catch (NumberFormatException nfe) {
                    delStmt.setString(2, itemId != null ? itemId.trim() : "");
                }
                delStmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[Mẹo chặn] Bản ghi cũ chưa tồn tại hoặc lỗi xóa: " + e.getMessage());
            }

            // Tiến hành luồng chèn mới độc lập hoàn toàn
            try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                insStmt.setString(1, username != null ? username.trim() : "");
                try {
                    insStmt.setInt(2, Integer.parseInt(itemId.trim()));
                } catch (NumberFormatException nfe) {
                    insStmt.setString(2, itemId != null ? itemId.trim() : "");
                }
                insStmt.setDouble(3, maxBid);
                insStmt.setDouble(4, increment);
                return insStmt.executeUpdate() > 0;
            }
        } catch (Exception e) {
            System.err.println("Lỗi nghiêm trọng tại hàm saveAutoBid: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}