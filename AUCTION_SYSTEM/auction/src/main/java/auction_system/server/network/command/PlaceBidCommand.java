package auction_system.server.network.command;

import auction_system.common.exceptions.AuctionClosedException;
import auction_system.common.exceptions.InvalidBidException;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.server.persistence.exceptions.DatabaseException;
import auction_system.server.services.AuctionBidService;
import auction_system.server.session.ClientSession;
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
 */
public class PlaceBidCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceBidCommand.class);

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

            if (parts.length < 3) {
                return buildBidFailResponse("Thiếu thông tin đặt giá.");
            }

            String auctionId = parts[1];
            double amount = parseAmount(parts[2]);
            User currentUser = session.getCurrentUser();

            BidTransaction bidTransaction =
                    auctionBidService.placeBid(auctionId, currentUser, amount);

            return Protocol.Response.BID_OK.name()
                    + Protocol.SEPARATOR
                    + auctionId
                    + Protocol.SEPARATOR
                    + bidTransaction.getAmount();
        } catch (NumberFormatException exception) {
            return buildBidFailResponse("Số tiền không hợp lệ.");
        } catch (AuctionClosedException
                | InvalidBidException
                | DatabaseException exception) {
            return buildBidFailResponse(exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.log(
                    Level.SEVERE,
                    "Lỗi hệ thống khi xử lý lệnh đặt giá.",
                    exception);

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
        return Protocol.Response.ERROR.name()
                + Protocol.SEPARATOR
                + message;
    }

    /**
     * Tạo response đặt giá thất bại.
     *
     * @param message thông báo lỗi
     * @return response theo protocol
     */
    private String buildBidFailResponse(final String message) {
        return Protocol.Response.BID_FAIL.name()
                + Protocol.SEPARATOR
                + message;
    }
}