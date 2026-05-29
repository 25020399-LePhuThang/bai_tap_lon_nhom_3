package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import com.auction.server.dao.ItemDAO;
import com.auction.shared.model.Auction;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.item.Art;
import com.auction.shared.model.item.Electronic;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.Vehicle;
import com.auction.shared.model.user.User;
import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
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
    @FXML private HBox boxSetupAuto;       // Khung nhập giá trần ban đầu
    @FXML private HBox boxCurrentAuto;     // Khung trạng thái sau khi cài thành công
    @FXML private Label lblCurrentMaxBid;  // Nhãn chữ hiển thị mức tiền trần đang cài
    @FXML private Button btnEditAuto;      // Nút "Thay đổi giá trần"

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

    // ─── LineChart lịch sử giá realtime ─────────────────────────────────────
    @FXML private LineChart<Number, Number> priceChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;


    private XYChart.Series<Number, Number> priceSeries;
    private int  bidCount = 0;
    // FIX: dùng volatile để đảm bảo thread nền thấy thay đổi ngay lập tức
    private volatile boolean historyLoaded = false;
    // FIX: hàng đợi chứa giá đến trong lúc lịch sử chưa load xong
    private final java.util.Queue<Double> pendingPrices = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private final NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // Mẹo nhỏ: Biến tĩnh hoặc lưu trạng thái tạm thời cho ô nhập để tránh bị reload FXML xóa trắng dữ liệu
    private static final java.util.Map<String, String> localAutoBidCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        clockInit();

        if (priceChart != null) {
            priceSeries = new XYChart.Series<>();
            priceSeries.setName("Giá đấu");
            priceChart.getData().add(priceSeries);
            priceChart.setAnimated(false);
            priceChart.setCreateSymbols(true);
            priceChart.setLegendVisible(false);
            if (xAxis != null) xAxis.setLabel("Lần đặt");
            if (yAxis != null) {
                yAxis.setLabel("Giá (đ)");
                yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
                    @Override
                    public String toString(Number value) {
                        return String.format("%,.0f", value.doubleValue());
                    }
                });
            }
        }
    }

    public void clockInit() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss  dd/MM/yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e ->
                lblTime4.setText(LocalDateTime.now().format(formatter))
        ), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    // Hàm nạp dữ liệu Sản phẩm và khôi phục trạng thái phòng đấu giá
    public void setItemData(Item item) {
        this.currentItem = item;
        if (currentItem != null) {
            lblProductname.setText(currentItem.getName());
            lblProductID.setText(currentItem.getItemID());
            lblSeller.setText(currentItem.getSeller_ID());
            lblStartPrice.setText(currentItem.getStartingPrice() + "$");
            lblRecentPrice.setText(currentItem.getCurrentPrice() + "$");
            lblStepPrice.setText(currentItem.getMinIncrement() + "$");
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
                System.out.println("Sản phẩm này chưa có link ảnh!");
            }
        }
        startCountdown();

        // 1. ✅ KIỂM TRA BỘ NHỚ ĐỆM TẠM THỜI TRƯỚC KHI LIÊN LẠC MẠNG
        String cacheKey = item.getId();
        if (localAutoBidCache.containsKey(cacheKey)) {
            String cachedPrice = localAutoBidCache.get(cacheKey);
            lblCurrentMaxBid.setText("Đã đặt giá trần: " + cachedPrice + " $");
            txtMaxAutoBid.setText(cachedPrice);
            toggleAutoBidView(true);
        } else {
            // Mặc định hiện ngay ô nhập liệu ban đầu nếu chưa có bộ nhớ đệm
            toggleAutoBidView(false);
        }

        // FIX: Reset trạng thái biểu đồ và hàng đợi pending trước khi tải dữ liệu mới
        historyLoaded = false;
        bidCount = 0;
        pendingPrices.clear();
        if (priceSeries != null) priceSeries.getData().clear();

        // FIX: Dừng listener cũ (nếu có) trước khi đăng ký listener mới, tránh bị chồng nhiều listener cùng lúc
        NetworkClient.getInstance().stopListening();

        // Tải lịch sử. Khi xong, hàng đợi pending sẽ được áp dụng (xem loadBidHistory).
        loadBidHistory(item.getId());

        // Bắt đầu lắng nghe BID_UPDATE realtime
        NetworkClient.getInstance().startListening(respone -> {
            if (respone == null) return;

            // Đón đầu xử lý chuỗi phản hồi trực tiếp từ lệnh gửi nhận của nút bấm hoặc luồng đồng bộ ngầm
            if (respone.startsWith("YES|") || respone.equals("NO")) {
                Platform.runLater(() -> {
                    if (respone.startsWith("YES")) {
                        String savedMaxBidStr = respone.split("\\|")[1];
                        double savedMaxBid = Double.parseDouble(savedMaxBidStr);

                        lblCurrentMaxBid.setText("Đã đặt giá trần: " + savedMaxBid + " $");
                        txtMaxAutoBid.setText(savedMaxBidStr);
                        localAutoBidCache.put(item.getId(), savedMaxBidStr); // Cập nhật lại bộ nhớ đệm tạm thời

                        toggleAutoBidView(true);
                    }
                });
                return;
            }

            if (!respone.startsWith("BID_UPDATE|")) return;
            String[] p = respone.split("\\|");
            if (p.length < 4) return;
            String msgItemId = p[1];
            if (!msgItemId.equals(item.getId())) return;

            double newPrice  = Double.parseDouble(p[2]);
            String winner    = p[3];

            currentItem.setCurrentPrice(newPrice);
            currentItem.setLastBidderId(winner);

            Platform.runLater(() -> {
                setText(lblRecentPrice, fmt.format(newPrice) + " $");
                if (historyLoaded) {
                    addChartPoint(newPrice);
                } else {
                    pendingPrices.offer(newPrice);
                }
            });
        });

        // 🔥 LUỒNG CHECK AUTOBID TRUYỀN THUYẾT (Dùng để đồng bộ lại với Database/Server khi cache hết hạn)
        new Thread(() -> {
            try {
                Thread.sleep(80); // Giảm thời gian chờ xuống tối đa để đồng bộ siêu tốc
                String username = lblUsername2.getText();
                if (username == null || username.isEmpty() || username.equals("Khách")) return;

                String itemId = item.getId();
                String msg = "CHECK_MY_AUTOBID|" + username + "|" + itemId;
                System.out.println("[UI CHECK] Hỏi trạng thái Auto-bid ngầm: " + msg);

                String response = NetworkClient.sendAndReceive(msg);
                System.out.println("[UI CHECK] Server phản hồi trạng thái: " + response);

                if (response != null && response.startsWith("YES")) {
                    String savedMaxBidStr = response.split("\\|")[1];
                    Platform.runLater(() -> {
                        lblCurrentMaxBid.setText("Đã đặt giá trần: " + savedMaxBidStr + " $");
                        txtMaxAutoBid.setText(savedMaxBidStr);
                        localAutoBidCache.put(itemId, savedMaxBidStr); // Lưu vết an toàn
                        toggleAutoBidView(true);
                    });
                } else if (response != null && response.equals("NO")) {
                    localAutoBidCache.remove(itemId); // Nếu phía server đã hết hạn, xóa bộ đệm cũ
                    Platform.runLater(() -> toggleAutoBidView(false));
                }
            } catch (Exception e) {
                System.err.println("Lỗi khôi phục trạng thái AutoBid: " + e.getMessage());
            }
        }, "AutoBid-Fast-Restore-Thread").start();
    }


    // Hàm nạp riêng Tên hiển thị (Tách biệt hoàn toàn)
    public void setDisplayName(String displayName) {
        if (displayName != null && !displayName.isEmpty()) {
            lblUsername2.setText(displayName);
        } else {
            lblUsername2.setText("Khách");
        }
    }

    // FIX: Hàm tiếp nhận "vết" màn hình chính từ Main chuyển sang
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
                mainController.resumeSocketListener(); // Kích hoạt lại luồng trang chủ
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

    private void switchScence(ActionEvent event, String fxmlFile) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    @FXML
    public void handleLogout(ActionEvent event) throws IOException {
        onClose();
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

            // ✅ THÊM ĐOẠN NÀY - validate giá
            double currentPrice = currentItem.getCurrentPrice();
            double minIncrement = currentItem.getMinIncrement();
            double minRequired = currentPrice + minIncrement;

            if (price <= currentPrice) {
                showAlert("Giá đấu tối thiểu phải là "
                        + minRequired + " $ (giá hiện tại + bước giá).");
                return;
            }
            if (price < minRequired) {
                showAlert("Giá đấu tối thiểu phải là "
                        + minRequired + " $ (giá hiện tại + bước giá).");
                return;
            }


            // Gửi lên server
            String username = lblUsername2.getText();
            String msg = "BID|" + username + "|" + price + "|" + currentItem.getId();
            String result = NetworkClient.sendAndReceive(msg);

            if (result != null && (result.startsWith("THÀNH CÔNG") || result.startsWith("BID_UPDATE"))) {
                currentItem.setCurrentPrice(price);
                Platform.runLater(() -> {
                    lblRecentPrice.setText(price + "$");
                    addChartPoint(price);
                });
            }

            showAlert("Đấu giá thành công với giá " + price + " $!");
            txtPrice.clear();
        } catch (NumberFormatException e) {
            showAlert("Giá không hợp lệ. Vui lòng nhập số.");
        }
    }

    // ─── Tải lịch sử bid từ server ───────────────────────────────────────────
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
                // FIX: Áp dụng các giá đến trong lúc đang tải lịch sử (tránh mất điểm)
                while (!pendingPrices.isEmpty()) {
                    addChartPoint(pendingPrices.poll());
                }
                // Đánh dấu đã sẵn sàng SAU KHI đã vẽ hết pending
                historyLoaded = true;
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
            // Re-index các điểm còn lại để trục X luôn liên tục
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
        alert.showAndWait();
    }


    // ─── Tiện ích ─────────────────────────────────────────────────────────────
    private void setText(Label lbl, String text) {
        if (lbl != null) lbl.setText(text);
    }

    @FXML
    public void onAutoBidButtonClicked() {
        if (currentItem == null) return;
        String raw = txtMaxAutoBid.getText().trim();
        if (raw.isEmpty()) {
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
            // Định dạng chuỗi truyền đi: REGISTER_AUTOBID|tên_user|giá_trần|mã_sp
            String msg = "REGISTER_AUTOBID|" + username + "|" + maxAutoPrice + "|" + currentItem.getId();
            String result = NetworkClient.sendAndReceive(msg);

            if (result != null && result.startsWith("THÀNH CÔNG")) {
                showAlert("Đã kích hoạt Auto-bid thành công! Giá trần: " + maxAutoPrice + " $");
                Platform.runLater(() -> {
                    lblCurrentMaxBid.setText("Đã đặt giá trần: " + maxAutoPrice + " $");

                    // ✅ GIỮ VẾT DỮ LIỆU: Đổ ngược giá trị mới vừa cài thành công vào ô text
                    txtMaxAutoBid.setText(String.valueOf(maxAutoPrice));

                    // Ghi đè cập nhật vào bộ đệm tĩnh cục bộ để tránh bị mất dữ liệu khi đổi phòng quay trở lại
                    localAutoBidCache.put(currentItem.getId(), String.valueOf(maxAutoPrice));

                    toggleAutoBidView(true); // Chuyển giao diện sang trạng thái 2
                });
            } else if (result != null) {
                showAlert(result); // Hiển thị lỗi từ server trả về nếu có
            }
        } catch (NumberFormatException e) {
            showAlert("Mức giá trần không hợp lệ. Vui lòng nhập số.");
        }
    }

    // Hàm xử lý khi người dùng bấm nút "Thay đổi giá trần" để nhập lại mức mới
    @FXML
    public void onEditAutoBidClicked() {
        toggleAutoBidView(false); // Quay về giao diện nhập số

        // ✅ THÊM: Nếu trước đó ô nhập bị xóa nhầm, trích xuất lại số từ nhãn hiển thị đổ vào ô nhập
        if (txtMaxAutoBid.getText().trim().isEmpty() && lblCurrentMaxBid.getText().contains(":")) {
            try {
                String parts = lblCurrentMaxBid.getText().split(":")[1].replace("$", "").trim();
                txtMaxAutoBid.setText(parts);
            } catch (Exception e) {
                // Phòng hờ chuỗi định dạng nhãn thay đổi
            }
        }

        txtMaxAutoBid.requestFocus(); // Tự động nhấp nháy con trỏ chuột vào ô nhập liệu
    }

    // Hàm tiện ích điều khiển ẩn/hiện mượt mà (được tối ưu để hỗ trợ trạng thái đang tải)
    private void toggleAutoBidView(Boolean isConfigured) {
        if (isConfigured == null) {
            // Trạng thái đang tải (Loading): Ẩn sạch cả 2 khung để không bị giật UI
            boxSetupAuto.setVisible(false);
            boxSetupAuto.setManaged(false);
            boxCurrentAuto.setVisible(false);
            boxCurrentAuto.setManaged(false);
        } else if (isConfigured) {
            // Trạng thái 2: Đã cài AutoBid thành công
            boxSetupAuto.setVisible(false);
            boxSetupAuto.setManaged(false);
            boxCurrentAuto.setVisible(true);
            boxCurrentAuto.setManaged(true);
        } else {
            // Trạng thái 1: Chưa cài AutoBid, hiện ô nhập liệu
            boxSetupAuto.setVisible(true);
            boxSetupAuto.setManaged(true);
            boxCurrentAuto.setVisible(false);
            boxCurrentAuto.setManaged(false);
        }
    }


    // ─── Dừng listener khi đóng màn hình ─────────────────────────────────────
    public void onClose() {
        NetworkClient.getInstance().stopListening();
    }

    @FXML
    public void toInfoScreen()  {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/InfoScreen.fxml"));
            Parent root = loader.load();

            InfoController infoController = loader.getController();
            infoController.initData(lblUsername2.getText(), this);


            Stage popUpStage = new Stage();
            popUpStage.setScene(new Scene(root));
            popUpStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popUpStage.setResizable(false);
            popUpStage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }
}