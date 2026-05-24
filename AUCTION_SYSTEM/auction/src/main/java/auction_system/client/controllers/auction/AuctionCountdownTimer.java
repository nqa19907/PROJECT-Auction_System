package auction_system.client.controllers.auction;

import java.time.LocalDateTime;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;

/**
 * Quản lý đồng hồ đếm ngược theo thời gian kết thúc thật của phiên đấu giá.
 */
final class AuctionCountdownTimer {

    /** Label hiển thị thời gian còn lại. */
    private final Label timerLabel;

    /** Thời gian kết thúc thật của phiên đấu giá. */
    private final LocalDateTime endTime;

    /** Callback chạy một lần khi phiên đấu giá hết giờ. */
    private final Runnable onFinished;

    /** Timeline cập nhật đồng hồ mỗi giây. */
    private final Timeline timeline;

    /** Đánh dấu callback kết thúc đã được gọi. */
    private boolean finishedNotified;

    /**
     * Tạo timer cho label hiển thị thời gian.
     *
     * @param timerLabel label cần cập nhật
     * @param endTime thời gian kết thúc thật của phiên
     * @param onFinished callback khi hết giờ
     */
    AuctionCountdownTimer(
            final Label timerLabel,
            final LocalDateTime endTime,
            final Runnable onFinished) {
        this.timerLabel = timerLabel;
        this.endTime = endTime;
        this.onFinished = onFinished;
        this.timeline = new Timeline(
                new KeyFrame(javafx.util.Duration.seconds(1), event -> updateLabel()));
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
     * Cập nhật text hiển thị thời gian.
     */
    private void updateLabel() {
        final long secondsLeft =
                java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();

        if (secondsLeft <= 0) {
            timerLabel.setText("Kết thúc");
            notifyFinishedOnce();
            timeline.stop();
            return;
        }

        final long hours = secondsLeft / 3600;
        final long minutes = (secondsLeft % 3600) / 60;
        final long seconds = secondsLeft % 60;

        if (hours > 0) {
            timerLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            return;
        }

        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    /**
     * Gọi callback kết thúc tối đa một lần.
     */
    private void notifyFinishedOnce() {
        if (finishedNotified || onFinished == null) {
            return;
        }

        finishedNotified = true;
        onFinished.run();
    }
}
