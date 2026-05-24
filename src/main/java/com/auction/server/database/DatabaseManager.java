package com.auction.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:auction_system.db";

    public static Connection getConnection() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối tới dữ liệu.");
        }
        return conn;
    }

    public static void initDB() {
        String sqlUsers =
                "CREATE TABLE IF NOT EXISTS Users (" +
                        "  userId   INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "  username TEXT UNIQUE NOT NULL," +
                        "  password TEXT NOT NULL," +
                        "  email    TEXT UNIQUE NOT NULL," +
                        "  phone    TEXT," +
                        "  role     TEXT DEFAULT 'BIDDER'" +
                        ");";

        String sqlItems =
                "CREATE TABLE IF NOT EXISTS items (" +
                        "  item_id         INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "  item_name       TEXT NOT NULL," +
                        "  start_price     REAL NOT NULL," +
                        "  currentprice    REAL NOT NULL," +
                        "  step_price      REAL NOT NULL DEFAULT 0," +
                        "  lastbidder_id   TEXT," +
                        "  status          TEXT DEFAULT 'ACTIVE'," +
                        "  type            TEXT," +
                        "  productImageURL TEXT," +
                        "  StartTime       TEXT," +
                        "  EndTime         TEXT" +
                        ");";

        // Bảng lịch sử bid — dùng cho Bid History Visualization
        String sqlBidTransactions =
                "CREATE TABLE IF NOT EXISTS BidTransactions (" +
                        "  id        INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "  itemId    TEXT    NOT NULL," +
                        "  bidderId  TEXT    NOT NULL," +
                        "  amount    REAL    NOT NULL," +
                        "  timestamp TEXT    NOT NULL" +
                        ");";

        try (Connection conn = getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute(sqlUsers);
            stmt.execute(sqlItems);
            stmt.execute(sqlBidTransactions);
            System.out.println("DB init OK!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
