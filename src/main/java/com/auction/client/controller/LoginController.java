package com.auction.client.controller;

import com.auction.client.DAO.UserDAO;
import com.auction.client.database.DatabaseManager;
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


public class LoginController implements Initializable {
    @FXML
    Label lbltime1;
    @FXML
    TextField txtLogin1;
    @FXML
    PasswordField txtPass1;
    @FXML
    Button btnRegister2;
    @FXML
    Label lblError1;
    @FXML
    Button btnLogin2;

    private UserDAO userDAO = new UserDAO();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        clockInit();
    }

    public void clockInit() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss  dd/MM/yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            lbltime1.setText(LocalDateTime.now().format(formatter));
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

    public void LoginLogic(ActionEvent event) {
        String user = txtLogin1.getText();
        String pass = txtPass1.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            lblError1.setText("Vui lòng nhập tài khoản và mật khẩu");
            return;
        }

        String requestMessage = "LOGIN|" + user + "|" + pass;

        //gửi qua server
        String serverResponse = NetworkClient.sendAndReceive(requestMessage);


        if (serverResponse.equals("LOGIN_SUCCESS")) {
            try {
                switchScence(event, "MainScreen.fxml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            lblError1.setText("Sai tài khoản hoặc mật khẩu! Server từ chối.");
        }
    }
    public String toRegisterScreen(ActionEvent event) throws IOException {
        switchScence(event, "RegisterScreen.fxml");
        return "chuyển đến phần đăng kí";
    }
}

