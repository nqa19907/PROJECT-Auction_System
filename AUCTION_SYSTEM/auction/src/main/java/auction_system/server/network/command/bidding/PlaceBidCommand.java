package auction_system.server.network.command.bidding;

import auction_system.common.exceptions.AuctionClosedException;
import auction_system.common.exceptions.InvalidBidException;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.network.command.Command;
import auction_system.server.persistence.exceptions.DatabaseException;
import auction_system.server.services.bidding.AuctionBidService;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý lệnh đặt giá từ client.
 *
 * <p>Command này chỉ chịu trách nhiệm đọc request, kiểm tra dữ liệu đầu vào ở
 * mức giao thức và trả response cho client. Logic đặt giá thật sự thuộc về
 * {@link AuctionBidService}.
 *
 * <p>Định dạng lệnh:
 * {@code PLACE_BID|auctionId|amount}
 *
 * <p>Phản hồi thành công:
 * {@code BID_OK|auctionId|amount|newBalance}
 */
public class PlaceBidCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceBidCommand.class);
    private static final int MIN_PLACE_BID_PARTS = 3;
    private static final int IDX_REQUEST_AUCTION_ID = 1;
    private static final int IDX_REQUEST_AMOUNT = 2;

    /** Service xử lý nghiệp vụ đặt giá. */
    private final AuctionBidService auctionBidService;

    /**
     * Khởi tạo command đặt giá.
     *
     * @param auctionBidService service xử lý nghiệp vụ đặt giá
     */
    public PlaceBidCommand(final AuctionBidService auctionBidService) {
        this.auctionBidService =
                Objects.requireNonNull(auctionBidService, "auctionBidService");
    }

    /**
     * Thực thi lệnh đặt giá từ client.
     *
     * @param parts   mảng tham số đã tách từ request
     * @param session phiên làm việc của client
     * @return response gửi lại cho client
     */
    @Override
    public String execute(final String[] parts, final ClientSession session) {
        try {
            if (!session.isLoggedIn()) {
                return buildErrorResponse("Bạn cần đăng nhập trước.");
            }

            if (parts.length < MIN_PLACE_BID_PARTS) {
                return buildBidFailResponse("Thiếu thông tin đặt giá.");
            }

            String auctionId = parts[IDX_REQUEST_AUCTION_ID];
            double amount = parseAmount(parts[IDX_REQUEST_AMOUNT]);
            User currentUser = session.getCurrentUser();

            BidTransaction bidTransaction =
                    auctionBidService.placeBid(auctionId, currentUser, amount);
            Participant bidder = bidTransaction.getParticipant();

            return buildSuccessResponse(
                    auctionId,
                    bidTransaction.getAmount(),
                    bidder.getBalance());
        } catch (NumberFormatException e) {
            return buildBidFailResponse("Số tiền không hợp lệ.");
        } catch (AuctionClosedException
                | InvalidBidException
                | DatabaseException e) {
            return buildBidFailResponse(e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.info("Lỗi hệ thống khi xử lý lệnh đặt giá.", e);

            return buildBidFailResponse(
                    "Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
        }
    }

    /**
     * Chuyển chuỗi số tiền client gửi lên thành số thực.
     *
     * @param rawAmount chuỗi số tiền
     * @return số tiền đặt giá
     */
    private double parseAmount(final String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            throw new NumberFormatException("Số tiền rỗng.");
        }

        double amount = Double.parseDouble(rawAmount);

        if (amount <= 0) {
            throw new InvalidBidException("Số tiền đặt giá phải lớn hơn 0.");
        }

        return amount;
    }

    /**
     * Tạo response lỗi chung.
     *
     * @param message thông báo lỗi
     * @return response theo protocol
     */
    private String buildErrorResponse(final String message) {
        return buildFailureLikeResponse(Protocol.Response.ERROR.name(), message);
    }

    /**
     * Tạo response đặt giá thất bại.
     *
     * @param message thông báo lỗi
     * @return response theo protocol
     */
    private String buildBidFailResponse(final String message) {
        return buildFailureLikeResponse(Protocol.Response.BID_FAIL.name(), message);
    }

    private String buildSuccessResponse(
            final String auctionId,
            final double amount,
            final double newBalance) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.BID_OK.name(),
                            null,
                            "OK",
                            JsonProtocol.payloadOf(Map.of(
                                    "auctionId", auctionId,
                                    "amount", amount,
                                    "newBalance", newBalance)),
                            "Đặt giá thành công."));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response đặt giá: {}", exception.getMessage());
            return Protocol.Response.BID_OK.name()
                    + Protocol.SEPARATOR
                    + auctionId
                    + Protocol.SEPARATOR
                    + amount
                    + Protocol.SEPARATOR
                    + newBalance;
        }
    }

    private String buildFailureLikeResponse(final String type, final String message) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            type,
                            null,
                            "FAIL",
                            null,
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON lỗi đặt giá: {}", exception.getMessage());
            return type
                    + Protocol.SEPARATOR
                    + message;
        }
    }
}
