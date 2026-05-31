package auction_system.server.network.payload;

/**
 * Payload JSON cho request chỉ cần mã người dùng.
 */
public record UserIdPayload(String userId) {

    /**
     * Kiểm tra payload thiếu mã người dùng.
     *
     * @return true nếu thiếu mã người dùng
     */
    public boolean hasMissingUserId() {
        return userId == null || userId.isBlank();
    }
}
