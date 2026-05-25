package auction_system.server.services;

import auction_system.common.exceptions.InvalidItemException;
import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.models.items.Item;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Service xử lý nghiệp vụ sản phẩm của người tham gia có quyền bán.
 *
 * <p>Lớp này thay thế việc đặt nghiệp vụ đăng và gỡ sản phẩm trong Seller. Khi
 * hệ thống gộp Seller và Bidder thành Participant, quyền bán phải được kiểm tra
 * bằng vai trò hoặc quyền của Participant, không dùng instanceof Seller nữa.
 */
public class ParticipantItemService {
    private static final Logger LOGGER = Logger.getLogger(ParticipantItemService.class.getName());

    private final SerializedDatabase database;

    /**
     * Khởi tạo service quản lý sản phẩm.
     *
     * @param database database serialization dùng chung của server
     */
    public ParticipantItemService(final SerializedDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    /**
     * Đăng một sản phẩm mới cho participant có quyền bán.
     *
     * <p>Service sẽ gán sellerId bằng id của participant hiện tại rồi lưu vào
     * database. Không lưu danh sách sản phẩm trong Participant để tránh dữ liệu
     * bị trùng với ItemRepository.
     *
     * @param currentUser người dùng hiện tại trong session
     * @param item sản phẩm cần đăng
     * @return sản phẩm đã được lưu
     * @throws InvalidItemException nếu người dùng hoặc sản phẩm không hợp lệ
     */
    public Item listItemForAuction(final User currentUser, final Item item) {
        final Participant seller = requireSellerParticipant(currentUser);
        validateItem(item);

        return database.executeInTransaction(
                () -> saveItemForSeller(seller, item)
        );
    }

    /**
     * Gỡ một sản phẩm của participant có quyền bán.
     *
     * <p>Không cho gỡ sản phẩm đang nằm trong phiên đấu giá OPEN hoặc RUNNING
     * để tránh làm hỏng dữ liệu realtime của các client đang theo dõi.
     *
     * @param currentUser người dùng hiện tại trong session
     * @param itemId mã sản phẩm cần gỡ
     * @return true nếu gỡ thành công
     * @throws InvalidItemException nếu không có quyền gỡ hoặc sản phẩm không hợp lệ
     */
    public boolean delistItem(final User currentUser, final String itemId) {
        final Participant seller = requireSellerParticipant(currentUser);
        validateText(itemId, "Mã sản phẩm không được rỗng.");

        return database.executeInTransaction(
                () -> deleteItemForSeller(seller, itemId.trim())
        );
    }

    /**
     * Lấy danh sách sản phẩm thuộc participant hiện tại.
     *
     * @param currentUser người dùng hiện tại trong session
     * @return danh sách sản phẩm của participant
     * @throws InvalidItemException nếu người dùng hiện tại không có quyền bán
     */
    public List<Item> getItemsBySeller(final User currentUser) {
        final Participant seller = requireSellerParticipant(currentUser);
        return database.items().findBySellerId(seller.getId());
    }

    /**
     * Lưu sản phẩm cho participant trong transaction.
     *
     * @param seller participant có quyền bán
     * @param item sản phẩm cần lưu
     * @return sản phẩm đã được lưu
     */
    private Item saveItemForSeller(final Participant seller, final Item item) {
        item.setSellerId(seller.getId());

        final Item savedItem = database.items().save(item);
        database.flushAll();

        LOGGER.info(
                "Participant "
                        + seller.getUsername()
                        + " đăng sản phẩm "
                        + savedItem.getItemName()
        );

        return savedItem;
    }

    /**
     * Xóa sản phẩm của participant trong transaction.
     *
     * @param seller participant có quyền bán
     * @param itemId mã sản phẩm cần xóa
     * @return true nếu xóa thành công
     */
    private boolean deleteItemForSeller(final Participant seller, final String itemId) {
        final Item item = findItemOrThrow(itemId);
        validateOwnership(seller, item);
        validateItemCanBeDeleted(itemId);

        final boolean deleted = database.items().deleteById(itemId);

        if (!deleted) {
            throw new InvalidItemException("Không thể gỡ sản phẩm khỏi database.");
        }

        database.flushAll();
        LOGGER.info("Participant " + seller.getUsername() + " đã gỡ sản phẩm " + itemId);

        return true;
    }

    /**
     * Tìm sản phẩm theo mã định danh.
     *
     * @param itemId mã sản phẩm cần tìm
     * @return sản phẩm tương ứng
     * @throws InvalidItemException nếu không tìm thấy sản phẩm
     */
    private Item findItemOrThrow(final String itemId) {
        return database.items()
                .findById(itemId)
                .orElseThrow(
                        () -> new InvalidItemException("Không tìm thấy sản phẩm: " + itemId)
                );
    }

    /**
     * Kiểm tra người dùng hiện tại có phải participant có quyền bán hay không.
     *
     * @param currentUser người dùng hiện tại trong session
     * @return participant có quyền bán
     * @throws InvalidItemException nếu chưa đăng nhập hoặc không có quyền bán
     */
    private Participant requireSellerParticipant(final User currentUser) {
        if (currentUser == null) {
            throw new InvalidItemException("Bạn cần đăng nhập trước khi quản lý sản phẩm.");
        }

        if (!(currentUser instanceof Participant)) {
            throw new InvalidItemException("Người dùng hiện tại không phải participant.");
        }

        final Participant participant = (Participant) currentUser;

        if (!canSell(participant)) {
            throw new InvalidItemException(
                    "Người dùng không có quyền bán sản phẩm."
            );
        }

        return participant;
    }

    /**
     * Kiểm tra participant có quyền bán sản phẩm hay không dựa trên role.
     *
     * @param participant participant cần kiểm tra
     * @return true nếu role là SELLER hoặc PARTICIPANT
     */
    private boolean canSell(final Participant participant) {
        final String role = participant.getRoleName();
        return "SELLER".equals(role) || "PARTICIPANT".equals(role);
    }

    /**
     * Kiểm tra sản phẩm trước khi lưu.
     *
     * @param item sản phẩm cần kiểm tra
     * @throws InvalidItemException nếu sản phẩm không hợp lệ
     */
    private void validateItem(final Item item) {
        if (item == null) {
            throw new InvalidItemException("Sản phẩm không được null.");
        }

        validateText(item.getItemName(), "Tên sản phẩm không được rỗng.");
        validateText(item.getDescription(), "Mô tả sản phẩm không được rỗng.");
        validateText(item.getCategory(), "Loại sản phẩm không được rỗng.");

        if (item.getStartPrice() <= 0) {
            throw new InvalidItemException("Giá khởi điểm phải lớn hơn 0.");
        }
    }

    /**
     * Kiểm tra participant có sở hữu sản phẩm hay không.
     *
     * @param seller participant có quyền bán
     * @param item sản phẩm cần kiểm tra
     * @throws InvalidItemException nếu sản phẩm không thuộc participant hiện tại
     */
    private void validateOwnership(final Participant seller, final Item item) {
        if (!seller.getId().equals(item.getSellerId())) {
            throw new InvalidItemException("Bạn không có quyền gỡ sản phẩm này.");
        }
    }

    /**
     * Kiểm tra sản phẩm có thể bị xóa khỏi danh sách hay không.
     *
     * @param itemId mã sản phẩm cần kiểm tra
     * @throws InvalidItemException nếu sản phẩm đang nằm trong phiên đấu giá hoạt động
     */
    private void validateItemCanBeDeleted(final String itemId) {
        final boolean hasActiveAuction = database.auctions()
                .findByItemId(itemId)
                .stream()
                .anyMatch(this::isActiveAuction);

        if (hasActiveAuction) {
            throw new InvalidItemException(
                    "Không thể gỡ sản phẩm đang có phiên đấu giá mở hoặc đang chạy."
            );
        }
    }

    /**
     * Kiểm tra phiên đấu giá có đang hoạt động hay không.
     *
     * @param auction phiên đấu giá cần kiểm tra
     * @return true nếu phiên đang OPEN hoặc RUNNING
     */
    private boolean isActiveAuction(final Auction auction) {
        final AuctionStatus status = auction.getStatus();

        return status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING;
    }

    /**
     * Kiểm tra chuỗi không được null hoặc rỗng.
     *
     * @param value giá trị cần kiểm tra
     * @param message thông báo lỗi
     * @throws InvalidItemException nếu chuỗi null hoặc rỗng
     */
    private void validateText(final String value, final String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidItemException(message);
        }
    }
}