package com.auction.server.dao;

import com.auction.server.database.DatabaseManager;
import com.auction.shared.model.Art;
import com.auction.shared.model.Electronic;
import com.auction.shared.model.Item; // Đảm bảo bạn đã có class Item trong thư mục shared/model
import com.auction.shared.model.Vehicle;
import com.auction.shared.model.user.Admin;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    public boolean insertItem(Item item) {
        String sql = "INSERT INTO items (item_name, start_price,currentprice, step_price, lastbidder_id, status,type,productImageURL) VALUES ( ?, ?, ?, ?, ?,?,?,?)";

        try (Connection conn1 = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn1.prepareStatement(sql)) {

            pstmt.setString(1, item.getId());
            pstmt.setString(2, item.getName());
            pstmt.setDouble(3, item.getStartingPrice());
            pstmt.setDouble(4, item.getCurrentPrice()); // Vừa đăng lên thì giá hiện tại = giá khởi điểm
            pstmt.setDouble(5, item.getMinIncrement());
            pstmt.setString(6, item.getLastBidderId());
            pstmt.setString(7, item.getStatus());
            pstmt.setString(8, item.getType());
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
        String sql2 = "SELECT * FROM items WHERE status = 'ACTIVE'";

        try (Connection conn2 = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = conn2.prepareStatement(sql2);
             ResultSet rs = preparedStatement.executeQuery()) {
            {
                try (ResultSet result = preparedStatement.executeQuery()) {
                    while (rs.next()) {
                        Item foundItem = null;
                        String type = result.getString("type");

                        if (type != null) {
                            switch (type.toUpperCase()) {
                                case "ELECTRONIC":
                                    foundItem = new Electronic();
                                    break;
                                case "VEHICLE":
                                    foundItem = new Vehicle();
                                    break;
                                case "ART":
                                default:
                                    foundItem = new Art();
                                    break;
                            }
                        }
                        if (foundItem != null) {
                            foundItem.setId(rs.getString("item_id"));
                            foundItem.setName(rs.getString("item_name"));
                            foundItem.setStartingPrice(rs.getDouble("starting_price"));
                            foundItem.setCurrentPrice(rs.getDouble("current_price"));
                            foundItem.setMinIncrement(rs.getDouble("step_price"));
                            foundItem.setLastBidderId(rs.getString("lastbidder_id"));
                            foundItem.setEndTime(rs.getDate("EndTime"));
                            foundItem.setStatus(rs.getString("status"));
                            foundItem.setStartTime(rs.getDate("StartTime"));
                            foundItem.setProductImageURL(rs.getString("productImageURL"));

                            itemList.add(foundItem);
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Lỗi khi lấy danh sách tài sản: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return itemList;
    }


    // 3. CẬP NHẬT GIÁ TÀI SẢN
    public boolean updateCurrentPrice(int itemId, double newPrice) {
        String sql = "UPDATE items SET current_price = ? WHERE item_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, newPrice);
            pstmt.setInt(2, itemId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật giá: " + e.getMessage());
            return false;
        }

    }

    //Lấy danh sách tài sản đang được đấu giá
    public List<Item> getPreparedItems() {
        List<Item> itemList = new ArrayList<>();
        String sql3 = "SELECT * FROM items WHERE status = 'PREPARED'";

        try (Connection conn2 = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = conn2.prepareStatement(sql3);
             ResultSet rs = preparedStatement.executeQuery()) {
            {
                try (ResultSet result = preparedStatement.executeQuery()) {
                    while (rs.next()) {
                        Item foundItem = null;
                        String type = result.getString("type");

                        if (type != null) {
                            switch (type.toUpperCase()) {
                                case "ELECTRONIC":
                                    foundItem = new Electronic();
                                    break;
                                case "VEHICLE":
                                    foundItem = new Vehicle();
                                    break;
                                case "ART":
                                default:
                                    foundItem = new Art();
                                    break;
                            }
                        }
                        if (foundItem != null) {
                            foundItem.setId(rs.getString("item_id"));
                            foundItem.setName(rs.getString("item_name"));
                            foundItem.setStartingPrice(rs.getDouble("starting_price"));
                            foundItem.setCurrentPrice(rs.getDouble("current_price"));
                            foundItem.setMinIncrement(rs.getDouble("step_price"));
                            foundItem.setLastBidderId(rs.getString("lastbidder_id"));
                            foundItem.setEndTime(rs.getDate("EndTime"));
                            foundItem.setStatus(rs.getString("status"));
                            foundItem.setStartTime(rs.getDate("StartTime"));
                            foundItem.setProductImageURL(rs.getString("productImageURL"));

                            itemList.add(foundItem);
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Lỗi khi lấy danh sách tài sản: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return itemList;
    }

    //Lấy danh sách tài sản đã được đấu giá
    public List<Item> getSoldItems() {
        List<Item> itemList = new ArrayList<>();
        String sql4 = "SELECT * FROM items WHERE status = 'SOLD'";

        try (Connection conn2 = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = conn2.prepareStatement(sql4);
             ResultSet rs = preparedStatement.executeQuery()) {
            {
                try (ResultSet result = preparedStatement.executeQuery()) {
                    while (rs.next()) {
                        Item foundItem = null;
                        String type = result.getString("type");

                        if (type != null) {
                            switch (type.toUpperCase()) {
                                case "ELECTRONIC":
                                    foundItem = new Electronic();
                                    break;
                                case "VEHICLE":
                                    foundItem = new Vehicle();
                                    break;
                                case "ART":
                                default:
                                    foundItem = new Art();
                                    break;
                            }
                        }
                        if (foundItem != null) {
                            foundItem.setId(rs.getString("item_id"));
                            foundItem.setName(rs.getString("item_name"));
                            foundItem.setStartingPrice(rs.getDouble("starting_price"));
                            foundItem.setCurrentPrice(rs.getDouble("current_price"));
                            foundItem.setMinIncrement(rs.getDouble("step_price"));
                            foundItem.setLastBidderId(rs.getString("lastbidder_id"));
                            foundItem.setEndTime(rs.getDate("EndTime"));
                            foundItem.setStatus(rs.getString("status"));
                            foundItem.setStartTime(rs.getDate("StartTime"));
                            foundItem.setProductImageURL(rs.getString("productImageURL"));

                            itemList.add(foundItem);
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Lỗi khi lấy danh sách tài sản: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return itemList;
    }

    //Cập nhật status
    public void updateToActive() throws SQLException {
        String sql5 = "UPDATE items SET status = 'ACTIVE' WHERE status='PREPARED' AND startTime<=? AND ?<=endTime";
        long TimeNow = System.currentTimeMillis();
        Timestamp now = new Timestamp(TimeNow);

        Connection conn2 = DatabaseManager.getConnection();
        PreparedStatement preparedStatement = conn2.prepareStatement(sql5);

        preparedStatement.setTimestamp(1, now);
        preparedStatement.setTimestamp(2, now);
    }

    public void updateToSold() throws SQLException {
        String sql5 = "UPDATE items SET status = 'SOLD' WHERE status='ACTIVE' AND ?>=endTime";
        long TimeNow = System.currentTimeMillis();
        Timestamp now = new Timestamp(TimeNow);

        Connection conn2 = DatabaseManager.getConnection();
        PreparedStatement preparedStatement = conn2.prepareStatement(sql5);

        preparedStatement.setTimestamp(1, now);
    }
}
}
