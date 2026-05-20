package auction_system.common.models.auctions;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model dữ liệu cho một hàng trong bảng lịch sử đấu giá.
 */
public class BidRow {

    /** Thời gian đặt giá. */
    private final StringProperty time;

    /** Tên người đặt giá. */
    private final StringProperty bidder;

    /** Mức giá đặt. */
    private final DoubleProperty price;

    /** Mức thay đổi so với lần trước. */
    private final DoubleProperty change;

    /** Trạng thái của lần đặt giá. */
    private final StringProperty status;

    /**
     * Khởi tạo một hàng dữ liệu đấu giá.
     *
     * @param timeVal thời gian
     * @param bidderVal tên người đấu
     * @param priceVal giá trả
     * @param changeVal mức thay đổi
     * @param statusVal trạng thái
     */
    public BidRow(
            final String timeVal,
            final String bidderVal,
            final double priceVal,
            final double changeVal,
            final String statusVal
    ) {
        this.time = new SimpleStringProperty(timeVal);
        this.bidder = new SimpleStringProperty(bidderVal);
        this.price = new SimpleDoubleProperty(priceVal);
        this.change = new SimpleDoubleProperty(changeVal);
        this.status = new SimpleStringProperty(statusVal);
    }

    /**
     * Lấy thời gian.
     *
     * @return chuỗi thời gian
     */
    public String getTime() {
        return time.get();
    }

    /**
     * Lấy tên người đấu giá.
     *
     * @return tên người đấu
     */
    public String getBidder() {
        return bidder.get();
    }

    /**
     * Lấy mức giá.
     *
     * @return giá trả
     */
    public double getPrice() {
        return price.get();
    }

    /**
     * Lấy mức thay đổi.
     *
     * @return delta so với lần trước
     */
    public double getChange() {
        return change.get();
    }

    /**
     * Lấy trạng thái.
     *
     * @return trạng thái
     */
    public String getStatus() {
        return status.get();
    }
}