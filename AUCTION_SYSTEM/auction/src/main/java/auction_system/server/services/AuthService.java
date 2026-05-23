package auction_system.server.services;

import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.common.utils.SecurityUtils;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service xử lý xác thực tài khoản ở phía server.
 *
 * <p>Lớp này chỉ phụ trách nghiệp vụ đăng nhập, đăng ký và kiểm tra tài khoản
 * với database serialization. Lớp này không phụ thuộc JavaFX, NetworkClient
 * hoặc bất kỳ class nào thuộc tầng client.
 */
public final class AuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private static final double defaultBalance = 0.0;
    private static final int minimumPasswordLength = 6;

    private final SerializedDatabase database;

    /**
     * Khởi tạo service xác thực bằng database dùng chung của server.
     *
     * @param database database serialization của server
     */
    public AuthService(final SerializedDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    /**
     * Xác thực đăng nhập bằng email và mật khẩu.
     *
     * <p>Phương thức này chỉ kiểm tra thông tin tài khoản. Trạng thái online,
     * logout và session runtime vẫn nên do ClientSession hoặc AuctionManager
     * quản lý vì đó là trạng thái của kết nối socket.
     *
     * @param email email người dùng gửi từ client
     * @param password mật khẩu dạng plaintext người dùng gửi từ client
     * @return người dùng nếu đăng nhập hợp lệ
     * @throws IllegalArgumentException nếu email hoặc mật khẩu rỗng
     */
    public Optional<User> login(final String email, final String password) {
        validateText(email, "Email không được rỗng.");
        validateText(password, "Mật khẩu không được rỗng.");

        final String normalizedEmail = email.trim();
        return database.executeInTransaction(
            () -> findValidUser(normalizedEmail, password)
        );
    
    }

    /**
     * Đăng ký tài khoản mới.
     *
     * <p>Service sẽ hash mật khẩu trước khi lưu xuống database. Command chỉ nên
     * đọc request, gọi service và trả response cho client.
     *
     * @param username tên đăng nhập
     * @param email email đăng nhập
     * @param password mật khẩu plaintext từ client
     * @param roleName vai trò tài khoản
     * @return người dùng đã được tạo và lưu
     * @throws IllegalArgumentException nếu dữ liệu đăng ký không hợp lệ
     */
    public User register(
        final String username,
        final String email,
        final String password,
        final String roleName) {

        validateRegisterRequest(username, email, password, roleName);

        final String normalizedUsername = username.trim();
        final String normalizedEmail = email.trim();
        final String normalizedRoleName = roleName.trim();

        return database.executeInTransaction(
            () -> registerInTransaction(
                normalizedUsername,
                normalizedEmail,
                password,
                normalizedRoleName
            )
        );
    }

    /**
     * Kiểm tra username đã tồn tại hay chưa.
     *
     * @param username tên đăng nhập cần kiểm tra
     * @return true nếu username đã tồn tại
     * @throws IllegalArgumentException nếu username rỗng
     */
    public boolean isUsernameTaken(final String username) {
        validateText(username, "Tên đăng nhập không được rỗng.");
        return database.users().existsByUsername(username.trim());
    }

    /**
     * Kiểm tra email đã tồn tại hay chưa.
     *
     * @param email email cần kiểm tra
     * @return true nếu email đã tồn tại
     * @throws IllegalArgumentException nếu email rỗng
     */
    public boolean isEmailTaken(final String email) {
        validateText(email, "Email không được rỗng.");
        return database.users().existsByEmail(email.trim());
    }

    /**
     * Tìm người dùng theo mã định danh.
     *
     * @param userId mã người dùng
     * @return người dùng nếu tồn tại
     * @throws IllegalArgumentException nếu mã người dùng rỗng
     */
    public Optional<User> findUserById(final String userId) {
        validateText(userId, "Mã người dùng không được rỗng.");
        return database.users().findById(userId.trim());
    }

    /**
     * Tìm người dùng hợp lệ theo email và mật khẩu.
     *
     * @param email email đã được chuẩn hóa
     * @param password mật khẩu plaintext từ client
     * @return người dùng nếu thông tin đăng nhập hợp lệ
     */
    private Optional<User> findValidUser(final String email, final String password) {
        final Optional<User> foundUser = database.users().findByEmail(email);

        if (foundUser.isEmpty()) {
            return Optional.empty();
        }

        final User user = foundUser.get();

        if (!isPasswordMatched(user, password)) {
            return Optional.empty();
        }

        upgradeLegacyPasswordIfNeeded(user, password);
        LOGGER.info("Xác thực đăng nhập thành công cho email: {}", email);

        return Optional.of(user);
    }

    /**
     * Lưu tài khoản mới trong một transaction của database.
     *
     * @param username tên đăng nhập đã được chuẩn hóa
     * @param email email đã được chuẩn hóa
     * @param password mật khẩu plaintext từ client
     * @param roleName vai trò đã được chuẩn hóa
     * @return người dùng đã được lưu
     * @throws IllegalArgumentException nếu username hoặc email đã tồn tại
     */
    private User registerInTransaction(
        final String username,
        final String email,
        final String password,
        final String roleName) {

        if (database.users().existsByUsername(username)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }

        if (database.users().existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã tồn tại.");
        }

        final String hashedPassword = SecurityUtils.hashPassword(password);
        final User newUser = createUserByRole(username, email, hashedPassword, roleName);

        database.users().save(newUser);
        database.flushAll();

        LOGGER.info("Đăng ký tài khoản mới: {} [{}]", newUser.getUsername(), newUser.getRoleName());

        return newUser;
    }

    /**
     * Kiểm tra dữ liệu đăng ký.
     *
     * @param username tên đăng nhập
     * @param email email đăng nhập
     * @param password mật khẩu plaintext
     * @param roleName vai trò tài khoản
     * @throws IllegalArgumentException nếu dữ liệu đăng ký không hợp lệ
     */
    private void validateRegisterRequest(
        final String username,
        final String email,
        final String password,
        final String roleName) {

        validateText(username, "Tên đăng nhập không được rỗng.");
        validateText(email, "Email không được rỗng.");
        validateText(password, "Mật khẩu không được rỗng.");
        validateText(roleName, "Vai trò không được rỗng.");

        if (!isValidEmail(email.trim())) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }

        if (password.length() < minimumPasswordLength) {
            throw new IllegalArgumentException(
                "Mật khẩu phải có ít nhất " + minimumPasswordLength + " ký tự."
            );
        }

        validateRoleName(roleName.trim());
    }

    /**
     * Tạo user theo vai trò đăng ký.
     *
     * @param username tên đăng nhập
     * @param email email đăng nhập
     * @param hashedPassword mật khẩu đã hash
     * @param roleName vai trò tài khoản
     * @return user mới theo đúng vai trò
     * @throws IllegalArgumentException nếu vai trò không hợp lệ
     */
    private User createUserByRole(
        final String username,
        final String email,
        final String hashedPassword,
        final String roleName) {

        final String normalizedRoleName = roleName.toUpperCase(Locale.ROOT);

        switch (normalizedRoleName) {
            case "SELLER":
                return new Participant(
                    username,
                    email,
                    hashedPassword,
                    defaultBalance,
                    normalizedRoleName
                );
            case "BIDDER": 
                return new Participant(
                    username,
                    email,
                    hashedPassword,
                    defaultBalance,
                    normalizedRoleName
                );
            default:
                throw new IllegalArgumentException("Vai trò chỉ được là BIDDER hoặc SELLER.");
        }
    }

    /**
     * Kiểm tra mật khẩu người dùng nhập có khớp với mật khẩu đã lưu hay không.
     *
     * <p>Hàm này hỗ trợ cả dữ liệu cũ đang lưu plaintext để tránh làm hỏng tài
     * khoản test cũ. Nếu mật khẩu cũ là plaintext, service sẽ nâng cấp sang hash
     * sau khi đăng nhập thành công.
     *
     * @param user người dùng cần kiểm tra
     * @param rawPassword mật khẩu plaintext từ client
     * @return true nếu mật khẩu khớp
     */
    private boolean isPasswordMatched(final User user, final String rawPassword) {
        final String storedPassword = user.getPassword();
        final String hashedPassword = SecurityUtils.hashPassword(rawPassword);

        return hashedPassword.equals(storedPassword) || rawPassword.equals(storedPassword);
    }

    /**
     * Nâng cấp mật khẩu plaintext cũ sang dạng hash.
     *
     * @param user người dùng đang đăng nhập
     * @param rawPassword mật khẩu plaintext từ client
     */
    private void upgradeLegacyPasswordIfNeeded(
        final User user,
        final String rawPassword) {

        final String hashedPassword = SecurityUtils.hashPassword(rawPassword);

        if (!hashedPassword.equals(user.getPassword())) {
            user.setPassword(hashedPassword);
            database.users().save(user);
            database.flushAll();

            LOGGER.info("Đã nâng cấp mật khẩu sang dạng hash cho: {}", user.getUsername());
        }
    }

    /**
     * Kiểm tra email ở mức cơ bản.
     *
     * @param email email cần kiểm tra
     * @return true nếu email có định dạng chấp nhận được
     */
    private boolean isValidEmail(final String email) {
        final int atIndex = email.indexOf('@');
        final int dotIndex = email.lastIndexOf('.');

        return atIndex > 0 && dotIndex > atIndex + 1 && dotIndex < email.length() - 1;
    }

    /**
     * Kiểm tra vai trò đăng ký có hợp lệ hay không.
     *
     * @param roleName vai trò cần kiểm tra
     * @throws IllegalArgumentException nếu vai trò không hợp lệ
     */
    private void validateRoleName(final String roleName) {
        final String normalizedRoleName = roleName.toUpperCase(Locale.ROOT);

        if (!"BIDDER".equals(normalizedRoleName) && !"SELLER".equals(normalizedRoleName)) {
            throw new IllegalArgumentException("Vai trò chỉ được là BIDDER hoặc SELLER.");
        }
    }

    /**
     * Kiểm tra chuỗi không được null hoặc rỗng.
     *
     * @param value giá trị cần kiểm tra
     * @param message thông báo lỗi
     * @throws IllegalArgumentException nếu chuỗi null hoặc rỗng
     */
    private void validateText(final String value, final String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
