package com.auction.client.controller;

import com.auction.client.network.NetworkClient;

import java.awt.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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


public class SettingController implements Initializable {
    String role;
    @FXML
    private Label lblTime6;
    @FXML
    private Button btnBackFromSettings;
    @FXML
    private Label lblUsernameSettings1;
    @FXML
    private TextField txtUpdateEmail;
    @FXML
    private TextField txtUpdatePhone;
    @FXML
    private Button btnUpdateInfo;
    @FXML
    private PasswordField txtOldPass;
    @FXML
    private PasswordField txtNewPass;
    @FXML
    private PasswordField txtConfirmNewPass;
    @FXML
    private TextField txtUpdateAddress;
    @FXML
    private Label lblAddressTitle;
    @FXML
    private Label lblSettingsMessage;
    @FXML
    private TextField txtUpdateName;
    @FXML
    private Button btnDeleteAccount;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        clockInit(lblTime6);
    }

    @FXML
    public void toPreviousScreen(ActionEvent event) {
        if(role.equals("BIDDER")){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainScreen.fxml"));
            Parent root = loader.load();

            MainScreenController mainScreenController = loader.getController();

            String currentUser = lblUsernameSettings1.getText();

            mainScreenController.setDisplayName(currentUser);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Cài đặt tài khoản");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }}
        else if(role.equals("SELLER")) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/SellerScreen.fxml"));
                Parent root = loader.load();

                SellerController sellerController = loader.getController();

                String currentUser = lblUsernameSettings1.getText();

                sellerController.setDisplayName(currentUser);

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.getScene().setRoot(root);
                stage.setTitle("Cài đặt tài khoản");
                stage.show();
            }catch (IOException e) {
                e.printStackTrace();
            }
        }

        else if(role.equals("ADMIN")) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminScreen.fxml"));
                Parent root = loader.load();
                AdminController adminController = loader.getController();
                String currentUser = lblUsernameSettings1.getText();
                adminController.setDisplayName(currentUser);
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.getScene().setRoot(root);
                stage.setTitle("Cài đặt tài khoản");
                stage.show();
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void initData(String username, String role) {
        lblUsernameSettings1.setText(username);
        setupUIBasedOnRole(role);
    }

    public void setupUIBasedOnRole(String role) {
        this.role=role;
        if (role.equals("ADMIN") || role.equals("SELLER")) {
            lblAddressTitle.setVisible(false);
            txtUpdateAddress.setVisible(false);

            lblAddressTitle.setManaged(false);
            txtUpdateAddress.setManaged(false);
        } else {
            lblAddressTitle.setVisible(true);
            txtUpdateAddress.setVisible(true);
            lblAddressTitle.setManaged(true);
            txtUpdateAddress.setManaged(true);
        }
    }

    @FXML
    public void changePassLogic(ActionEvent event) {
        String oldPass = txtOldPass.getText();
        String newPass = txtNewPass.getText();
        String confirmPass = txtConfirmNewPass.getText();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            lblSettingsMessage.setStyle("-fx-text-fill: #f39c12;");
            lblSettingsMessage.setText(" Vui lòng nhập đầy đủ các ô mật khẩu!");
            return;
        }

        if (newPass.equals(oldPass)) {
            lblSettingsMessage.setStyle("-fx-text-fill: #e74c3c;");
            lblSettingsMessage.setText("Mật khẩu mới không được giống hệt mật khẩu cũ!");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            lblSettingsMessage.setStyle("-fx-text-fill: #e74c3c;");
            lblSettingsMessage.setText("Xác nhận mật khẩu không khớp!");
            return;
        }

        String currentUser = lblUsernameSettings1.getText().trim();

        lblSettingsMessage.setStyle("-fx-text-fill: #3498db;");
        lblSettingsMessage.setText("⏳ Đang xử lý yêu cầu đổi mật khẩu...");

        new Thread(() -> {
            String response = NetworkClient.sendChangePasswordRequest(currentUser, oldPass, newPass);

            javafx.application.Platform.runLater(() -> {
                if (response != null && response.startsWith("CHANGE_PASS_SUCCESS")) {
                    lblSettingsMessage.setStyle("-fx-text-fill: #27ae60;");
                    String msg = response.contains("|") ? response.split("\\|")[1] : "Đổi mật khẩu thành công!";
                    lblSettingsMessage.setText(" " + msg);

                    txtOldPass.clear();
                    txtNewPass.clear();
                    txtConfirmNewPass.clear();

                } else if (response != null && response.startsWith("CHANGE_PASS_FAIL")) {
                    lblSettingsMessage.setStyle("-fx-text-fill: #e74c3c;");
                    String errorMsg = response.contains("|") ? response.split("\\|")[1] : "Đổi mật khẩu thất bại!";
                    lblSettingsMessage.setText(" " + errorMsg);

                } else {
                    lblSettingsMessage.setStyle("-fx-text-fill: #e74c3c;");
                    lblSettingsMessage.setText(" Mất kết nối! Không nhận được phản hồi từ máy chủ.");
                }
            });
        }).start();
    }

    @FXML
    public void updateInfoLogic() {
        String newName = txtUpdateName.getText().trim();
        String newEmail = txtUpdateEmail.getText().trim();
        String newPhone = txtUpdatePhone.getText().trim();
        String newAddress = txtUpdateAddress.getText().trim();

        if (newName.isEmpty() && newEmail.isEmpty() && newPhone.isEmpty() && newAddress.isEmpty()) {
            lblSettingsMessage.setStyle("-fx-text-fill: #f39c12;");
            lblSettingsMessage.setText(" Vui lòng nhập ít nhất 1 thông tin muốn thay đổi!");
            return;
        }

        String currentUser = lblUsernameSettings1.getText().trim();

        lblSettingsMessage.setStyle("-fx-text-fill: #3498db;");
        lblSettingsMessage.setText("⏳ Đang xử lý cập nhật...");

        new Thread(() -> {
            String response = NetworkClient.sendUpdateInfoRequest(currentUser, newName, newEmail, newPhone, newAddress);

            javafx.application.Platform.runLater(() -> {
                if (response != null && response.startsWith("UPDATE_SUCCESS")) {
                    lblSettingsMessage.setStyle("-fx-text-fill: #27ae60;");
                    String msg = response.contains("|") ? response.split("\\|")[1] : "Cập nhật thông tin thành công!";
                    lblSettingsMessage.setText(" " + msg);

                    if (!newName.isEmpty()) {
                        lblUsernameSettings1.setText(newName);
                    }

                    txtUpdateName.clear();
                    txtUpdateEmail.clear();
                    txtUpdatePhone.clear();
                    txtUpdateAddress.clear();

                } else if (response != null && response.startsWith("UPDATE_FAIL")) {
                    lblSettingsMessage.setStyle("-fx-text-fill: #e74c3c;");
                    String errorMsg = response.contains("|") ? response.split("\\|")[1] : "Cập nhật thất bại!";
                    lblSettingsMessage.setText(" " + errorMsg);

                } else {
                    lblSettingsMessage.setStyle("-fx-text-fill: #e74c3c;");
                    lblSettingsMessage.setText(" Mất kết nối! Không nhận được phản hồi từ máy chủ.");
                }
            });
        }).start();
    }
    public void DeleteUser(ActionEvent event) throws IOException{
        String response = NetworkClient.deleteUser(lblUsernameSettings1.getText());

        String[] parts = response.split("\\|");
        String status = parts[0];
        String message = (parts.length > 1) ? parts[1] : "Lỗi không xác định";

        if (status.equals("DELETE_SUCCESS")) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thành công");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();

            SceneSwitchUtil.switchScene(event,"/SignInScreen.fxml");
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi xóa tài khoản");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
        }
    }
