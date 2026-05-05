package com.auction.server.DAO;

import com.auction.client.database.DatabaseManager;
import com.auction.shared.model.user.Admin;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;
import java.sql.*;

public class UserDAO {
    //Hàm đăng ký
    public boolean register(User user) {
        String sql = "INSERT INTO Users(username,password,email,phone) VALUES (?,?,?,?)";
        try( Connection conn=DatabaseManager.getConnection();
        PreparedStatement preparedStatement=conn.prepareStatement(sql)){
            preparedStatement.setString(1,user.getName());
            preparedStatement.setString(2,user.getPassword());
            preparedStatement.setString(3,user.getEmail());
            preparedStatement.setString(4,user.getPhoneNumber());
            int rows=preparedStatement.executeUpdate();
            return rows>0;
        } catch (SQLException e) {
            System.err.println("Lỗi DAO");
            return false;
        }
    }
    //Hàm đăng nhập
    public static User Login(String name, String pass){
        String sql="SELECT * FROM Users WHERE username=? AND password=?";
        try(Connection connection=DatabaseManager.getConnection();
            PreparedStatement preparedStatement=connection.prepareStatement(sql)) {
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, pass);

            try (ResultSet result = preparedStatement.executeQuery()) {
                if (result.next()) {
                    User foundUser = null;
                    String role = result.getString("role");

                    if (role != null) {
                        switch (role.toUpperCase()) {
                            case "ADMIN":
                                foundUser = new Admin();
                                break;
                            case "SELLER":
                                foundUser = new Seller();
                                break;
                            case "BIDDER":
                            default:
                                foundUser = new Bidder();
                                break;
                        }
                    }
                    if (foundUser != null) {
                        foundUser.setId(result.getString("user_id"));
                        foundUser.setName(result.getString("username"));
                        foundUser.setPassword(result.getString("password"));
                        foundUser.setEmail(result.getString("email"));
                        foundUser.setPhoneNumber(result.getString("phone"));
                        foundUser.setStatus(result.getString("status"));
                        foundUser.setRole(result.getString("role"));
                    }
                    return foundUser;
                }
            }
        }catch(SQLException e){
                        System.err.println("Lỗi DAO");
                    }
                    return null;
                }
            }
