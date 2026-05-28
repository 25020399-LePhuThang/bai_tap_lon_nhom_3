package com.auction;

import com.auction.shared.exception.AuctionClosedException;
import com.auction.shared.exception.InvalidBidException;
import com.auction.shared.model.AutoBid;
import com.auction.shared.model.user.Admin;
import com.auction.shared.model.user.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho AutoBid model, Exception classes,
 * và các test bổ sung cho Admin/Seller.
 */
@DisplayName("AutoBid, Exceptions & các model bổ sung")
class AutoBidAndExceptionTest {

    // -----------------------------------------------------------------------
    // AutoBid model
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("AutoBid constructor thiết lập đúng bidderId, maxBid, increment")
    void testAutoBid_Constructor_FieldsSetCorrectly() {
        AutoBid bid = new AutoBid("bidder_01", 50_000, 2_000);

        assertEquals("bidder_01", bid.getBidderId());
        assertEquals(50_000, bid.getMaxBid(), 0.001);
        assertEquals(2_000, bid.getIncrement(), 0.001);
    }

    @Test
    @DisplayName("AutoBid.timestamp được gán tự động khi tạo")
    void testAutoBid_TimestampIsSet() {
        long before = System.currentTimeMillis();
        AutoBid bid = new AutoBid("b01", 10_000, 500);
        long after = System.currentTimeMillis();

        assertTrue(bid.getTimestamp() >= before && bid.getTimestamp() <= after,
                "Timestamp phải nằm trong khoảng thời gian tạo object");
    }

    @Test
    @DisplayName("Hai AutoBid tạo ra liên tiếp có timestamp tăng dần")
    void testAutoBid_TwoInstances_TimestampOrdered() throws InterruptedException {
        AutoBid first = new AutoBid("b01", 10_000, 500);
        Thread.sleep(2);
        AutoBid second = new AutoBid("b02", 20_000, 500);

        assertTrue(second.getTimestamp() >= first.getTimestamp(),
                "AutoBid tạo sau phải có timestamp >= AutoBid tạo trước");
    }

    // -----------------------------------------------------------------------
    // AuctionClosedException
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("AuctionClosedException là RuntimeException")
    void testAuctionClosedException_IsRuntimeException() {
        AuctionClosedException ex = new AuctionClosedException("Phiên đã đóng");
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    @DisplayName("AuctionClosedException giữ đúng message")
    void testAuctionClosedException_MessagePreserved() {
        String msg = "Phiên đấu giá #001 đã đóng";
        AuctionClosedException ex = new AuctionClosedException(msg);
        assertEquals(msg, ex.getMessage());
    }

    @Test
    @DisplayName("AuctionClosedException có thể bắt được bằng try-catch")
    void testAuctionClosedException_CanBeCaught() {
        assertThrows(AuctionClosedException.class, () -> {
            throw new AuctionClosedException("Phiên đã đóng");
        });
    }

    // -----------------------------------------------------------------------
    // InvalidBidException
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("InvalidBidException là Exception")
    void testInvalidBidException_IsCheckedException() {
        assertInstanceOf(Exception.class, new InvalidBidException());
    }

    @Test
    @DisplayName("InvalidBidException có thể ném và bắt được")
    void testInvalidBidException_ThrowAndCatch() {
        assertThrows(InvalidBidException.class, () -> {
            throw new InvalidBidException();
        });
    }

    // -----------------------------------------------------------------------
    // Admin bổ sung
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Admin.getRole() trả về ADMIN")
    void testAdmin_GetRole_ReturnsAdmin() {
        Admin admin = new Admin("a01", "Superuser", "pass", "admin@sys.com", "0911", "ACTIVE");
        assertEquals("ADMIN", admin.getRole());
    }

    @Test
    @DisplayName("Admin.getEmployeeId() = 'EMP' + id")
    void testAdmin_GetEmployeeId_Correct() {
        Admin admin = new Admin("001", "Superuser", "pass", "admin@sys.com", "0911", "ACTIVE");
        assertEquals("EMP001", admin.getEmployeeId());
    }

    @Test
    @DisplayName("Admin.adminLevel mặc định là MODERATOR")
    void testAdmin_DefaultAdminLevel_IsModerator() {
        Admin admin = new Admin("a01", "Su", "pass", "su@sys.com", "09", "ACTIVE");
        assertEquals("MODERATOR", admin.getAdminLevel());
    }

    @Test
    @DisplayName("Admin.setAdminLevel() cập nhật đúng")
    void testAdmin_SetAdminLevel() {
        Admin admin = new Admin("a01", "Su", "pass", "su@sys.com", "09", "ACTIVE");
        admin.setAdminLevel("SUPER_ADMIN");
        assertEquals("SUPER_ADMIN", admin.getAdminLevel());
    }

    // -----------------------------------------------------------------------
    // Seller bổ sung
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Seller.getRole() trả về SELLER")
    void testSeller_GetRole_ReturnsSeller() {
        Seller seller = new Seller("s01", "Bob", "pass", "bob@sell.com", "0922", "ACTIVE", "SELLER");
        assertEquals("SELLER", seller.getRole());
    }

    @Test
    @DisplayName("Seller.rating mặc định là 0.0")
    void testSeller_DefaultRating_IsZero() {
        Seller seller = new Seller();
        assertEquals(0.0, seller.getRating(), 0.001);
    }

    @Test
    @DisplayName("Seller.setRating() cập nhật đúng")
    void testSeller_SetRating() {
        Seller seller = new Seller();
        seller.setRating(4.8);
        assertEquals(4.8, seller.getRating(), 0.001);
    }

    @Test
    @DisplayName("Seller.addedItems không null sau khởi tạo đầy đủ")
    void testSeller_AddedItems_NotNullAfterFullConstructor() {
        Seller seller = new Seller("s01", "Bob", "pass", "bob@sell.com", "0922", "ACTIVE", "SELLER");
        assertNotNull(seller.getAddedItems(),
                "addedItems phải được khởi tạo là danh sách rỗng, không phải null");
    }
}
