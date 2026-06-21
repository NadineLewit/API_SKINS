package skinsmarket.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skinsmarket.demo.controller.payment.BrickPaymentResponse;
import skinsmarket.demo.entity.OperationType;
import skinsmarket.demo.entity.Order;
import skinsmarket.demo.entity.TradeStatus;
import skinsmarket.demo.entity.User;
import skinsmarket.demo.repository.OrderRepository;
import skinsmarket.demo.repository.SkinRepository;
import skinsmarket.demo.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private SkinRepository skinRepository;
    @Mock private BotTradeOrdersFileService botFileService;

    @InjectMocks private PaymentServiceImpl paymentService;

    private User user;
    private Order order;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(7L)
                .username("buyer")
                .email("buyer@test.local")
                .password("encoded")
                .firstName("Test")
                .lastName("Buyer")
                .saldo(10.0)
                .build();

        order = new Order();
        order.setId(21L);
        order.setUser(user);
        order.setDate(LocalDateTime.now());
        order.setTotalPrice(3.0);
        order.setTotalFinal(3.0);
        order.setPaymentStatus("PENDING_PAYMENT");
        order.setOperationType(OperationType.PURCHASE);
        order.setTradeStatus(TradeStatus.WAITING_PAYMENT);

        when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));
        when(userRepository.findByEmailForUpdate(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    void paysPurchaseWithAvailableBalance() {
        BrickPaymentResponse response = paymentService.processBalancePayment(user.getEmail(), 21L);

        assertEquals("approved", response.getStatus());
        assertEquals("PAID", order.getPaymentStatus());
        assertEquals(7.0, user.getSaldo());
        verify(userRepository).save(user);
        verify(orderRepository).save(order);
    }

    @Test
    void rejectsPurchaseWhenBalanceIsInsufficient() {
        user.setSaldo(2.99);

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> paymentService.processBalancePayment(user.getEmail(), 21L));

        assertEquals("Saldo insuficiente. Te faltan $0,01 USD.", error.getMessage());
        assertEquals("PENDING_PAYMENT", order.getPaymentStatus());
        verify(userRepository, never()).save(user);
        verify(orderRepository, never()).save(order);
    }

    @Test
    void doesNotChargeBalanceTwiceWhenOrderIsAlreadyPaid() {
        order.setPaymentStatus("PAID");

        BrickPaymentResponse response = paymentService.processBalancePayment(user.getEmail(), 21L);

        assertEquals("approved", response.getStatus());
        assertEquals(10.0, user.getSaldo());
        verify(userRepository, never()).save(user);
        verify(orderRepository, never()).save(order);
    }

    @Test
    void rejectsBalanceWhenMercadoPagoWasAlreadyStarted() {
        order.setMercadopagoPreferenceId("LOCAL-21");

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> paymentService.processBalancePayment(user.getEmail(), 21L));

        assertEquals("Ya iniciaste el pago con Mercado Pago para esta orden", error.getMessage());
        assertEquals(10.0, user.getSaldo());
        verify(userRepository, never()).save(user);
        verify(orderRepository, never()).save(order);
    }
}
