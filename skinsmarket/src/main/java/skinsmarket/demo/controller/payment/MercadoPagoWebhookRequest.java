package skinsmarket.demo.controller.payment;

import lombok.Data;

@Data
public class MercadoPagoWebhookRequest {

    private String action;
    private String type;
    private WebhookData data;

    @Data
    public static class WebhookData {
        private String id;
    }
}
