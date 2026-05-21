package com.auction.server.DAO;

import com.auction.server.database.DatabaseManager;
import com.auction.shared.model.BidTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

    /**
     * Lưu một lần đặt giá vào database
     */
    public boolean save(BidTransaction bid) {
        String sql = "INSERT INTO BidTransactions (itemId, bidderId, amount, timestamp) " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, bid.getItemId());
            stmt.setString(2, bid.getBidderId());
            stmt.setDouble(3, bid.getAmount());
            stmt.setString(4, bid.getTimestamp().toString());

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("BidDAO.save() lỗi: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy toàn bộ lịch sử bid của 1 sản phẩm
     * Sắp xếp theo thời gian mới nhất lên đầu
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
                bid.setAmount(rs.getDouble("amount"));
                list.add(bid);
            }

        } catch (SQLException e) {
            System.err.println("BidDAO.findByItemId() lỗi: " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy bid cao nhất (mới nhất) của 1 sản phẩm
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
                bid.setAmount(rs.getDouble("amount"));
                return bid;
            }

        } catch (SQLException e) {
           System.err.println("BidDAO.findLatestByItemId() lỗi: " + e.getMessage());
        }
        return null;
    }

    /**
     * Đếm số lần bid của 1 sản phẩm
     */
    public int countByItemId(String itemId) {
        String sql = "SELECT COUNT(*) FROM BidTransactions WHERE itemId = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, itemId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("BidDAO.countByItemId() lỗi: " + e.getMessage());
        }
        return 0;
    }
}