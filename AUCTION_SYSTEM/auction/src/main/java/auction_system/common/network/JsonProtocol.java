package auction_system.common.network;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

/**
 * Utility chung cho protocol socket dạng JSON.
 */
public final class JsonProtocol {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonProtocol() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Parse raw JSON thành {@link JsonMessage}.
     *
     * @param rawJson dòng JSON nhận qua socket
     * @return message đã parse
     * @throws IOException nếu JSON không hợp lệ
     */
    public static JsonMessage parse(final String rawJson) throws IOException {
        return OBJECT_MAPPER.readValue(rawJson, JsonMessage.class);
    }

    /**
     * Serialize {@link JsonMessage} thành một dòng JSON.
     *
     * @param message message cần gửi qua socket
     * @return JSON một dòng
     * @throws JsonProcessingException nếu message không serialize được
     */
    public static String stringify(final JsonMessage message) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(message);
    }

    /**
     * Serialize message hoặc dừng xử lý nếu JSON không thể tạo được.
     *
     * <p>Socket protocol chỉ hỗ trợ JSON; không fallback về text protocol cũ.
     *
     * @param message message cần gửi qua socket
     * @return JSON một dòng
     */
    public static String stringifyRequired(final JsonMessage message) {
        try {
            return stringify(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không thể tạo JSON message.", exception);
        }
    }

    /**
     * Chuyển object payload thành {@link JsonNode}.
     *
     * @param payload object payload
     * @return JSON tree tương ứng
     */
    public static JsonNode payloadOf(final Object payload) {
        return OBJECT_MAPPER.valueToTree(payload);
    }
}
