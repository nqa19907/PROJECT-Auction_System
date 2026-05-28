package auction_system.common.models.auctions;

import auction_system.common.models.users.Participant;
import java.time.LocalDateTime;

/**
 * Cấu hình đấu giá tự động của một participant trong một phiên đấu giá.
 *
 * <p>Đây không phải là một lượt bid thật. Nó chỉ lưu luật server sẽ dùng để tự
 * tạo bid thay người dùng khi giá hiện tại bị người khác vượt qua.
 */
public class AutoBidSetting extends Entity {

    private final String auctionId;
    private final Participant participant;
    private final long maxAmount;
    private final long stepAmount;
    private final LocalDateTime createdAt;
    private boolean active;

    /**
     * Tạo cấu hình auto-bid mới.
     *
     * @param auctionId mã phiên đấu giá
     * @param participant người bật auto-bid
     * @param maxAmount giá tối đa người dùng sẵn sàng trả
     * @param stepAmount bước tăng mỗi lần bị vượt giá
     */
    public AutoBidSetting(
            final String auctionId,
            final Participant participant,
            final long maxAmount,
            final long stepAmount) {

        super();
        this.auctionId = auctionId;
        this.participant = participant;
        this.maxAmount = maxAmount;
        this.stepAmount = stepAmount;
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public Participant getParticipant() {
        return participant;
    }

    public long getMaxAmount() {
        return maxAmount;
    }

    public long getStepAmount() {
        return stepAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }
}