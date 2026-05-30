package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class InfoController {
    @FXML
    public Button btnClose;
    @FXML
    public Label lblRole;
    @FXML
    public Label lblUserId;
    @FXML
    public Label lblUsername;
    @FXML
    public Label lblEmail;
    @FXML
    public Label lblPhone;
    @FXML
    public Label lblShippingAddress;
    @FXML
    public Label lblRating;
    @FXML
    public Label lblRateTitle;
    @FXML
    public Label lblAddressTitle;
    private SellerController sellerParent;
    private MainScreenController bidderParent;

    @FXML
    public void handleCancel(javafx.event.ActionEvent event) {
        javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.close();
    }


    private Object parentController;

    public void initData(String username, Object parent) {
        this.parentController = parent;

        User user = NetworkClient.getUserInfo(username);

        if (user == null) {
            System.err.println("Lỗi: Không thể lấy dữ liệu cho tài khoản " + username);
            return;
        }

        lblUserId.setText(user.getId());
        lblUsername.setText(user.getName());
        lblEmail.setText(user.getEmail());
        lblPhone.setText(user.getPhoneNumber());

        String userRole = user.getRole().toUpperCase();

        if (userRole.equals("BIDDER")) {
            Bidder bidder = (Bidder) user;

            lblShippingAddress.setText(bidder.getShippingAddress());
            lblRateTitle.setVisible(false);
            lblRating.setVisible(false);

            lblRateTitle.setManaged(false);
            lblRating.setManaged(false);



            lblRole.setText("NGƯỜI MUA (BIDDER)");
            lblRole.setStyle("-fx-background-color: #27ae60; -fx-padding: 5 15; -fx-background-radius: 15; -fx-text-fill: white;");

        } else if (userRole.equals("SELLER")) {
            Seller seller = (Seller) user;

            lblRating.setText(seller.getRating()+"");
            lblAddressTitle.setVisible(false);
            lblShippingAddress.setVisible(false);

            lblShippingAddress.setManaged(false);
            lblAddressTitle.setManaged(false);


            lblRole.setText("NGƯỜI BÁN (SELLER)");
            lblRole.setStyle("-fx-background-color: #8e44ad; -fx-padding: 5 15; -fx-background-radius: 15; -fx-text-fill: white;");
        } else if (userRole.equals("ADMIN")) {
            lblAddressTitle.setVisible(false);
            lblShippingAddress.setVisible(false);
            lblRateTitle.setVisible(false);
            lblRating.setVisible(false);

            lblShippingAddress.setManaged(false);
            lblAddressTitle.setManaged(false);
            lblRateTitle.setManaged(false);
            lblRating.setManaged(false);

            lblRole.setText("QUẢN TRỊ VIÊN (ADMIN)");
            lblRole.setStyle("-fx-background-color: #c0392b; -fx-padding: 5 15; -fx-background-radius: 15; -fx-text-fill: white;");

        }
    }
}
