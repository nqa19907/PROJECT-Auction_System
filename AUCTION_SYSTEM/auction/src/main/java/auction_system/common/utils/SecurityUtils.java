package auction_system.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Tiện ích hỗ trợ bảo mật, mã hóa dữ liệu (Singleton).
 */
public final class SecurityUtils {
    private SecurityUtils() {
    }

    /**
     * Hàm băm mật khẩu.
     *
     * @param password Chuỗi mật khẩu gốc.
     * @return         Mật khẩu đã được băm.
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi hệ thống: Không tìm thấy thuật toán SHA-256", e);
        }
    }
}
