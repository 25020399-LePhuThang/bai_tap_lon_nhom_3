package com.auction.client.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String URL="jdbc:sqlite:auction_data.db";
 public static Connection getConnection() {
        Connection conn=null;
        try{
            conn=DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối tới dữ liệu.");
        }
        return conn;
 }
}
