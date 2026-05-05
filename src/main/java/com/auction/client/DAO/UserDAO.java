package com.auction.client.DAO;

import com.auction.client.database.DatabaseManager;
import com.auction.shared.model.user.User;
import java.sql.*;

public class UserDAO {
    //Hàm đăng ký
    public boolean register(User user) {
        String sql = "INSERT INTO Users(name,password,email,phoneNumber,status) VALUES (?,?,?,?,?)";
        try( Connection conn=DatabaseManager.getConnection();
        PreparedStatement preparedStatement=conn.prepareStatement(sql)){
            preparedStatement.setString(1,user.getName());
            preparedStatement.setString(2,user.getPassword());
            preparedStatement.setString(3,user.getEmail());
            preparedStatement.setLong(4,user.getPhonenumber());
            preparedStatement.setString(5,user.getStatus());
            int rows=preparedStatement.executeUpdate();
            return rows>0;
        } catch (SQLException e) {
            System.err.println("Lỗi DAO");
            return false;
        }
    }
    public User Login(String name,String pass){
        String sql="SELECT * FROM Users WHERE username=? AND password=?";
        try(Connection connection=DatabaseManager.getConnection();
            PreparedStatement preparedStatement=connection.prepareStatement(sql)){
            preparedStatement.setString(1,name);
            preparedStatement.setString(2,pass);

            ResultSet result=preparedStatement.executeQuery();

            if(result.next()){
                String role=result.getString("role");
                User foundUser=null;
            }

        }
    }
}
