package skinsmarket.demo.controller.payment;

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
    private String paymentMethodId;
}
