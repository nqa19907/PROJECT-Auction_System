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
import auction_system.server.network.command.JsonPayloadCommand;
import auction_system.server.network.payload.bidding.PublishItemPayload;
import auction_system.server.services.auction.ParticipantItemService;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
public final class PublishItemCommand implements JsonPayloadCommand {
    private static final Logger LOGGER = Logger.getLogger(PublishItemCommand.class.getName());

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
     * @param payload Payload JSON của request.
     * @param session Phiên làm việc của client hiện tại.
     * @return Response JSON một dòng.
     */
    @Override
    public String execute(final JsonNode payload, final ClientSession session) {
        try {
            // Chặn request chưa đăng nhập trước khi đọc payload nghiệp vụ.
            validateSession(session);
            // Map payload JSON theo field name sang DTO đăng bán.
            final PublishItemPayload publishItemPayload = readPayload(payload);

            // Chỉ Participant mới được tạo item và mở phiên đấu giá.
            Participant seller = requireParticipant(session.getCurrentUser());
            // Tạo item, lưu item rồi mở auction theo mốc thời gian client gửi lên.
            Item item = createItem(publishItemPayload, seller.getId());
            Item savedItem = participantItemService.listItemForAuction(seller, item);

            // Chuyển các field thời gian/boolean từ DTO sang kiểu domain cần dùng.
            LocalDateTime startTime = LocalDateTime.parse(publishItemPayload.startTime());
            LocalDateTime endTime = LocalDateTime.parse(publishItemPayload.endTime());
            boolean antiSnipingEnabled = Boolean.TRUE.equals(
                    publishItemPayload.antiSnipingEnabled());
            Auction auction = auctionManager.createAuction(
                    savedItem,
                    seller,
                    startTime,
                    endTime,
                    antiSnipingEnabled);

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
     * @param session Phiên làm việc hiện tại.
     */
    private void validateSession(final ClientSession session) {
        if (session == null || session.getCurrentUser() == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập trước khi đăng bán.");
        }
    }

    private PublishItemPayload readPayload(final JsonNode payload) {
        // JsonProtocol dùng Jackson treeToValue để map JsonNode sang record DTO.
        final PublishItemPayload publishItemPayload =
                JsonProtocol.payloadAs(payload, PublishItemPayload.class);
        if (publishItemPayload.hasMissingRequiredFields()) {
            throw new IllegalArgumentException("Thiếu dữ liệu đăng bán sản phẩm.");
        }
        return publishItemPayload;
    }

    /**
     * Ép kiểu người dùng hiện tại thành Participant.
     *
     * @param user Người dùng hiện tại.
     * @return Participant hợp lệ.
     */
    private Participant requireParticipant(User user) {
        if (!(user instanceof Participant)) {
            throw new IllegalArgumentException("Tài khoản hiện tại không có quyền đăng bán.");
        }
        Participant participant = (Participant) user;
        return participant;
    }

    /**
     * Tạo domain item từ dữ liệu request.
     *
     * @param payload Payload đăng bán đã map từ JSON.
     * @param sellerId Mã người bán.
     * @return Item domain tương ứng với danh mục.
     */
    private Item createItem(final PublishItemPayload payload, final String sellerId) {
        // Đọc và chuẩn hóa các field item trước khi gọi factory theo category.
        String category = required(payload.category(), "Danh mục không được để trống.");
        String itemName = required(payload.itemName(), "Tên sản phẩm không được để trống.");
        String description = required(payload.description(), "Mô tả không được để trống.");
        String condition = required(payload.condition(), "Tình trạng không được để trống.");
        double startPrice = parsePositivePrice(payload.startPrice());
        double bidStep = parsePositivePrice(payload.bidStep());
        // Đọc metadata ảnh nếu client mới có gửi kèm.
        String imagePath = optional(payload.imagePath());

        // Ghép tình trạng vào mô tả theo format domain hiện tại đang lưu trữ.
        String fullDescription = description + "\nTình trạng: " + condition;
        Item item = ItemCreatorFactory.createItem(
                category,
                itemName,
                fullDescription,
                startPrice,
                sellerId,
                imagePath);
        item.setBidStep(bidStep);
        return item;
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
     * Đọc chuỗi tùy chọn từ request.
     *
     * @param value Giá trị cần đọc.
     * @return Giá trị đã trim hoặc chuỗi rỗng.
     */
    private String optional(final String value) {
        return value == null ? "" : value.trim();
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
            // Trả id item và auction mới để client có thể đồng bộ sau khi đăng bán.
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
            throw new IllegalStateException("Không tạo được JSON PUBLISH_ITEM_OK.", exception);
        }
    }

    private String buildFailResponse(final String message) {
        try {
            // Mọi lỗi đăng bán đều trả cùng response type để client route về callback.
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
            throw new IllegalStateException("Không tạo được JSON PUBLISH_ITEM_FAIL.", exception);
        }
    }
}
