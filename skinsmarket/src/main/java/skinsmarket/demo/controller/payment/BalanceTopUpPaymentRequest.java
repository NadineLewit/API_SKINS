package skinsmarket.demo.controller.payment;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BalanceTopUpPaymentRequest extends TestCardPaymentRequest {

    private Double amountArs;
}
