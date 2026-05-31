package auction_system.server.network.payload.bidding;

/**
 * Payload JSON cho request cập nhật phiên đấu giá của user hiện tại.
 */
public record UpdateMyAuctionPayload(
        String auctionId,
        String category,
        String itemName,
        String description,
        String condition,
        String endTime,
        String imagePath) {

    /**
     * Kiểm tra payload thiếu dữ liệu bắt buộc.
     *
     * @return true nếu thiếu dữ liệu cập nhật phiên
     */
    public boolean hasMissingRequiredFields() {
        // imagePath là tùy chọn; các field còn lại cần đủ để cập nhật phiên.
        return isBlank(auctionId)
                || isBlank(category)
                || isBlank(itemName)
                || isBlank(description)
                || isBlank(condition)
                || isBlank(endTime);
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
