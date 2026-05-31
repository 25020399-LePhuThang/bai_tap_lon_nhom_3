package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static com.auction.client.controller.ClockUtil.clockInit;
import static com.auction.client.controller.SceneSwitchUtil.switchScene;

public class RegisterController implements Initializable {

    @FXML
    private Button btnRegister1;
    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPass;
    @FXML
    private PasswordField txtPass2;
    @FXML
    private TextField txtEmail;
    @FXML
    private TextField txtPhone;
    @FXML
    private Button btnOut;
    @FXML
    private Label lblTime4;
    @FXML
    private Button SignIN;
    @FXML
    private Label lblERROR;
    @FXML
    private ComboBox<String> cbRole;

    public void initialize(URL url, ResourceBundle resourceBundle) {
        clockInit(lblTime4);
        cbRole.setItems(FXCollections.observableArrayList("Người mua (Bidder)", "Người bán (Seller)"));
        cbRole.getSelectionModel().selectFirst();
    }

    public void toWelcomeScreen(ActionEvent event) throws IOException {
        switchScene(event, "/hello-view.fxml");
    }

    public void RegisterLogic(ActionEvent event) {
        lblERROR.setText("");
        lblERROR.setStyle("-fx-text-fill: #FF0000;");


        String username = txtUsername.getText().trim();
        String password1 = txtPass.getText();
        String password2 = txtPass2.getText();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        String role = cbRole.getValue().trim();

        if (username.isEmpty() || phone.isEmpty() || email.isEmpty() || password1.isEmpty() || role == null) {
            lblERROR.setText("Nhập đủ thông tin để tiếp tục!");
        } else if (!username.matches("^[a-zA-Z0-9_]{4,20}$")) {
            lblERROR.setText("Tên đăng nhập từ 4-20 ký tự, không chứa kí tự đặc biệt");
            txtUsername.requestFocus();
        } else if (password1.length() < 8) {
            lblERROR.setText("Mật khẩu quá ngắn");
            txtPass.requestFocus();
        } else if (password1.length() > 30) {
            lblERROR.setText("Mật khẩu quá dài");
            txtPass.requestFocus();
        } else if (!password1.equals(password2)) {
            lblERROR.setText("Nhập lại mật khẩu không khớp");
            txtPass2.requestFocus();
        } else if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            lblERROR.setText("Email không hợp lệ");
            txtEmail.requestFocus();
        } else if (!phone.matches("^0[0-9]{9}$")) {
            lblERROR.setText("Số điện thoại không hợp lệ");
            txtPhone.requestFocus();
        } else {
            lblERROR.setStyle("-fx-text-fill: #2E7D32;");
            lblERROR.setText("Dữ liệu hợp lệ. Đang xử lý đăng ký...");

            String finalRole = "BIDDER"; // Mặc định là mua
            if (role.contains("Seller") || role.contains("bán") || role.contains("Bán")) {
                finalRole = "SELLER";
            }
            // đợi liên kết với server
            boolean isSuccess = NetworkClient.sendRegisterRequest(username, password1, email, phone, finalRole);
            if (isSuccess) {
                switchScene(event, "/SignInScreen.fxml");

            } else {
                lblERROR.setText("         Tên đăng nhập hoặc Email đã tồn tại! Vui lòng thử lại.");
            }
        }
    }

    public void toSignInScreen(ActionEvent event) throws IOException {
        switchScene(event, "/SignInScreen.fxml");
    }
}

