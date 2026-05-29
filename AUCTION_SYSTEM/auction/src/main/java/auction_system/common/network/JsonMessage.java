package auction_system.common.network;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Message wrapper chung cho protocol socket dạng JSON.
 *
 * @param type loại response/notification để client route handler
 * @param command command request để server dispatch
 * @param status trạng thái xử lý, ví dụ OK hoặc FAIL
 * @param payload dữ liệu chính của message
 * @param message thông báo hiển thị hoặc lỗi
 */
public record JsonMessage(
        String type,
        String command,
        String status,
        JsonNode payload,
        String message) {
}
