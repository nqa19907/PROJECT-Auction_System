package auction_system.common.models.users;

import auction_system.common.models.items.Item;

/**
 * Lớp đại diện cho người đấu giá (người mua) trong hệ thống.
 */
public class Bidder extends Participant {
    
    /**
     * Khởi tạo một người đấu giá mới.
     *
     * @param username Tên đăng nhập.
     * @param email    Địa chỉ email.
     * @param password Mật khẩu.
     * @param balance  Số dư tài khoản ban đầu.
     */
    public Bidder(String username, String email, String password, double balance) {
        super(username, email, password, balance);
    }


    @Override
    public void update(String message) {
        // TODO: Cập nhật thông báo trạng thái từ phiên đấu giá cho Bidder
    }

    @Override
    public String getRoleName() {
        return "BIDDER";
    }

    /**
     * Đặt giá cho một sản phẩm trong phiên đấu giá.
     *
     * @param item   Sản phẩm cần đặt giá.
     * @param amount Số tiền đặt giá.
     */
    public void placeBid(Item item, double amount) {
        // TODO: Cài đặt logic đặt giá cho sản phẩm
    }

    /**
     * Xem lịch sử đặt giá của người đấu giá.
     */
    public void viewBidHistory() {
        // TODO: Cài đặt logic xem lịch sử đặt giá
    }

    /**
     * Cài đặt tính năng tự động đặt giá cho một sản phẩm.
     *
     * @param item     Sản phẩm muốn đặt tự động.
     * @param maxPrice Mức giá tối đa cho phép hệ thống tự động đặt.
     */
    public void setupAutoBid(Item item, double maxPrice) {
        // TODO: Cài đặt tính năng tự động đặt giá (Auto-bid)
    }
}