package auction_system.server.network.command.bidding;

import auction_system.common.exceptions.InvalidItemException;
import auction_system.common.models.auctions.Auction;
import auction_system.common.models.items.Item;
import auction_system.common.models.items.factory.ItemCreatorFactory;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
import auction_system.server.services.auction.ParticipantItemService;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command xử lý yêu cầu đăng bán sản phẩm từ Client.
 *
 * <p>Command chỉ làm nhiệm vụ parse request, kiểm tra session và gọi service.
 * Logic lưu item nằm trong {@link ParticipantItemService}, còn logic tạo phiên
 * đấu giá nằm trong {@link AuctionManager}.</p>
 */
public final class PublishItemCommand implements Command {
    private static final Logger LOGGER = Logger.getLogger(PublishItemCommand.class.getName());

    private static final int REQUIRED_PART_COUNT = 8;
    private static final int CATEGORY_INDEX = 1;
    private static final int ITEM_NAME_INDEX = 2;
    private static final int DESCRIPTION_INDEX = 3;
    private static final int CONDITION_INDEX = 4;
    private static final int START_PRICE_INDEX = 5;
    private static final int START_TIME_INDEX = 6;
    private static final int END_TIME_INDEX = 7;

    private final ParticipantItemService participantItemService;
    private final AuctionManager auctionManager;

    /**
     * Khởi tạo command đăng bán sản phẩm.
     *
     * @param participantItemService Service quản lý item của participant.
     * @param auctionManager Thành phần quản lý phiên đấu giá.
     */
    public PublishItemCommand(
            ParticipantItemService participantItemService,
            AuctionManager auctionManager) {
        this.participantItemService = Objects.requireNonNull(participantItemService);
        this.auctionManager = Objects.requireNonNull(auctionManager);
    }

    /**
     * Xử lý request đăng bán sản phẩm.
     *
     * @param parts Các phần đã tách từ request.
     * @param session Phiên làm việc của client hiện tại.
     * @return Response dạng text protocol.
     */
    @Override
    public String execute(String[] parts, ClientSession session) {
        try {
            validateRequest(parts, session);

            Participant seller = requireParticipant(session.getCurrentUser());
            Item item = createItem(parts, seller.getId());
            Item savedItem = participantItemService.listItemForAuction(seller, item);

            LocalDateTime startTime = LocalDateTime.parse(parts[START_TIME_INDEX]);
            LocalDateTime endTime = LocalDateTime.parse(parts[END_TIME_INDEX]);
            Auction auction = auctionManager.createAuction(savedItem, seller, startTime, endTime);

            LOGGER.info("Đăng bán thành công item " + savedItem.getId()
                    + " với auction " + auction.getId());

            return buildSuccessResponse(savedItem.getId(), auction.getId());
        } catch (IllegalArgumentException
            | DateTimeParseException
            | InvalidItemException exception) {
            LOGGER.warning("Đăng bán sản phẩm thất bại: " + exception.getMessage());

            return buildFailResponse(exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Lỗi hệ thống khi đăng bán sản phẩm.", exception);

            return buildFailResponse("Lỗi hệ thống khi đăng bán sản phẩm. Vui lòng thử lại sau.");
        }
    }

    /**
     * Kiểm tra request và session trước khi xử lý.
     *
     * @param parts Các phần của request.
     * @param session Phiên làm việc hiện tại.
     */
    private void validateRequest(String[] parts, ClientSession session) {
        if (parts.length < REQUIRED_PART_COUNT) {
            throw new IllegalArgumentException("Thiếu dữ liệu đăng bán sản phẩm.");
        }
        if (session == null || session.getCurrentUser() == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập trước khi đăng bán.");
        }
    }

    /**
     * Ép kiểu người dùng hiện tại thành Participant.
     *
     * @param user Người dùng hiện tại.
     * @return Participant hợp lệ.
     */
    private Participant requireParticipant(User user) {
        Participant participant = (Participant) user;
        if (!(user instanceof Participant)) {
            throw new IllegalArgumentException("Tài khoản hiện tại không có quyền đăng bán.");
        }
        return participant;
    }

    /**
     * Tạo domain item từ dữ liệu request.
     *
     * @param parts Các phần request đã tách.
     * @param sellerId Mã người bán.
     * @return Item domain tương ứng với danh mục.
     */
    private Item createItem(String[] parts, String sellerId) {
        String category = required(parts[CATEGORY_INDEX], "Danh mục không được để trống.");
        String itemName = required(parts[ITEM_NAME_INDEX], "Tên sản phẩm không được để trống.");
        String description = required(parts[DESCRIPTION_INDEX], "Mô tả không được để trống.");
        String condition = required(parts[CONDITION_INDEX], "Tình trạng không được để trống.");
        double startPrice = parsePositivePrice(parts[START_PRICE_INDEX]);

        String fullDescription = description + "\nTình trạng: " + condition;
        return ItemCreatorFactory.createItem(
                category,
                itemName,
                fullDescription,
                startPrice,
                sellerId);
    }

    /**
     * Kiểm tra chuỗi bắt buộc.
     *
     * @param value Giá trị cần kiểm tra.
     * @param message Thông báo lỗi.
     * @return Giá trị đã trim.
     */
    private String required(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * Chuyển giá khởi điểm thành số dương.
     *
     * @param value Giá trị dạng text.
     * @return Giá khởi điểm hợp lệ.
     */
    private double parsePositivePrice(String value) {
        double price = Double.parseDouble(value.trim());
        if (price <= 0) {
            throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn 0.");
        }
        return price;
    }

    private String buildSuccessResponse(final String itemId, final String auctionId) {
        final String message = "Đăng bán sản phẩm thành công.";
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.PUBLISH_ITEM_OK.name(),
                            null,
                            "OK",
                            JsonProtocol.payloadOf(Map.of(
                                    "itemId", itemId,
                                    "auctionId", auctionId)),
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warning("Không tạo được JSON response đăng bán sản phẩm: "
                    + exception.getMessage());
            return Protocol.Response.PUBLISH_ITEM_OK.name()
                    + Protocol.SEPARATOR
                    + message;
        }
    }

    private String buildFailResponse(final String message) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.PUBLISH_ITEM_FAIL.name(),
                            null,
                            "FAIL",
                            null,
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warning("Không tạo được JSON lỗi đăng bán sản phẩm: "
                    + exception.getMessage());
            return Protocol.Response.PUBLISH_ITEM_FAIL.name()
                    + Protocol.SEPARATOR
                    + message;
        }
    }
}
