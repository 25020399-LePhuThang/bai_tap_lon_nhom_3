//thuật toán quản lí sản phẩm đấu giá
package com.auction.server.controller;

import com.auction.shared.model.item.Electronic;
import com.auction.shared.model.item.Item;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class ProductManager {

    // 1. Tạo một cái kho chứa đồ giả (Lưu tạm trên RAM)
    private static final List<Item> mockDatabase = new ArrayList<>();

    // 2. Nhét sẵn vài món đồ vào kho để thầy có cái mà xem
    static {
        // (Zưin lưu ý: Sửa lại tên hàm set... cho khớp với class Item của nhóm nha)
        Item item1 = new Electronic();
        item1.setId("IPHONE_15");
        item1.setName("iPhone 15 Pro Max 256GB");
        item1.setCurrentPrice(20000000);
        item1.setMinIncrement(500000);
        item1.setStartTime(new Date(System.currentTimeMillis() - 10000)); // Đã bắt đầu
        item1.setEndTime(new Date(System.currentTimeMillis() + 86400000)); // Mai mới hết hạn

        Item item2 = new Electronic();
        item2.setId("MACBOOK_M3");
        item2.setName("Macbook Pro M3 14 inch");
        item2.setCurrentPrice(35000000);
        item2.setMinIncrement(1000000);
        item2.setStartTime(new Date(System.currentTimeMillis() - 10000));
        item2.setEndTime(new Date(System.currentTimeMillis() + 86400000));

        mockDatabase.add(item1);
        mockDatabase.add(item2);
    }

    // 3. Hàm cho Client lấy danh sách sản phẩm để hiển thị lên màn hình
    public static String getAllProducts() {
        if (mockDatabase.isEmpty()) {
            return "PRODUCT_LIST_EMPTY|Chưa có sản phẩm nào.";
        }

        // Gom tất cả sản phẩm lại thành 1 chuỗi dài gửi cho Client
        StringBuilder sb = new StringBuilder("PRODUCT_LIST|");
        for (Item item : mockDatabase) {
            // Định dạng: ID-Tên-Giá
            sb.append(item.getId()).append("-").append(item.getName()).append("-").append(item.getCurrentPrice()).append(";");
        }
        return sb.toString();
    }

    // 4. Hàm cho BiddingService tìm món đồ để đặt giá
    public static Item getItemById(String targetId) {
        for (Item item : mockDatabase) {
            if (item.getId().equals(targetId)) {
                return item;
            }
        }
        return null; // Không tìm thấy
    }
}