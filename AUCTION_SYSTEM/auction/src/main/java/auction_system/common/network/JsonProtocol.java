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
     * Tạo request message không payload cho command.
     *
     * @param command command cần gửi
     * @return message request JSON
     */
    public static JsonMessage request(final Protocol.Command command) {
        return request(command, null);
    }

    /**
     * Tạo request message có payload cho command.
     *
     * @param command command cần gửi
     * @param payload object payload, hoặc null nếu request không có payload
     * @return message request JSON
     */
    public static JsonMessage request(final Protocol.Command command, final Object payload) {
        return new JsonMessage(
                null,
                command.name(),
                null,
                payload == null ? null : payloadOf(payload),
                null);
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

    /**
     * Map JSON payload tree thành DTO/record request.
     *
     * @param payload payload JSON nhận từ socket
     * @param payloadType class DTO đích
     * @param <T> kiểu DTO đích
     * @return DTO đã map
     * @throws IllegalArgumentException nếu payload null hoặc không map được sang DTO
     */
    public static <T> T payloadAs(
            final JsonNode payload,
            final Class<T> payloadType) {
        if (payload == null || payload.isNull()) {
            throw new IllegalArgumentException("Payload không hợp lệ.");
        }

        try {
            return OBJECT_MAPPER.treeToValue(payload, payloadType);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Payload không đúng định dạng.", exception);
        }
    }
}
