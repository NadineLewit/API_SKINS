package skinsmarket.demo.controller.payment;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BrickPaymentRequest {

    @JsonAlias({"transaction_amount", "transactionAmount", "amount"})
    private BigDecimal transactionAmount;

    private String token;

    private String description;

    private Integer installments;

    @JsonAlias({"payment_method_id", "paymentMethodId"})
    private String paymentMethodId;

    @JsonAlias({"issuer_id", "issuerId", "issuer"})
    private String issuerId;

    private Payer payer;

    @Data
    public static class Payer {
        private String email;

        @JsonAlias({"first_name", "firstName", "name"})
        private String firstName;

        @JsonAlias({"last_name", "lastName"})
        private String lastName;

        private Identification identification;
    }

    @Data
    public static class Identification {
        private String type;
        private String number;
    }
}
