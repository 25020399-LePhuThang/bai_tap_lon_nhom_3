package com.auction.client.controller;

import com.auction.client.Network.NetworkClient;
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

public class RegisterController implements Initializable{

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
    private Label lblError;
    @FXML
    private Label lblTime4;
    @FXML
    private Button SignIN;

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

    public void RegisterLogic(ActionEvent event){
        String username=txtUsername.getText();
        String password1=txtPass.getText();
        String password2=txtPass2.getText();
        String email=txtEmail.getText();
        String phone=txtPhone.getText();

        if(username.isEmpty()||phone.isEmpty()||email.isEmpty()||password1.isEmpty()){
            lblError.setText("        Quý khách vui lòng nhập đủ thông tin để tiếp tục!");
        }

        if(!username.matches("^[a-zA-Z0-9_]{4,20}$")){
            lblError.setText("       Tên đăng nhập chỉ từ 4-20 ký tự, không chứa kí tự đặc biệt");
            return;
        }

        if(password1.length()<8 ){
            lblError.setText("                Mật khẩu quá ngắn");
            return;
        }
        if(password1.length()>30){
            lblError.setText("                Mật khẩu quá dài");
            return;
        }
        if(password1!=password2){
            lblError.setText("     Nhập lại mật khẩu không khớp");
        }
        if(!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")){
            lblError.setText("       Email không hợp lệ");
        }
        if(!phone.matches("^0[0-9]{9}$")){
            lblError.setText("       Số điện thoại không hợp lệ");
        }





    }
    public void toSignInScreen(ActionEvent event) throws IOException {
        switchScence(event, "/SignInScreen.fxml");
    }
}
