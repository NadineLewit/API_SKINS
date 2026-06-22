package skinsmarket.demo.controller.payment;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class TestCardPaymentRequest {

    private String cardNumber;
    private Integer expirationMonth;
    private Integer expirationYear;
    private String securityCode;
    private String cardholderName;
    private String email;
    private String documentType;
    private String documentNumber;
    private Integer installments;
    @JsonAlias({"payment_method_id", "paymentMethodId"})
    private String paymentMethodId;
}
