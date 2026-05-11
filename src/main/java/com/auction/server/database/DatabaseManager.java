package com.auction.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String URL="jdbc:sqlite:auction_system.db";
 public static Connection getConnection() {
        Connection conn=null;
        try{
            conn=DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối tới dữ liệu.");
        }
        return conn;
 }
 public static void initDB() {
     String sql = "CREATE TABLE IF NOT EXISTS Users (" +
             "userId INTEGER PRIMARY KEY AUTOINCREMENT," +
             "username TEXT UNIQUE NOT NULL," +
             "password TEXT NOT NULL," +
             "email TEXT UNIQUE NOT NULL," +
             "phone TEXT," +
             "role TEXT DEFAULT 'BIDDER'" +
             ");";
     try (Connection conn = getConnection();
          var stmt = conn.createStatement()) {
         stmt.execute(sql);
         System.out.println("DB init OK!");
     } catch (Exception e) {
         e.printStackTrace();
     }
 }
}
