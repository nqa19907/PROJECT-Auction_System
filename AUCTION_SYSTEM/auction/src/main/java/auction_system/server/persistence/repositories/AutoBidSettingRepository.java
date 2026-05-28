package auction_system.server.persistence.repositories;

import auction_system.common.models.auctions.AutoBidSetting;
import auction_system.server.persistence.exceptions.DatabaseException;
import auction_system.server.persistence.serialization.SerializedFileStorage;
import auction_system.server.persistence.serialization.SerializedRepository;
import auction_system.server.services.autobid.AutoBidSettingStore;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Repository quản lý cấu hình auto-bid đã lưu của participant.
 */
public class AutoBidSettingRepository extends SerializedRepository<AutoBidSetting>
        implements AutoBidSettingStore {

    /**
     * Khởi tạo repository cấu hình auto-bid với đường dẫn file lưu trữ.
     *
     * @param storagePath đường dẫn tới file auto_bid_settings.ser
     */
    public AutoBidSettingRepository(final Path storagePath) {
        super(new SerializedFileStorage<>(storagePath), AutoBidSetting::getId);
    }

    /**
     * Lưu mới hoặc cập nhật cấu hình auto-bid.
     *
     * @param setting cấu hình auto-bid cần lưu
     * @return cấu hình đã được lưu
     */
    @Override
    public synchronized AutoBidSetting save(final AutoBidSetting setting) {
        Objects.requireNonNull(setting, "setting");

        validateSetting(setting);

        return super.save(setting);
    }

    /**
     * Tìm danh sách cấu hình auto-bid theo mã phiên đấu giá.
     *
     * @param auctionId mã phiên đấu giá
     * @return danh sách cấu hình thuộc phiên đó
     */
    public List<AutoBidSetting> findByAuctionId(final String auctionId) {
        validateText(auctionId, "Mã phiên đấu giá không được rỗng.");

        return findAll().stream()
                .filter(setting -> auctionId.equals(setting.getAuctionId()))
                .toList();
    }

    private void validateSetting(final AutoBidSetting setting) {
        validateText(setting.getId(), "Mã cấu hình auto-bid không được rỗng.");
        validateText(setting.getAuctionId(), "Mã phiên đấu giá không được rỗng.");

        if (setting.getParticipant() == null) {
            throw new DatabaseException("Người bật auto-bid không được null.");
        }

        validateText(
                setting.getParticipant().getId(),
                "Mã người bật auto-bid không được rỗng.");

        if (setting.getMaxAmount() <= 0) {
            throw new DatabaseException("Giá tối đa auto-bid phải là số dương.");
        }

        if (setting.getStepAmount() <= 0) {
            throw new DatabaseException("Bước tăng auto-bid phải là số dương.");
        }

        // createdAt được dùng để tie-break ưu tiên giữa các auto-bidder.
        if (setting.getCreatedAt() == null) {
            throw new DatabaseException("Thời điểm tạo auto-bid không được null.");
        }
    }

    private void validateText(final String value, final String message) {
        if (value == null || value.isBlank()) {
            throw new DatabaseException(message);
        }
    }
}
