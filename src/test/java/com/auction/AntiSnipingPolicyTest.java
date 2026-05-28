package com.auction;

import com.auction.server.controller.AntiSnipingPolicy;
import com.auction.shared.model.item.Electronic;
import com.auction.shared.model.item.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho AntiSnipingPolicy.
 * Kiểm tra logic gia hạn phiên đấu giá khi có bid trong X giây cuối.
 */
@DisplayName("AntiSnipingPolicy - Kiểm tra gia hạn phiên đấu giá")
class AntiSnipingPolicyTest {

    private AntiSnipingPolicy policy;
    private Item item;

    @BeforeEach
    void setUp() {
        policy = new AntiSnipingPolicy();
        item = new Electronic();
        item.setStartTime(new Date(System.currentTimeMillis() - 60_000)); // bắt đầu 1 phút trước
    }

    @Test
    @DisplayName("Còn < 30 giây — phải gia hạn thêm 60 giây")
    void testApply_WithinLastSeconds_ShouldExtend() {
        // Đặt endTime còn 10 giây nữa (trong ngưỡng 30s)
        long originalEnd = System.currentTimeMillis() + 10_000;
        item.setEndTime(new Date(originalEnd));

        boolean extended = policy.apply(item);

        assertTrue(extended, "Phải trả về true khi gia hạn");
        // endTime mới phải lớn hơn endTime cũ ít nhất 60 giây
        long newEnd = item.getEndTime().getTime();
        assertTrue(newEnd >= originalEnd + 60_000,
                "endTime phải tăng thêm 60 giây. Thực tế: " + (newEnd - originalEnd) + "ms");
    }

    @Test
    @DisplayName("Còn > 30 giây — không gia hạn")
    void testApply_MoreThan30Seconds_ShouldNotExtend() {
        long originalEnd = System.currentTimeMillis() + 120_000; // còn 2 phút
        item.setEndTime(new Date(originalEnd));

        boolean extended = policy.apply(item);

        assertFalse(extended, "Không gia hạn khi còn nhiều thời gian");
        assertEquals(originalEnd, item.getEndTime().getTime(),
                "endTime không được thay đổi");
    }

    @Test
    @DisplayName("Đúng bằng 30 giây còn lại — không gia hạn (ngưỡng strict < 30s)")
    void testApply_ExactlyAtThreshold_ShouldNotExtend() {
        // 30_000ms = 30 giây → timeLeft = 30_000, điều kiện < 30_000 không thỏa
        long originalEnd = System.currentTimeMillis() + 30_000;
        item.setEndTime(new Date(originalEnd));

        boolean extended = policy.apply(item);

        // timeLeft = 30000 → NOT < 30000 → không gia hạn
        assertFalse(extended, "Đúng 30 giây không nằm trong ngưỡng gia hạn");
    }

    @Test
    @DisplayName("Phiên đã hết hạn — không gia hạn")
    void testApply_AlreadyExpired_ShouldNotExtend() {
        long expiredEnd = System.currentTimeMillis() - 5_000; // hết hạn 5 giây trước
        item.setEndTime(new Date(expiredEnd));

        boolean extended = policy.apply(item);

        assertFalse(extended, "Phiên đã hết hạn không cần gia hạn");
        assertEquals(expiredEnd, item.getEndTime().getTime(),
                "endTime không được thay đổi cho phiên đã hết");
    }

    @Test
    @DisplayName("Gia hạn nhiều lần liên tiếp — mỗi lần cộng thêm 60 giây")
    void testApply_MultipleExtensions_Accumulate() {
        item.setEndTime(new Date(System.currentTimeMillis() + 5_000)); // 5s còn lại

        policy.apply(item); // lần 1 — giờ còn ~65s
        // Lần 2: còn ~65s → > 30s → không gia hạn
        boolean secondExtension = policy.apply(item);

        assertFalse(secondExtension,
                "Sau khi gia hạn, thời gian còn lại > 30s nên không gia hạn tiếp");
    }

    @Test
    @DisplayName("Còn 1 giây — gia hạn, endTime tăng ~60 giây")
    void testApply_OneSecondLeft_ShouldExtend() {
        long originalEnd = System.currentTimeMillis() + 1_000;
        item.setEndTime(new Date(originalEnd));

        boolean extended = policy.apply(item);

        assertTrue(extended);
        assertTrue(item.getEndTime().getTime() > originalEnd + 55_000,
                "endTime phải tăng gần 60 giây");
    }
}
