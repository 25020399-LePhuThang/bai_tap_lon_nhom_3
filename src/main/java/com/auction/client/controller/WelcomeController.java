package com.auction.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static com.auction.client.controller.ClockUtil.clockInit;

public class WelcomeController implements Initializable {
    @FXML
    private Button btnDangNhap;
    @FXML
    private Button btnDangKy;
    @FXML
    private Label lblThoiGian1;

    public void initialize(URL url, ResourceBundle resourceBundle) {
        clockInit(lblThoiGian1);
    }

    public void toSignInScreen(ActionEvent event) throws IOException {
        SceneSwitchUtil.switchScene(event, "/SignInScreen.fxml");
    }

    public void toRegisterScreen(ActionEvent event) throws IOException {
        SceneSwitchUtil.switchScene(event, "/RegisterScreen.fxml");
    }
}