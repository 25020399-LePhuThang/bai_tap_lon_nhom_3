package com.auction;

import com.auction.shared.model.Auction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho Auction model.
 * Kiểm tra: khởi tạo, trạng thái, chuyển trạng thái, getter/setter.
 */
@DisplayName("Auction - Kiểm tra model phiên đấu giá")
class AuctionTest {

    private Auction auction;

    @BeforeEach
    void setUp() {
        auction = new Auction(
                "AUC_001",
                "ITEM_001",
                "SELLER_001",
                10_000_000,   // startPrice
                500_000,      // minIncrement
                LocalDateTime.now().plusHours(2)
        );
    }

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Constructor: currentHighestPrice ban đầu = startPrice")
    void testConstructor_CurrentHighestPriceEqualsStartPrice() {
        assertEquals(auction.getStartPrice(), auction.getCurrentHighestPrice(), 0.001,
                "Giá cao nhất ban đầu phải bằng giá khởi điểm");
    }

    @Test
    @DisplayName("Constructor: trạng thái ban đầu là PENDING")
    void testConstructor_DefaultStatusIsPending() {
        assertEquals("PENDING", auction.getStatus(),
                "Phiên mới tạo phải ở trạng thái PENDING");
    }

    @Test
    @DisplayName("Constructor: currentWinnerId ban đầu là null")
    void testConstructor_NoWinnerInitially() {
        assertNull(auction.getCurrentWinnerId(),
                "Chưa có ai thắng khi mới tạo phiên");
    }

    @Test
    @DisplayName("Constructor: auctionId, itemId, sellerId đúng")
    void testConstructor_IdsAreSetCorrectly() {
        assertEquals("AUC_001", auction.getAuctionId());
        assertEquals("ITEM_001", auction.getItemId());
        assertEquals("SELLER_001", auction.getSellerId());
    }

    @Test
    @DisplayName("Constructor: startTime không null")
    void testConstructor_StartTimeNotNull() {
        assertNotNull(auction.getStartTime());
    }

    // -----------------------------------------------------------------------
    // Chuyển trạng thái (State Transition)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Chuyển trạng thái PENDING → ACTIVE")
    void testStatusTransition_PendingToActive() {
        auction.setStatus("ACTIVE");
        assertEquals("ACTIVE", auction.getStatus());
    }

    @Test
    @DisplayName("Chuyển trạng thái ACTIVE → COMPLETED")
    void testStatusTransition_ActiveToCompleted() {
        auction.setStatus("ACTIVE");
        auction.setStatus("COMPLETED");
        assertEquals("COMPLETED", auction.getStatus());
    }

    @Test
    @DisplayName("Chuyển trạng thái ACTIVE → CANCELED")
    void testStatusTransition_ActiveToCanceled() {
        auction.setStatus("ACTIVE");
        auction.setStatus("CANCELED");
        assertEquals("CANCELED", auction.getStatus());
    }

    // -----------------------------------------------------------------------
    // Cập nhật giá và winner
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("setCurrentHighestPrice() cập nhật giá mới chính xác")
    void testSetCurrentHighestPrice_UpdatesCorrectly() {
        double newPrice = 15_000_000;
        auction.setCurrentHighestPrice(newPrice);
        assertEquals(newPrice, auction.getCurrentHighestPrice(), 0.001);
    }

    @Test
    @DisplayName("setCurrentWinnerId() cập nhật người thắng")
    void testSetCurrentWinnerId_UpdatesCorrectly() {
        auction.setCurrentWinnerId("BIDDER_007");
        assertEquals("BIDDER_007", auction.getCurrentWinnerId());
    }

    @Test
    @DisplayName("Cập nhật giá và winner cùng lúc — nhất quán")
    void testUpdatePriceAndWinner_Consistent() {
        auction.setCurrentHighestPrice(20_000_000);
        auction.setCurrentWinnerId("BIDDER_007");

        assertEquals(20_000_000, auction.getCurrentHighestPrice(), 0.001);
        assertEquals("BIDDER_007", auction.getCurrentWinnerId());
    }

    // -----------------------------------------------------------------------
    // minIncrement
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getMinIncrement() trả về đúng bước giá khởi tạo")
    void testGetMinIncrement_CorrectValue() {
        assertEquals(500_000, auction.getMinIncrement(), 0.001);
    }

    // -----------------------------------------------------------------------
    // toString()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("toString() chứa auctionId")
    void testToString_ContainsAuctionId() {
        assertTrue(auction.toString().contains("AUC_001"));
    }

    @Test
    @DisplayName("toString() chứa status")
    void testToString_ContainsStatus() {
        assertTrue(auction.toString().contains("PENDING"));
    }
}
