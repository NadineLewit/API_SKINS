package skinsmarket.demo.controller.order;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SellerSaleResponse {
    private Long orderId;
    private Long skinId;
    private String skinName;
    private String imageUrl;
    private Double unitPrice;
    private String paymentStatus;
    private String tradeStatus;
    private LocalDateTime date;
}
