package auction_system.client.controllers.auction;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * Quản lý đồng hồ đếm ngược của phiên đấu giá.
 */
final class AuctionCountdownTimer {

    /** Số giây mặc định còn lại của phiên đấu giá demo. */
    static final int DEFAULT_SECONDS_LEFT = 14 * 60 + 32;

    /** Label hiển thị thời gian còn lại. */
    private final Label timerLabel;

    /** Timeline cập nhật đồng hồ mỗi giây. */
    private final Timeline timeline;

    /** Số giây còn lại. */
    private int secondsLeft;

    /**
     * Tạo timer cho label hiển thị thời gian.
     *
     * @param timerLabel label cần cập nhật
     * @param initialSeconds số giây ban đầu
     */
    AuctionCountdownTimer(final Label timerLabel, final int initialSeconds) {
        this.timerLabel = timerLabel;
        this.secondsLeft = initialSeconds;
        this.timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> tick()));
        this.timeline.setCycleCount(Animation.INDEFINITE);
    }

    /**
     * Bắt đầu chạy đồng hồ đếm ngược.
     */
    void start() {
        updateLabel();
        timeline.play();
    }

    /**
     * Dừng đồng hồ đếm ngược.
     */
    void stop() {
        timeline.stop();
    }

    /**
     * Giảm thời gian còn lại và cập nhật label.
     */
    private void tick() {
        if (secondsLeft > 0) {
            secondsLeft--;
        }

        updateLabel();
    }

    /**
     * Cập nhật text hiển thị thời gian.
     */
    private void updateLabel() {
        if (secondsLeft == 0) {
            timerLabel.setText("Kết thúc");
            return;
        }

        final int minutes = secondsLeft / 60;
        final int seconds = secondsLeft % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }
}
