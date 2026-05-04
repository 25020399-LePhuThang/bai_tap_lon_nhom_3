// Giữ nguyên package của bạn
package com.auction.client;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Gọi file giao diện Welcome của bạn (lưu ý đường dẫn phải chính xác)
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/com/auction/client/view/hello-view.fxml"));

        // Tạo cảnh kịch và đặt kích thước ban đầu (ví dụ: rộng 800, cao 600)
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        // Đặt tiêu đề cho cửa sổ phần mềm
        stage.setTitle("Hệ thống Đấu giá TTVN");
        stage.setScene(scene);
        stage.show(); // Lên đèn, mở rèm!
    }

    public static void main(String[] args) {
        // Dòng lệnh đặc biệt để khởi động ứng dụng JavaFX
        launch();
    }
}