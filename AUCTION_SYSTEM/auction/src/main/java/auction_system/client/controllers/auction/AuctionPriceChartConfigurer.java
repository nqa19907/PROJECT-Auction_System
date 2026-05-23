package auction_system.client.controllers.auction;

import java.text.DecimalFormat;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.util.StringConverter;

/**
 * Cấu hình biểu đồ lịch sử giá của màn hình chi tiết đấu giá.
 */
final class AuctionPriceChartConfigurer {

    /** Tên series hiển thị trên biểu đồ. */
    private static final String PRICE_SERIES_NAME = "Giá trả";
    /** Số vạch chia chính mong muốn trên trục Y. */
    private static final int TARGET_MAJOR_TICK_COUNT = 6;
    /** Tick nhỏ nhất cho trục giá để tránh bước chia lẻ khó đọc. */
    private static final double MIN_TICK_UNIT = 50_000D;
    /** Mức trần tối thiểu khi phiên chưa có giá hợp lệ. */
    private static final double MIN_UPPER_BOUND = 100_000D;
    /** Formatter cho nhãn số tiền trên trục Y. */
    private static final DecimalFormat AXIS_NUMBER_FORMAT = new DecimalFormat("#,##0");

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
     * @param priceSeries series dữ liệu giá đang hiển thị
     */
    static void updateAxis(
        final NumberAxis numberAxis,
        final long openingPrice,
        final XYChart.Series<String, Number> priceSeries
    ) {
        double minPrice = Double.MAX_VALUE;
        double maxPrice = 0D;

        for (XYChart.Data<String, Number> data : priceSeries.getData()) {
            final Number value = data.getYValue();
            if (value == null) {
                continue;
            }

            final double price = value.doubleValue();
            if (price <= 0D) {
                continue;
            }

            minPrice = Math.min(minPrice, price);
            maxPrice = Math.max(maxPrice, price);
        }

        if (maxPrice <= 0D) {
            maxPrice = Math.max(MIN_UPPER_BOUND, openingPrice);
            minPrice = maxPrice;
        }

        final double range = Math.max(MIN_TICK_UNIT, maxPrice - minPrice);
        final double padding = Math.max(MIN_TICK_UNIT, range * 0.2D);
        final double rawLowerBound = Math.max(0D, minPrice - padding);
        final double rawUpperBound = Math.max(
                rawLowerBound + MIN_UPPER_BOUND,
                maxPrice + padding
        );
        final double tickUnit = calculateTickUnit(rawUpperBound - rawLowerBound);
        final double lowerBound = Math.floor(rawLowerBound / tickUnit) * tickUnit;
        final double upperBound = Math.ceil(rawUpperBound / tickUnit) * tickUnit;

        numberAxis.setLowerBound(lowerBound);
        numberAxis.setUpperBound(upperBound);
        numberAxis.setForceZeroInRange(false);
        numberAxis.setMinorTickVisible(false);
        numberAxis.setTickUnit(tickUnit);
        numberAxis.setTickLabelFormatter(createTickLabelFormatter());
    }

    /**
     * Tạo formatter để nhãn trục tiền không hiển thị phần thập phân.
     *
     * @return formatter cho tick label
     */
    private static StringConverter<Number> createTickLabelFormatter() {
        return new StringConverter<>() {
            @Override
            public String toString(final Number value) {
                if (value == null) {
                    return "";
                }

                return AXIS_NUMBER_FORMAT.format(Math.round(value.doubleValue()));
            }

            @Override
            public Number fromString(final String value) {
                return 0;
            }
        };
    }

    /**
     * Tính bước chia trục theo dạng dễ đọc: 1, 2, 5, 10 * 10^n.
     *
     * @param range khoảng giá đang hiển thị
     * @return bước chia phù hợp cho trục Y
     */
    private static double calculateTickUnit(final double range) {
        final double rawTickUnit = Math.max(
                MIN_TICK_UNIT,
                range / TARGET_MAJOR_TICK_COUNT
        );
        final double magnitude = Math.pow(
                10D,
                Math.floor(Math.log10(rawTickUnit))
        );
        final double normalized = rawTickUnit / magnitude;

        if (normalized <= 1D) {
            return magnitude;
        }

        if (normalized <= 2D) {
            return 2D * magnitude;
        }

        if (normalized <= 5D) {
            return 5D * magnitude;
        }

        return 10D * magnitude;
    }
}
