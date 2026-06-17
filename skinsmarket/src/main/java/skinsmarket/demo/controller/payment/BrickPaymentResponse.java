package skinsmarket.demo.controller.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import skinsmarket.demo.controller.order.OrderResponse;

@Data
@AllArgsConstructor
public class BrickPaymentResponse {

    private OrderResponse order;
    private Long paymentId;
    private String status;
    private String statusDetail;
    private String paymentMethodId;
    private String paymentTypeId;
    private String externalResourceUrl;
}
