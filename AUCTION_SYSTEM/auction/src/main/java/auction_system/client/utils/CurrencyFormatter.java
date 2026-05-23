package auction_system.client.utils;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility format tiền VNĐ.
 */
public final class CurrencyFormatter {

    /** Formatter VNĐ. */
    private static final NumberFormat VND_FORMAT =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    /**
     * Constructor private để tránh tạo object utility.
     */
    private CurrencyFormatter() {

    }

    /**
     * Format số tiền sang VNĐ.
     *
     * @param amount số tiền cần format
     * @return chuỗi VNĐ đã format
     */
    public static String formatVnd(final long amount) {
        return VND_FORMAT.format(amount) + " VNĐ";
    }

    /**
     * Format số tiền không kèm đơn vị.
     *
     * <p>Dùng cho các màn hình đã có label đơn vị riêng, tránh hiển thị trùng
     * chữ "VNĐ".
     *
     * @param amount số tiền cần format
     * @return chuỗi số tiền đã format
     */
    public static String formatAmount(final double amount) {
        return VND_FORMAT.format(amount);
    }
}
