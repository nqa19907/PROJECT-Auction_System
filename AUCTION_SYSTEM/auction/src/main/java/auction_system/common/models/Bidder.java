package auction_system.common.models;

import auction_system.common.exceptions.InvalidBidException;

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
        System.out.println("[NOTIFY]: " + message);
    }

    /**
     * Đặt giá cho một sản phẩm trong phiên đấu giá.
     *
     * @param item   Sản phẩm cần đặt giá.
     * @param amount Số tiền đặt giá.
     */
    public void placeBid(Auction auction, double amount) {
        if (amount > this.balance){
            throw new InvalidBidException("Không đủ số dư");
        }
        if (auction == null){
            throw new InvalidBidException("Phiên đấu giá không tồn tại");
        }
        BidTransaction bid = new BidTransaction(this, amount);
        auction.placeBid(bid);
    }

    /**
     * Xem lịch sử đặt giá của người đấu giá.
     */
    public void viewBidHistory() {
        // to be coded
    }

    /**
     * Cài đặt tính năng tự động đặt giá cho một sản phẩm.
     *
     * @param item     Sản phẩm muốn đặt tự động.
     * @param maxPrice Mức giá tối đa cho phép hệ thống tự động đặt.
     */
    public void setupAutoBid(Item item, double maxPrice) {
        // to be coded
    }
}