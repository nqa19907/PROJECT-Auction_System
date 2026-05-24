package auction_system.client.controllers.auction;

import javafx.scene.control.Label;
import javafx.scene.shape.Circle;

/**
 * Quản lý animation/timeline riêng của màn hình AuctionDetail.
 */
final class AuctionDetailVisuals {

    private final AuctionCountdownTimer countdownTimer;
    private final LiveIndicatorAnimation liveIndicatorAnimation;

    AuctionDetailVisuals(final Label timerLabel, final Circle liveDot) {
        this.liveIndicatorAnimation = new LiveIndicatorAnimation(liveDot);
        this.countdownTimer = new AuctionCountdownTimer(
                timerLabel,
                AuctionCountdownTimer.DEFAULT_SECONDS_LEFT);
    }

    void start() {
        liveIndicatorAnimation.start();
        countdownTimer.start();
    }

    void stop() {
        countdownTimer.stop();
        liveIndicatorAnimation.stop();
    }
}
