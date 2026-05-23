package auction_system.server.network.command;

import auction_system.common.network.Protocol;
import auction_system.server.services.AuthService;
import auction_system.server.session.ClientSession;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý lệnh nạp tiền vào ví người dùng.
 *
 * <p>Định dạng lệnh: {@code DEPOSIT|amount}
 */
public class DepositCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(DepositCommand.class);

    private final AuthService authService;

    /**
     * Khởi tạo command nạp tiền.
     *
     * @param authService service xử lý tài khoản
     */
    public DepositCommand(final AuthService authService) {
        this.authService = Objects.requireNonNull(authService, "authService");
    }

    /**
     * Thực thi lệnh nạp tiền.
     *
     * @param parts tham số request đã tách
     * @param session phiên làm việc hiện tại
     * @return response gửi về client
     */
    @Override
    public String execute(final String[] parts, final ClientSession session) {
        try {
            if (!session.isLoggedIn()) {
                return buildFailResponse("Bạn cần đăng nhập trước.");
            }

            if (parts.length < 2) {
                return buildFailResponse("Thiếu số tiền cần nạp.");
            }

            double amount = parseAmount(parts[1]);
            double newBalance = authService.deposit(session.getCurrentUser(), amount);

            return Protocol.Response.DEPOSIT_OK.name()
                    + Protocol.SEPARATOR
                    + newBalance;
        } catch (NumberFormatException e) {
            return buildFailResponse("Số tiền không hợp lệ.");
        } catch (IllegalArgumentException e) {
            return buildFailResponse(e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.error("Lỗi hệ thống khi xử lý nạp tiền.", e);
            return buildFailResponse("Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
        }
    }

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
        return Protocol.Response.DEPOSIT_FAIL.name()
                + Protocol.SEPARATOR
                + message;
    }
}
