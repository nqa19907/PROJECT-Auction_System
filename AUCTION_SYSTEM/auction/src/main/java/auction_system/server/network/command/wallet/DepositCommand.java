package auction_system.server.network.command.wallet;

import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.network.command.JsonPayloadCommand;
import auction_system.server.network.payload.wallet.DepositPayload;
import auction_system.server.services.auth.AuthService;
import auction_system.server.services.bidding.AuctionBidService;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý lệnh nạp tiền vào ví người dùng.
 *
 * <p>Lệnh JSON {@code DEPOSIT} nhận số tiền trong payload.
 */
public class DepositCommand implements JsonPayloadCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DepositCommand.class);

    private final AuthService authService;
    private final AuctionBidService auctionBidService;

    /**
     * Khởi tạo command nạp tiền.
     *
     * @param authService service xử lý tài khoản
     * @param auctionBidService service xử lý retry auto-bid sau khi ví thay đổi
     */
    public DepositCommand(
            final AuthService authService,
            final AuctionBidService auctionBidService) {
        this.authService = Objects.requireNonNull(authService, "authService");
        this.auctionBidService = Objects.requireNonNull(auctionBidService, "auctionBidService");
    }

    /**
     * Thực thi lệnh nạp tiền.
     *
     * @param payload payload JSON của request
     * @param session phiên làm việc hiện tại
     * @return response gửi về client
     */
    @Override
    public String execute(final JsonNode payload, final ClientSession session) {
        try {
            // Kiểm tra trạng thái đăng nhập. Chỉ user đã đăng nhập mới được nạp tiền.
            if (!session.isLoggedIn()) {
                return buildFailResponse("Bạn cần đăng nhập trước.");
            }

            final DepositPayload depositPayload;
            try {
                depositPayload = JsonProtocol.payloadAs(payload, DepositPayload.class);
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("Không map được payload nạp tiền: {}", exception.getMessage());
                return buildFailResponse("Thiếu số tiền cần nạp.");
            }

            if (depositPayload.hasMissingAmount()) {
                return buildFailResponse("Thiếu số tiền cần nạp.");
            }

            // Parse và validate số tiền từ chuỗi text
            double amount = parseAmount(depositPayload.amount());
            
            // Giữ lại user trong session để retry auto-bid bằng object đã được nạp tiền.
            final User currentUser = session.getCurrentUser();

            // Gọi AuthService để cập nhật số dư của user hiện tại.
            authService.deposit(currentUser, amount);

            // Sau khi ví tăng, thử lại các auto-bid active từng bị bỏ qua vì thiếu tiền.
            auctionBidService.triggerAutoBidsAfterBalanceChange(currentUser);

            // Lấy số dư cuối cùng vì auto-bid sau nạp tiền có thể đã giữ bớt tiền.
            final double finalBalance = ((Participant) currentUser).getBalance();

            // Trả về kết quả thành công cùng số dư mới
            return buildSuccessResponse(finalBalance);
        } catch (NumberFormatException e) {
            return buildFailResponse("Số tiền không hợp lệ.");
        } catch (IllegalArgumentException e) {
            return buildFailResponse(e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.error("Lỗi hệ thống khi xử lý nạp tiền.", e);
            return buildFailResponse("Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
        }
    }

    private String buildSuccessResponse(final double balance) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.DEPOSIT_OK.name(),
                            null,
                            "OK",
                            JsonProtocol.payloadOf(Map.of("balance", balance)),
                            "Nạp tiền thành công."));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response nạp tiền: {}", exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON DEPOSIT_OK.", exception);
        }
    }

    /**
     * Chuyển đổi và validate chuỗi số tiền nạp.
     *
     * @param rawAmount chuỗi số tiền từ client
     * @return số tiền (double) hợp lệ
     * @throws NumberFormatException nếu chuỗi không phải số
     * @throws IllegalArgumentException nếu số tiền <= 0
     */
    private double parseAmount(final String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            throw new NumberFormatException("Số tiền rỗng.");
        }

        double amount = Double.parseDouble(rawAmount);
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0.");
        }

        return amount;
    }

    private String buildFailResponse(final String message) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.DEPOSIT_FAIL.name(),
                            null,
                            "FAIL",
                            null,
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON lỗi nạp tiền: {}", exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON DEPOSIT_FAIL.", exception);
        }
    }
}
