package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.item.Art;
import com.auction.shared.model.item.Electronic;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.Vehicle;
import com.auction.shared.model.user.User;
import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.text.NumberFormat;
import java.util.Locale;

import java.io.IOException;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.text.SimpleDateFormat;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import static com.auction.client.controller.ClockUtil.clockInit;
import static com.auction.client.controller.SceneSwitchUtil.switchScene;

public class AuctionDetailController implements Initializable {

    private Parent previousRoot;
    private MainScreenController mainController;

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
    @FXML private Button btnAvt;

    // ================= CÀI ĐẶT AUTOBID =================
    @FXML private TextField txtMaxAutoBid;
    @FXML private Button btnAutoBid;
    @FXML private HBox boxSetupAuto;
    @FXML private HBox boxCurrentAuto;
    @FXML private Label lblCurrentMaxBid;
    @FXML private Button btnEditAuto;

    // ================= BẢNG & THỐNG KÊ =================
    @FXML private Label lblUserAuctionCount;
    @FXML private Label lblBidCount;

    // ================= BIẾN TOÀN CỤC =================
    private Item currentItem;
    private User currentUser;
    private Timeline countdownTimeline;
    private Timeline clockTimeline;

    // ─── LineChart lịch sử giá realtime ─────────────────────────────────────
    @FXML private LineChart<Number, Number> priceChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;

    private XYChart.Series<Number, Number> priceSeries;
    private int bidCount = 0;

    private volatile boolean historyLoaded = false;
    private final java.util.Queue<Double> pendingPrices = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private final NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private static final java.util.Map<String, String> localAutoBidCache = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile boolean listenerActive = false;
    private volatile boolean isMyManualBidTriggered = false;
    // 🛠️ CỜ HIỆU MỚI: Đồng bộ hóa thông báo khi bị AutoBid đè
    private volatile boolean wasOutbidByAutoBid = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        clockInit(lblTime4);

        if (priceChart != null) {
            priceSeries = new XYChart.Series<>();
            priceSeries.setName("Giá đấu");
            priceChart.getData().add(priceSeries);
            priceChart.setAnimated(false);
            priceChart.setCreateSymbols(true);
            priceChart.setLegendVisible(false);
            if (xAxis != null) xAxis.setLabel("Lần đặt");
            if (yAxis != null) {
                yAxis.setLabel("Giá ($)");
                yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
                    @Override
                    public String toString(Number value) {
                        return String.format("%,.0f", value.doubleValue());
                    }
                });
            }
        }
    }

    public void setItemData(Item item) {
        this.currentItem = item;
        if (currentItem != null) {
            lblProductname.setText(currentItem.getName());
            lblProductID.setText(currentItem.getItemID());
            lblSeller.setText(currentItem.getSeller_ID());
            lblStartPrice.setText(currentItem.getStartingPrice() + "$");
            lblRecentPrice.setText(currentItem.getCurrentPrice() + "$");
            lblStepPrice.setText(currentItem.getMinIncrement() + "$");

            // [Tâm] Đồng bộ định dạng ngày tháng đầy đủ
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

            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                try {
                    Image image = new javafx.scene.image.Image(imageUrl, true);
                    imgProduct.setImage(image);
                } catch (Exception e) {
                    System.err.println("Lỗi không tải được ảnh: " + e.getMessage());
                }
            } else {
                // [Tâm] Cảnh báo hệ thống nếu thiếu ảnh
                System.out.println("Sản phẩm này chưa có link ảnh!");
            }
        }
        startCountdown();

        // [Nhung] Ép mã sản phẩm về dạng String chuẩn để bảo đảm tính nhất quán của khóa tìm kiếm
        String cacheKey = String.valueOf(item.getId()).trim();

        // [Tâm & Nhung] KIỂM TRA BỘ NHỚ ĐỆM TẠM THỜI
        // Luôn dọn dẹp và đặt giao diện về trạng thái ẩn mặc định trước khi đọc trạng thái
        toggleAutoBidView(false);
        txtMaxAutoBid.clear();
        lblCurrentMaxBid.setText("");

        if (localAutoBidCache.containsKey(cacheKey)) {
            String cachedPrice = localAutoBidCache.get(cacheKey);
            lblCurrentMaxBid.setText("Đã đặt giá trần: " + cachedPrice + " $");
            txtMaxAutoBid.setText(cachedPrice);
            toggleAutoBidView(true);
        }

        // Reset toàn bộ trạng thái chart trước khi đăng ký listener mới
        historyLoaded = false;
        bidCount = 0;
        pendingPrices.clear();
        if (priceSeries != null) priceSeries.getData().clear();

        // [Tâm] FIX: Dừng listener cũ trước khi đăng ký listener mới để tránh bị chồng chéo luồng
        NetworkClient.getInstance().detachListener();

        // [Nhung] Đảm bảo cờ quản lý Chart vẫn hoạt động
        listenerActive = true;

        loadBidHistory(item.getId());

        // Bắt đầu lắng nghe Socket Realtime
        NetworkClient.getInstance().startListening(response -> {
            if (!listenerActive || response == null) return;

            // [Tâm] Đón đầu xử lý chuỗi phản hồi trực tiếp (nếu luồng Server trả về trực tiếp trên Listener)
            if (response.startsWith("YES|") || response.equals("NO")) {
                Platform.runLater(() -> {
                    if (response.startsWith("YES")) {
                        String savedMaxBidStr = response.split("\\|")[1];
                        double savedMaxBid = Double.parseDouble(savedMaxBidStr);
                        lblCurrentMaxBid.setText("Đã đặt giá trần: " + savedMaxBid + " $");
                        txtMaxAutoBid.setText(savedMaxBidStr);
                        localAutoBidCache.put(cacheKey, savedMaxBidStr);
                        toggleAutoBidView(true);
                    }
                });
                return;
            }

            // [Tâm] Lắng nghe gia hạn thời gian
            if (response.startsWith("TIME_EXTENDED|")) {
                String[] p = response.split("\\|");
                if (p[1].equals(cacheKey)) {
                    long newEndMillis = Long.parseLong(p[2]);
                    currentItem.setEndTime(new java.util.Date(newEndMillis));
                    Platform.runLater(() -> startCountdown());
                }
                return;
            }

            // [Tâm] Lắng nghe cập nhật thời gian
            if (response.startsWith("TIME_UPDATE|")) {
                String[] p = response.split("\\|");
                long newEndTime = Long.parseLong(p[2]);
                currentItem.setEndTime(new java.util.Date(newEndTime));
                Platform.runLater(() -> startCountdown());
                return;
            }

            // [Nhung + Tâm] Lắng nghe cập nhật giá thầu
            if (response.startsWith("BID_UPDATE|")) {
                String[] p = response.split("\\|");
                if (p.length < 4) return;
                String msgItemId = p[1];
                if (!msgItemId.equals(cacheKey)) return;

                double newPrice = Double.parseDouble(p[2]);
                String winner = p[3].trim();

                // [Nhung] Lưu vết thông tin người dẫn đầu cũ để đối chiếu Alert
                String previousWinner = currentItem.getLastBidderId() != null ? currentItem.getLastBidderId().trim() : "";

                currentItem.setCurrentPrice(newPrice);
                currentItem.setLastBidderId(winner);

                Platform.runLater(() -> {
                    setText(lblRecentPrice, fmt.format(newPrice) + " $");

                    String myUsername = lblUsername2.getText().trim();

                    // [Nhung] Bóc tách an toàn các tham số nhận từ Server
                    String mode = (p.length >= 5) ? p[4].trim() : "";
                    String manualBidder = (p.length >= 6) ? p[5].trim() : "";
                    String oldWinner = (p.length >= 7) ? p[6].trim() : "";

                    // [Nhung] ─── ĐẦU NÃO HIỂN THỊ TẬP TRUNG TỪ SOCKET ───
                    if ("AUTOBID_TRIGGERED".equalsIgnoreCase(mode)) {
                        if (!winner.equalsIgnoreCase(myUsername)) {
                            lblRecentPrice.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

                            if (isMyManualBidTriggered || wasOutbidByAutoBid || myUsername.equalsIgnoreCase(oldWinner) || myUsername.equalsIgnoreCase(manualBidder)) {
                                showAlert("Hệ thống AutoBid của tài khoản [" + winner + "] đã tự động đặt mức giá " + newPrice + " $ và vượt qua bạn!");
                            }
                            isMyManualBidTriggered = false;
                            wasOutbidByAutoBid = false;
                        } else {
                            lblRecentPrice.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                            if (isMyManualBidTriggered || myUsername.equalsIgnoreCase(manualBidder)) {
                                showAlert("Hệ thống AutoBid của bạn đã tự động nâng giá giữ vững vị trí dẫn đầu lên " + newPrice + " $!");
                            }
                            isMyManualBidTriggered = false;
                            wasOutbidByAutoBid = false;
                        }
                    } else {
                        if (winner.equalsIgnoreCase(myUsername)) {
                            lblRecentPrice.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                            if (isMyManualBidTriggered) {
                                showAlert("Đặt giá thành công! Bạn đang dẫn đầu phiên với mức giá " + newPrice + " $.");
                                isMyManualBidTriggered = false;
                                wasOutbidByAutoBid = false;
                            }
                        } else {
                            lblRecentPrice.setStyle("-fx-text-fill: #2c3e50;");
                            if (isMyManualBidTriggered || myUsername.equalsIgnoreCase(previousWinner)) {
                                showAlert("Tài khoản [" + winner + "] vừa tự tay đặt mức giá mới " + newPrice + " $ và vượt qua bạn!");
                                isMyManualBidTriggered = false;
                                wasOutbidByAutoBid = false;
                            }
                        }
                    }

                    synchronized (AuctionDetailController.this) {
                        if (historyLoaded) {
                            addChartPoint(newPrice);
                        } else {
                            pendingPrices.offer(newPrice);
                        }
                    }
                });
            }
        });

        // 🔥 LUỒNG CHECK AUTOBID TRUYỀN THUYẾT (Phục hồi ngầm)
        new Thread(() -> {
            try {
                Thread.sleep(80); // [Tâm] Giảm chờ để đồng bộ siêu tốc
                String username = lblUsername2.getText();
                if (username == null || username.isEmpty() || username.equals("Khách")) return;

                String msg = "CHECK_MY_AUTOBID|" + username + "|" + cacheKey;
                System.out.println("[UI CHECK] Hỏi trạng thái Auto-bid ngầm: " + msg);

                String response = NetworkClient.sendAndReceive(msg);
                System.out.println("[UI CHECK] Server phản hồi trạng thái: " + response);

                if (response != null && response.startsWith("YES")) {
                    String savedMaxBidStr = response.split("\\|")[1];

                    // [Nhung] Format lại double cho an toàn và đẹp
                    double maxBidVal = Double.parseDouble(savedMaxBidStr);
                    String formattedMaxBid = String.format("%.1f", maxBidVal);

                    Platform.runLater(() -> {
                        lblCurrentMaxBid.setText("Đã đặt giá trần: " + formattedMaxBid + " $");
                        txtMaxAutoBid.setText(formattedMaxBid);
                        localAutoBidCache.put(cacheKey, formattedMaxBid); // [Tâm] Lưu vết an toàn
                        toggleAutoBidView(true);
                    });
                } else if (response != null && response.equals("NO")) {
                    localAutoBidCache.remove(cacheKey); // [Tâm] Xóa đệm cũ nếu server báo hết hạn
                    Platform.runLater(() -> {
                        txtMaxAutoBid.clear();
                        lblCurrentMaxBid.setText("");
                        toggleAutoBidView(false);
                    });
                }
            } catch (Exception e) {
                System.err.println("Lỗi khôi phục trạng thái AutoBid: " + e.getMessage());
                Platform.runLater(() -> toggleAutoBidView(false));
            }
        }, "AutoBid-Fast-Restore-Thread").start();
    }

    public void setDisplayName(String displayName) {
        if (displayName != null && !displayName.isEmpty()) {
            lblUsername2.setText(displayName);
        } else {
            lblUsername2.setText("Khách");
        }
    }

    public void setPreviousScreen(Parent root, MainScreenController controller) {
        this.previousRoot = root;
        this.mainController = controller;
    }

    @FXML
    public void toMainScreen(ActionEvent event) {
        // Nếu có lưu vết màn hình cũ thì quay lại trực tiếp không load lại FXML
        if (previousRoot != null) {
            onClose(); // Đóng bộ lắng nghe phòng chi tiết trước khi đi

            if (mainController != null) {
                // [Nhung] Load lại danh sách sản phẩm để cập nhật trạng thái mới nhất khi quay về
                mainController.loadActiveAndPreparedItems();

                // [Tâm] Kích hoạt lại luồng trang chủ
                mainController.resumeSocketListener();
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(previousRoot);
            stage.setTitle("Trang chủ Đấu giá");
        } else {
            // Dự phòng: Nếu vào thẳng phòng mà không qua trang chủ thì mới load mới
            try {
                onClose();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainScreen.fxml"));
                Parent root = loader.load();
                MainScreenController controller = loader.getController();
                controller.setDisplayName(lblUsername2.getText());

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.getScene().setRoot(root);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @FXML
    public void handleLogout(ActionEvent event) throws IOException {
        onClose();
        NetworkClient.disconnect(lblUsername2.getText());
        switchScene(event,"/SignInScreen.fxml");
    }

    private void startCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        if (currentItem == null || currentItem.getEndTime() == null) {
            lblClock.setText("00:00:00");
            return;
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            Date now = new Date();
            Date endTime = currentItem.getEndTime();
            long diffInMillis = endTime.getTime() - now.getTime();

            if (diffInMillis <= 0) {
                lblClock.setText("ĐÃ KẾT THÚC");
                lblClock.setStyle("-fx-text-fill: #e74c3c;");
                btnAuction.setDisable(true);

                if (countdownTimeline != null) {
                    countdownTimeline.stop();
                }
            } else {
                long diffInSeconds = diffInMillis / 1000;
                long hours = diffInSeconds / 3600;
                long minutes = (diffInSeconds % 3600) / 60;
                long seconds = diffInSeconds % 60;

                lblClock.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            }
        }));

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
            // Giữ lại cách căn lề và dấu cách chuẩn của Nhung
            extraDetails += "Thương hiệu: " + vehicle.getBrand() + "\n"
                    + "Bảo hành: " + vehicle.getWarrantyPeriod() + " tháng\n"
                    + "Nhiên liệu: " + vehicle.getFuelType() + "\n"
                    + "Dung tích động cơ: " + vehicle.getEngineCapacity() + "\n";
        } else {
            extraDetails = "";
        }

        alert.setContentText(details + extraDetails);

        try {
            String imageUrl = currentItem.getProductImageURL();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // Giữ lại cách gọi an toàn của Tâm để tránh xung đột thư viện Image
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

    @FXML
    public void onBidButtonClicked() {
        if (currentItem == null) return;

        String raw = txtPrice.getText().trim();
        if (raw.isEmpty()) {
            showAlert("Vui lòng nhập mức giá muốn đặt.");
            return;
        }

        try {
            double price = Double.parseDouble(raw.replace(",", "").replace(".", ""));

            double currentPrice = currentItem.getCurrentPrice();
            double minIncrement = currentItem.getMinIncrement();
            double minRequired = currentPrice + minIncrement;

            if (price <= currentPrice || price < minRequired) {
                showAlert("Giá đấu tối thiểu phải là " + minRequired + " $ (giá hiện tại + bước giá).");
                return;
            }

            String username = lblUsername2.getText();
            String msg = "BID|" + username + "|" + price + "|" + currentItem.getId();

            btnAuction.setDisable(true);
            isMyManualBidTriggered = true;

            new Thread(() -> {
                String result = NetworkClient.sendAndReceive(msg);

                Platform.runLater(() -> {
                    btnAuction.setDisable(false); // Mở khóa nút

                    if (result != null) {
                        // FIX: Kiểm tra kết quả thành công trước, còn lại quăng vào Alert hết
                        if (result.startsWith("THÀNH CÔNG")) {
                            txtPrice.clear();
                        } else if (result.startsWith("SERVER_PROCESSED_AUTOBID")) {
                            wasOutbidByAutoBid = true;
                            txtPrice.clear();
                        } else {
                            // Nếu Server trả về "Phiên đã kết thúc" hay "Số dư không đủ", nó sẽ bay vào đây và hiện lên
                            showAlert(result);
                            isMyManualBidTriggered = false;
                        }
                    } else {
                        isMyManualBidTriggered = false;
                    }
                });
            }).start();

        } catch (NumberFormatException e) {
            showAlert("Giá không hợp lệ. Vui lòng nhập số.");
            isMyManualBidTriggered = false;
        }
    }

    private void loadBidHistory(String itemId) {
        new Thread(() -> {
            List<BidTransaction> history = NetworkClient.getBidHistory(itemId.trim());
            Platform.runLater(() -> {
                if (priceSeries == null) return;
                priceSeries.getData().clear();
                bidCount = 0;
                for (BidTransaction tx : history) {
                    addChartPoint(tx.getBidAmount());
                }
                synchronized (AuctionDetailController.this) {
                    while (!pendingPrices.isEmpty()) {
                        addChartPoint(pendingPrices.poll());
                    }
                    historyLoaded = true;
                }
            });
        }, "load-history-thread").start();
    }


    private void addChartPoint(double price) {
        if (priceSeries == null) return;
        bidCount++;
        priceSeries.getData().add(new XYChart.Data<>(bidCount, price));
        if (priceSeries.getData().size() > 60) {
            priceSeries.getData().remove(0);
            int size = priceSeries.getData().size();
            for (int i = 0; i < size; i++) {
                priceSeries.getData().get(i).setXValue(bidCount - size + 1 + i);
            }
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }

    private void setText(Label lbl, String text) {
        if (lbl != null) lbl.setText(text);
    }

    @FXML
    public void onAutoBidButtonClicked() {
        if (currentItem == null) return;

        String raw = txtMaxAutoBid.getText().trim();
        if (raw.isEmpty()) {
            // Thông báo chi tiết từ bản 1
            showAlert("Vui lòng nhập mức giá trần muốn cài đặt tự động.");
            return;
        }

        try {
            double maxAutoPrice = Double.parseDouble(raw.replace(",", "").replace(".", ""));
            double currentPrice = currentItem.getCurrentPrice();

            if (maxAutoPrice <= currentPrice) {
                showAlert("Giá trần tự động phải lớn hơn giá hiện tại (" + currentPrice + " $).");
                return;
            }

            String username = lblUsername2.getText();

            // CHÚ Ý: Đang sử dụng định dạng gửi đi của Tâm (có minIncrement).
            // Nếu Server của bạn chỉ nhận "REGISTER_AUTOBID|user|giá|id", hãy sửa lại dòng này.
            String msg = "AUTO_BID|" + username + "|" + maxAutoPrice + "|" + currentItem.getMinIncrement() + "|" + currentItem.getId();

            // [Tâm] Khóa nút để tránh người dùng nhấn nhiều lần trong lúc chờ Server
            btnAutoBid.setDisable(true);

            // [Tâm] Chạy luồng phụ để không làm đơ giao diện JavaFX
            new Thread(() -> {
                String result = NetworkClient.sendAndReceive(msg);

                // Đưa kết quả về cập nhật trên luồng UI chính
                Platform.runLater(() -> {
                    btnAutoBid.setDisable(false); // Mở khóa nút

                    // Gom cả 2 điều kiện kiểm tra thành công của bạn và Tâm
                    if (result != null && (result.startsWith("AutoBid") || result.startsWith("THÀNH CÔNG"))) {
                        // [Bản 1] Thông báo thành công kèm theo mức giá trần
                        showAlert("Đã kích hoạt Auto-bid thành công! Giá trần: " + maxAutoPrice + " $");

                        // [Tâm] Format giá trị hiển thị cho đẹp
                        String formattedPrice = String.format("%.1f", maxAutoPrice);

                        lblCurrentMaxBid.setText("Đã đặt giá trần: " + formattedPrice + " $");

                        // [Bản 1] GIỮ VẾT DỮ LIỆU: Đổ ngược giá trị mới vừa cài thành công vào ô text
                        txtMaxAutoBid.setText(formattedPrice);

                        // [Bản 1 & Tâm] Ghi đè cập nhật vào bộ đệm tĩnh, dùng .trim() cho an toàn key
                        localAutoBidCache.put(String.valueOf(currentItem.getId()).trim(), formattedPrice);

                        // Chuyển giao diện sang trạng thái 2
                        toggleAutoBidView(true);
                    } else if (result != null) {
                        showAlert(result); // Hiển thị lỗi từ server trả về nếu có
                    }
                });
            }).start();

        } catch (NumberFormatException e) {
            showAlert("Mức giá trần không hợp lệ. Vui lòng nhập số.");
        }
    }

    @FXML
    public void onEditAutoBidClicked() {
        toggleAutoBidView(false);
        if (txtMaxAutoBid.getText().trim().isEmpty() && lblCurrentMaxBid.getText().contains(":")) {
            try {
                String parts = lblCurrentMaxBid.getText().split(":")[1].replace("$", "").trim();
                txtMaxAutoBid.setText(parts);
            } catch (Exception e) { }
        }
        txtMaxAutoBid.requestFocus();
    }

    private void toggleAutoBidView(Boolean isConfigured) {
        if (isConfigured == null) {
            boxSetupAuto.setVisible(false);
            boxSetupAuto.setManaged(false);
            boxCurrentAuto.setVisible(false);
            boxCurrentAuto.setManaged(false);
        } else if (isConfigured) {
            boxSetupAuto.setVisible(false);
            boxSetupAuto.setManaged(false);
            boxCurrentAuto.setVisible(true);
            boxCurrentAuto.setManaged(true);
        } else {
            boxSetupAuto.setVisible(true);
            boxSetupAuto.setManaged(true);
            boxCurrentAuto.setVisible(false);
            boxCurrentAuto.setManaged(false);
        }
    }

    public void onClose() {
        NetworkClient.getInstance().detachListener();
    }

    @FXML
    public void toInfoScreen()  {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/InfoScreen.fxml"));
            Parent root = loader.load();

            InfoController infoController = loader.getController();
            infoController.initData(lblUsername2.getText(), mainController);

            Stage popUpStage = new Scene(root).getWindow() != null ? (Stage) new Scene(root).getWindow() : new Stage();
            popUpStage.setScene(new Scene(root));
            popUpStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popUpStage.setResizable(false);
            popUpStage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }
}