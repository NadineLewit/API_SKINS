package skinsmarket.demo.controller.payment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.mercadopago.exceptions.MPApiException;
import skinsmarket.demo.controller.common.ApiResponse;
import skinsmarket.demo.service.PaymentService;

@RestController
@RequestMapping("payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/bricks/preferences/from-carrito")
    public ResponseEntity<?> createBrickPreferenceFromCarrito(
            Authentication auth,
            @RequestParam(required = false) String codigoCupon) {
        try {
            BrickPreferenceResponse response =
                    paymentService.createBrickPreferenceFromCarrito(auth.getName(), codigoCupon);
            return ResponseEntity.status(201)
                    .body(ApiResponse.of("Preferencia para Payment Brick creada", response));
        } catch (Exception e) {
            return mercadoPagoError(e);
        }
    }

    @PostMapping("/bricks/preferences/orders/{orderId}")
    public ResponseEntity<?> createBrickPreferenceForOrder(
            Authentication auth,
            @PathVariable Long orderId) {
        try {
            BrickPreferenceResponse response =
                    paymentService.createBrickPreferenceForExistingOrder(auth.getName(), orderId);
            return ResponseEntity.status(201)
                    .body(ApiResponse.of("Preferencia para Payment Brick creada", response));
        } catch (Exception e) {
            return mercadoPagoError(e);
        }
    }

    @PostMapping("/bricks/orders/{orderId}/process-payment")
    public ResponseEntity<?> processBrickPayment(
            Authentication auth,
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) BrickPaymentRequest request) {
        try {
            BrickPaymentResponse response =
                    paymentService.processBrickPayment(auth.getName(), orderId, request, idempotencyKey);
            return ResponseEntity.status(201)
                    .body(ApiResponse.of("Pago procesado por Mercado Pago", response));
        } catch (Exception e) {
            return mercadoPagoError(e);
        }
    }

    @PostMapping("/bricks/orders/{orderId}/process-test-card")
    public ResponseEntity<?> processTestCardPayment(
            Authentication auth,
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) TestCardPaymentRequest request) {
        try {
            BrickPaymentResponse response =
                    paymentService.processTestCardPayment(auth.getName(), orderId, request, idempotencyKey);
            return ResponseEntity.status(201)
                    .body(ApiResponse.of("Pago de prueba procesado por Mercado Pago", response));
        } catch (Exception e) {
            return mercadoPagoError(e);
        }
    }

    @PostMapping("/bricks/orders/{orderId}/process-balance")
    public ResponseEntity<?> processBalancePayment(
            Authentication auth,
            @PathVariable Long orderId) {
        try {
            BrickPaymentResponse response =
                    paymentService.processBalancePayment(auth.getName(), orderId);
            return ResponseEntity.status(201)
                    .body(ApiResponse.of("Compra pagada con saldo", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        }
    }

    @PostMapping("/bricks/balance/process-test-card")
    public ResponseEntity<?> processBalanceTopUpTestCard(
            Authentication auth,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) BalanceTopUpPaymentRequest request) {
        try {
            BrickPaymentResponse response =
                    paymentService.processBalanceTopUpTestCard(auth.getName(), request, idempotencyKey);
            return ResponseEntity.status(201)
                    .body(ApiResponse.of("Recarga procesada por Mercado Pago", response));
        } catch (Exception e) {
            return mercadoPagoError(e);
        }
    }

    @PostMapping("/bricks/orders/{orderId}/sync")
    public ResponseEntity<?> syncPaymentStatus(Authentication auth, @PathVariable Long orderId) {
        try {
            BrickPaymentResponse response = paymentService.syncPaymentStatus(auth.getName(), orderId);
            return ResponseEntity.ok(ApiResponse.of("Estado de pago sincronizado", response));
        } catch (Exception e) {
            return mercadoPagoError(e);
        }
    }

    @PostMapping("/bricks/orders/sync-pending")
    public ResponseEntity<?> syncPendingPayments(Authentication auth) {
        try {
            BrickPaymentResponse response = paymentService.syncPendingPayments(auth.getName());
            return ResponseEntity.ok(ApiResponse.of("Pagos pendientes sincronizados", response));
        } catch (Exception e) {
            return mercadoPagoError(e);
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> receiveWebhook(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String id,
            @RequestBody(required = false) MercadoPagoWebhookRequest body) {
        paymentService.processWebhook(type, topic, id, body);
        return ResponseEntity.ok(ApiResponse.of("Notificacion recibida"));
    }

    @GetMapping("/return/success")
    public ResponseEntity<?> successReturn() {
        return ResponseEntity.ok(ApiResponse.of("Pago aprobado. La confirmación final se procesa por webhook."));
    }

    @GetMapping("/return/failure")
    public ResponseEntity<?> failureReturn() {
        return ResponseEntity.badRequest().body(ApiResponse.of("Pago rechazado o cancelado."));
    }

    @GetMapping("/return/pending")
    public ResponseEntity<?> pendingReturn() {
        return ResponseEntity.ok(ApiResponse.of("Pago pendiente. La confirmación final se procesa por webhook."));
    }

    private ResponseEntity<?> mercadoPagoError(Exception e) {
        if (e instanceof MPApiException apiException) {
            String detail = apiException.getApiResponse() != null
                    ? apiException.getApiResponse().getContent()
                    : apiException.getMessage();
            return ResponseEntity.status(apiException.getStatusCode())
                    .body(ApiResponse.of("Mercado Pago API error: " + detail));
        }
        return ResponseEntity.badRequest().body(ApiResponse.of(safeErrorMessage(e)));
    }

    private String safeErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return "No se pudo procesar el pago. Intenta nuevamente.";
        }

        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("sql") ||
                lowerMessage.contains("could not execute statement") ||
                lowerMessage.contains("deadlock") ||
                lowerMessage.contains("constraint")) {
            return "No se pudo procesar el pago. Intenta nuevamente.";
        }

        return message;
    }
}
