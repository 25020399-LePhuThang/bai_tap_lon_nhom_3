package com.auction.client.controller;

import javafx.application.Platform;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import com.auction.client.network.NetworkClient;
import com.auction.server.dao.ItemDAO;
import com.auction.shared.model.Auction;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class AdminController implements Initializable {
    // ==========================================
    // 1. CÁC THÀNH PHẦN HEADER (THÔNG TIN ADMIN & ĐIỀU HƯỚNG)
    // ==========================================
    @FXML
    private Label lblAdminName;
    @FXML
    private Label lblAdminLevel;
    @FXML
    private Label lblTimeAdmin;
    @FXML
    private Button btnProfileAdmin;
    @FXML
    private Button btnRefreshAdmin;
    @FXML
    private Button btnLogoutAdmin;
    @FXML
    private Button btnBackFromAdmin;

    // ==========================================
    // 2. TAB QUẢN LÝ SẢN PHẨM (TAB 1)
    // ==========================================
    @FXML
    private TextField txtSearchItem;
    @FXML
    private TableView<Item> tbvPendingItems; // Chú ý: Dùng đúng class Item của cậu
    @FXML
    private TableColumn<Item, String> colItemId; // Nếu ID của cậu là Int thì đổi String thành Integer nhé
    @FXML
    private TableColumn<Item, String> colItemName;
    @FXML
    private TableColumn<Item, String> colItemType;
    @FXML
    private TableColumn<Item, String> colSellerId;
    @FXML
    private TableColumn<Item, Double> colStartPrice;
    @FXML
    private TableColumn<Item, String> colStatus;

    @FXML
    private Button btnApproveItem;
    @FXML
    private Button btnRejectItem;

    // ==========================================
    // 3. TAB QUẢN LÝ NGƯỜI DÙNG (TAB 2)
    // ==========================================
    @FXML
    private TextField txtSearchUser;
    @FXML
    private TableView<User> tbvUsers; // Chú ý: Dùng đúng class User (Abstract) của cậu
    @FXML
    private TableColumn<User, String> colUserId; // Nếu ID là Int thì đổi String thành Integer
    @FXML
    private TableColumn<User, String> colUsername;
    @FXML
    private TableColumn<User, String> colRole;
    @FXML
    private TableColumn<User, String> colPhone;
    @FXML
    private TableColumn<User, String> colUserStatus;

    @FXML
    private Button btnBanUser;
    @FXML
    private Button btnUnbanUser;
    @FXML
    private Button btnSettingsAdmin;
    @FXML
    private Label lblItemMessage;
    @FXML
    private Label lblUserMessage;
    @FXML
    private Button DeleteUser;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        clockInit();
        setupTableColumns();
    }

    private void switchScence(ActionEvent event, String fxmlFile) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    public void clockInit() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss  dd/MM/yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e ->
                lblTimeAdmin.setText(LocalDateTime.now().format(formatter))
        ), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    public void toSignInScreen(ActionEvent event) throws IOException{
        switchScence(event,"/SignInScreen.fxml");
    }

    public void toInfoScreen(){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/InfoScreen.fxml"));
            Parent root = loader.load();

            InfoController infoController = loader.getController();
            infoController.initData(lblAdminName.getText(), this);

            Stage popUpStage = new Stage();
            popUpStage.setScene(new Scene(root));
            popUpStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popUpStage.setResizable(false);
            popUpStage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void toSettingScreen(ActionEvent event){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SettingScreen.fxml"));
            Parent root = loader.load();

            SettingController settingController = loader.getController();
            settingController.initData(lblAdminName.getText(), "ADMIN");

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Cài đặt tài khoản");
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void handleLogout(ActionEvent event) throws IOException {
        NetworkClient.disconnect(lblAdminName.getText());
        switchScence(event,"/SignInScreen.fxml");
    }



    public void setDisplayName(String currentUser) {
        lblAdminName.setText(currentUser);

        new Thread(() -> {

            List<Item> waitingItems;
            try {
                waitingItems = NetworkClient.takeWaitingItemRequest();
            } catch (Exception e) { waitingItems = new ArrayList<>(); }

            List<User> AllUsers;
            try {
                AllUsers = NetworkClient.getAllUsers();
            } catch (Exception e) { AllUsers = new ArrayList<>(); }

            final List<Item> finalActive = waitingItems;
            final List<User> finalPrepared = AllUsers;


                bindItemDataAndSearch(tbvPendingItems, finalActive,txtSearchItem);
                bindUserDataAndSearch(tbvUsers, finalPrepared,txtSearchUser);
            }).start();
    }

    private void bindItemDataAndSearch(TableView<Item> table, List<Item> items, TextField searchField) {
        if (items == null) items = new ArrayList<>();

        ObservableList<Item> masterList = FXCollections.observableArrayList(items);
        FilteredList<Item> filteredData = new FilteredList<>(masterList, b -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty()) return true;

                String lowerCaseFilter = newValue.toLowerCase();

                // Tìm theo Tên
                if (item.getName() != null && item.getName().toLowerCase().contains(lowerCaseFilter)) return true;
                // Tìm theo Mã SP (Giữ nguyên getItemID() theo code của cậu)
                if (String.valueOf(item.getItemID()).contains(lowerCaseFilter)) return true;
                // Tìm theo Mã Người bán
                if (item.getSeller_ID() != null && item.getSeller_ID().toLowerCase().contains(lowerCaseFilter)) return true;
                // Tìm theo Loại SP (Ví dụ: gõ "Art" ra tranh ảnh)
                if (item.getType() != null && item.getType().toLowerCase().contains(lowerCaseFilter)) return true;

                return false;
            });
        });

        SortedList<Item> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);
    }

    private void bindUserDataAndSearch(TableView<User> table, List<User> users, TextField searchField) {
        if (users == null) users = new ArrayList<>();

        ObservableList<User> masterList = FXCollections.observableArrayList(users);
        FilteredList<User> filteredData = new FilteredList<>(masterList, b -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(user -> {
                if (newValue == null || newValue.isEmpty()) return true;

                String lowerCaseFilter = newValue.toLowerCase();

                // Tìm theo Username
                if (user.getName() != null && user.getName().toLowerCase().contains(lowerCaseFilter)) return true;
                // Tìm theo Mã User
                if (String.valueOf(user.getId()).contains(lowerCaseFilter)) return true;
                // Tìm theo SĐT
                if (user.getPhoneNumber() != null && user.getPhoneNumber().contains(lowerCaseFilter)) return true;
                // Tìm theo Vai trò (BIDDER/SELLER)
                if (user.getRole() != null && user.getRole().toLowerCase().contains(lowerCaseFilter)) return true;

                return false;
            });
        });

        SortedList<User> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);
    }

    private void setupTableColumns() {
        colItemId.setCellValueFactory(new PropertyValueFactory<>("itemID"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colItemType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colSellerId.setCellValueFactory(new PropertyValueFactory<>("seller_ID"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // --- 2. BẢNG NGƯỜI DÙNG (Tab 2) ---
        // Tương tự, phải khớp với tên biến trong class User gốc
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("name"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        colUserStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }




// ... (Các phần code khác của AdminController)

    // ==========================================
    // 1. XỬ LÝ NÚT TAB SẢN PHẨM (DUYỆT / TỪ CHỐI)
    // ==========================================
    @FXML
    private void handleApproveItem() {
        // Lấy sản phẩm đang được click chọn trên bảng
        Item selectedItem = tbvPendingItems.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
            lblItemMessage.setText("Thông báo, Vui lòng click chọn một sản phẩm trong bảng để duyệt!");
            return;
        }

        // Đảm bảo dùng đúng hàm lấy ID của cậu (getItemID() hoặc getId())
        String itemId = String.valueOf(selectedItem.getItemID());
        boolean success = NetworkClient.approveItem(itemId);

        if (success) {
            lblItemMessage.setText("Sản phẩm đã được duyệt");
            lblItemMessage.setStyle("-fx-text-fill: #27ae60;");
        } else {
            lblItemMessage.setText("Vui lòng chọn một sản phẩm để thao tác!");
            lblItemMessage.setStyle("-fx-text-fill: #e74c3c;");
        }
    }
    @FXML
    private void handleRejectItem() {
        Item selectedItem = tbvPendingItems.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
            lblItemMessage.setText("Thông báo, Vui lòng click chọn một sản phẩm để từ chối!");
            return;
        }

        String itemId = String.valueOf(selectedItem.getItemID());
        boolean success = NetworkClient.rejectItem(itemId);

        if (success) {
            lblItemMessage.setText("Thành công, Đã xóa sản phẩm " + selectedItem.getName() + " khỏi hệ thống!");
        } else {
            lblItemMessage.setText("Lỗi, Không thể xóa sản phẩm này.");
        }
    }

    // ==========================================
    // 2. XỬ LÝ NÚT TAB NGƯỜI DÙNG (KHÓA / MỞ KHÓA)
    // ==========================================
    @FXML
    private void handleBanUser() {
        User selectedUser = tbvUsers.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            lblUserMessage.setText("Thông báo, Vui lòng click chọn một người dùng để khóa tài khoản!");
            return;
        }

        String userId = String.valueOf(selectedUser.getId());
        boolean success = NetworkClient.banUser(userId);

        if (success) {
            lblUserMessage.setText("Thành công, Đã đình chỉ tài khoản: " + selectedUser.getName());
        } else {
            lblUserMessage.setText("Lỗi, Không thể khóa tài khoản này.");
        }
    }
    @FXML
    private void handleUnbanUser() {
        User selectedUser = tbvUsers.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            lblUserMessage.setText("Thông báo, Vui lòng click chọn một người dùng để mở khóa!");
            return;
        }

        String userId = String.valueOf(selectedUser.getId());
        boolean success = NetworkClient.unbanUser(userId);

        if (success) {
            lblUserMessage.setText("Thành công, Đã mở khóa cho tài khoản: " + selectedUser.getName());
        } else {
            lblUserMessage.setText("Lỗi,Không thể mở khóa tài khoản này.");
        }
    }


    @FXML
    public void handleRefresh() {
        String currentUser = lblAdminName.getText();
        if (currentUser != null && !currentUser.isEmpty()) {
            setDisplayName(currentUser);
        }
    }

    public void DeleteUser(ActionEvent event) throws IOException{
        String response = NetworkClient.deleteUser(lblAdminName.getText());

        String[] parts = response.split("\\|");
        String status = parts[0];
        String message = (parts.length > 1) ? parts[1] : "Lỗi không xác định";

        if (status.equals("DELETE_SUCCESS")) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thành công");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();

            switchScence(event,"/SignInScreen.fxml");
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi xóa tài khoản");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }
    }

