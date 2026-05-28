package auction_system.server.services.autobid;

import auction_system.common.models.auctions.AutoBidSetting;
import java.util.List;

/**
 * Port lưu trữ cấu hình auto-bid cho tầng service.
 *
 * <p>AutoBidService chỉ cần các thao tác theo nghiệp vụ auto-bid, không cần
 * biết implementation phía dưới đang lưu bằng serialization hay cách khác.
 */
public interface AutoBidSettingStore {

    /**
     * Lưu mới hoặc cập nhật cấu hình auto-bid.
     *
     * @param setting cấu hình auto-bid cần lưu
     * @return cấu hình đã được lưu
     */
    AutoBidSetting save(AutoBidSetting setting);

    /**
     * Lấy toàn bộ cấu hình auto-bid đã lưu.
     *
     * @return danh sách cấu hình auto-bid
     */
    List<AutoBidSetting> findAll();

    /**
     * Tìm danh sách cấu hình auto-bid theo mã phiên đấu giá.
     *
     * @param auctionId mã phiên đấu giá
     * @return danh sách cấu hình thuộc phiên đó
     */
    List<AutoBidSetting> findByAuctionId(String auctionId);

    /**
     * Xóa cấu hình auto-bid theo mã định danh.
     *
     * @param id mã cấu hình auto-bid
     * @return true nếu có xóa, false nếu không tìm thấy
     */
    boolean deleteById(String id);
}
