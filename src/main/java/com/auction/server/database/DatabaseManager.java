package com.auction.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    // Sửa chuỗi URL để bật chế độ ghi đợi (WAL) và đặt thời gian chờ nếu file bị bận
    private static final String URL = "jdbc:sqlite:/app/data/auction_system.db";

    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(URL);
        } catch (ClassNotFoundException e) {
            System.err.println("CRASH: Không tìm thấy thư viện SQLite trong lúc chạy (thiếu file jar)!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("CRASH: Lỗi tạo hoặc đọc file Database trên Railway!");
            e.printStackTrace();
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

        String sqlBidTransactions =
                "CREATE TABLE IF NOT EXISTS BidTransactions (" +
                        "  id        INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "  itemId    TEXT    NOT NULL," +
                        "  bidderId  TEXT    NOT NULL," +
                        "  amount    REAL    NOT NULL," +
                        "  timestamp TEXT    NOT NULL" +
                        ");";

        String sqlAutoBids = "CREATE TABLE IF NOT EXISTS autobids ("
                + "username TEXT NOT NULL,"
                + "item_id TEXT NOT NULL,"
                + "max_bid REAL NOT NULL,"
                + "increment REAL NOT NULL,"
                + "PRIMARY KEY (username, item_id)"
                + ");";

        try (Connection conn = getConnection();
             var stmt = conn.createStatement()) {
            if (conn != null) {
                stmt.execute(sqlUsers);
                stmt.execute(sqlItems);
                stmt.execute(sqlBidTransactions);
                stmt.execute(sqlAutoBids);
                System.out.println("DB init OK!");
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tạo bảng trong Database: ");
            e.printStackTrace();
        }
    }
}