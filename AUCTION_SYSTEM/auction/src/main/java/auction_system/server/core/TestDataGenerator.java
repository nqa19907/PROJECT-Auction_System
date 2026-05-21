package auction_system.server.core;

import auction_system.common.models.items.Item;
import auction_system.common.models.items.builder.ArtBuilder;
import auction_system.common.models.items.builder.ElectronicBuilder;
import auction_system.common.models.users.Admin;
import auction_system.common.models.users.Bidder;
import auction_system.common.models.users.Seller;
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
            {"SieuThiDienMay", "meomaybe@gmail.com", "1", 10_000_000.0, "SELLER"},
            {"Thế giới chân dài", "thegioichandai@gmail.com", "2", 100_000_000.0, "SELLER"},
            {"Nguyễn Văn User", "1", "1", 100_000_000.0, "BIDDER"},
            {"Nguyễn Văn Admin", "2", "2", 100_000_000.0, "ADMIN"},
            {"NguoiDauGia1", "3", "3", 100_000_000.0, "BIDDER"},
            {"NguoiDauGia2", "4", "4", 50_000_000.0, "BIDDER"},
            {"AdminHub", "admin@auctionhub.vn", "admin", 0.0, "ADMIN"}
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
            } else if ("SELLER".equals(role)) {
                user = new Seller(name, email, pass, balance);
            } else {
                user = new Bidder(name, email, pass, balance);
            }

            manager.registerUser(user);
            userCache.put(name, user);
        }

        // 2. Định nghĩa mảng phiên đấu giá: 
        // {Type, Name, Description, StartPrice, SellerName, StartOffsetHours, EndOffsetHours}
        Object[][] auctionData = {
            {"E", "iPhone 15 Pro Max", "Bản 256GB màu Titan.", 28_000_000.0,
                "SieuThiDienMay", -1, 48},
            {"A", "Tranh 'Hoàng Hôn'", "Sơn dầu 80x120cm.", 5_000_000.0,
                "Thế giới chân dài", 2, 120},
            {"E", "Laptop Dell XPS 15", "i9, 32GB RAM, RTX 4070.", 45_000_000.0,
                "SieuThiDienMay", -24, 12},
            {"A", "Bức Tượng 'Suy Tư'", "Đồng nguyên khối, cao 50cm.", 12_000_000.0,
                "Thế giới chân dài", -120, -24},
            {"E", "Sony WH-1000XM5", "Chống ồn, fullbox.", 4_500_000.0, "SieuThiDienMay", 24, 72}
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
            if (user instanceof Seller seller) {
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