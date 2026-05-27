package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import com.auction.shared.model.item.Item;

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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        clockInit();
        setupTableColumns();

        // Cắt avatar thành hình tròn
        if (imgUserAvatar != null) {
            double radius = imgUserAvatar.getFitWidth() / 2;
            Circle clip = new Circle(radius, radius, radius);
            imgUserAvatar.setClip(clip);
        }

        // Sự kiện click đúp bảng 1
        tbvIsPresenting.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Item selectedItem = tbvIsPresenting.getSelectionModel().getSelectedItem();
                if (selectedItem != null){
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/AuctionDetail.fxml"));
                        Parent root = loader.load();
                        AuctionDetailController auctionDetailController = loader.getController();
                        auctionDetailController.setItemData(selectedItem);
                        auctionDetailController.setDisplayName(lblName.getText());

                        // Lấy cửa sổ (Stage) hiện tại thông qua cái bảng tbvItems của màn hình chính
                        Stage stage = (Stage) tbvIsPresenting.getScene().getWindow();
                        stage.setTitle("Chi tiết: " + selectedItem.getName());
                        stage.setScene(new Scene(root));
                        stage.setMaximized(true);
                        stage.show();
                    } catch (IOException e) { e.printStackTrace(); }
                };
            }
        });

        // Sự kiện click đúp bảng 2
        tbvWillPresent.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Item selectedItem = tbvWillPresent.getSelectionModel().getSelectedItem();
                if (selectedItem != null){
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/AuctionDetail.fxml"));
                        Parent root = loader.load();
                        AuctionDetailController auctionDetailController = loader.getController();
                        auctionDetailController.setItemData(selectedItem);
                        auctionDetailController.setDisplayName(lblName.getText());

                        // Lấy cửa sổ (Stage) hiện tại thông qua cái bảng tbvItems của màn hình chính
                        Stage stage = (Stage) tbvWillPresent.getScene().getWindow();
                        stage.setTitle("Chi tiết: " + selectedItem.getName());
                        stage.setScene(new Scene(root));
                        stage.setMaximized(true);
                        stage.show();
                    } catch (IOException e) { e.printStackTrace(); }
                };
            }
        });

        // KHÔNG GỌI LOAD MẠNG Ở ĐÂY NỮA, ĐỂ NHƯỜNG CHO HÀM setDisplayName!
    }

    private void setupTableColumns() {
        NumberFormat usdFormat = NumberFormat.getCurrencyInstance(Locale.US);

        // Bảng 1
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

        // Bảng 2
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss  dd/MM/yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e ->
                lblTime5.setText(LocalDateTime.now().format(formatter))
        ), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    // =========================================================================
    // 1 THREAD KIỂM SOÁT TẤT CẢ (TRÁNH NGHẼN MẠNG)
    // =========================================================================
    public void setDisplayName(String currentUser) {
        lblName.setText(currentUser);

        new Thread(() -> {
            // Bước 1: Xin số dư ví
            String balanceResponse = NetworkClient.getBalanceRequest(currentUser);

            // Bước 2: Xin danh sách bảng 1
            List<Item> activeItems;
            try {
                activeItems = NetworkClient.takeActiveItems();
            } catch (Exception e) { activeItems = new ArrayList<>(); }

            // Bước 3: Xin danh sách bảng 2
            List<Item> preparedItems;
            try {
                preparedItems = NetworkClient.takePreparedItems();
            } catch (Exception e) { preparedItems = new ArrayList<>(); }

            // Đẩy tất cả dữ liệu lên UI cùng lúc cho mượt
            final List<Item> finalActive = activeItems;
            final List<Item> finalPrepared = preparedItems;

            Platform.runLater(() -> {
                // Đập tiền
                if (balanceResponse != null && balanceResponse.startsWith("BALANCE_SUCCESS")) {
                    lblBalance.setText(balanceResponse.split("\\|")[1] + " $");
                } else {
                    lblBalance.setText("0.0 $");
                }

                // Đập hàng vào 2 bảng + Gắn bộ lọc tìm kiếm
                bindDataAndSearch(tbvIsPresenting, finalActive);
                bindDataAndSearch(tbvWillPresent, finalPrepared);
            });
        }).start();
    }

    /**
     * Hàm dùng chung để đổ dữ liệu vào bảng và gắn chức năng ô tìm kiếm
     */
    private void bindDataAndSearch(TableView<Item> table, List<Item> items) {
        if (items == null) items = new ArrayList<>();
        ObservableList<Item> masterList = FXCollections.observableArrayList(items);
        FilteredList<Item> filteredData = new FilteredList<>(masterList, b -> true);

        txtSearch1.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty()) return true;

                String lowerCaseFilter = newValue.toLowerCase();

                // Đã fix lỗi toLowerCase() của Integer
                if (item.getName() != null && item.getName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (String.valueOf(item.getItemID()).contains(lowerCaseFilter)) return true;
                if (item.getSeller_ID() != null && item.getSeller_ID().toLowerCase().contains(lowerCaseFilter)) return true;

                return false;
            });
        });

        SortedList<Item> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);
    }

    // =========================================================================
    // HÀM DÀNH CHO POP-UP NẠP/RÚT TIỀN GỌI VỀ
    // =========================================================================
    public void updateBalanceDisplay(String newBalance) {
        Platform.runLater(() -> lblBalance.setText(newBalance + " $"));
    }

    private void switchScence(ActionEvent event, String fxmlFile) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    public void toInfoScreen()  {
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
        switchScence(event, "/SignInScreen.fxml");
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
            // Truyền "this" để Pop-up biết đường trả kết quả về màn hình Main
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
        NetworkClient.disconnect(lblName.getText());
        switchScence(event,"/SignInScreen.fxml");
    }

    public void handleRefresh(ActionEvent event){
       String currentUser=lblName.getText();
        if (currentUser != null && !currentUser.isEmpty()) {
            setDisplayName(currentUser);
        }
    }
    }
