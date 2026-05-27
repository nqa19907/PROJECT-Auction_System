package auction_system.client.controllers.auction.components;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Quản lý animation nhấp nháy của live indicator.
 */
public final class LiveIndicatorAnimation {

    /** Thời lượng một nhịp nhấp nháy. */
    private static final int PULSE_MILLIS = 900;

    /** Animation nhấp nháy. */
    private final FadeTransition transition;

    /**
     * Tạo animation cho live dot.
     *
     * @param liveDot node cần áp dụng hiệu ứng
     */
    public LiveIndicatorAnimation(final Circle liveDot) {
        this.transition = new FadeTransition(Duration.millis(PULSE_MILLIS), liveDot);
        this.transition.setFromValue(1.0);
        this.transition.setToValue(0.2);
        this.transition.setCycleCount(Animation.INDEFINITE);
        this.transition.setAutoReverse(true);
    }

    /**
     * Bắt đầu animation.
     */
    public void start() {
        transition.play();
    }

    /**
     * Dừng animation.
     */
    public void stop() {
        transition.stop();
    }
}
