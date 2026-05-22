package auction_system.client.controllers.auction;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

/**
 * Cấu hình biểu đồ lịch sử giá của màn hình chi tiết đấu giá.
 */
final class AuctionPriceChartConfigurer {

    /** Tên series hiển thị trên biểu đồ. */
    private static final String PRICE_SERIES_NAME = "Giá trả";

    /**
     * Constructor private cho utility class.
     */
    private AuctionPriceChartConfigurer() {

    }

    /**
     * Cấu hình chart và gắn series dữ liệu giá.
     *
     * @param bidLineChart biểu đồ đường hiển thị giá
     * @param priceSeries series dữ liệu giá
     */
    static void configure(
        final LineChart<String, Number> bidLineChart,
        final XYChart.Series<String, Number> priceSeries
    ) {
        priceSeries.setName(PRICE_SERIES_NAME);
        bidLineChart.getData().add(priceSeries);
        bidLineChart.setLegendVisible(false);
        bidLineChart.setCreateSymbols(true);
        bidLineChart.setAnimated(true);
    }

    /**
     * Cập nhật giới hạn trục Y theo giá hiện tại.
     *
     * @param numberAxis trục Y của chart
     * @param openingPrice giá khởi điểm
     * @param currentPrice giá hiện tại
     */
    static void updateAxis(
        final NumberAxis numberAxis,
        final long openingPrice,
        final long currentPrice
    ) {
        final double padding = Math.max(50_000D, currentPrice * 0.1);
        final double suggestedLower = Math.max(0D, openingPrice - padding);
        final double suggestedUpper = currentPrice + padding;

        if (suggestedLower < numberAxis.getLowerBound()) {
            numberAxis.setLowerBound(suggestedLower);
        }

        if (suggestedUpper > numberAxis.getUpperBound()) {
            numberAxis.setUpperBound(suggestedUpper);
        }
    }
}
