package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import javafx.scene.control.Button;
import java.io.IOException;

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