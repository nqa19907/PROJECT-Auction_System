package auction_system.server.core;

import auction_system.common.models.items.Item;
import auction_system.common.models.items.builder.ArtBuilder;
import auction_system.common.models.items.builder.ElectronicBuilder;
import auction_system.common.models.users.Bidder;
import auction_system.common.models.users.Seller;
import auction_system.common.utils.SecurityUtils;
import java.time.LocalDateTime;
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

        // 1. Tạo người dùng "fake"
        Seller seller1 = new Seller("SieuThiDienMay", "1", 
                SecurityUtils.hashPassword("1"), 0.0, 5.0f);
        Seller seller2 = new Seller("PhongTranhArt", "2", 
                SecurityUtils.hashPassword("2"), 0.0, 5.0f);
        Bidder bidder1 = new Bidder("NguoiDauGia1", "3", 
                SecurityUtils.hashPassword("3"), 100_000_000.0);
        Bidder bidder2 = new Bidder("NguoiDauGia2", "4", 
                SecurityUtils.hashPassword("4"), 50_000_000.0);

        // 2. Đăng ký người dùng vào hệ thống
        manager.registerUser(seller1);
        manager.registerUser(seller2);
        manager.registerUser(bidder1);
        manager.registerUser(bidder2);

        Item item1 = new ElectronicBuilder()
                .itemName("iPhone 15 Pro Max")
                .description("Hàng mới, nguyên seal, bản 256GB màu Titan tự nhiên.")
                .startPrice(28_000_000.0)
                .sellerId(seller1.getId())
                .build();

        Item item2 = new ArtBuilder()
                .itemName("Tranh Sơn Dầu 'Hoàng Hôn'")
                .description("Tranh sơn dầu vẽ cảnh hoàng hôn trên biển, kích thước 80x120cm.")
                .startPrice(5_000_000.0)
                .sellerId(seller2.getId())
                .build();

        Item item3 = new ElectronicBuilder()
                .itemName("Laptop Dell XPS 15")
                .description("Cấu hình Core i9, 32GB RAM, 1TB SSD, card RTX 4070.")
                .startPrice(45_000_000.0)
                .sellerId(seller1.getId())
                .build();

        Item item4 = new ArtBuilder()
                .itemName("Bức Tượng Đồng 'Suy Tư'")
                .description("Tượng điêu khắc bằng đồng nguyên khối, cao 50cm.")
                .startPrice(12_000_000.0)
                .sellerId(seller2.getId())
                .build();

        Item item5 = new ElectronicBuilder()
                .itemName("Tai nghe Sony WH-1000XM5")
                .description("Tai nghe chống ồn chủ động, hàng đã qua sử dụng, còn bảo hành.")
                .startPrice(4_500_000.0)
                .sellerId(seller1.getId())
                .build();

        // 4. Tạo 5 phiên đấu giá "fake"
        LocalDateTime now = LocalDateTime.now();
        manager.createAuction(item1, seller1, now.minusHours(1), now.plusDays(2));
        manager.createAuction(item2, seller2, now.plusHours(2), now.plusDays(5));
        manager.createAuction(item3, seller1, now.minusDays(1), now.plusHours(12));
        // Phiên này đã kết thúc
        manager.createAuction(item4, seller2, now.minusDays(5), now.minusDays(1));
        manager.createAuction(item5, seller1, now.plusDays(1), now.plusDays(3));

        LOGGER.info("Tạo dữ liệu mẫu thành công.");
    }
}