package skinsmarket.demo.controller.payment;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BalanceTopUpPaymentRequest extends TestCardPaymentRequest {

    private Double amountArs;
    private String token;

    @JsonAlias({"issuer_id", "issuerId", "issuer"})
    private String issuerId;

    @JsonAlias({"transaction_amount", "transactionAmount"})
    private Double transactionAmount;

    private BrickPaymentRequest.Payer payer;
}
