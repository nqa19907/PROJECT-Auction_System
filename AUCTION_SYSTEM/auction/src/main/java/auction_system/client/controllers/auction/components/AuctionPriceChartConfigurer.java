package auction_system.client.controllers.auction.components;

import java.text.DecimalFormat;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.util.StringConverter;

/**
 * Cấu hình biểu đồ lịch sử giá của màn hình chi tiết đấu giá.
 */
public final class AuctionPriceChartConfigurer {

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
     * @param numberXaxis trục X của chart
     * @param priceSeries series dữ liệu giá
     */
    public static void configure(
        final LineChart<Number, Number> bidLineChart,
        final NumberAxis numberXaxis,
        final XYChart.Series<Number, Number> priceSeries
    ) {
        priceSeries.setName(PRICE_SERIES_NAME);
        bidLineChart.getData().add(priceSeries);
        bidLineChart.setLegendVisible(false);
        bidLineChart.setCreateSymbols(true);
        bidLineChart.setAnimated(true);
        configureHorizontalAxis(numberXaxis, priceSeries);
    }

    /**
     * Cập nhật giới hạn trục X/Y theo dữ liệu hiện tại.
     *
     * @param numberXaxis trục X của chart
     * @param numberAxis trục Y của chart
     * @param openingPrice giá khởi điểm
     * @param priceSeries series dữ liệu giá đang hiển thị
     */
    public static void updateAxes(
        final NumberAxis numberXaxis,
        final NumberAxis numberAxis,
        final long openingPrice,
        final XYChart.Series<Number, Number> priceSeries
    ) {
        updateHorizontalAxis(numberXaxis, priceSeries);
        updateAxis(numberAxis, openingPrice, priceSeries);
    }

    /**
     * Cập nhật giới hạn trục Y theo giá hiện tại.
     *
     * @param numberAxis trục Y của chart
     * @param openingPrice giá khởi điểm
     * @param priceSeries series dữ liệu giá đang hiển thị
     */
    private static void updateAxis(
        final NumberAxis numberAxis,
        final long openingPrice,
        final XYChart.Series<Number, Number> priceSeries
    ) {
        double minPrice = Double.MAX_VALUE;
        double maxPrice = 0D;

        for (XYChart.Data<Number, Number> data : priceSeries.getData()) {
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
     * Cấu hình trục X dùng số thứ tự bid, nhưng hiển thị nhãn thời gian.
     *
     * @param numberXaxis trục X của chart
     * @param priceSeries series dữ liệu giá đang hiển thị
     */
    private static void configureHorizontalAxis(
            final NumberAxis numberXaxis,
            final XYChart.Series<Number, Number> priceSeries) {
        numberXaxis.setAutoRanging(false);
        numberXaxis.setForceZeroInRange(false);
        numberXaxis.setMinorTickVisible(false);
        numberXaxis.setTickUnit(1D);
        numberXaxis.setTickLabelFormatter(createTimeTickLabelFormatter(priceSeries));
    }

    /**
     * Cập nhật biên trục X để mỗi lượt bid chiếm một vị trí riêng.
     *
     * @param numberXaxis trục X của chart
     * @param priceSeries series dữ liệu giá đang hiển thị
     */
    private static void updateHorizontalAxis(
            final NumberAxis numberXaxis,
            final XYChart.Series<Number, Number> priceSeries) {
        configureHorizontalAxis(numberXaxis, priceSeries);

        final int pointCount = priceSeries.getData().size();
        numberXaxis.setLowerBound(0D);
        numberXaxis.setUpperBound(Math.max(2D, pointCount + 1D));
    }

    /**
     * Tạo formatter đổi số thứ tự trên trục X thành timestamp của điểm tương ứng.
     *
     * @param priceSeries series dữ liệu giá đang hiển thị
     * @return formatter cho tick label trục X
     */
    private static StringConverter<Number> createTimeTickLabelFormatter(
            final XYChart.Series<Number, Number> priceSeries) {
        return new StringConverter<>() {
            @Override
            public String toString(final Number value) {
                if (value == null) {
                    return "";
                }

                final int bidIndex = (int) Math.round(value.doubleValue());
                if (Math.abs(value.doubleValue() - bidIndex) > 0.001D || bidIndex <= 0) {
                    return "";
                }

                for (XYChart.Data<Number, Number> point : priceSeries.getData()) {
                    final Number xValue = point.getXValue();
                    if (xValue != null && xValue.intValue() == bidIndex) {
                        final Object label = point.getExtraValue();
                        return label == null ? "" : label.toString();
                    }
                }

                return "";
            }

            @Override
            public Number fromString(final String value) {
                return 0;
            }
        };
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
