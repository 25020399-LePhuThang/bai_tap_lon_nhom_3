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

public class WelcomeController implements Initializable {
    @FXML
    private Button btnDangNhap;
    @FXML
    private Button btnDangKy;
    @FXML
    private Label lblThoiGian1;

    public void initialize(URL url, ResourceBundle resourceBundle) {
        initClock();
    }

    private void initClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss  dd/MM/yyyy");

        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            lblThoiGian1.setText(LocalDateTime.now().format(formatter));
        }), new KeyFrame(Duration.seconds(1)));

        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    public void toSignInScreen(ActionEvent event) throws IOException {
        switchScence(event, "SignInScreen.fxml");
    }

    public void toRegisterScreen(ActionEvent event) throws IOException {
        switchScence(event, "RegisterScreen.fxml");
    }

    private void switchScence(ActionEvent event, String fxmlFile) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}