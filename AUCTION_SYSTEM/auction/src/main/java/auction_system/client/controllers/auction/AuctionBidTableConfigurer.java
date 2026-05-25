package auction_system.client.controllers.auction;

import auction_system.client.utils.CurrencyFormatter;
import auction_system.common.models.auctions.BidRow;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Font;

/**
 * Cấu hình bảng lịch sử đặt giá của màn hình chi tiết đấu giá.
 */
final class AuctionBidTableConfigurer {
    /** Tên font dùng chung cho dữ liệu trong bảng. */
    private static final String TABLE_FONT_FAMILY = "Google Sans";

    /** Cỡ chữ dùng chung cho dữ liệu trong bảng. */
    private static final double TABLE_FONT_SIZE = 13D;

    /** Font thống nhất cho các ô hiển thị dữ liệu số. */
    private static final String NUMERIC_FONT_STYLE =
            "-fx-font-family: 'Google Sans'; -fx-font-size: 13px;";

    /** Style cho số tiền tăng thêm hợp lệ. */
    private static final String POSITIVE_CHANGE_STYLE =
            "-fx-text-fill: #2E7D52; -fx-font-weight: bold;";

    /** Style cho dòng không có số tiền tăng thêm. */
    private static final String EMPTY_CHANGE_STYLE = "-fx-text-fill: #A07040;";

    /** Style class dùng cho các ô hiển thị dữ liệu số. */
    private static final String NUMERIC_CELL_CLASS = "numeric-table-cell";
    /** Style class cho hàng đặt giá mới nhất. */
    private static final String LATEST_ROW_CLASS = "latest-bid-row";
    /** Style class cho ô giá trả. */
    private static final String PRICE_CELL_CLASS = "price-table-cell";
    /** Style class cho ô mức tăng. */
    private static final String CHANGE_CELL_CLASS = "change-table-cell";
    /** Style class cho badge trạng thái hợp lệ. */
    private static final String STATUS_BADGE_CLASS = "status-badge";

    /**
     * Constructor private cho utility class.
     */
    private AuctionBidTableConfigurer() {

    }

    /**
     * Gắn dữ liệu và cell formatter cho bảng lịch sử bid.
     *
     * @param bidTable   bảng lịch sử đặt giá
     * @param colTime    cột thời gian
     * @param colBidder  cột người đặt giá
     * @param colPrice   cột giá đặt
     * @param colChange  cột mức tăng
     * @param colStatus  cột trạng thái
     * @param bidHistory dữ liệu lịch sử đặt giá
     */
    static void configure(
            final TableView<BidRow> bidTable,
            final TableColumn<BidRow, String> colTime,
            final TableColumn<BidRow, String> colBidder,
            final TableColumn<BidRow, Double> colPrice,
            final TableColumn<BidRow, Double> colChange,
            final TableColumn<BidRow, String> colStatus,
            final ObservableList<BidRow> bidHistory) {
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colBidder.setCellValueFactory(new PropertyValueFactory<>("bidder"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colChange.setCellValueFactory(new PropertyValueFactory<>("change"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colTime.setCellFactory(col -> createTextCell(Pos.CENTER_LEFT));
        colBidder.setCellFactory(col -> createTextCell(Pos.CENTER_LEFT));
        colPrice.setCellFactory(col -> createPriceCell());
        colChange.setCellFactory(col -> createChangeCell());
        colStatus.setCellFactory(col -> createStatusCell());
        bidTable.setRowFactory(table -> createBidRow());
        bidTable.setItems(bidHistory);
    }

    /**
     * Tạo row formatter để làm nổi hàng mới nhất.
     *
     * @return row formatter cho bảng bid
     */
    private static TableRow<BidRow> createBidRow() {
        return new TableRow<>() {
            @Override
            protected void updateItem(final BidRow item, final boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove(LATEST_ROW_CLASS);
                if (!empty && getIndex() == 0) {
                    getStyleClass().add(LATEST_ROW_CLASS);
                }
            }
        };
    }

    /**
     * Tạo cell text căn chỉnh thống nhất.
     *
     * @param alignment vị trí căn chữ
     * @return cell formatter cho text
     */
    private static TableCell<BidRow, String> createTextCell(final Pos alignment) {
        return new TableCell<>() {
            {
                setAlignment(alignment);
            }

            @Override
            protected void updateItem(final String value, final boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value);
            }
        };
    }

    /**
     * Tạo cell formatter cho cột giá.
     *
     * @return cell formatter cho giá VNĐ
     */
    private static TableCell<BidRow, Double> createPriceCell() {
        return new TableCell<>() {
            {
                getStyleClass().add(NUMERIC_CELL_CLASS);
                getStyleClass().add(PRICE_CELL_CLASS);
                setFont(Font.font(TABLE_FONT_FAMILY, TABLE_FONT_SIZE));
                setAlignment(Pos.CENTER_RIGHT);
            }

            @Override
            protected void updateItem(final Double value, final boolean empty) {
                super.updateItem(value, empty);
                setStyle(NUMERIC_FONT_STYLE);
                setFont(Font.font(TABLE_FONT_FAMILY, TABLE_FONT_SIZE));
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
            {
                getStyleClass().add(NUMERIC_CELL_CLASS);
                getStyleClass().add(CHANGE_CELL_CLASS);
                setFont(Font.font(TABLE_FONT_FAMILY, TABLE_FONT_SIZE));
                setAlignment(Pos.CENTER_RIGHT);
            }

            @Override
            protected void updateItem(final Double value, final boolean empty) {
                super.updateItem(value, empty);
                setFont(Font.font(TABLE_FONT_FAMILY, TABLE_FONT_SIZE));
                if (empty || value == null) {
                    setText(null);
                    setStyle(NUMERIC_FONT_STYLE);
                    return;
                }

                if (value > 0) {
                    setText("+" + CurrencyFormatter.formatVnd(value.longValue()));
                    setStyle(NUMERIC_FONT_STYLE + POSITIVE_CHANGE_STYLE);
                } else {
                    setText("-");
                    setStyle(NUMERIC_FONT_STYLE + EMPTY_CHANGE_STYLE);
                }
            }
        };
    }

    /**
     * Tạo cell formatter cho cột trạng thái.
     *
     * @return cell formatter cho trạng thái bid
     */
    private static TableCell<BidRow, String> createStatusCell() {
        return new TableCell<>() {
            private final Label statusBadge = new Label();

            {
                setAlignment(Pos.CENTER);
                statusBadge.getStyleClass().add(STATUS_BADGE_CLASS);
            }

            @Override
            protected void updateItem(final String value, final boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null || value.isBlank()) {
                    setGraphic(null);
                    return;
                }

                statusBadge.setText(value);
                setGraphic(statusBadge);
            }
        };
    }
}
