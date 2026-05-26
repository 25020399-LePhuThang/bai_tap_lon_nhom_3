package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class TransactionController {


    @FXML private Label lblTitle;
    @FXML private TextField txtAmount;
    @FXML private Button btnConfirm;
    @FXML private Label lblStatusMsg;

    private String actionType;
    private String username;


    private SellerController sellerParent;
    private MainScreenController bidderParent;


    public void initData(String type, String user, Object parent) {
        this.actionType = type;
        this.username = user;

        if (parent instanceof SellerController) {
            this.sellerParent = (SellerController) parent;
        } else if (parent instanceof MainScreenController) {
            this.bidderParent = (MainScreenController) parent;
        }

        if ("NAP_TIEN".equals(type)) {
            lblTitle.setText("GIAO DỊCH NẠP TIỀN");
            btnConfirm.setText("Xác nhận nạp");
            btnConfirm.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            lblTitle.setText("GIAO DỊCH RÚT TIỀN");
            btnConfirm.setText("Xác nhận rút");
            btnConfirm.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        }
    }

    @FXML
    public void handleConfirm(ActionEvent event) {
        String amountStr = txtAmount.getText().trim();

        if (amountStr.isEmpty() || !amountStr.matches("\\d+(\\.\\d+)?")) {
            lblStatusMsg.setText("Vui lòng nhập số tiền hợp lệ!");
            lblStatusMsg.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        double amount = Double.parseDouble(amountStr);
        if (amount <= 0) {
            lblStatusMsg.setText("Số tiền phải lớn hơn 0!");
            lblStatusMsg.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }
        btnConfirm.setDisable(true);
        lblStatusMsg.setText("⏳ Đang xử lý giao dịch...");
        lblStatusMsg.setStyle("-fx-text-fill: #3498db;");

        new Thread(() -> {
            String response = ("NAP_TIEN".equals(actionType))
                    ? NetworkClient.sendDepositRequest(username, amount)
                    : NetworkClient.sendWithdrawRequest(username, amount);

            System.out.println("Kết quả giao dịch từ Server: " + response);

            Platform.runLater(() -> {
                if (response != null && response.contains("SUCCESS")) {

                    String newBalance = response.split("\\|")[1];

                    if (sellerParent != null) {
                        sellerParent.updateBalanceDisplay(newBalance);
                    } else if (bidderParent != null) {
                        bidderParent.updateBalanceDisplay(newBalance);
                    }

                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.close();
                } else {
                    btnConfirm.setDisable(false);
                    lblStatusMsg.setText("Giao dịch thất bại! Có thể do số dư không đủ.");
                    lblStatusMsg.setStyle("-fx-text-fill: #e74c3c;");
                }
            });
        }).start();}

    @FXML
    public void handleCancel(javafx.event.ActionEvent event) {
        javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}