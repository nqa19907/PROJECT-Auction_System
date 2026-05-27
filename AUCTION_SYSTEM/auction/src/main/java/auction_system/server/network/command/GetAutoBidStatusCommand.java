package auction_system.server.network.command;

import auction_system.common.models.auctions.AutoBidSetting;
import auction_system.common.models.users.Participant;
import auction_system.common.network.Protocol;
import auction_system.server.services.AutoBidService;
import auction_system.server.session.ClientSession;
import java.util.Objects;
import java.util.Optional;

/**
 * Command trả trạng thái auto-bid hiện tại của user trong một phiên đấu giá.
 */
public final class GetAutoBidStatusCommand implements Command {

    private static final int MIN_GET_AUTO_BID_PARTS = 2;
    private static final int IDX_AUCTION_ID = 1;

    private final AutoBidService autoBidService;

    /**
     * Khởi tạo command lấy trạng thái auto-bid.
     *
     * @param autoBidService service quản lý cấu hình auto-bid
     */
    public GetAutoBidStatusCommand(final AutoBidService autoBidService) {
        this.autoBidService = Objects.requireNonNull(autoBidService, "autoBidService");
    }

    @Override
    public String execute(final String[] parts, final ClientSession session) {
        if (!(session.getCurrentUser() instanceof Participant participant)) {
            return disabledStatus();
        }

        if (parts.length < MIN_GET_AUTO_BID_PARTS) {
            return disabledStatus();
        }

        final String auctionId = parts[IDX_AUCTION_ID];
        final Optional<AutoBidSetting> setting =
                autoBidService.findSetting(auctionId, participant.getId());

        if (setting.isEmpty() || !setting.get().isActive()) {
            return disabledStatus();
        }

        final AutoBidSetting activeSetting = setting.get();
        return Protocol.Response.AUTO_BID_STATUS.name()
                + Protocol.SEPARATOR
                + "ENABLED"
                + Protocol.SEPARATOR
                + activeSetting.getMaxAmount()
                + Protocol.SEPARATOR
                + activeSetting.getStepAmount();
    }

    private String disabledStatus() {
        return Protocol.Response.AUTO_BID_STATUS.name()
                + Protocol.SEPARATOR
                + "DISABLED";
    }
}
