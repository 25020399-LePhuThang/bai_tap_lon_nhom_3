package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.item.Item;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.event.ActionEvent;

public class AuctionDetailController implements Initializable {

    // ─── Labels thông tin sản phẩm ───────────────────────────────────────────
    @FXML private Label lblProductname;
    @FXML private Label lblProductID;
    @FXML private Label lblSeller;
    @FXML private Label lblTimeStart;
    @FXML private Label lblTimeEnd;
    @FXML private Label lblRecentPrice;
    @FXML private Label lblStartPrice;
    @FXML private Label lblStepPrice;
    @FXML private Label lblClock;       // Đồng hồ đếm ngược
    @FXML private Label lblTime4;       // Đồng hồ thực tế
    @FXML private Label lblUsername2;

    // ─── Ô nhập giá + nút ────────────────────────────────────────────────────
    @FXML private TextField txtPrice;
    @FXML private Button    btnAuction;
    @FXML private Button    btnBack;
    @FXML private Button    btnLogout;

    // ─── Ảnh sản phẩm ────────────────────────────────────────────────────────
    @FXML private ImageView imgProduct;

    // ─── TableView lịch sử (text) ─────────────────────────────────────────────
    @FXML private TableColumn<BidTransaction, String> tbvHistory; // cột "Số tiền"

    // ─── LineChart lịch sử giá realtime ─────────────────────────────────────
    @FXML private LineChart<Number, Number> priceChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;

    // ─── State ───────────────────────────────────────────────────────────────
    private Item currentItem;
    private XYChart.Series<Number, Number> priceSeries;
    private int  bidCount = 0;
    private final NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private String currentUser = "Bạn";

    // ─── initialize() ────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Khởi tạo đồng hồ thực tế
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss  dd/MM/yyyy");
        Timeline clock = new Timeline(
                new KeyFrame(Duration.ZERO, e -> {
                    if (lblTime4 != null) lblTime4.setText(LocalDateTime.now().format(dtf));
                }),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        // Khởi tạo LineChart
        if (priceChart != null) {
            priceSeries = new XYChart.Series<>();
            priceSeries.setName("Giá đấu");
            priceChart.getData().add(priceSeries);
            priceChart.setAnimated(false);
            priceChart.setCreateSymbols(true);
            priceChart.setLegendVisible(false);
            if (xAxis != null) xAxis.setLabel("Lần đặt");
            if (yAxis != null) yAxis.setLabel("Giá (đ)");
        }
    }

    // ─── setItemData() — gọi từ MainScreenController ─────────────────────────
    public void setItemData(Item item) {
        this.currentItem = item;

        // Hiển thị thông tin sản phẩm
        setText(lblProductname, "            " + item.getName());
        setText(lblProductID,   "    " + item.getId());
        setText(lblSeller,      "    " + (item.getLastBidderId() != null ? item.getLastBidderId() : "N/A"));
        setText(lblRecentPrice, fmt.format(item.getCurrentPrice()) + " đ");
        setText(lblStartPrice,  fmt.format(item.getStartingPrice()) + " đ");
        setText(lblStepPrice,   fmt.format(item.getMinIncrement()) + " đ");

        if (item.getStartTime() != null)
            setText(lblTimeStart, "           " + item.getStartTime().toString());
        if (item.getEndTime() != null)
            setText(lblTimeEnd, "           " + item.getEndTime().toString());

        // Ảnh sản phẩm
        if (imgProduct != null && item.getProductImageURL() != null && !item.getProductImageURL().isEmpty()) {
            try {
                imgProduct.setImage(new Image(item.getProductImageURL(), true));
            } catch (Exception ignored) {}
        }

        // Đồng hồ đếm ngược
        startCountdown(item);

        // Tải lịch sử giá ban đầu từ server (chạy nền)
        loadBidHistory(item.getId());

        // Bắt đầu lắng nghe BID_UPDATE realtime
        NetworkClient.getInstance().startListening(message -> {
            if (!message.startsWith("BID_UPDATE|")) return;
            String[] p = message.split("\\|");
            if (p.length < 4) return;
            String msgItemId = p[1];
            if (!msgItemId.equals(item.getId())) return;

            double newPrice  = Double.parseDouble(p[2]);
            String winner    = p[3];

            Platform.runLater(() -> {
                // Cập nhật label giá
                setText(lblRecentPrice, fmt.format(newPrice) + " đ");
                // Thêm điểm mới vào LineChart
                addChartPoint(newPrice);
            });
        });
    }

    // ─── Nút "Đấu giá!" ──────────────────────────────────────────────────────
    @FXML
    public void onBidButtonClicked(ActionEvent event) {
        if (currentItem == null) return;
        String raw = txtPrice.getText().trim();
        if (raw.isEmpty()) {
            showAlert("Vui lòng nhập mức giá muốn đặt.");
            return;
        }
        try {
            double price = Double.parseDouble(raw.replace(",", "").replace(".", ""));
            // Gửi lên server: BID|userId|price|itemId
            String msg    = "BID|" + currentUser + "|" + price + "|" + currentItem.getId();
            String result = NetworkClient.sendAndReceive(msg);
            showAlert(result != null ? result : "Không nhận được phản hồi từ server.");
            txtPrice.clear();
        } catch (NumberFormatException e) {
            showAlert("Giá không hợp lệ. Vui lòng nhập số.");
        }
    }

    // ─── Nút "Quay lại" ──────────────────────────────────────────────────────
    @FXML
    public void onBackButtonClicked(ActionEvent event) {
        onClose();
        Stage stage = (Stage) btnBack.getScene().getWindow();
        stage.close();
    }

    // ─── Dừng listener khi đóng màn hình ─────────────────────────────────────
    public void onClose() {
        NetworkClient.getInstance().stopListening();
    }

    // ─── Tải lịch sử bid từ server ───────────────────────────────────────────
    private void loadBidHistory(String itemId) {
        new Thread(() -> {
            List<BidTransaction> history = NetworkClient.getBidHistory(itemId);
            Platform.runLater(() -> {
                if (priceSeries == null) return;
                priceSeries.getData().clear();
                bidCount = 0;
                for (BidTransaction tx : history) {
                    addChartPoint(tx.getBidAmount());
                }
            });
        }, "load-history-thread").start();
    }

    // ─── Thêm điểm vào LineChart ──────────────────────────────────────────────
    private void addChartPoint(double price) {
        if (priceSeries == null) return;
        bidCount++;
        priceSeries.getData().add(new XYChart.Data<>(bidCount, price));
        // Giữ tối đa 60 điểm để chart không quá dày
        if (priceSeries.getData().size() > 60) {
            priceSeries.getData().remove(0);
        }
    }

    // ─── Đồng hồ đếm ngược ───────────────────────────────────────────────────
    private void startCountdown(Item item) {
        if (lblClock == null || item.getEndTime() == null) return;
        Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long diff = item.getEndTime().getTime() - System.currentTimeMillis();
            if (diff <= 0) {
                lblClock.setText("ĐÃ KẾT THÚC");
            } else {
                long h = diff / 3_600_000;
                long m = (diff % 3_600_000) / 60_000;
                long s = (diff % 60_000) / 1_000;
                lblClock.setText(String.format("%02d:%02d:%02d", h, m, s));
            }
        }));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    // ─── Tiện ích ─────────────────────────────────────────────────────────────
    private void setText(Label lbl, String text) {
        if (lbl != null) lbl.setText(text);
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public void setCurrentUser(String username) {
        this.currentUser = username;
        setText(lblUsername2, "            " + username);
    }
}
