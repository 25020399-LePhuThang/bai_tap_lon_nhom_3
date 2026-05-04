package com.auction.client.view.java.com;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import javafx.scene.control.Button;
import java.io.IOException;

public class HelloController {
    @FXML
    private Button btnDangNhap;
    @FXML
    private Button btnDangKy;
    @FXML
    private Label lblThoiGian1;

    public void toSignInScreen(ActionEvent event) throws IOException {
        switchScence( event,"SignInScreen.fxml");
    }
    public void toRegisterScreen(ActionEvent event) throws IOException{
        switchScence(event,"RegisterScreen.fxml");
    }
    private void switchScence(ActionEvent event, String fxmlFile) throws IOException{
        Parent root= FXMLLoader.load(getClass().getResource(fxmlFile));
        Stage stage=(Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene=new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}