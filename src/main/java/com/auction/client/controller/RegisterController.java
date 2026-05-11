package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import javafx.scene.control.Button;
import java.io.IOException;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

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

    public void initialize(URL url, ResourceBundle resourceBundle) {
        clockInit();
    }

    public void clockInit() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss  dd/MM/yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            lblTime4.setText(LocalDateTime.now().format(formatter));
        }), new KeyFrame(Duration.seconds(1)));

        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private void switchScence(ActionEvent event, String fxmlFile) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void toWelcomeScreen(ActionEvent event) throws IOException {
        switchScence(event, "/hello-view.fxml");
    }

    public void RegisterLogic(ActionEvent event) {
        lblERROR.setText("");
        lblERROR.setStyle("-fx-text-fill: #FF0000;");


        String username = txtUsername.getText().trim();
        String password1 = txtPass.getText();
        String password2 = txtPass2.getText();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();

        if (username.isEmpty() || phone.isEmpty() || email.isEmpty() || password1.isEmpty()) {
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

            Bidder newBidder = new Bidder();
            newBidder.setName(username);
            newBidder.setPassword(password1);
            newBidder.setEmail(email);
            newBidder.setPhoneNumber(phone);

            // đợi liên kết với server
            boolean isSuccess = NetworkClient.sendRegisterRequest(username, password1, email, phone);
            if (isSuccess) {
                try {
                    switchScence(event, "/SignInScreen.fxml");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                lblERROR.setText("         Tên đăng nhập hoặc Email đã tồn tại! Vui lòng thử lại.");
            }
        }
    }

    public void toSignInScreen(ActionEvent event) throws IOException {
        switchScence(event, "/SignInScreen.fxml");
    }
}

