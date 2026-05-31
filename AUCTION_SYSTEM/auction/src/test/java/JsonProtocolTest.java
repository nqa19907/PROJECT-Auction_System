import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * Kiểm thử lớp tiện ích {@link JsonProtocol}.
 *
 * <p>Các nhóm test:
 * <ol>
 *   <li>parse — JSON hợp lệ trả về JsonMessage đúng trường.</li>
 *   <li>parse — JSON không hợp lệ ném IOException.</li>
 *   <li>stringify — message không null trả về JSON string.</li>
 *   <li>stringifyRequired — message tốt trả về JSON; message lỗi ném
 *       IllegalStateException.</li>
 *   <li>request — không payload và có payload.</li>
 *   <li>payloadOf — object bất kỳ chuyển đổi thành JsonNode không null.</li>
 *   <li>payloadAs — payload null/rỗng ném IllegalArgumentException.</li>
 * </ol>
 */
public class JsonProtocolTest {

    // =========================================================================
    // parse
    // =========================================================================

    /**
     * parse JSON hợp lệ có đầy đủ trường phải trả về JsonMessage đúng.
     */
    @Test
    void parse_ValidJson_ReturnsCorrectMessage() throws IOException {
        String raw = "{\"type\":\"LOGIN_OK\",\"command\":null,"
                + "\"status\":\"OK\",\"payload\":null,\"message\":\"Success\"}";

        JsonMessage msg = JsonProtocol.parse(raw);

        assertNotNull(msg, "parse khong duoc tra null.");
        assertEquals("LOGIN_OK", msg.type(), "type phai dung.");
        assertEquals("OK", msg.status(), "status phai dung.");
        assertEquals("Success", msg.message(), "message phai dung.");
    }

    /**
     * parse JSON có payload object phải giữ payload nguyên vẹn.
     */
    @Test
    void parse_JsonWithPayload_PayloadNotNull() throws IOException {
        String raw = "{\"type\":\"PLACE_BID\",\"command\":null,"
                + "\"status\":\"OK\",\"payload\":{\"amount\":5000},\"message\":null}";

        JsonMessage msg = JsonProtocol.parse(raw);

        assertNotNull(msg.payload(), "Payload khong duoc null khi JSON co payload.");
    }

    /**
     * parse chuỗi rỗng phải ném IOException.
     */
    @Test
    void parse_EmptyString_ThrowsIoException() {
        assertThrows(IOException.class, () -> JsonProtocol.parse(""),
                "Chuoi rong phai nem IOException.");
    }

    /**
     * parse JSON malformed phải ném IOException.
     */
    @Test
    void parse_MalformedJson_ThrowsIoException() {
        assertThrows(IOException.class, () -> JsonProtocol.parse("{bad json}"),
                "JSON khong hop le phai nem IOException.");
    }

    // =========================================================================
    // stringify
    // =========================================================================

    /**
     * stringify JsonMessage hợp lệ phải trả về chuỗi JSON không rỗng.
     */
    @Test
    void stringify_ValidMessage_ReturnsNonBlankJson() throws Exception {
        JsonMessage msg = new JsonMessage("LOGIN_OK", null, "OK", null, null);

        String json = JsonProtocol.stringify(msg);

        assertNotNull(json, "stringify khong duoc tra null.");
        assertNotNull(json, "Ket qua khong duoc rong.");
    }

    /**
     * stringify rồi parse lại phải cho cùng type và status.
     */
    @Test
    void stringify_ThenParse_RoundTripPreservesFields() throws Exception {
        JsonMessage original = new JsonMessage("UPDATE_PRICE", null, "OK", null, "done");

        String json = JsonProtocol.stringify(original);
        JsonMessage parsed = JsonProtocol.parse(json);

        assertEquals(original.type(), parsed.type(), "type phai giu nguyen sau round-trip.");
        assertEquals(original.status(), parsed.status(),
                "status phai giu nguyen sau round-trip.");
        assertEquals(original.message(), parsed.message(),
                "message phai giu nguyen sau round-trip.");
    }

    // =========================================================================
    // stringifyRequired
    // =========================================================================

    /**
     * stringifyRequired với message hợp lệ phải trả về JSON string.
     */
    @Test
    void stringifyRequired_ValidMessage_ReturnsJson() {
        JsonMessage msg = new JsonMessage("REGISTER_OK", null, "OK", null, null);

        String result = JsonProtocol.stringifyRequired(msg);

        assertNotNull(result, "stringifyRequired khong duoc tra null.");
    }

    // =========================================================================
    // request — không payload
    // =========================================================================

    /**
     * request không payload phải có command đúng và payload null.
     */
    @Test
    void request_WithoutPayload_CommandSetPayloadNull() {
        JsonMessage msg = JsonProtocol.request(Protocol.Command.LOGIN);

        assertEquals(Protocol.Command.LOGIN.name(), msg.command(),
                "command phai dung.");
        assertNull(msg.payload(), "Payload phai null khi khong truyen payload.");
    }

    /**
     * request không payload phải có type null và status null.
     */
    @Test
    void request_WithoutPayload_TypeAndStatusNull() {
        JsonMessage msg = JsonProtocol.request(Protocol.Command.LOGOUT);

        assertNull(msg.type(), "type phai null trong request.");
        assertNull(msg.status(), "status phai null trong request.");
    }

    // =========================================================================
    // request — có payload
    // =========================================================================

    /**
     * request có payload phải giữ payload không null.
     */
    @Test
    void request_WithPayload_PayloadNotNull() {
        java.util.Map<String, Object> data = java.util.Map.of("auctionId", "abc123");

        JsonMessage msg = JsonProtocol.request(Protocol.Command.PLACE_BID, data);

        assertNotNull(msg.payload(), "Payload khong duoc null khi truyen map.");
    }

    /**
     * request với payload null phải tạo message có payload null.
     */
    @Test
    void request_WithNullPayload_PayloadNull() {
        JsonMessage msg = JsonProtocol.request(Protocol.Command.LIST_AUCTIONS, null);

        assertNull(msg.payload(), "Payload phai null khi truyen null.");
    }

    // =========================================================================
    // payloadOf
    // =========================================================================

    /**
     * payloadOf Map phải trả về JsonNode không null.
     */
    @Test
    void payloadOf_Map_ReturnsNonNullNode() {
        java.util.Map<String, String> map = java.util.Map.of("key", "value");

        JsonNode node = JsonProtocol.payloadOf(map);

        assertNotNull(node, "payloadOf khong duoc tra null.");
    }

    /**
     * payloadOf String phải trả về JsonNode chứa giá trị đó.
     */
    @Test
    void payloadOf_StringValue_ReturnsTextNode() {
        JsonNode node = JsonProtocol.payloadOf("hello");

        assertNotNull(node);
        assertEquals("hello", node.asText(), "TextNode phai chua chuoi goc.");
    }

    // =========================================================================
    // payloadAs
    // =========================================================================

    /**
     * payloadAs với payload null phải ném IllegalArgumentException.
     */
    @Test
    void payloadAs_NullPayload_ThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> JsonProtocol.payloadAs(null, String.class),
                "payload null phai nem IllegalArgumentException.");
    }
}