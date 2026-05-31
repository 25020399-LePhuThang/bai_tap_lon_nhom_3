package com.auction.client.controller; // Thay bằng package chuẩn của bạn

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneSwitchUtil {
    public static void switchScene(ActionEvent event, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(SceneSwitchUtil.class.getResource(fxmlFile));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Lỗi không thể tải được giao diện: " + fxmlFile);
            e.printStackTrace();
        }
    }
}