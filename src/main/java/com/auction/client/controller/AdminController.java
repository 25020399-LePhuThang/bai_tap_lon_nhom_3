package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.user.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static com.auction.client.controller.ClockUtil.clockInit;
import static com.auction.client.controller.SceneSwitchUtil.switchScene;

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
    private TableView<Item> tbvPendingItems;
    @FXML
    private TableColumn<Item, String> colItemId;
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
    private TableView<User> tbvUsers;
    @FXML
    private TableColumn<User, String> colUserId;
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

    // --- BIẾN QUẢN LÝ DỮ LIỆU REAL-TIME ---
    private final ObservableList<Item> masterItemList = FXCollections.observableArrayList();
    private final ObservableList<User> masterUserList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        clockInit(lblTimeAdmin);
        setupTableColumns();

        // 1. Cấu hình tính năng Search (CHỈ GỌI 1 LẦN DUY NHẤT LÚC KHỞI TẠO)
        setupSearchBindings();

        // 2. Lắng nghe tín hiệu Real-time từ Server
        NetworkClient.getInstance().startListening(response -> {
            if (response == null) return;

            // Bắt mọi tín hiệu có thể làm thay đổi dữ liệu để tải lại bảng
            if (response.equals("SERVER_SIGNAL_REFRESH") || response.startsWith("NOTIFY_") || response.startsWith("UPDATE_")) {
                Platform.runLater(() -> {
                    loadDataFromServer(); // Tải data âm thầm đằng sau
                });
            }
        });
    }

    // ==========================================
    // XỬ LÝ DỮ LIỆU & TÌM KIẾM CỐT LÕI
    // ==========================================
    public void setDisplayName(String currentUser) {
        lblAdminName.setText(currentUser);
        loadDataFromServer(); // Lần đầu vào màn hình thì tải data lên
    }

    @FXML
    public void handleRefresh() {
        // Nút bấm thủ công trên giao diện
        String currentUser = lblAdminName.getText();
        if (currentUser != null && !currentUser.isEmpty()) {
            loadDataFromServer();
        }
    }

    private void loadDataFromServer() {
        // Mở luồng phụ để đi lấy dữ liệu, không làm đơ giật giao diện
        new Thread(() -> {
            List<Item> waitingItems;
            try {
                waitingItems = NetworkClient.takeWaitingItemRequest();
            } catch (Exception e) {
                waitingItems = new ArrayList<>();
            }

            List<User> allUsers;
            try {
                allUsers = NetworkClient.getAllUsers();
            } catch (Exception e) {
                allUsers = new ArrayList<>();
            }

            // 1. Tạo 2 biến copy final để "chốt" dữ liệu
            final List<Item> finalWaitingItems = waitingItems;
            final List<User> finalAllUsers = allUsers;

            // 2. Ném 2 biến final đó vào luồng chính để cập nhật UI
            Platform.runLater(() -> {
                masterItemList.setAll(finalWaitingItems);
                masterUserList.setAll(finalAllUsers);

                tbvPendingItems.refresh();
                tbvUsers.refresh();
            });
        }).start();
    }

    private void setupSearchBindings() {
        // --- TÌM KIẾM SẢN PHẨM ---
        FilteredList<Item> filteredItems = new FilteredList<>(masterItemList, b -> true);
        txtSearchItem.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredItems.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();

                if (item.getName() != null && item.getName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (String.valueOf(item.getItemID()).contains(lowerCaseFilter)) return true;
                if (item.getSeller_ID() != null && item.getSeller_ID().toLowerCase().contains(lowerCaseFilter)) return true;
                return item.getType() != null && item.getType().toLowerCase().contains(lowerCaseFilter);
            });
        });
        SortedList<Item> sortedItems = new SortedList<>(filteredItems);
        sortedItems.comparatorProperty().bind(tbvPendingItems.comparatorProperty());
        tbvPendingItems.setItems(sortedItems);

        // --- TÌM KIẾM NGƯỜI DÙNG ---
        FilteredList<User> filteredUsers = new FilteredList<>(masterUserList, b -> true);
        txtSearchUser.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredUsers.setPredicate(user -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();

                if (user.getName() != null && user.getName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (String.valueOf(user.getId()).contains(lowerCaseFilter)) return true;
                if (user.getPhoneNumber() != null && user.getPhoneNumber().contains(lowerCaseFilter)) return true;
                return user.getRole() != null && user.getRole().toLowerCase().contains(lowerCaseFilter);
            });
        });
        SortedList<User> sortedUsers = new SortedList<>(filteredUsers);
        sortedUsers.comparatorProperty().bind(tbvUsers.comparatorProperty());
        tbvUsers.setItems(sortedUsers);
    }

    private void setupTableColumns() {
        colItemId.setCellValueFactory(new PropertyValueFactory<>("itemID"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colItemType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colSellerId.setCellValueFactory(new PropertyValueFactory<>("seller_ID"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("name"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        colUserStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    // ==========================================
    // CÁC HÀM XỬ LÝ SỰ KIỆN NÚT BẤM
    // ==========================================
    @FXML
    private void handleApproveItem() {
        Item selectedItem = tbvPendingItems.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            lblItemMessage.setText("Thông báo, Vui lòng click chọn một sản phẩm trong bảng để duyệt!");
            return;
        }

        String itemId = String.valueOf(selectedItem.getItemID());
        boolean success = NetworkClient.approveItem(itemId);

        if (success) {
            lblItemMessage.setText("Sản phẩm đã được duyệt");
            lblItemMessage.setStyle("-fx-text-fill: #27ae60;");
            loadDataFromServer(); // Load lại bảng ngay
        } else {
            lblItemMessage.setText("Lỗi thao tác trên Server!");
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
            lblItemMessage.setText("Thành công, Đã từ chối sản phẩm " + selectedItem.getName());
            loadDataFromServer(); // Load lại bảng ngay
        } else {
            lblItemMessage.setText("Lỗi, Không thể xóa sản phẩm này.");
        }
    }

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
            loadDataFromServer(); // Load lại bảng
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
            loadDataFromServer(); // Load lại bảng
        } else {
            lblUserMessage.setText("Lỗi, Không thể mở khóa tài khoản này.");
        }
    }

    @FXML
    public void DeleteUser() {
        User selectedUser = tbvUsers.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cảnh báo");
            alert.setHeaderText(null);
            alert.setContentText("Vui lòng click chọn một người dùng trên bảng để xóa!");
            alert.showAndWait();
            return;
        }

        String targetUsername = selectedUser.getName();

        new Thread(() -> {
            String response = NetworkClient.deleteUser(targetUsername);

            Platform.runLater(() -> {
                if (response != null && response.startsWith("DELETE_SUCCESS")) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Thành công");
                    alert.setHeaderText(null);
                    alert.setContentText("Đã xóa thành công tài khoản: " + targetUsername);
                    alert.showAndWait();

                    loadDataFromServer(); // Load lại bảng
                } else {
                    String errorMsg = (response != null && response.contains("|"))
                            ? response.split("\\|")[1]
                            : "Lỗi không xác định hoặc mất kết nối";

                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Lỗi xóa tài khoản");
                    alert.setHeaderText(null);
                    alert.setContentText(errorMsg);
                    alert.showAndWait();
                }
            });
        }).start();
    }

    // ==========================================
    // CHUYỂN MÀN HÌNH VÀ TIỆN ÍCH
    // ==========================================
    public void toSignInScreen(ActionEvent event) throws IOException {
        switchScene(event, "/SignInScreen.fxml");
    }

    public void toInfoScreen() {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void toSettingScreen(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SettingScreen.fxml"));
            Parent root = loader.load();
            SettingController settingController = loader.getController();
            settingController.initData(lblAdminName.getText(), "ADMIN");
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Cài đặt tài khoản");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) throws IOException {
        NetworkClient.getInstance().detachListener();
        NetworkClient.disconnect(lblAdminName.getText());
        switchScene(event, "/SignInScreen.fxml");
    }
}