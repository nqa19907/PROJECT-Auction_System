package auction_system.client.controllers.auction;

import auction_system.client.utils.CurrencyFormatter;
import auction_system.common.models.auctions.BidRow;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Cấu hình bảng lịch sử đặt giá của màn hình chi tiết đấu giá.
 */
final class AuctionBidTableConfigurer {

    /** Style cho số tiền tăng thêm hợp lệ. */
    private static final String POSITIVE_CHANGE_STYLE =
        "-fx-text-fill: #2E7D52; -fx-font-weight: bold;";

    /** Style cho dòng không có số tiền tăng thêm. */
    private static final String EMPTY_CHANGE_STYLE = "-fx-text-fill: #A07040;";

    /**
     * Constructor private cho utility class.
     */
    private AuctionBidTableConfigurer() {

    }

    /**
     * Gắn dữ liệu và cell formatter cho bảng lịch sử bid.
     *
     * @param bidTable bảng lịch sử đặt giá
     * @param colTime cột thời gian
     * @param colBidder cột người đặt giá
     * @param colPrice cột giá đặt
     * @param colChange cột mức tăng
     * @param colStatus cột trạng thái
     * @param bidHistory dữ liệu lịch sử đặt giá
     */
    static void configure(
        final TableView<BidRow> bidTable,
        final TableColumn<BidRow, String> colTime,
        final TableColumn<BidRow, String> colBidder,
        final TableColumn<BidRow, Double> colPrice,
        final TableColumn<BidRow, Double> colChange,
        final TableColumn<BidRow, String> colStatus,
        final ObservableList<BidRow> bidHistory
    ) {
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colBidder.setCellValueFactory(new PropertyValueFactory<>("bidder"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colChange.setCellValueFactory(new PropertyValueFactory<>("change"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colPrice.setCellFactory(col -> createPriceCell());
        colChange.setCellFactory(col -> createChangeCell());
        bidTable.setItems(bidHistory);
    }

    /**
     * Tạo cell formatter cho cột giá.
     *
     * @return cell formatter cho giá VNĐ
     */
    private static TableCell<BidRow, Double> createPriceCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(final Double value, final boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(CurrencyFormatter.formatVnd(value.longValue()));
                }
            }
        };
    }

    /**
     * Tạo cell formatter cho cột mức tăng.
     *
     * @return cell formatter cho mức tăng giá
     */
    private static TableCell<BidRow, Double> createChangeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(final Double value, final boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                if (value > 0) {
                    setText("+" + CurrencyFormatter.formatVnd(value.longValue()));
                    setStyle(POSITIVE_CHANGE_STYLE);
                } else {
                    setText("-");
                    setStyle(EMPTY_CHANGE_STYLE);
                }
            }
        };
    }
}
