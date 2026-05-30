package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import com.auction.shared.model.item.Art;
import com.auction.shared.model.item.Electronic;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.Vehicle;

import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import static com.auction.client.controller.SceneSwitchUtil.switchScene;

public class MainScreenController implements Initializable {

    @FXML private Button btnUpdate;
    @FXML private Button btnQuaylai2;
    @FXML private TextField txtSearch1;
    @FXML private Label lblTime5;
    @FXML private Button btnDangXuat1;
    @FXML private Label lblName;
    @FXML private Button btnAvt;
    @FXML private ImageView imgUserAvatar;
    @FXML private Button btnDeposit;
    @FXML private Button btnWithdraw;
    @FXML private Label lblBalance;

    // --- Bảng 1: Đang diễn ra ---
    @FXML private TableView<Item> tbvIsPresenting;
    @FXML private TableColumn<Item, String> NameColumn1;
    @FXML private TableColumn<Item, Integer> IDcolumn1;
    @FXML private TableColumn<Item, String> typeColumn1;
    @FXML private TableColumn<Item, Double> RecentPriceColumn;
    @FXML private TableColumn<Item, Date> EndTimeColumn1;

    // --- Bảng 2: Sắp diễn ra ---
    @FXML private TableView<Item> tbvWillPresent;
    @FXML private TableColumn<Item, String> NameColumn2;
    @FXML private TableColumn<Item, Integer> IDcolumn2;
    @FXML private TableColumn<Item, String> typeColumn2;
    @FXML private TableColumn<Item, Double> StartPriceColumn;
    @FXML private TableColumn<Item, Date> StartTimeColumn;
    @FXML private TableColumn<Item, Date> EndTimeColumn2;
    @FXML private Button btnRefresh;

    private final ObservableList<Item> activeMasterList = FXCollections.observableArrayList();
    private final ObservableList<Item> preparedMasterList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        clockInit();
        setupTableColumns();

        if (imgUserAvatar != null) {
            double radius = imgUserAvatar.getFitWidth() / 2;
            Circle clip = new Circle(radius, radius, radius);
            imgUserAvatar.setClip(clip);
        }

        // Sự kiện click đúp bảng 1 (ĐANG DIỄN RA) -> Chuyển sang màn hình Đấu giá
        tbvIsPresenting.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Item selectedItem = tbvIsPresenting.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    try {
                        // NGẮT luồng nhận giá của MainScreen trước khi sang phòng chi tiết
                        NetworkClient.getInstance().detachListener();

                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/AuctionDetail.fxml"));
                        Parent root = loader.load();
                        AuctionDetailController auctionDetailController = loader.getController();

                        auctionDetailController.setDisplayName(lblName.getText());
                        auctionDetailController.setItemData(selectedItem);

                        Parent mainRoot = tbvIsPresenting.getScene().getRoot();
                        auctionDetailController.setPreviousScreen(mainRoot, this);

                        Stage stage = (Stage) tbvIsPresenting.getScene().getWindow();
                        stage.setTitle("Chi tiết: " + selectedItem.getName());
                        stage.getScene().setRoot(root);
                        stage.show();
                    } catch (IOException e) { e.printStackTrace(); }
                }
            }
        });

        // Sự kiện click đúp bảng 2 (SẮP DIỄN RA) -> Mở hộp thoại thông tin tĩnh
        tbvWillPresent.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Item selectedItem = tbvWillPresent.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Chi tiết sản phẩm");
                    alert.setHeaderText("Tên sản phẩm: " + selectedItem.getName());

                    String sellerId = selectedItem.getSeller_ID();
                    if (sellerId == null || sellerId.trim().isEmpty() || sellerId.equalsIgnoreCase("null")) {
                        sellerId = "Không rõ";
                    }

                    String winnerId = selectedItem.getLastBidderId();
                    if (winnerId == null || winnerId.trim().isEmpty() || winnerId.equalsIgnoreCase("null")) {
                        winnerId = "Chưa có người thắng";
                    }

                    String details = "Mã SP: " + selectedItem.getId() + "\n"
                            + "Phân loại: " + selectedItem.getType() + "\n"
                            + "Giá khởi điểm: " + selectedItem.getStartingPrice() + " $\n"
                            + "Giá hiện tại: " + selectedItem.getCurrentPrice() + " $\n"
                            + "ID Người bán: " + sellerId + "\n"
                            + "ID Người thắng: " + winnerId + "\n"
                            + "Trạng thái: " + selectedItem.getStatus() + "\n";

                    String extraDetails = "\n Thông tin chi tiết \n";

                    if (selectedItem instanceof Electronic) {
                        Electronic electronic = (Electronic) selectedItem;
                        extraDetails += "Thương hiệu: " + electronic.getBrand() + "\n"
                                + "Bảo hành: " + electronic.getWarrantyPeriod() + " tháng\n";
                    } else if (selectedItem instanceof Art) {
                        Art art = (Art) selectedItem;
                        extraDetails += "Tác giả: " + art.getAuthor() + "\n"
                                + "Năm sáng tác: " + art.getCreationYear() + "\n";
                    } else if (selectedItem instanceof Vehicle) {
                        Vehicle vehicle = (Vehicle) selectedItem;
                        extraDetails += "Thương hiệu: " + vehicle.getBrand() + "\n"
                                + "Bảo hành: " + vehicle.getWarrantyPeriod() + " tháng\n"
                                + "Nhiên liệu: " + vehicle.getFuelType() + "\n"
                                + "Dung tích động cơ: " + vehicle.getEngineCapacity() + "\n";
                    } else {
                        extraDetails = "";
                    }

                    alert.setContentText(details + extraDetails);

                    try {
                        String imageUrl = selectedItem.getProductImageURL();
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            javafx.scene.image.Image image = new javafx.scene.image.Image(imageUrl, 150, 150, true, true);
                            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
                            imageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 5, 0, 0, 0);");
                            alert.setGraphic(imageView);
                        }
                    } catch (Exception e) {
                        System.err.println("Không thể load ảnh lên hộp thoại: " + e.getMessage());
                    }
                    alert.showAndWait();
                }
            }
        });

        Search();
        // setupSocketListener() sẽ được gọi bên trong hàm setDisplayName sau khi tải xong Data
    }

    private void setupTableColumns() {
        NumberFormat usdFormat = NumberFormat.getCurrencyInstance(Locale.US);

        NameColumn1.setCellValueFactory(new PropertyValueFactory<>("name"));
        IDcolumn1.setCellValueFactory(new PropertyValueFactory<>("itemID"));
        typeColumn1.setCellValueFactory(new PropertyValueFactory<>("type"));
        RecentPriceColumn.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        RecentPriceColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText((empty || price == null) ? null : usdFormat.format(price));
            }
        });
        EndTimeColumn1.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        NameColumn2.setCellValueFactory(new PropertyValueFactory<>("name"));
        IDcolumn2.setCellValueFactory(new PropertyValueFactory<>("itemID"));
        typeColumn2.setCellValueFactory(new PropertyValueFactory<>("type"));
        StartPriceColumn.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        StartPriceColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText((empty || price == null) ? null : usdFormat.format(price));
            }
        });
        StartTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        EndTimeColumn2.setCellValueFactory(new PropertyValueFactory<>("endTime"));
    }

    public void clockInit() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss  dd/MM");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            lblTime5.setText(LocalDateTime.now().format(formatter));

            // TỰ ĐỘNG LỌC BỎ SẢN PHẨM HẾT HẠN KHỎI TRANG CHỦ THEO GIÂY REALTIME
            if (activeMasterList != null && !activeMasterList.isEmpty()) {
                Date now = new Date();
                boolean dynamicRemoved = activeMasterList.removeIf(item ->
                        item.getEndTime() != null && !now.before(item.getEndTime())
                );
                if (dynamicRemoved && tbvIsPresenting != null) {
                    tbvIsPresenting.refresh();
                }
            }
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    public void setDisplayName(String currentUser) {
        if (btnRefresh != null) btnRefresh.setDisable(true);
        NetworkClient.getInstance().detachListener();

        new Thread(() -> {
            try {
                Thread.sleep(200); // Đợi ngắt luồng cũ an toàn

                String balanceResponse = NetworkClient.getBalanceRequest(currentUser);
                List<Item> activeItems = NetworkClient.takeActiveItems();
                List<Item> preparedItems = NetworkClient.takePreparedItems();

                Platform.runLater(() -> {
                    lblName.setText(currentUser);

                    if (balanceResponse != null && balanceResponse.startsWith("BALANCE_SUCCESS")) {
                        lblBalance.setText(balanceResponse.split("\\|")[1] + " $");
                    } else {
                        lblBalance.setText("0.0 $");
                    }

                    activeMasterList.setAll(activeItems != null ? activeItems : new ArrayList<>());
                    preparedMasterList.setAll(preparedItems != null ? preparedItems : new ArrayList<>());

                    if (btnRefresh != null) btnRefresh.setDisable(false);
                    setupSocketListener(); // Bật lại luồng lắng nghe cho màn Main
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    if (btnRefresh != null) btnRefresh.setDisable(false);
                    setupSocketListener();
                });
            }
        }).start();
    }

    // Hàm cực kỳ quan trọng để reload data và nối lại luồng mạng khi quay lại từ AuctionDetail
    public void loadActiveAndPreparedItems() {
        if (btnRefresh != null) btnRefresh.setDisable(true);

        new Thread(() -> {
            try {
                List<Item> activeItems = NetworkClient.takeActiveItems();
                List<Item> preparedItems = NetworkClient.takePreparedItems();

                String currentUser = lblName.getText();
                String balanceResponse = null;
                if (currentUser != null && !currentUser.isEmpty() && !currentUser.equals("Khách")) {
                    balanceResponse = NetworkClient.getBalanceRequest(currentUser);
                }

                final String finalBalance = balanceResponse;
                Platform.runLater(() -> {
                    activeMasterList.setAll(activeItems != null ? activeItems : new ArrayList<>());
                    preparedMasterList.setAll(preparedItems != null ? preparedItems : new ArrayList<>());

                    if (finalBalance != null && finalBalance.startsWith("BALANCE_SUCCESS")) {
                        lblBalance.setText(finalBalance.split("\\|")[1] + " $");
                    }
                    if (btnRefresh != null) btnRefresh.setDisable(false);
                });
            } catch (Exception e) {
                System.err.println("Lỗi đồng bộ nạp lại bảng: " + e.getMessage());
                Platform.runLater(() -> {
                    if (btnRefresh != null) btnRefresh.setDisable(false);
                });
            }
        }).start();
    }

    public void updateBalanceDisplay(String newBalance) {
        Platform.runLater(() -> lblBalance.setText(newBalance + " $"));
    }


    @FXML
    public void toInfoScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/InfoScreen.fxml"));
            Parent root = loader.load();

            InfoController infoController = loader.getController();
            infoController.initData(lblName.getText(), this);

            Stage popUpStage = new Stage();
            popUpStage.setScene(new Scene(root));
            popUpStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popUpStage.setResizable(false);
            popUpStage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void toLoginScreen(ActionEvent event) throws IOException {
        switchScene(event, "/SignInScreen.fxml");
    }

    @FXML
    public void toSettingScreen(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SettingScreen.fxml"));
            Parent root = loader.load();

            SettingController settingController = loader.getController();
            settingController.initData(lblName.getText(), "BIDDER");

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Cài đặt tài khoản");
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void toDepositScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Transaction.fxml"));
            Parent root = loader.load();

            TransactionController transactionController = loader.getController();
            transactionController.initData("NAP_TIEN", lblName.getText(), this);

            Stage popUpStage = new Stage();
            popUpStage.setScene(new Scene(root));
            popUpStage.setTitle("GIAO DỊCH NẠP TIỀN");
            popUpStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popUpStage.setResizable(false);
            popUpStage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void toWithdrawScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Transaction.fxml"));
            Parent root = loader.load();

            TransactionController transactionController = loader.getController();
            transactionController.initData("RUT_TIEN", lblName.getText(), this);

            Stage popUpStage = new Stage();
            popUpStage.setScene(new Scene(root));
            popUpStage.setTitle("GIAO DỊCH RÚT TIỀN");
            popUpStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popUpStage.setResizable(false);
            popUpStage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void handleLogout(ActionEvent event) throws IOException {
        NetworkClient.getInstance().detachListener();
        NetworkClient.disconnect(lblName.getText());
        switchScene(event, "/SignInScreen.fxml");
    }

    public void handleRefresh(ActionEvent event) {
        String currentUser = lblName.getText();
        if (currentUser != null && !currentUser.isEmpty()) {
            setDisplayName(currentUser);
        }
    }

    private void Search() {
        FilteredList<Item> filteredActive = new FilteredList<>(activeMasterList, b -> true);
        FilteredList<Item> filteredPrepared = new FilteredList<>(preparedMasterList, b -> true);

        txtSearch1.textProperty().addListener((observable, oldValue, newValue) -> {
            String lowerCaseFilter = (newValue == null) ? "" : newValue.toLowerCase().trim();

            java.util.function.Predicate<Item> filterPredicate = item -> {
                if (lowerCaseFilter.isEmpty()) return true;
                if (item.getName() != null && item.getName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (String.valueOf(item.getItemID()).contains(lowerCaseFilter)) return true;
                if (item.getSeller_ID() != null && item.getSeller_ID().toLowerCase().contains(lowerCaseFilter)) return true;
                return false;
            };

            filteredActive.setPredicate(filterPredicate);
            filteredPrepared.setPredicate(filterPredicate);
        });

        SortedList<Item> sortedActive = new SortedList<>(filteredActive);
        sortedActive.comparatorProperty().bind(tbvIsPresenting.comparatorProperty());
        tbvIsPresenting.setItems(sortedActive);

        SortedList<Item> sortedPrepared = new SortedList<>(filteredPrepared);
        sortedPrepared.comparatorProperty().bind(tbvWillPresent.comparatorProperty());
        tbvWillPresent.setItems(sortedPrepared);
    }

    private void setupSocketListener() {
        NetworkClient.getInstance().startListening(response -> {
            if (response == null || !response.startsWith("BID_UPDATE|")) return;

            // Xử lý cả trường hợp bản tin trả về có 3 hoặc 4 tham số (có hay không có winner)
            String[] p = response.split("\\|");
            if (p.length < 3) return;

            String msgItemId = p[1];
            double newPrice = Double.parseDouble(p[2]);
            String winner = (p.length >= 4) ? p[3] : "Chưa có";

            Platform.runLater(() -> {
                boolean isUpdated = false;
                for (Item item : activeMasterList) {
                    if (String.valueOf(item.getItemID()).equals(msgItemId) || String.valueOf(item.getId()).equals(msgItemId)) {
                        item.setCurrentPrice(newPrice);
                        item.setLastBidderId(winner);
                        isUpdated = true;
                        break;
                    }
                }

                if (isUpdated) {
                    tbvIsPresenting.refresh();
                    System.out.println("MainScreen: Cập nhật giá Live SP " + msgItemId + " -> " + newPrice + "$ (Winner: " + winner + ")");
                }
            });
        });
    }

    public void resumeSocketListener() {
        setupSocketListener();
    }
}