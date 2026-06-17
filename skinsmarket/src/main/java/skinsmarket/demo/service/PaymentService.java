package skinsmarket.demo.service;

import skinsmarket.demo.controller.payment.BrickPaymentRequest;
import skinsmarket.demo.controller.payment.BrickPaymentResponse;
import skinsmarket.demo.controller.payment.BrickPreferenceResponse;
import skinsmarket.demo.controller.payment.MercadoPagoWebhookRequest;
import skinsmarket.demo.controller.payment.TestCardPaymentRequest;

public interface PaymentService {

    BrickPreferenceResponse createBrickPreferenceFromCarrito(String email, String codigoCupon) throws Exception;

    BrickPreferenceResponse createBrickPreferenceForExistingOrder(String email, Long orderId) throws Exception;

    BrickPaymentResponse processBrickPayment(String email, Long orderId, BrickPaymentRequest request, String idempotencyKey) throws Exception;

    BrickPaymentResponse processTestCardPayment(String email, Long orderId, TestCardPaymentRequest request, String idempotencyKey) throws Exception;

    BrickPaymentResponse syncPaymentStatus(String email, Long orderId) throws Exception;

    BrickPaymentResponse syncPendingPayments(String email) throws Exception;

    void processWebhook(String type, String topic, String id, MercadoPagoWebhookRequest body);
}
