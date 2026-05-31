import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import auction_system.client.utils.CurrencyFormatter;

import org.junit.jupiter.api.Test;

/**
 * Kiểm thử lớp tiện ích {@link CurrencyFormatter}.
 *
 * <p>Các nhóm test:
 * <ol>
 *   <li>formatVnd — kết thúc bằng đơn vị "VNĐ", không rỗng, số đúng.</li>
 *   <li>formatAmount — không có đơn vị, format đúng số.</li>
 *   <li>Giá trị biên — số 0, số âm, số lớn.</li>
 * </ol>
 */
public class CurrencyFormatterTest {

    /** Đơn vị tiền tệ kỳ vọng cuối chuỗi. */
    private static final String VND_SUFFIX = "VNĐ";

    // =========================================================================
    // formatVnd
    // =========================================================================

    /**
     * Kết quả formatVnd không được null.
     */
    @Test
    void formatVnd_NotNull() {
        assertNotNull(CurrencyFormatter.formatVnd(1_000L));
    }

    /**
     * Kết quả formatVnd phải kết thúc bằng " VNĐ".
     */
    @Test
    void formatVnd_EndsWithVndSuffix() {
        String result = CurrencyFormatter.formatVnd(50_000L);

        assertTrue(result.endsWith(" " + VND_SUFFIX),
                "Ket qua phai ket thuc bang ' VND'.");
    }

    /**
     * Giá trị 0 VNĐ không được rỗng và vẫn có hậu tố đúng.
     */
    @Test
    void formatVnd_ZeroValue_StillHasSuffix() {
        String result = CurrencyFormatter.formatVnd(0L);

        assertFalse(result.isBlank(), "Ket qua khong duoc rong.");
        assertTrue(result.endsWith(" " + VND_SUFFIX),
                "So 0 van phai co hau to VND.");
    }

    /**
     * Số lớn 1 triệu phải có định dạng phân cách hàng nghìn (locale vi_VN dùng dấu chấm).
     */
    @Test
    void formatVnd_LargeNumber_ContainsThousandSeparator() {
        String result = CurrencyFormatter.formatVnd(1_000_000L);

        // Locale vi_VN dùng dấu chấm (.) hoặc dấu phẩy làm phân cách hàng nghìn.
        assertTrue(result.contains(".") || result.contains(","),
                "So lon phai co ky tu phan cach hang nghin.");
    }

    /**
     * Số âm không được throw — phải trả về chuỗi có hậu tố VNĐ.
     */
    @Test
    void formatVnd_NegativeValue_ReturnsSuffixedString() {
        String result = CurrencyFormatter.formatVnd(-500L);

        assertNotNull(result);
        assertTrue(result.endsWith(" " + VND_SUFFIX),
                "So am van phai co hau to VND.");
    }

    // =========================================================================
    // formatAmount
    // =========================================================================

    /**
     * Kết quả formatAmount không được null.
     */
    @Test
    void formatAmount_NotNull() {
        assertNotNull(CurrencyFormatter.formatAmount(2_000.0));
    }

    /**
     * Kết quả formatAmount không được chứa hậu tố "VNĐ".
     */
    @Test
    void formatAmount_DoesNotContainVndSuffix() {
        String result = CurrencyFormatter.formatAmount(10_000.0);

        assertFalse(result.contains(VND_SUFFIX),
                "formatAmount khong duoc chua hau to VND.");
    }

    /**
     * formatAmount(0.0) không được rỗng.
     */
    @Test
    void formatAmount_ZeroValue_NotBlank() {
        String result = CurrencyFormatter.formatAmount(0.0);

        assertFalse(result.isBlank(), "Ket qua cua 0.0 khong duoc rong.");
    }

    /**
     * Hai lần gọi formatAmount với cùng input phải cho cùng output.
     */
    @Test
    void formatAmount_SameInputReturnsSameOutput() {
        double amount = 99_999.99;

        assertEquals(
                CurrencyFormatter.formatAmount(amount),
                CurrencyFormatter.formatAmount(amount),
                "Cung input phai cho cung output.");
    }

    /**
     * formatAmount và formatVnd với cùng số nguyên phải cho prefix giống nhau.
     */
    @Test
    void formatVnd_PrefixMatchesFormatAmount() {
        long amount = 75_000L;
        String vndResult = CurrencyFormatter.formatVnd(amount);
        String amountResult = CurrencyFormatter.formatAmount(amount);

        // formatVnd chỉ thêm " VNĐ" vào sau formatAmount
        assertTrue(vndResult.startsWith(amountResult),
                "formatVnd phai la formatAmount cong them ' VND'.");
    }
}