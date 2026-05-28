package com.auction;

import com.auction.shared.model.BidTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho BidTransaction.
 * Kiểm tra logic validate giá đấu — đây là nghiệp vụ cốt lõi nhất.
 */
@DisplayName("BidTransaction - Kiểm tra logic đặt giá")
class BidTransactionTest {

    private BidTransaction bid;

    @BeforeEach
    void setUp() {
        bid = new BidTransaction("bidder_01", "item_01", 1500_000);
    }

    // -----------------------------------------------------------------------
    // isValidBid()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Giá đặt hợp lệ: đúng bằng currentPrice + minIncrement")
    void testIsValidBid_ExactMinimum_ShouldReturnTrue() {
        // currentPrice = 1_000_000, minIncrement = 500_000
        // bidAmount = 1_500_000 >= 1_500_000 → hợp lệ
        assertTrue(bid.isValidBid(1_000_000, 500_000));
    }

    @Test
    @DisplayName("Giá đặt hợp lệ: cao hơn mức tối thiểu yêu cầu")
    void testIsValidBid_AboveMinimum_ShouldReturnTrue() {
        // bidAmount = 1_500_000 >= 1_000_000 + 200_000 = 1_200_000 → hợp lệ
        assertTrue(bid.isValidBid(1_000_000, 200_000));
    }

    @Test
    @DisplayName("Giá đặt không hợp lệ: thấp hơn currentPrice + minIncrement")
    void testIsValidBid_BelowMinimum_ShouldReturnFalse() {
        // bidAmount = 1_500_000 < 1_000_000 + 600_000 = 1_600_000 → không hợp lệ
        assertFalse(bid.isValidBid(1_000_000, 600_000));
    }

    @Test
    @DisplayName("Giá đặt không hợp lệ: bằng đúng giá hiện tại (thiếu minIncrement)")
    void testIsValidBid_EqualCurrentPrice_ShouldReturnFalse() {
        // bidAmount = 1_500_000, currentPrice = 1_500_000, minIncrement = 100_000
        // Cần >= 1_600_000 → không hợp lệ
        assertFalse(bid.isValidBid(1_500_000, 100_000));
    }

    @Test
    @DisplayName("Giá đặt = 0 với currentPrice = 0 và minIncrement = 0 → hợp lệ")
    void testIsValidBid_ZeroValues_ShouldReturnTrue() {
        BidTransaction zeroBid = new BidTransaction("b", "i", 0);
        assertTrue(zeroBid.isValidBid(0, 0));
    }

    // -----------------------------------------------------------------------
    // Constructor & getters
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Constructor tạo đúng bidderId, itemId, bidAmount")
    void testConstructor_FieldsAreSetCorrectly() {
        assertEquals("bidder_01", bid.getBidderId());
        assertEquals("item_01", bid.getItemId());
        assertEquals(1_500_000, bid.getBidAmount());
    }

    @Test
    @DisplayName("Status mặc định phải là ACTIVE khi vừa tạo")
    void testDefaultStatus_IsActive() {
        assertEquals("ACTIVE", bid.getStatus());
    }

    @Test
    @DisplayName("Timestamp được thiết lập tự động khi tạo")
    void testTimestamp_IsNotNull() {
        assertNotNull(bid.getTimestamp());
    }

    @Test
    @DisplayName("transactionId tự tăng — hai transaction khác nhau có ID khác nhau")
    void testTransactionId_AutoIncrement() {
        BidTransaction bid2 = new BidTransaction("bidder_02", "item_02", 2_000_000);
        assertNotEquals(bid.getTransactionId(), bid2.getTransactionId());
    }

    // -----------------------------------------------------------------------
    // Setter
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("setStatus() thay đổi trạng thái thành công")
    void testSetStatus() {
        bid.setStatus("CANCELLED");
        assertEquals("CANCELLED", bid.getStatus());
    }

    @Test
    @DisplayName("setBidAmount() cập nhật đúng giá")
    void testSetBidAmount() {
        bid.setBidAmount(3_000_000);
        assertEquals(3_000_000, bid.getBidAmount());
    }
}
