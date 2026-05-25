package auction_system.client.controllers.auction;

import java.time.LocalDateTime;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;

/**
 * Quản lý animation/timeline riêng của màn hình AuctionDetail.
 */
final class AuctionDetailVisuals {

    private final Label timerLabel;
    private final LiveIndicatorAnimation liveIndicatorAnimation;
    private AuctionCountdownTimer countdownTimer;

    AuctionDetailVisuals(final Label timerLabel, final Circle liveDot) {
        this.timerLabel = timerLabel;
        this.liveIndicatorAnimation = new LiveIndicatorAnimation(liveDot);
    }

    void start() {
        liveIndicatorAnimation.start();
    }

    void startCountdown(final LocalDateTime endTime, final Runnable onFinished) {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        countdownTimer = new AuctionCountdownTimer(timerLabel, endTime, onFinished);
        countdownTimer.start();
    }

    void stop() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        liveIndicatorAnimation.stop();
    }
}
