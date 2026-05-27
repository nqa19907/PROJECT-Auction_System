package auction_system.client.controllers.auction.components;

import auction_system.client.network.NetworkClient;
import auction_system.client.services.AuctionService;
import auction_system.common.network.Protocol;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

/**
 * Điều phối checkbox bật/tắt tự động gia hạn phiên khi có bid phút chót.
 */
public final class AuctionAntiSnipingControl {

    private static final int MIN_UPDATE_PARTS = 3;
    private static final int IDX_UPDATE_AUCTION_ID = 1;
    private static final int IDX_UPDATE_ENABLED = 2;
    private static final int MIN_FAIL_PARTS = 2;
    private static final int IDX_FAIL_MESSAGE = 1;

    private final CheckBox checkbox;
    private final Label errorLabel;
    private final Supplier<String> activeAuctionIdSupplier;
    private final Consumer<String> updatedHandler = this::handleUpdated;
    private final Consumer<String> updateFailHandler = this::handleUpdateFail;
    private boolean sellerObserveOnly;
    private boolean syncing;

    /**
     * Khởi tạo control cấu hình gia hạn phiên phút chót.
     *
     * @param checkbox checkbox bật/tắt tự động gia hạn khi có bid phút chót
     * @param errorLabel label lỗi chung của màn chi tiết
     * @param activeAuctionIdSupplier supplier trả về mã phiên hiện tại
     */
    public AuctionAntiSnipingControl(
            final CheckBox checkbox,
            final Label errorLabel,
            final Supplier<String> activeAuctionIdSupplier) {

        this.checkbox = checkbox;
        this.errorLabel = errorLabel;
        this.activeAuctionIdSupplier = activeAuctionIdSupplier;
    }

    /**
     * Đăng ký listener gửi request khi seller thao tác checkbox.
     */
    public void registerHandlers() {
        // Chỉ thao tác trực tiếp của seller mới gửi request.
        // Update từ server được chặn bằng cờ syncing.
        checkbox.selectedProperty().addListener((obs, oldValue, selected) ->
                handleToggleChanged(selected));
    }

    /**
     * Đăng ký socket handler nhận kết quả cập nhật cấu hình gia hạn phút chót.
     */
    public void registerNetworkHandlers() {
        // Control tự sở hữu handler gia hạn phút chót để controller không phải parse response này.
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ANTI_SNIPING_UPDATED.name(),
                updatedHandler);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ANTI_SNIPING_UPDATE_FAIL.name(),
                updateFailHandler);
    }

    /**
     * Gỡ socket handler của control này khi rời màn chi tiết.
     */
    public void unregisterNetworkHandlers() {
        NetworkClient.getInstance().unregisterHandler(
                Protocol.Response.ANTI_SNIPING_UPDATED.name(),
                updatedHandler);
        NetworkClient.getInstance().unregisterHandler(
                Protocol.Response.ANTI_SNIPING_UPDATE_FAIL.name(),
                updateFailHandler);
    }

    /**
     * Áp dụng trạng thái ban đầu theo dữ liệu phiên và quyền của user hiện tại.
     *
     * @param enabled true nếu phiên đang bật tự động gia hạn phút chót
     * @param sellerObserveOnly true nếu user hiện tại là người bán của phiên
     */
    public void applyInitialState(final boolean enabled, final boolean sellerObserveOnly) {
        // Người bán được chỉnh cấu hình gia hạn, bidder chỉ nhìn trạng thái và không thao tác.
        this.sellerObserveOnly = sellerObserveOnly;
        syncCheckbox(enabled);
        checkbox.setDisable(!sellerObserveOnly);
    }

    /**
     * Xử lý broadcast cập nhật trạng thái tự động gia hạn từ server.
     *
     * @param response thông báo ANTI_SNIPING_UPDATED
     */
    public void handleUpdated(final String response) {
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX, -1);
        final String activeAuctionId = activeAuctionIdSupplier.get();
        if (parts.length < MIN_UPDATE_PARTS
                || activeAuctionId == null
                || !activeAuctionId.equals(parts[IDX_UPDATE_AUCTION_ID])) {
            // Bỏ qua broadcast sai format hoặc không thuộc phiên đang mở.
            return;
        }

        // Server là nguồn sự thật cuối cùng cho trạng thái checkbox.
        syncCheckbox(Boolean.parseBoolean(parts[IDX_UPDATE_ENABLED]));
        checkbox.setDisable(!sellerObserveOnly);
    }

    /**
     * Xử lý response lỗi khi server từ chối đổi trạng thái chống sniping.
     *
     * @param response thông báo ANTI_SNIPING_UPDATE_FAIL
     */
    public void handleUpdateFail(final String response) {
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX, -1);
        // Response lỗi có thể thiếu message, nên dùng thông báo mặc định khi cần.
        final String message = parts.length >= MIN_FAIL_PARTS
                ? parts[IDX_FAIL_MESSAGE]
                : "Không thể cập nhật tự động gia hạn phút chót.";

        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        // Hoàn checkbox về trạng thái trước khi user thao tác vì server đã từ chối request.
        syncCheckbox(!checkbox.isSelected());
        checkbox.setDisable(!sellerObserveOnly);
    }

    private void handleToggleChanged(final boolean enabled) {
        final String activeAuctionId = activeAuctionIdSupplier.get();
        if (syncing || !sellerObserveOnly || activeAuctionId == null) {
            // Không gửi request khi đang đồng bộ từ server, không phải seller, hoặc chưa có phiên.
            return;
        }

        // Khóa checkbox trong lúc chờ server xác nhận để tránh gửi nhiều trạng thái liên tiếp.
        checkbox.setDisable(true);
        AuctionService.getInstance().setAntiSniping(activeAuctionId, enabled);
    }

    private void syncCheckbox(final boolean enabled) {
        // Cờ syncing ngăn listener coi cập nhật từ server là thao tác mới của người dùng.
        syncing = true;
        checkbox.setSelected(enabled);
        syncing = false;
    }
}
