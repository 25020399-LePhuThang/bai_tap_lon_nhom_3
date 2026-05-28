package com.auction;

import com.auction.shared.model.user.Admin;
import com.auction.shared.model.user.Bidder;
import com.auction.shared.model.user.Seller;
import com.auction.shared.model.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho cây kế thừa User (Bidder, Seller, Admin).
 * Kiểm tra: role, getter/setter, kế thừa, đóng gói (encapsulation).
 */
@DisplayName("User - Kiểm tra phân cấp người dùng")
class UserTest {

    // -----------------------------------------------------------------------
    // Bidder
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Bidder.getRole() trả về BIDDER")
    void testBidder_GetRole_ReturnsBidder() {
        Bidder bidder = new Bidder("u01", "Alice", "pass", "alice@mail.com", "0900000001", "ACTIVE", "BIDDER");
        assertEquals("BIDDER", bidder.getRole());
    }

    @Test
    @DisplayName("Bidder: balance mặc định là 0")
    void testBidder_DefaultBalance_IsZero() {
        Bidder bidder = new Bidder();
        assertEquals(0f, bidder.getBalance(), 0.001f);
    }

    @Test
    @DisplayName("Bidder: setBalance/getBalance hoạt động đúng")
    void testBidder_SetBalance_UpdatesCorrectly() {
        Bidder bidder = new Bidder();
        bidder.setBalance(5_000_000f);
        assertEquals(5_000_000f, bidder.getBalance(), 0.001f);
    }

    @Test
    @DisplayName("Bidder: setShippingAddress/getShippingAddress hoạt động đúng")
    void testBidder_ShippingAddress() {
        Bidder bidder = new Bidder();
        bidder.setShippingAddress("123 Phố Huế, Hà Nội");
        assertEquals("123 Phố Huế, Hà Nội", bidder.getShippingAddress());
    }

    @Test
    @DisplayName("Bidder là instance của User")
    void testBidder_IsInstanceOfUser() {
        Bidder bidder = new Bidder();
        assertInstanceOf(User.class, bidder);
    }

    @Test
    @DisplayName("Bidder.printInfo() không ném exception")
    void testBidder_PrintInfo_NoException() {
        Bidder bidder = new Bidder("u01", "Alice", "pass", "alice@mail.com", "0900000001", "ACTIVE", "BIDDER");
        assertDoesNotThrow(bidder::printInfo);
    }

    // -----------------------------------------------------------------------
    // Seller
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Seller là instance của User")
    void testSeller_IsInstanceOfUser() {
        Seller seller = new Seller();
        assertInstanceOf(User.class, seller);
    }

    @Test
    @DisplayName("Seller: setName/getName hoạt động đúng")
    void testSeller_SetName_UpdatesCorrectly() {
        Seller seller = new Seller();
        seller.setName("Bob");
        assertEquals("Bob", seller.getName());
    }

    @Test
    @DisplayName("Seller: setEmail/getEmail hoạt động đúng")
    void testSeller_SetEmail_UpdatesCorrectly() {
        Seller seller = new Seller();
        seller.setEmail("bob@seller.com");
        assertEquals("bob@seller.com", seller.getEmail());
    }

    // -----------------------------------------------------------------------
    // Admin
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Admin là instance của User")
    void testAdmin_IsInstanceOfUser() {
        Admin admin = new Admin();
        assertInstanceOf(User.class, admin);
    }

    @Test
    @DisplayName("Admin: setId/getId hoạt động đúng")
    void testAdmin_SetId_UpdatesCorrectly() {
        Admin admin = new Admin();
        admin.setId("ADMIN_001");
        assertEquals("ADMIN_001", admin.getId());
    }

    // -----------------------------------------------------------------------
    // Encapsulation — password không bị lộ qua toString()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Bidder.toString() không chứa password (encapsulation)")
    void testBidder_ToString_DoesNotExposePassword() {
        Bidder bidder = new Bidder("u01", "Alice", "secret_pass", "alice@mail.com", "0900000001", "ACTIVE", "BIDDER");
        bidder.setPassword("secret_pass");
        String str = bidder.toString();
        assertFalse(str.contains("secret_pass"),
                "toString() không được lộ mật khẩu: " + str);
    }

    // -----------------------------------------------------------------------
    // printInfo() — polymorphism
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Polymorphism: User reference gọi printInfo() của Bidder")
    void testPolymorphism_PrintInfo_UsesBidderImpl() {
        // Kiểm tra polymorphism: gán Bidder vào biến kiểu User
        // Bidder.printInfo() ghi đè User.printInfo()
        Bidder bidder = new Bidder("u01", "Alice", "pass", "alice@mail.com", "090", "ACTIVE", "BIDDER");
        assertDoesNotThrow(() -> {
            User user = bidder;
            user.printInfo(); // gọi override của Bidder, không phải User
        });
    }
}
