package auction_system.client.services;

import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Service lưu trạng thái phiên đăng nhập hiện tại ở phía client.
 *
 * <p>Đây là nguồn dữ liệu chung cho thông tin user/balance, giúp các service
 * nghiệp vụ không phải tự mutate model người dùng theo cách riêng lẻ.
 *
 * <p>Mục đích chính của JavaFX property ở đây là cho UI bind/listen vào state
 * phiên đăng nhập. Khi service cập nhật số dư sau bid hoặc nạp tiền, các màn
 * hình như Profile có thể tự refresh mà không cần service gọi trực tiếp vào
 * controller.
 *
 * <p>Property nội bộ vẫn được giữ private để quyền cập nhật tập trung ở các
 * method nghiệp vụ như {@link #setCurrentUser(User)} và
 * {@link #updateCurrentUserBalance(double)}.
 */
public final class UserSessionService {
    private static final UserSessionService INSTANCE = new UserSessionService();

    // ObjectProperty phát tín hiệu khi user đăng nhập/đăng xuất thay đổi.
    private final ObjectProperty<User> currentUser = new SimpleObjectProperty<>();

    // DoubleProperty phát tín hiệu để ProfileController tự cập nhật label số dư.
    private final DoubleProperty currentBalance = new SimpleDoubleProperty(0);

    private UserSessionService() {
    }

    public static UserSessionService getInstance() {
        return INSTANCE;
    }

    /**
     * Thiết lập user hiện tại sau khi đăng nhập thành công.
     *
     * <p>Nếu user là participant, balance property được đồng bộ từ model để UI
     * đang listen/bind nhận đúng số dư ban đầu.
     *
     * @param user user hiện tại của phiên đăng nhập
     */
    public void setCurrentUser(final User user) {
        currentUser.set(user);

        if (user instanceof Participant participant) {
            // Set property để các listener UI nhận được số dư ban đầu sau login.
            currentBalance.set(participant.getBalance());
            return;
        }

        // User không có ví, ví dụ Admin, thì balance chung được reset về 0.
        currentBalance.set(0);
    }

    /**
     * Xóa trạng thái phiên hiện tại khi đăng xuất.
     */
    public void clearSession() {
        currentUser.set(null);
        currentBalance.set(0);
    }

    public User getCurrentUser() {
        return currentUser.get();
    }

    /**
     * Cập nhật số dư hiện tại từ phản hồi server.
     *
     * <p>Server là nguồn sự thật cho số dư vì các nghiệp vụ như bid có thể
     * vừa hoàn tiền bid cũ vừa giữ tiền bid mới trong cùng transaction.
     *
     * @param newBalance số dư mới do server trả về
     */
    public void updateCurrentUserBalance(final double newBalance) {
        User user = currentUser.get();
        if (user instanceof Participant participant) {
            // Model giữ dữ liệu; property phát tín hiệu để UI bind/listen tự refresh.
            participant.setBalance(newBalance);
            currentBalance.set(newBalance);
        }
    }

    /**
     * Property user hiện tại cho các lớp cần bind/listen trạng thái đăng nhập.
     *
     * @return property chỉ đọc của user hiện tại
     */
    public ReadOnlyObjectProperty<User> currentUserProperty() {
        // Trả read-only property để lớp khác observe, không tự ý set user.
        return currentUser;
    }

    /**
     * Property số dư hiện tại cho UI bind/listen và tự cập nhật khi balance đổi.
     *
     * @return property chỉ đọc của số dư hiện tại
     */
    public ReadOnlyDoubleProperty currentBalanceProperty() {
        // Trả read-only property để UI bind/listen, còn update phải đi qua method phía trên.
        return currentBalance;
    }
}
