import auction_system.common.utils.SecurityUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Kiểm thử lớp tiện ích {@link SecurityUtils}.
 *
 * <p>Các nhóm test:
 * <ol>
 *   <li>Định dạng output — độ dài hex, không null, không rỗng.</li>
 *   <li>Tính xác định — cùng input luôn cho cùng output.</li>
 *   <li>Phân biệt hoa thường — "abc" và "ABC" cho hash khác nhau.</li>
 *   <li>Chuỗi rỗng — không throw, vẫn trả về hash hợp lệ.</li>
 *   <li>Input khác nhau — hash phải khác nhau.</li>
 * </ol>
 */
public class SecurityUtilsTest {

    /**
     * Độ dài chuỗi hex SHA-256 chuẩn (256 bit / 4 bit-per-char).
     */
    private static final int SHA256_HEX_LENGTH = 64;

    /**
     * Kết quả hash không được null.
     */
    @Test
    void hashPasswordNotNull() {
        assertNotNull(SecurityUtils.hashPassword("password123"));
    }

    /**
     * Kết quả hash phải đúng 64 ký tự hex của SHA-256.
     */
    @Test
    void hashPasswordReturns64CharHexString() {
        String hash = SecurityUtils.hashPassword("anyInput");

        assertEquals(SHA256_HEX_LENGTH, hash.length(),
                "Hash SHA-256 phai co dung 64 ky tu hex.");
    }

    /**
     * Kết quả hash chỉ chứa ký tự hex hợp lệ (0-9, a-f).
     */
    @Test
    void hashPasswordOutputContainsOnlyHexCharacters() {
        String hash = SecurityUtils.hashPassword("test");

        assertTrue(hash.matches("[0-9a-f]+"),
                "Hash chi duoc chua ky tu hex 0-9 va a-f.");
    }

    /**
     * Hàm hash phải cho cùng kết quả khi gọi nhiều lần với cùng input
     * (deterministic / idempotent).
     */
    @Test
    void hashPasswordSameInputReturnsSameHashEachTime() {
        String hash1 = SecurityUtils.hashPassword("mySecret");
        String hash2 = SecurityUtils.hashPassword("mySecret");

        assertEquals(hash1, hash2,
                "Hash cua cung mot input phai luon giong nhau.");
    }

    /**
     * Hash phân biệt hoa thường — "abc" và "ABC" phải cho hash khác nhau.
     */
    @Test
    void hashPasswordCaseSensitiveDifferentHashes() {
        String lowerHash = SecurityUtils.hashPassword("abc");
        String upperHash = SecurityUtils.hashPassword("ABC");

        assertNotEquals(lowerHash, upperHash,
                "Hash phai phan biet hoa thuong.");
    }

    /**
     * Hai input khác nhau phải cho hash khác nhau.
     */
    @Test
    void hashPasswordDifferentInputsReturnDifferentHashes() {
        String hash1 = SecurityUtils.hashPassword("password1");
        String hash2 = SecurityUtils.hashPassword("password2");

        assertNotEquals(hash1, hash2,
                "Input khac nhau phai cho hash khac nhau.");
    }

    /**
     * Chuỗi rỗng không được throw, phải trả về hash hợp lệ độ dài 64.
     */
    @Test
    void hashPasswordEmptyStringReturnsValidHash() {
        String hash = SecurityUtils.hashPassword("");

        assertNotNull(hash);
        assertEquals(SHA256_HEX_LENGTH, hash.length(),
                "Chuoi rong van phai cho hash hop le.");
    }

    /**
     * Chuỗi chỉ có khoảng trắng phải cho hash khác chuỗi rỗng.
     */
    @Test
    void hashPasswordWhitespaceInputDifferentFromEmpty() {
        String emptyHash = SecurityUtils.hashPassword("");
        String spaceHash = SecurityUtils.hashPassword(" ");

        assertNotEquals(emptyHash, spaceHash,
                "Khoang trang phai cho hash khac voi chuoi rong.");
    }

    /**
     * Giá trị hash của "secret" phải khớp SHA-256 đã biết trước để tránh
     * regression khi thay đổi thuật toán.
     */
    @Test
    void hashPasswordKnownInputMatchesExpectedSha256() {
        String expected =
                "2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b";

        assertEquals(expected, SecurityUtils.hashPassword("secret"),
                "Hash cua 'secret' phai khop SHA-256 da biet truoc.");
    }
}