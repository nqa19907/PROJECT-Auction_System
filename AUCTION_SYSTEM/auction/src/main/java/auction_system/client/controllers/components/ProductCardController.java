package auction_system.client.controllers.components;

import auction_system.client.utils.ProductImageStyleUtil;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Controller điều khiển giao diện của từng thẻ sản phẩm trong danh sách.
 */
public class ProductCardController {

    @FXML private VBox cardContainer;
    @FXML private Region imageRegion;
    @FXML private Label titleLabel;
    @FXML private Label priceLabel;
    @FXML private Button bidButton;

    // Lưu ID để biết thẻ này đại diện cho mòn hàng nào
    private String currentItemId;

    // Callback: Truyền ID sản phẩm ngược lên cho màn hình cha xử lý
    private Consumer<String> onBidActionHandler;

    /**
     * Khởi tạo giao diện thẻ sản phẩm.
     */
    @FXML
    public void initialize() {
        // Tạm thời vô hiệu hóa nút bấm khi chưa có dữ liệu thật
        bidButton.setDisable(true);

        // Gán sự kiện bằng code. Khi bấm, ném ID sản phẩm cho Handler
        bidButton.setOnAction(event -> {
            if (onBidActionHandler != null && currentItemId != null) {
                onBidActionHandler.accept(currentItemId);
            }
        });

    }

    /**
     * Cập nhật thông tin hiển thị của thẻ sản phẩm.
     *
     * @param itemId             Mã định danh của sản phẩm.
     * @param title              Tiêu đề hiển thị của sản phẩm.
     * @param currentPrice       Giá thầu hiện tại.
     * @param onBidClickCallback Hàm callback tiếp nhận ID gửi tín hiệu lên màn hình cha.
     */
    public void setCardDetails(String itemId, String title, double currentPrice,
                                Consumer<String> onBidClickCallback) {
        // Giữ API cũ hoạt động khi card chưa nhận ảnh.
        setCardDetails(itemId, title, currentPrice, "", onBidClickCallback);
    }

    /**
     * Cập nhật thông tin hiển thị của thẻ sản phẩm kèm ảnh.
     *
     * @param itemId             Mã định danh của sản phẩm.
     * @param title              Tiêu đề hiển thị của sản phẩm.
     * @param currentPrice       Giá thầu hiện tại.
     * @param imagePath          Đường dẫn ảnh sản phẩm.
     * @param onBidClickCallback Hàm callback tiếp nhận ID gửi tín hiệu lên màn hình cha.
     */
    public void setCardDetails(String itemId, String title, double currentPrice,
                                String imagePath, Consumer<String> onBidClickCallback) {
        this.currentItemId = itemId;
        this.titleLabel.setText(title);
        this.priceLabel.setText(String.format("Giá hiện tại: %,.0f VNĐ", currentPrice));
        // Áp dụng ảnh sản phẩm động nếu dữ liệu có đường dẫn hợp lệ.
        applyProductImage(imagePath);
        
        // Nhận handler ủy quyền từ cha
        this.onBidActionHandler = onBidClickCallback;

        bidButton.setDisable(false);
    }

    /**
     * Áp dụng ảnh sản phẩm vào vùng ảnh của card.
     *
     * @param imagePath đường dẫn ảnh sản phẩm
     */
    private void applyProductImage(final String imagePath) {
        // Ghi đè background mặc định bằng ảnh sản phẩm thật.
        imageRegion.setStyle(ProductImageStyleUtil.toBackgroundImageStyle(imagePath));
    }
}
