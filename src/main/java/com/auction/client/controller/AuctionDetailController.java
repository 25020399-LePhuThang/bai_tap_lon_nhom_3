package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import com.auction.server.dao.ItemDAO;
import com.auction.shared.model.Auction;
import com.auction.shared.model.item.Art;
import com.auction.shared.model.item.Electronic;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.Vehicle;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.User;
import javafx.animation.Animation;
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
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.text.SimpleDateFormat;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;



public class AuctionDetailController implements Initializable {

    // ================= HEADER & FOOTER =================
    @FXML private Label lblUsername2;
    @FXML private Button btnLogout;
    @FXML private Label lblTime4;
    @FXML private Button btnBack;

    // ================= CHI TIẾT SẢN PHẨM =================
    @FXML private ImageView imgProduct;
    @FXML private Label lblProductname;
    @FXML private Label lblProductID;
    @FXML private Label lblSeller;
    @FXML private Label lblTimeStart;
    @FXML private Label lblTimeEnd;

    // ================= THÔNG TIN GIÁ & ĐẤU GIÁ =================
    @FXML private Label lblClock;
    @FXML private Label lblStartPrice;
    @FXML private Label lblStepPrice;
    @FXML private Label lblRecentPrice;
    @FXML private TextField txtPrice;
    @FXML private Button btnAuction;
    @FXML private Button btnDetail;

    // ================= BẢNG & THỐNG KÊ =================
    // LƯU Ý: Thay chữ "History" bằng đúng tên Class lịch sử của cậu
//    @FXML private TableView<History> tbvHistory;
//    @FXML private TableColumn<History, String> colHistoryName;
//    @FXML private TableColumn<History, Double> colHistoryPrice;

    @FXML private Label lblUserAuctionCount;
    @FXML private Label lblBidCount;

    // ================= BIẾN TOÀN CỤC =================
    private Item currentItem;
    private User currentUser;
    private Timeline countdownTimeline;
    private Timeline clockTimeline;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        clockInit();
    }

    public void clockInit() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss  dd/MM/yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e ->
                lblTime4.setText(LocalDateTime.now().format(formatter))
        ), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    // Hàm nạp dữ liệu Sản phẩm
    public void setItemData(Item item) {
        this.currentItem = item;
        if (currentItem != null) {
            lblProductname.setText(currentItem.getName());
            lblProductID.setText(currentItem.getItemID());
            lblSeller.setText(currentItem.getSeller_ID());
            lblStartPrice.setText(currentItem.getStartingPrice()+"$");
            lblRecentPrice.setText(currentItem.getCurrentPrice()+"$");
            lblStepPrice.setText(currentItem.getMinIncrement()+"$");
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

            if (currentItem.getStartTime() != null) {
                lblTimeStart.setText(sdf.format(currentItem.getStartTime()));
            } else {
                lblTimeStart.setText("Chưa xác định");
            }

            if (currentItem.getEndTime() != null) {
                lblTimeEnd.setText(sdf.format(currentItem.getEndTime()));
            } else {
                lblTimeEnd.setText("Chưa xác định");
            }

            String imageUrl = currentItem.getProductImageURL();

            // 2. Kiểm tra xem URL có tồn tại không để tránh lỗi sập app
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                try {
                    // Tạo Bức ảnh từ URL (tham số 'true' giúp tải ảnh ngầm, không làm đơ giao diện)
                    Image image = new javafx.scene.image.Image(imageUrl, true);

                    // Lồng bức ảnh vào cái Khung imgProduct
                    imgProduct.setImage(image);
                } catch (Exception e) {
                    System.err.println("Lỗi không tải được ảnh: " + e.getMessage());
                }
            } else {
                System.out.println("Sản phẩm này chưa có link ảnh!");
            }
        }
        startCountdown();
    }

    // Hàm nạp riêng Tên hiển thị (Tách biệt hoàn toàn)
    public void setDisplayName(String displayName) {
        if (displayName != null && !displayName.isEmpty()) {
            lblUsername2.setText(displayName);
        } else {
            lblUsername2.setText("Khách");
        }
    }

    public void toMainScreen(ActionEvent event){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainScreen.fxml"));
            Parent root = loader.load();

            MainScreenController mainScreenController = loader.getController();
            mainScreenController.setDisplayName(lblUsername2.getText());

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }

    }

    private void switchScence(ActionEvent event, String fxmlFile) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @FXML
    public void handleLogout(ActionEvent event) throws IOException {
        NetworkClient.disconnect(lblUsername2.getText());
        switchScence(event,"/SignInScreen.fxml");
    }

    private void startCountdown() {
        // Dọn dẹp đồng hồ cũ nếu có chạy trước đó
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        // Nếu không có thời gian kết thúc thì cho nó về 0
        if (currentItem == null || currentItem.getEndTime() == null) {
            lblClock.setText("00:00:00");
            return;
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            Date now = new Date();
            Date endTime = currentItem.getEndTime();

            // Tính khoảng cách giữa 2 mốc thời gian (bằng mili-giây)
            long diffInMillis = endTime.getTime() - now.getTime();

            if (diffInMillis <= 0) {
                // HẾT GIỜ!
                lblClock.setText("ĐÃ KẾT THÚC");
                lblClock.setStyle("-fx-text-fill: #e74c3c;"); // Đổi màu chữ thành đỏ
                btnAuction.setDisable(true); // Khóa luôn nút Đấu giá không cho bấm nữa

                if (countdownTimeline != null) {
                    countdownTimeline.stop(); // Tắt đồng hồ
                }
            } else {
                // CÒN GIỜ: Quy đổi mili-giây ra Giờ, Phút, Giây
                long diffInSeconds = diffInMillis / 1000;
                long hours = diffInSeconds / 3600;
                long minutes = (diffInSeconds % 3600) / 60;
                long seconds = diffInSeconds % 60;

                // Nạp vào Label với format 2 chữ số (VD: 05:09:12)
                lblClock.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            }
        }));

        // Cho đồng hồ chạy lặp đi lặp lại vô hạn (đến khi mình gọi lệnh stop)
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    @FXML
    public void showProductDetails() {
        if (currentItem == null) return;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chi tiết sản phẩm");
        alert.setHeaderText("Tên sản phẩm: " + currentItem.getName());

        String sellerId = currentItem.getSeller_ID();
        if (sellerId == null || sellerId.trim().isEmpty() || sellerId.equalsIgnoreCase("null")) {
            sellerId = "Không rõ";
        }

        String winnerId = currentItem.getLastBidderId();
        if (winnerId == null || winnerId.trim().isEmpty() || winnerId.equalsIgnoreCase("null")) {
            winnerId = "Chưa có người thắng";
        }

        String details = "Mã SP: " + currentItem.getId() + "\n"
                + "Phân loại: " + currentItem.getType() + "\n"
                + "Giá khởi điểm: " + currentItem.getStartingPrice() + " $\n"
                + "Giá hiện tại: " + currentItem.getCurrentPrice() + " $\n"
                + "ID Người bán: " + sellerId + "\n"
                + "ID Người thắng: " + winnerId + "\n"
                + "Trạng thái: " + currentItem.getStatus() + "\n";


        String extraDetails = "\n Thông tin chi tiết \n";

        if (currentItem instanceof Electronic) {
            Electronic electronic = (Electronic) currentItem;
            extraDetails += "Thương hiệu: " + electronic.getBrand() + "\n"
                    + "Bảo hành: " + electronic.getWarrantyPeriod() + " tháng\n";

        } else if (currentItem instanceof Art) {
            Art art = (Art) currentItem;
            extraDetails += "Tác giả: " + art.getAuthor() + "\n"
                    + "Năm sáng tác: " + art.getCreationYear() + "\n";

        } else if (currentItem instanceof Vehicle) {
            Vehicle vehicle = (Vehicle) currentItem;
            extraDetails += "Thương hiệu: "+vehicle.getBrand()+"\n"
                    + "Bảo hành: "+vehicle.getWarrantyPeriod()+"tháng\n"
                    +"Nhiên liệu: " + vehicle.getFuelType() + "\n"
                    + "Dung tích động cơ: " + vehicle.getEngineCapacity() + "\n"
            ;

        } else {
            extraDetails = "";
        }

        alert.setContentText(details + extraDetails);

        try {
            String imageUrl = currentItem.getProductImageURL();
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
