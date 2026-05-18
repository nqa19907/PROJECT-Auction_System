import auction_system.common.models.Bidder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

public class ParticipantTest {
    private static final Logger logger = LoggerFactory.getLogger(ParticipantTest.class);

    private static final double INITIAL_BALANCE = 10000.0;
    private Bidder bidder;

    @BeforeEach
    void setUp() {
        // Arrange: Khởi tạo một Bidder dùng chung với số dư cố định
        bidder = new Bidder("MinhTran", "minh@gmail.com", "pass123", INITIAL_BALANCE);
    }

    // ======================= addFunds =======================

    @Test
    void testAddFunds_PositiveAmount_IncreasesBalance() {
        // Arrange: Số tiền nạp hợp lệ
        double depositAmount = 5000.0;

        // Act: Thực hiện nạp tiền
        bidder.addFunds(depositAmount);

        // Assert: Số dư phải tăng đúng bằng số tiền nạp vào
        assertEquals(INITIAL_BALANCE + depositAmount, bidder.getBalance(), 0.001,
                "Số dư sau khi nạp phải bằng số dư ban đầu cộng số tiền nạp");
    }

    @Test
    void testAddFunds_ZeroAmount_ThrowsIllegalArgumentException() {
        // Arrange: Số tiền nạp bằng 0 (không hợp lệ)

        // Act & Assert: Nạp 0 đồng phải ném ra IllegalArgumentException
        String actualMessage = assertThrows(IllegalArgumentException.class, () -> {
            bidder.addFunds(0);
        }).getMessage();

        String expectedMessage = "Số tiền nạp phải lớn hơn 0";
        assertEquals(expectedMessage, actualMessage,
                "Thông báo lỗi khi nạp 0 đồng phải khớp");
    }

    @Test
    void testAddFunds_NegativeAmount_ThrowsIllegalArgumentException() {
        // Arrange: Số tiền âm (không hợp lệ)

        // Act & Assert: Nạp số âm phải ném ra IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            bidder.addFunds(-1000.0);
        }, "Nạp số tiền âm phải ném ra IllegalArgumentException");
    }

    @Test
    void testAddFunds_MultipleDeposits_BalanceAccumulatesCorrectly() {
        // Arrange: Nạp nhiều lần với các khoản khác nhau
        double first = 2000.0;
        double second = 3500.0;
        double third = 1500.0;

        // Act: Nạp liên tiếp ba lần
        bidder.addFunds(first);
        bidder.addFunds(second);
        bidder.addFunds(third);

        // Assert: Số dư cuối phải bằng tổng tất cả các khoản nạp
        double expectedBalance = INITIAL_BALANCE + first + second + third;
        assertEquals(expectedBalance, bidder.getBalance(), 0.001,
                "Số dư phải tích lũy đúng sau nhiều lần nạp tiền");
    }

    // ======================= withdrawFunds =======================

    @Test
    void testWithdrawFunds_SufficientBalance_ReturnsTrueAndDeductsAmount() {
        // Arrange: Số tiền rút nhỏ hơn số dư
        double withdrawAmount = 3000.0;

        // Act: Thực hiện rút tiền
        boolean result = bidder.withdrawFunds(withdrawAmount);

        // Assert: Kết quả phải là true và số dư phải giảm đúng
        assertTrue(result, "Rút tiền hợp lệ phải trả về true");
        assertEquals(INITIAL_BALANCE - withdrawAmount, bidder.getBalance(), 0.001,
                "Số dư phải giảm đúng số tiền đã rút");
    }

    @Test
    void testWithdrawFunds_ExactBalance_ReturnsTrueAndBalanceBecomesZero() {
        // Arrange: Rút đúng bằng toàn bộ số dư (kiểm tra biên)

        // Act: Rút toàn bộ số dư
        boolean result = bidder.withdrawFunds(INITIAL_BALANCE);

        // Assert: Phải thành công và số dư về đúng 0
        assertTrue(result, "Rút toàn bộ số dư phải trả về true");
        assertEquals(0.0, bidder.getBalance(), 0.001,
                "Sau khi rút hết tiền, số dư phải về 0");
    }

    @Test
    void testWithdrawFunds_InsufficientBalance_ReturnsFalse() {
        // Arrange: Số tiền rút lớn hơn số dư hiện có
        double excessiveAmount = INITIAL_BALANCE + 1.0;

        // Act: Thực hiện rút quá số dư
        boolean result = bidder.withdrawFunds(excessiveAmount);

        // Assert: Phải thất bại và số dư không bị thay đổi
        assertFalse(result, "Rút quá số dư phải trả về false");
        assertEquals(INITIAL_BALANCE, bidder.getBalance(), 0.001,
                "Số dư không được thay đổi khi rút thất bại");
    }

    @Test
    void testWithdrawFunds_ZeroAmount_ReturnsFalse() {
        // Arrange: Số tiền rút bằng 0 (không hợp lệ)

        // Act: Thực hiện rút 0 đồng
        boolean result = bidder.withdrawFunds(0);

        // Assert: Phải thất bại
        assertFalse(result, "Rút 0 đồng phải trả về false");
    }

    @Test
    void testWithdrawFunds_NegativeAmount_ReturnsFalse() {
        // Arrange: Số tiền âm (không hợp lệ)

        // Act: Thực hiện rút số âm
        boolean result = bidder.withdrawFunds(-500.0);

        // Assert: Phải thất bại và số dư không bị thay đổi
        assertFalse(result, "Rút số tiền âm phải trả về false");
        assertEquals(INITIAL_BALANCE, bidder.getBalance(), 0.001,
                "Số dư không được thay đổi khi rút số âm");
    }

    @Test
    void testGetBalance_MatchesConstructorValue() {
        // Assert: Số dư phải khớp với giá trị truyền vào khi khởi tạo
        assertEquals(INITIAL_BALANCE, bidder.getBalance(), 0.001,
                "Số dư ban đầu phải khớp với giá trị truyền vào constructor");
    }

    @Test
    void testAddFunds_ThenWithdraw_FinalBalanceIsCorrect() {
        // Arrange: Nạp tiền rồi rút tiền theo thứ tự
        double depositAmount = 5000.0;
        double withdrawAmount = 8000.0;

        // Act: Nạp rồi rút
        bidder.addFunds(depositAmount);
        boolean withdrawResult = bidder.withdrawFunds(withdrawAmount);

        // Assert: Rút phải thành công vì đủ tiền sau khi nạp
        assertTrue(withdrawResult, "Rút sau khi nạp đủ tiền phải thành công");
        double expectedBalance = INITIAL_BALANCE + depositAmount - withdrawAmount;
        assertEquals(expectedBalance, bidder.getBalance(), 0.001,
                "Số dư cuối phải phản ánh đúng thứ tự nạp rồi rút");
    }
}
