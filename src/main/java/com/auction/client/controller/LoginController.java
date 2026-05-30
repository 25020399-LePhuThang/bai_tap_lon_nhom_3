package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import com.auction.shared.model.user.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import static com.auction.client.controller.ClockUtil.clockInit;
import static com.auction.client.controller.SceneSwitchUtil.switchScene;


public class LoginController implements Initializable {
    @FXML
   private Label lbltime1;
    @FXML
    private TextField txtLogin1;
    @FXML
   private PasswordField txtPass1;
    @FXML
    private Button btnRegister2;
    @FXML
    private Label lblError1;
    @FXML
   private Button btnLogin2;
    @FXML
    private Button btnBack3;
    @FXML
    private ComboBox<String> cbxRole;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        clockInit(lbltime1);
        cbxRole.setItems(FXCollections.observableArrayList("Người mua (Bidder)", "Người bán (Seller)", "Quản trị (Admin)"));


        cbxRole.getSelectionModel().selectFirst();
    }
    @FXML
    public void LoginLogic(ActionEvent event) throws IOException {
        // Lấy dữ liệu từ các ô nhập liệu
        String user = txtLogin1.getText();
        String pass = txtPass1.getText();

        // 1. Lấy vai trò mà người dùng đã chọn từ ComboBox
        String selectedRole = cbxRole.getValue();

        // Chặn lỗi: Nếu chưa nhập đủ thông tin thì dừng luôn, không gửi lên Server
        if (user == null || user.trim().isEmpty() || pass == null || pass.trim().isEmpty() || selectedRole == null) {
            System.out.println("Vui lòng nhập đầy đủ tài khoản, mật khẩu và chọn vai trò!");
            lblError1.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // 2. Dịch lựa chọn tiếng Việt sang mã (Code) để Server dễ hiểu
        String roleCode = "BIDDER"; // Mặc định là Người mua
        if (selectedRole.equals("Người bán (Seller)")) {
            roleCode = "SELLER";
        } else if (selectedRole.equals("Quản trị (Admin)")) {
            roleCode = "ADMIN";
        }

        // 3. Đóng gói chuỗi gửi đi (Thêm roleCode vào cuối)
        String request = "LOGIN|" + user + "|" + pass + "|" + roleCode;

        String response = NetworkClient.sendAndReceive(request);

        if (response != null && response.startsWith("LOGIN_SUCCESS")) {
            System.out.println("Đăng nhập thành công với vai trò: " + roleCode);

            if (roleCode.equals("BIDDER")) {
                UsernamePass(user);

            } else if (roleCode.equals("SELLER")) {
                UsernamePass2(user);

            } else if (roleCode.equals("ADMIN")) {
                UsernamePass3(user);
            }

        } else {

            String errorMsg = "Đăng nhập thất bại! Không kết nối được Server.";
            if (response != null && response.contains("|")) {
                errorMsg = response.split("\\|")[1];
            }
            System.out.println(errorMsg);
            lblError1.setText(errorMsg);
        }
    }
    public void toRegisterScreen(ActionEvent event) throws IOException {
        switchScene(event, "/RegisterScreen.fxml");
    }

    public void toWelcome(ActionEvent event) throws IOException {
        switchScene(event, "/hello-view.fxml");
    }

    public void UsernamePass(String currentUser) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainScreen.fxml"));
            Parent root = loader.load();

            MainScreenController mainController = loader.getController();


            mainController.setDisplayName(currentUser);

            Stage stage = (Stage) btnLogin2.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void UsernamePass2(String currentUser) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SellerScreen.fxml"));
            Parent root = loader.load();

            SellerController sellerController = loader.getController();


            sellerController.setDisplayName(currentUser);

            Stage stage = (Stage) btnLogin2.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void UsernamePass3(String currentUser) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminScreen.fxml"));
            Parent root = loader.load();
            AdminController adminController = loader.getController();
            adminController.setDisplayName(currentUser);
            Stage stage = (Stage) btnLogin2.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

