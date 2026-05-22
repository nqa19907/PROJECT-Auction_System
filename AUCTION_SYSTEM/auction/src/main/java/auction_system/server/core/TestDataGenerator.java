package auction_system.server.core;

import auction_system.common.models.items.Item;
import auction_system.common.models.items.builder.ArtBuilder;
import auction_system.common.models.items.builder.ElectronicBuilder;
import auction_system.common.models.users.Admin;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.common.utils.SecurityUtils;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp tiện ích để tạo dữ liệu mẫu cho việc kiểm thử.
 */
public final class TestDataGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataGenerator.class);

    private TestDataGenerator() {
        // Private constructor for utility class
    }

    /**
     * Tạo và nạp dữ liệu mẫu (người dùng, vật phẩm, phiên đấu giá) vào AuctionManager.
     *
     * @param manager Instance của AuctionManager để nạp dữ liệu vào.
     */
    public static void generate(AuctionManager manager) {
        LOGGER.info("Bắt đầu tạo dữ liệu mẫu...");

        // Cache để lưu trữ User nhằm liên kết với Item/Auction phía sau
        Map<String, User> userCache = new HashMap<>();

        // 1. Định nghĩa mảng người dùng: {Username, Email, RawPassword, Balance, Role}
        Object[][] userData = {
            {"Test Seller", "s1", "1", 20_000_000.0, "SELLER"},
            {"Test Art Seller", "s2", "1", 20_000_000.0, "SELLER"},
            {"Test Bidder", "b1", "1", 100_000_000.0, "BIDDER"},
            {"Low Balance Bidder", "b2", "1", 200_000.0, "BIDDER"},
            {"Test Admin", "a", "1", 0.0, "ADMIN"}
        };

        for (Object[] data : userData) {
            String name = (String) data[0];
            String email = (String) data[1];
            String pass = SecurityUtils.hashPassword((String) data[2]);
            double balance = (double) data[3];
            String role = (String) data[4];

            User user;
            if ("ADMIN".equals(role)) {
                user = new Admin(name, email, pass);
            } else {
                user = new Participant(name, email, pass, balance, role);
            }

            User persistedUser = manager.registerUser(user);
            userCache.put(name, persistedUser);
        }

        // 2. Định nghĩa mảng phiên đấu giá: 
        // {Type, Name, Description, StartPrice, SellerName, StartOffsetHours, EndOffsetHours}
        Object[][] auctionData = {
            {"E", "Test Live Phone", "Phiên đang chạy, dùng để test đặt giá nhanh.",
                100_000.0, "Test Seller", -1, 48},
            {"E", "Test Live Laptop", "Phiên đang chạy với giá cao hơn một chút.",
                500_000.0, "Test Seller", -2, 24},
            {"A", "Test Upcoming Painting", "Phiên chưa bắt đầu, dùng để test trạng thái OPEN.",
                300_000.0, "Test Art Seller", 2, 72},
            {"A", "Test Finished Statue", "Phiên đã kết thúc, dùng để test trạng thái FINISHED.",
                400_000.0, "Test Art Seller", -120, -24},
            {"E", "Test Future Headphone", "Phiên bắt đầu sau 24 giờ.",
                150_000.0, "Test Seller", 24, 96}
        };

        LocalDateTime now = LocalDateTime.now();

        for (Object[] data : auctionData) {
            String type = (String) data[0];
            String itemName = (String) data[1];
            String desc = (String) data[2];
            double price = (double) data[3];
            String sellerName = (String) data[4];
            int startOffset = (int) data[5];
            int endOffset = (int) data[6];

            User user = userCache.get(sellerName);
            if (user instanceof Participant seller) {
                Item item = createItem(type, itemName, desc, price, seller.getId());
                manager.createAuction(
                    item,
                    seller,
                    now.plusHours(startOffset),
                    now.plusHours(endOffset)
                );
            }
        }

        LOGGER.info("Tạo dữ liệu mẫu thành công.");
    }

    /**
     * Helper để tạo Item dựa trên loại (Electronic hoặc Art).
     *
     * @param type  Loại sản phẩm (E cho Electronic, A cho Art).
     * @param name  Tên sản phẩm.
     * @param desc  Mô tả sản phẩm.
     * @param price Giá khởi điểm.
     * @param sid   ID của người bán.
     * @return Đối tượng Item tương ứng.
     */
    private static Item createItem(
            String type, String name, String desc, double price, String sid) {
        if ("E".equals(type)) {
            return new ElectronicBuilder()
                    .itemName(name)
                    .description(desc)
                    .startPrice(price)
                    .sellerId(sid)
                    .build();
        }
        return new ArtBuilder()
                .itemName(name)
                .description(desc)
                .startPrice(price)
                .sellerId(sid)
                .build();
    }
}
