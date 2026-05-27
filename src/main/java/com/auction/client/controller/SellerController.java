package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import com.auction.shared.model.item.Art;
import com.auction.shared.model.item.Electronic;
import com.auction.shared.model.item.Item;

import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import com.auction.shared.model.item.Vehicle;
import com.auction.shared.model.user.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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
import javafx.util.Duration;

public class SellerController implements Initializable {

    @FXML
    private Label lblSellerName;
    @FXML
    private Label lblBalance;
    @FXML
    private Label lblTime;


    @FXML
    private Button btnDeposit;
    @FXML
    private Button btnWithdraw;
    @FXML
    private Button btnSettings;
    @FXML
    private Button btnLogout;
    @FXML
    private Button btnRefresh;
    @FXML
    private Button btnAddItem;
    @FXML
    private Button btnBackFromSeller;


    @FXML
    private TableView<Item> tableItems;
    @FXML
    private TableColumn<Item, Integer> colId;
    @FXML
    private TableColumn<Item, String> colName;
    @FXML
    private TableColumn<Item, String> colType;
    @FXML
    private TableColumn<Item, Double> colStartPrice;
    @FXML
    private TableColumn<Item, Double> colCurrentPrice;
    @FXML
    private TableColumn<Item, String> colStatus;


    @FXML
    private TextField txtItemName;
    @FXML
    private TextField txtStartPrice;
    @FXML
    private TextField txtStepPrice;
    @FXML
    private TextField txtImageUrl;
    @FXML
    private DatePicker dpEndDate;
    @FXML
    private DatePicker dpStartDate;
    @FXML
    private ComboBox<String> cbItemType;
    @FXML
    private TextField txtBrand;
    @FXML
    private TextField txtWarranty;
    @FXML
    private TextField txtAuthor;
    @FXML
    private TextField txtCreationYear;
    @FXML
    private TextField txtFuelType;
    @FXML
    private TextField txtEngineCapacity;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        clockInit();
        setupTableColumns();

        tableItems.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Item selectedItem = tableItems.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    openItemScreen(selectedItem);
                }
            }
        });

        cbItemType.setItems(FXCollections.observableArrayList("ART", "VEHICLE", "ELECTRONIC"));


        cbItemType.getSelectionModel().selectFirst();
    }

    private void setupTableColumns() {
        NumberFormat usdFormat = NumberFormat.getCurrencyInstance(Locale.US);

        colId.setCellValueFactory(new PropertyValueFactory<>("itemID"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colStartPrice.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText((empty || price == null) ? null : usdFormat.format(price));
            }
        });

        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colCurrentPrice.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText((empty || price == null) ? null : usdFormat.format(price));
            }
        });
    }

    public void clockInit() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss  dd/MM/yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e ->
                lblTime.setText(LocalDateTime.now().format(formatter))
        ), new KeyFrame(Duration.seconds(1)));

        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    public void setDisplayName(String currentUser) {
        lblSellerName.setText(currentUser);

        new Thread(() -> {
            String balanceResponse = NetworkClient.getBalanceRequest(currentUser);

            Platform.runLater(() -> {
                if (balanceResponse != null && balanceResponse.startsWith("BALANCE_SUCCESS")) {
                    String balance = balanceResponse.split("\\|")[1];
                    lblBalance.setText(balance + " $");
                } else {
                    lblBalance.setText("0.0 $");
                }
            });

            List<Item> myItems = NetworkClient.getItemsBySellerIdRequest(currentUser);

            Platform.runLater(() -> {
                if (myItems != null && !myItems.isEmpty()) {
                    tableItems.setItems(FXCollections.observableArrayList(myItems));
                } else {
                    tableItems.setItems(FXCollections.observableArrayList());
                }
            });

        }).start();
    }

    public void updateBalanceDisplay(String newBalance) {
        Platform.runLater(() -> lblBalance.setText(newBalance + " $"));
    }

    //Chi tiết sản phẩm
    public void openItemScreen(Item selectedItem) {
        if (selectedItem == null) return;

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

        //Thuộc tính phụ
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
                    + "Bảo hành: " + vehicle.getWarrantyPeriod() + "tháng\n"
                    + "Nhiên liệu: " + vehicle.getFuelType() + "\n"
                    + "Dung tích động cơ: " + vehicle.getEngineCapacity() + "\n"
            ;

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

    // CÁC HÀM CHUYỂN TRANG
    private void switchScence(ActionEvent event, String fxmlFile) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);    }

    public void toSignInScreen(ActionEvent event) throws IOException {
        switchScence(event, "/SignInScreen.fxml");
    }

    public void toSettingScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SettingScreen.fxml"));
            Parent root = loader.load();

            SettingController settingController = loader.getController();
            settingController.initData(lblSellerName.getText(), "SELLER");

            Stage stage = (Stage) btnSettings.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void toDepositScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Transaction.fxml"));
            Parent root = loader.load();

            TransactionController transactionController = loader.getController();
            transactionController.initData("NAP_TIEN", lblSellerName.getText(), this);

            Stage popUpStage = new Stage();
            popUpStage.setScene(new Scene(root));
            popUpStage.setTitle("GIAO DỊCH NẠP TIỀN");
            popUpStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popUpStage.setResizable(false);
            popUpStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void toWithdrawScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Transaction.fxml"));
            Parent root = loader.load();

            TransactionController transactionController = loader.getController();
            transactionController.initData("RUT_TIEN", lblSellerName.getText(), this);

            Stage popUpStage = new Stage();
            popUpStage.setScene(new Scene(root));
            popUpStage.setTitle("GIAO DỊCH RÚT TIỀN");
            popUpStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popUpStage.setResizable(false);
            popUpStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleAddItem(ActionEvent event) {
        try {
            String name = txtItemName.getText();
            String type = cbItemType.getValue();
            String imgUrl = txtImageUrl.getText();
            String sellerName = lblSellerName.getText();


            if (name.isEmpty() || type == null || txtStartPrice.getText().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập Tên, Loại và Giá khởi điểm!");
                return;
            }

            double startPrice = Double.parseDouble(txtStartPrice.getText());
            double stepPrice = Double.parseDouble(txtStepPrice.getText());

            if (dpStartDate.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng chọn Ngày bắt đầu");
                return;
            }
            LocalDate localStartDate = dpStartDate.getValue();
            Date startTime = Date.from(localStartDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());

            if (dpEndDate.getValue() == null) {
                showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng chọn Ngày kết thúc!");
                return;
            }
            LocalDate localEndDate = dpEndDate.getValue();
            Date endTime = Date.from(localEndDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());


            Item newItem = null;

            switch (type.toUpperCase()) {
                case "ELECTRONIC":
                    Electronic el = new Electronic();
                    el.setBrand(txtBrand.getText());
                    el.setWarrantyPeriod(Integer.parseInt(txtWarranty.getText().isEmpty() ? "0" : txtWarranty.getText()));
                    newItem = el;
                    break;

                case "VEHICLE":
                    Vehicle v = new Vehicle();
                    v.setBrand(txtBrand.getText());
                    v.setWarrantyPeriod(Integer.parseInt(txtWarranty.getText().isEmpty() ? "0" : txtWarranty.getText()));
                    v.setEngineCapacity(txtEngineCapacity.getText());
                    v.setFuelType(txtFuelType.getText());
                    newItem = v;
                    break;

                case "ART":
                    Art a = new Art();
                    a.setAuthor(txtAuthor.getText());
                    a.setCreationYear(Integer.parseInt(txtCreationYear.getText().isEmpty() ? "0" : txtCreationYear.getText()));
                    newItem = a;
                    break;

                default:
                    showAlert(Alert.AlertType.ERROR, "Lỗi phân loại", "Loại sản phẩm không hợp lệ!");
                    return;
            }


            newItem.setName(name);
            newItem.setType(type.toUpperCase());
            newItem.setStartingPrice(startPrice);
            newItem.setCurrentPrice(startPrice);
            newItem.setMinIncrement(stepPrice);
            newItem.setSeller_ID(sellerName);
            newItem.setProductImageURL(imgUrl);
            newItem.setStatus("WAITING");
            newItem.setStartTime(startTime);
            newItem.setEndTime(endTime);

            boolean isSuccess = NetworkClient.createItemRequest(newItem);


            if (isSuccess) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã đăng sản phẩm lên sàn đấu giá thành công!");
                clearForm();
                setDisplayName(sellerName);
                handleRefresh();
            } else {
                showAlert(Alert.AlertType.ERROR, "Thất bại", "Lỗi Server! Không thể tạo sản phẩm lúc này.");
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Giá tiền, Năm sáng tác hoặc Thời gian bảo hành phải là SỐ!");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi không xác định", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleRefresh() {
        String currentUser = lblSellerName.getText();
        if (currentUser != null && !currentUser.isEmpty()) {
            setDisplayName(currentUser);
        }
    }

    private void clearForm() {
        txtItemName.clear();
        txtStartPrice.clear();
        txtStepPrice.clear();
        txtImageUrl.clear();
        dpEndDate.setValue(null);
        cbItemType.getSelectionModel().clearSelection();

        txtBrand.clear();
        txtWarranty.clear();
        txtAuthor.clear();
        txtCreationYear.clear();
        txtFuelType.clear();
        txtEngineCapacity.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    public void handleLogout(ActionEvent event) throws IOException {
        NetworkClient.disconnect(lblSellerName.getText());
        switchScence(event, "/SignInScreen.fxml");
    }

    public void toInfoScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/InfoScreen.fxml"));
            Parent root = loader.load();

            InfoController infoController = loader.getController();
            infoController.initData(lblSellerName.getText(), this);

            Stage popUpStage = new Stage();
            popUpStage.setScene(new Scene(root));
            popUpStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popUpStage.setResizable(false);
            popUpStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleDeleteItem() {
        Item selectedItem = tableItems.getSelectionModel().getSelectedItem();
        String itemID = selectedItem.getItemID();

        if (selectedItem == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi không xác định", "Không tìm thấy sản phẩm");
            return;
        }
        String status = selectedItem.getStatus();
        if (!"WAITING".equalsIgnoreCase(status) && !"PREPARED".equalsIgnoreCase(status)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi thực thi", "Sản phẩm đang trong thời kì đấu giá, không được phép xóa.");
            return;
        }

        String msg = NetworkClient.deleteItem(itemID);
        if(msg.startsWith("DELETE_SUCCESS")){
            showAlert(Alert.AlertType.INFORMATION,"Thông báo","Xóa sản phẩm thành công");
            handleRefresh();
        }
    }
}