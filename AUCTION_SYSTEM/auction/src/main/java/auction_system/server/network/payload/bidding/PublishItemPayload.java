package auction_system.server.network.payload.bidding;

/**
 * Payload JSON cho request đăng bán sản phẩm.
 */
public record PublishItemPayload(
        String category,
        String itemName,
        String description,
        String condition,
        String startPrice,
        String startTime,
        String endTime,
        String imagePath,
        Boolean antiSnipingEnabled) {

    /**
     * Kiểm tra payload thiếu dữ liệu bắt buộc.
     *
     * @return true nếu thiếu dữ liệu đăng bán sản phẩm
     */
    public boolean hasMissingRequiredFields() {
        // imagePath và antiSnipingEnabled là tùy chọn; các field còn lại bắt buộc.
        return isBlank(category)
                || isBlank(itemName)
                || isBlank(description)
                || isBlank(condition)
                || isBlank(startPrice)
                || isBlank(startTime)
                || isBlank(endTime);
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
