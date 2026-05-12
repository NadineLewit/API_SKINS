package skinsmarket.demo.controller.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import skinsmarket.demo.controller.order.OrderResponse;

@Data
@AllArgsConstructor
public class BrickPreferenceResponse {

    private OrderResponse order;
    private String preferenceId;
    private String publicKey;
    private String paymentStatus;
}
