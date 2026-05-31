package com.auction;

import com.auction.server.controller.AuctionAutoBid;
import com.auction.server.controller.AuctionAutoBid.AutoBidResult;
import com.auction.shared.model.AutoBid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho AuctionAutoBid.
 * Kiểm tra logic Proxy Bidding (auto-bid) — bao gồm:
 * - Đăng ký auto-bid cơ bản
 * - Ưu tiên maxBid cao hơn
 * - Ưu tiên đăng ký sớm hơn khi maxBid bằng nhau
 * - Ngăn tự đấu giá với chính mình
 * - Concurrent bidding an toàn (không race condition)
 */
@DisplayName("AuctionAutoBid - Kiểm tra logic Auto-Bid")
class AuctionAutoBidTest {

    private AuctionAutoBid autoBid;

    private static final double START_PRICE = 10_000;
    private static final double INCREMENT = 1_000;

    @BeforeEach
    void setUp() {
        // Giá khởi điểm 10.000, chưa ai thắng
        autoBid = new AuctionAutoBid(START_PRICE, null);
    }

    // -----------------------------------------------------------------------
    // registerAutoBid() — trường hợp cơ bản
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Đăng ký auto-bid thành công — giá tăng lên currentPrice + increment")
    void testRegisterAutoBid_Basic_PriceIncreases() {
        AutoBid bid = new AutoBid("bidderA", 15_000, INCREMENT);
        AutoBidResult result = autoBid.registerAutoBid(bid);

        assertTrue(result.priceChanged(), "Giá phải thay đổi sau khi có auto-bid");
        assertEquals("bidderA", result.finalWinnerId());
        // Giá mới phải là START_PRICE + INCREMENT = 11_000
        assertEquals(START_PRICE + INCREMENT, result.finalPrice(), 0.001);
    }

    @Test
    @DisplayName("maxBid thấp hơn hoặc bằng giá hiện tại → bị từ chối")
    void testRegisterAutoBid_MaxBidTooLow_ShouldFail() {
        AutoBid bid = new AutoBid("bidderA", START_PRICE, INCREMENT); // maxBid = currentPrice
        AutoBidResult result = autoBid.registerAutoBid(bid);

        assertFalse(result.priceChanged(), "Không nên thay đổi giá nếu maxBid <= currentPrice");
        assertNotNull(result.message());
    }

    // -----------------------------------------------------------------------
    // Ưu tiên maxBid cao hơn
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Bidder có maxBid cao hơn thắng dù đăng ký sau")
    void testRegisterAutoBid_HigherMaxBidWins() throws InterruptedException {
        // bidderA đăng ký trước với maxBid thấp hơn
        AutoBid bidA = new AutoBid("bidderA", 12_000, INCREMENT);
        autoBid.registerAutoBid(bidA);

        Thread.sleep(5); // đảm bảo timestamp khác nhau

        // bidderB đăng ký sau với maxBid cao hơn
        AutoBid bidB = new AutoBid("bidderB", 20_000, INCREMENT);
        AutoBidResult result = autoBid.registerAutoBid(bidB);

        assertEquals("bidderB", result.finalWinnerId(),
                "bidderB phải thắng vì có maxBid cao hơn");
    }

    // -----------------------------------------------------------------------
    // Ưu tiên đăng ký sớm khi maxBid bằng nhau
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Khi maxBid bằng nhau — người đăng ký trước thắng")
    void testRegisterAutoBid_TieMaxBid_EarlierBidderWins() throws InterruptedException {
        double sameMaxBid = 20_000;

        AutoBid bidA = new AutoBid("bidderA", sameMaxBid, INCREMENT);
        autoBid.registerAutoBid(bidA);

        Thread.sleep(10); // bidderB đăng ký sau

        AutoBid bidB = new AutoBid("bidderB", sameMaxBid, INCREMENT);
        autoBid.registerAutoBid(bidB);

        // bidderA đăng ký trước nên phải là winner
        assertEquals("bidderA", autoBid.getCurrentWinnerId(),
                "Khi maxBid bằng nhau, người đăng ký sớm hơn phải thắng");
    }

    // -----------------------------------------------------------------------
    // Không tự đấu giá với chính mình
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Người đang dẫn đầu không thể đăng ký auto-bid lại")
    void testRegisterAutoBid_CurrentWinnerCannotBidAgainst_Self() {
        // Đầu tiên bidderA trở thành winner
        autoBid = new AuctionAutoBid(START_PRICE, "bidderA");

        AutoBid selfBid = new AutoBid("bidderA", 50_000, INCREMENT);
        AutoBidResult result = autoBid.registerAutoBid(selfBid);

        assertFalse(result.priceChanged(),
                "Winner hiện tại không thể tự đấu giá với chính mình");
        assertNotNull(result.message(), "Phải có thông báo giải thích lý do từ chối");
    }

    // -----------------------------------------------------------------------
    // onManualBidPlaced() — khi có bid thường đến
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Sau bid thường, auto-bid phải phản ứng để lấy lại vị trí dẫn đầu")
    void testOnManualBidPlaced_AutoBidReacts() {
        // bidderA đã đăng ký auto-bid với maxBid cao
        AutoBid bidA = new AutoBid("bidderA", 30_000, INCREMENT);
        autoBid.registerAutoBid(bidA);

        // bidderB đặt giá thủ công 15_000, vượt qua bidderA
        AutoBidResult result = autoBid.onManualBidPlaced(15_000, "bidderB");

        // bidderA có maxBid 30_000 > 15_000 → phải phản ứng và giành lại
        assertTrue(result.priceChanged(), "Auto-bid phải phản ứng sau manual bid");
        assertEquals("bidderA", result.finalWinnerId(),
                "bidderA phải giành lại vì maxBid đủ cao");
    }

    @Test
    @DisplayName("Auto-bid không phản ứng khi danh sách trống")
    void testOnManualBidPlaced_EmptyAutoBids_NoChange() {
        AutoBidResult result = autoBid.onManualBidPlaced(15_000, "bidderB");
        assertFalse(result.priceChanged(), "Không có auto-bid nào nên không thay đổi");
    }

    // -----------------------------------------------------------------------
    // cancelAutoBid()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Hủy auto-bid — bidder bị xóa khỏi danh sách")
    void testCancelAutoBid_BidderRemovedFromList() {
        AutoBid bidA = new AutoBid("bidderA", 20_000, INCREMENT);
        autoBid.registerAutoBid(bidA);

        autoBid.cancelAutoBid("bidderA");

        // Sau khi hủy, manual bid từ bidderB không bị phản ứng
        AutoBidResult result = autoBid.onManualBidPlaced(12_000, "bidderB");
        assertFalse(result.priceChanged(),
                "bidderA đã hủy, không còn ai auto-bid phản ứng");
    }

    // -----------------------------------------------------------------------
    // Concurrent Bidding — thread safety
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Concurrent: nhiều auto-bid đồng thời không gây race condition")
    void testConcurrentAutoBid_NoRaceCondition() throws InterruptedException {
        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    AutoBid bid = new AutoBid("bidder_" + idx,
                            20_000 + idx * 1_000,   // maxBid khác nhau
                            INCREMENT);
                    autoBid.registerAutoBid(bid);
                } catch (Exception e) {
                    synchronized (errors) {
                        errors.add(e.getMessage());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        pool.shutdown();

        assertTrue(errors.isEmpty(), "Không được có exception khi xử lý đồng thời: " + errors);

        // Winner phải là bidder có maxBid cao nhất = bidder_9 với maxBid = 29_000
        assertEquals("bidder_9", autoBid.getCurrentWinnerId(),
                "bidder có maxBid cao nhất phải thắng");
    }

    @Test
    @DisplayName("Concurrent: nhiều onManualBidPlaced đồng thời không làm hỏng giá")
    void testConcurrentManualBid_PriceNeverRollsBack() throws InterruptedException {
        // Đăng ký một auto-bid có maxBid cao
        autoBid.registerAutoBid(new AutoBid("autoBot", 100_000, INCREMENT));

        int threadCount = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final double price = 20_000 + i * 500;
            pool.submit(() -> {
                try {
                    autoBid.onManualBidPlaced(price, "manualBidder");
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        pool.shutdown();

        // Giá không được âm hoặc nhỏ hơn START_PRICE
        assertTrue(autoBid.getCurrentPrice() >= START_PRICE,
                "Giá không được nhỏ hơn giá khởi điểm: " + autoBid.getCurrentPrice());
    }
}
