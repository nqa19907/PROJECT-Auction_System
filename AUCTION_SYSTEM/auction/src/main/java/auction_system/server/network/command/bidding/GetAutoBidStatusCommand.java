package auction_system.server.network.command.bidding;

import auction_system.common.models.auctions.AutoBidSetting;
import auction_system.common.models.users.Participant;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.network.command.JsonPayloadCommand;
import auction_system.server.services.autobid.AutoBidService;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command trả trạng thái auto-bid hiện tại của user trong một phiên đấu giá.
 */
public final class GetAutoBidStatusCommand implements JsonPayloadCommand {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GetAutoBidStatusCommand.class);

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
    public String execute(final JsonNode payload, final ClientSession session) {
        if (!(session.getCurrentUser() instanceof Participant participant)) {
            return disabledStatus();
        }

        final String auctionId = readAuctionId(payload);
        if (auctionId.isBlank()) {
            return disabledStatus();
        }

        final Optional<AutoBidSetting> setting =
                autoBidService.findSetting(auctionId, participant.getId());

        if (setting.isEmpty() || !setting.get().isActive()) {
            return disabledStatus();
        }

        final AutoBidSetting activeSetting = setting.get();
        return enabledStatus(activeSetting.getMaxAmount(), activeSetting.getStepAmount());
    }

    private String readAuctionId(final JsonNode payload) {
        if (payload == null) {
            return "";
        }
        return payload.path("auctionId").asText("");
    }

    private String disabledStatus() {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.AUTO_BID_STATUS.name(),
                            null,
                            "OK",
                            JsonProtocol.payloadOf(Map.of("enabled", false)),
                            null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON trạng thái tắt auto-bid: {}",
                    exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON AUTO_BID_STATUS.", exception);
        }
    }

    private String enabledStatus(final long maxAmount, final long stepAmount) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.AUTO_BID_STATUS.name(),
                            null,
                            "OK",
                            JsonProtocol.payloadOf(Map.of(
                                    "enabled", true,
                                    "maxAmount", maxAmount,
                                    "stepAmount", stepAmount)),
                            null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON trạng thái bật auto-bid: {}",
                    exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON AUTO_BID_STATUS.", exception);
        }
    }
}
