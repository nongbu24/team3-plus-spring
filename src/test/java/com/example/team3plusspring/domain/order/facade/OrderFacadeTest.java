package com.example.team3plusspring.domain.order.facade;

import com.example.team3plusspring.domain.cart.service.CartService;
import com.example.team3plusspring.domain.coupon.service.UserCouponService;
import com.example.team3plusspring.domain.order.dto.CancelOrderResponse;
import com.example.team3plusspring.domain.order.dto.GetOneOrderResponse;
import com.example.team3plusspring.domain.order.dto.GetOrderListResponse;
import com.example.team3plusspring.domain.order.entity.Order;
import com.example.team3plusspring.domain.order.entity.OrderItem;
import com.example.team3plusspring.domain.order.entity.OrderStatus;
import com.example.team3plusspring.domain.order.service.OrderService;
import com.example.team3plusspring.domain.payment.entity.Payment;
import com.example.team3plusspring.domain.payment.entity.PaymentStatus;
import com.example.team3plusspring.domain.payment.service.PaymentService;
import com.example.team3plusspring.domain.product.service.ProductService;
import com.example.team3plusspring.domain.user.service.UserService;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long ORDER_ID = 10L;
    private static final Long PAYMENT_ID = 20L;

    @Mock
    UserService userService;

    @Mock
    ProductService productService;

    @Mock
    UserCouponService userCouponService;

    @Mock
    OrderService orderService;

    @Mock
    PaymentService paymentService;

    @Mock
    CartService cartService;

    @InjectMocks
    OrderFacade orderFacade;

    @Test
    void 주문취소_결제시작전본인주문이면_상태를변경하고재고와쿠폰을복구한다() {
        // given
        Order order = readyOrder(USER_ID);
        Payment payment = payment();
        List<OrderItem> orderItems = List.of(org.mockito.Mockito.mock(OrderItem.class));

        when(paymentService.findPaymentForUpdateByOrderId(ORDER_ID)).thenReturn(payment);
        when(orderService.findOrderForUpdate(ORDER_ID)).thenReturn(order);
        when(orderService.findOrderItems(ORDER_ID)).thenReturn(orderItems);

        // when
        CancelOrderResponse response = orderFacade.cancel(USER_ID, ORDER_ID);

        // then
        assertThat(response.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(response.getPreviousStatus()).isEqualTo(OrderStatus.READY);
        assertThat(response.getCurrentStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(response.getCanceledAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        verify(productService).restoreStocks(orderItems);
        verify(userCouponService).restoreCouponByOrderId(ORDER_ID);
    }

    @Test
    void 주문취소_이미결제를시작한주문이면_실패하고재고와쿠폰을복구하지않는다() {
        // given
        Order order = readyOrder(USER_ID);
        order.markAsPaymentPending();
        Payment payment = payment();

        when(paymentService.findPaymentForUpdateByOrderId(ORDER_ID)).thenReturn(payment);
        when(orderService.findOrderForUpdate(ORDER_ID)).thenReturn(order);

        // when & then
        assertThatThrownBy(() -> orderFacade.cancel(USER_ID, ORDER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_CANCEL_NOT_ALLOWED));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(orderService, never()).findOrderItems(ORDER_ID);
        verifyNoInteractions(productService, userCouponService);
    }

    @Test
    void 주문취소_타인의주문이면_실패한다() {
        // given
        Order order = readyOrder(OTHER_USER_ID);
        Payment payment = payment();
        when(paymentService.findPaymentForUpdateByOrderId(ORDER_ID)).thenReturn(payment);
        when(orderService.findOrderForUpdate(ORDER_ID)).thenReturn(order);

        // when & then
        assertThatThrownBy(() -> orderFacade.cancel(USER_ID, ORDER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_ACCESS_DENIED));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verifyNoInteractions(productService, userCouponService);
    }

    @Test
    void 주문상세조회_본인주문이면_주문정보와주문상품을반환한다() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 29, 12, 0);
        Order order = order(USER_ID, createdAt);
        OrderItem firstItem = orderItem(100L, 1000L, "첫 번째 상품", 10_000, 2);
        OrderItem secondItem = orderItem(101L, 1001L, "두 번째 상품", 5_000, 1);

        when(orderService.findOrder(ORDER_ID)).thenReturn(order);
        when(orderService.findOrderItems(ORDER_ID)).thenReturn(List.of(firstItem, secondItem));

        // when
        GetOneOrderResponse response = orderFacade.getOneOrder(USER_ID, ORDER_ID);

        // then
        assertThat(response.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(response.getOrderNumber()).isEqualTo("order-number");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(response.getTotalProductAmount()).isEqualTo(25_000);
        assertThat(response.getUsedCouponAmount()).isEqualTo(5_000);
        assertThat(response.getPaymentAmount()).isEqualTo(20_000);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getCanceledAt()).isNull();
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getOrderItemId()).isEqualTo(100L);
        assertThat(response.getItems().get(0).getLineAmount()).isEqualTo(20_000);
        assertThat(response.getItems().get(1).getOrderItemId()).isEqualTo(101L);
        assertThat(response.getItems().get(1).getLineAmount()).isEqualTo(5_000);
        verify(orderService).findOrderItems(ORDER_ID);
    }

    @Test
    void 주문상세조회_타인의주문이면_접근이거부된다() {
        // given
        Order order = org.mockito.Mockito.mock(Order.class);
        when(order.getUserId()).thenReturn(OTHER_USER_ID);
        when(orderService.findOrder(ORDER_ID)).thenReturn(order);

        // when & then
        assertThatThrownBy(() -> orderFacade.getOneOrder(USER_ID, ORDER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_ACCESS_DENIED));
        verify(orderService, never()).findOrderItems(anyLong());
    }

    @Test
    void 주문상세조회_주문이존재하지않으면_실패한다() {
        // given
        when(orderService.findOrder(ORDER_ID))
                .thenThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> orderFacade.getOneOrder(USER_ID, ORDER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND));
        verify(orderService, never()).findOrderItems(anyLong());
    }

    @Test
    void 주문목록조회_조회된주문을응답페이지로변환한다() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 29, 12, 0);
        Order order = orderListItem(createdAt);
        Page<Order> orders = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);

        when(orderService.findOrders(USER_ID, OrderStatus.COMPLETED, 0, 10)).thenReturn(orders);

        // when
        Page<GetOrderListResponse> response = orderFacade.getOrderList(
                USER_ID,
                OrderStatus.COMPLETED,
                0,
                10
        );

        // then
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getNumber()).isZero();
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getContent()).singleElement().satisfies(item -> {
            assertThat(item.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(item.getOrderNumber()).isEqualTo("order-number");
            assertThat(item.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            assertThat(item.getTotalProductAmount()).isEqualTo(25_000);
            assertThat(item.getUsedCouponAmount()).isEqualTo(5_000);
            assertThat(item.getPaymentAmount()).isEqualTo(20_000);
            assertThat(item.getCreatedAt()).isEqualTo(createdAt);
        });
    }

    private Order order(Long userId, LocalDateTime createdAt) {
        Order order = org.mockito.Mockito.mock(Order.class);
        when(order.getId()).thenReturn(ORDER_ID);
        when(order.getUserId()).thenReturn(userId);
        when(order.getOrderNumber()).thenReturn("order-number");
        when(order.getStatus()).thenReturn(OrderStatus.PAYMENT_PENDING);
        when(order.getTotalProductAmount()).thenReturn(25_000);
        when(order.getUsedCouponAmount()).thenReturn(5_000);
        when(order.getPaymentAmount()).thenReturn(20_000);
        when(order.getCreatedAt()).thenReturn(createdAt);
        when(order.getCanceled_at()).thenReturn(null);
        return order;
    }

    private OrderItem orderItem(Long orderItemId, Long productId, String productName, int unitPrice, int quantity) {
        OrderItem orderItem = org.mockito.Mockito.mock(OrderItem.class);
        when(orderItem.getId()).thenReturn(orderItemId);
        when(orderItem.getProductId()).thenReturn(productId);
        when(orderItem.getProductName()).thenReturn(productName);
        when(orderItem.getUnitPrice()).thenReturn(unitPrice);
        when(orderItem.getQuantity()).thenReturn(quantity);
        when(orderItem.getLineAmount()).thenReturn(unitPrice * quantity);
        return orderItem;
    }

    private Order orderListItem(LocalDateTime createdAt) {
        Order order = org.mockito.Mockito.mock(Order.class);
        when(order.getId()).thenReturn(ORDER_ID);
        when(order.getOrderNumber()).thenReturn("order-number");
        when(order.getStatus()).thenReturn(OrderStatus.COMPLETED);
        when(order.getTotalProductAmount()).thenReturn(25_000);
        when(order.getUsedCouponAmount()).thenReturn(5_000);
        when(order.getPaymentAmount()).thenReturn(20_000);
        when(order.getCreatedAt()).thenReturn(createdAt);
        return order;
    }

    private Order readyOrder(Long userId) {
        Order order = Order.create(userId, 10_000, 0);
        ReflectionTestUtils.setField(order, "id", ORDER_ID);
        return order;
    }

    private Payment payment() {
        Payment payment = Payment.create(ORDER_ID, 10_000, 0);
        ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);
        return payment;
    }
}
