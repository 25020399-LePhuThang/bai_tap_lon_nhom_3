package com.auction;

import com.auction.shared.model.item.Art;
import com.auction.shared.model.item.Electronic;
import com.auction.shared.model.item.Item;
import com.auction.shared.model.item.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho Item và các lớp con (Electronic, Art, Vehicle).
 * Kiểm tra: constructor, isActive(), getter/setter, tính đa hình.
 */
@DisplayName("Item - Kiểm tra model sản phẩm đấu giá")
class ItemTest {

    // -----------------------------------------------------------------------
    // isActive()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("isActive() = true khi status=ACTIVE và trong khoảng thời gian")
    void testIsActive_ValidActiveItem_ShouldReturnTrue() {
        Electronic item = new Electronic("iPhone 15", "Apple", 12, 20_000_000, 500_000);
        item.setStatus("ACTIVE");
        item.setStartTime(new Date(System.currentTimeMillis() - 10_000)); // 10s trước
        item.setEndTime(new Date(System.currentTimeMillis() + 60_000));   // 1 phút sau

        assertTrue(item.isActive());
    }

    @Test
    @DisplayName("isActive() = false khi status=COMPLETED")
    void testIsActive_CompletedStatus_ShouldReturnFalse() {
        Electronic item = new Electronic("iPhone 15", "Apple", 12, 20_000_000, 500_000);
        item.setStatus("COMPLETED");
        item.setStartTime(new Date(System.currentTimeMillis() - 10_000));
        item.setEndTime(new Date(System.currentTimeMillis() + 60_000));

        assertFalse(item.isActive());
    }

    @Test
    @DisplayName("isActive() = false khi đã hết hạn")
    void testIsActive_ExpiredEndTime_ShouldReturnFalse() {
        Electronic item = new Electronic("iPhone 15", "Apple", 12, 20_000_000, 500_000);
        item.setStatus("ACTIVE");
        item.setStartTime(new Date(System.currentTimeMillis() - 120_000));
        item.setEndTime(new Date(System.currentTimeMillis() - 10_000)); // đã kết thúc

        assertFalse(item.isActive());
    }

    @Test
    @DisplayName("isActive() = false khi chưa đến giờ bắt đầu")
    void testIsActive_BeforeStartTime_ShouldReturnFalse() {
        Electronic item = new Electronic("iPhone 15", "Apple", 12, 20_000_000, 500_000);
        item.setStatus("ACTIVE");
        item.setStartTime(new Date(System.currentTimeMillis() + 60_000)); // 1 phút nữa
        item.setEndTime(new Date(System.currentTimeMillis() + 120_000));

        assertFalse(item.isActive());
    }

    // -----------------------------------------------------------------------
    // Constructor — giá khởi đầu
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Constructor: currentPrice ban đầu = startingPrice")
    void testConstructor_CurrentPriceEqualsStartingPrice() {
        Electronic item = new Electronic("MacBook", "Apple", 24, 35_000_000, 1_000_000);
        assertEquals(item.getStartingPrice(), item.getCurrentPrice(), 0.001,
                "currentPrice ban đầu phải bằng startingPrice");
    }

    @Test
    @DisplayName("Constructor: status mặc định là ACTIVE")
    void testConstructor_DefaultStatusIsActive() {
        Electronic item = new Electronic("MacBook", "Apple", 24, 35_000_000, 1_000_000);
        assertEquals("ACTIVE", item.getStatus());
    }

    // -----------------------------------------------------------------------
    // Getter / Setter
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("setCurrentPrice() và getCurrentPrice() hoạt động đúng")
    void testSetGetCurrentPrice() {
        Electronic item = new Electronic("TV", "Samsung", 12, 10_000_000, 200_000);
        item.setCurrentPrice(15_000_000);
        assertEquals(15_000_000, item.getCurrentPrice(), 0.001);
    }

    @Test
    @DisplayName("setLastBidderId() lưu đúng người đặt giá sau cùng")
    void testSetLastBidderId() {
        Electronic item = new Electronic("TV", "Samsung", 12, 10_000_000, 200_000);
        item.setLastBidderId("user_42");
        assertEquals("user_42", item.getLastBidderId());
    }

    @Test
    @DisplayName("setId() và getId() hoạt động nhất quán")
    void testSetAndGetId() {
        Electronic item = new Electronic();
        item.setId("ITEM_999");
        assertEquals("ITEM_999", item.getId());
        assertEquals("ITEM_999", item.getItemID());
    }

    // -----------------------------------------------------------------------
    // Đa hình — printInfo() (Polymorphism)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Electronic.printInfo() không ném exception")
    void testElectronic_PrintInfo_NoException() {
        Electronic item = new Electronic("iPhone", "Apple", 12, 20_000_000, 500_000);
        assertDoesNotThrow(item::printInfo);
    }

    @Test
    @DisplayName("Mỗi item mới từ constructor đều có itemID không null")
    void testConstructor_ItemID_IsNotNull() {
        Electronic e = new Electronic("Phone", "Samsung", 6, 5_000_000, 100_000);
        assertNotNull(e.getItemID(), "itemID không được null");
    }

    // -----------------------------------------------------------------------
    // Kế thừa — các lớp con kế thừa Item
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Electronic kế thừa Item — là instance của Item")
    void testElectronic_IsInstanceOfItem() {
        Electronic e = new Electronic();
        assertInstanceOf(Item.class, e);
    }

    @Test
    @DisplayName("Art kế thừa Item — có thể gọi các phương thức của Item")
    void testArt_IsInstanceOfItem() {
        Art art = new Art();
        assertInstanceOf(Item.class, art);
    }

    @Test
    @DisplayName("Vehicle kế thừa Item — có thể gọi các phương thức của Item")
    void testVehicle_IsInstanceOfItem() {
        Vehicle v = new Vehicle();
        assertInstanceOf(Item.class, v);
    }
}
