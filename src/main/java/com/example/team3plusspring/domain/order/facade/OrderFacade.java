package com.example.team3plusspring.domain.order.facade;

import com.example.team3plusspring.domain.coupon.service.UserCouponService;
import com.example.team3plusspring.domain.order.dto.CreateDirectOrderRequest;
import com.example.team3plusspring.domain.order.dto.CreateOrderResponse;
import com.example.team3plusspring.domain.order.dto.OrderItemResponse;
import com.example.team3plusspring.domain.order.entity.Order;
import com.example.team3plusspring.domain.order.entity.OrderItem;
import com.example.team3plusspring.domain.order.service.OrderService;
import com.example.team3plusspring.domain.payment.entity.Payment;
import com.example.team3plusspring.domain.payment.service.PaymentService;
import com.example.team3plusspring.domain.product.entity.Product;
import com.example.team3plusspring.domain.product.service.ProductService;
import com.example.team3plusspring.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final UserService userService;
    private final ProductService productService;
    private final UserCouponService userCouponService;
    private final OrderService orderService;
    private final PaymentService paymentService;

    @Transactional
    public CreateOrderResponse createDirectOrder(Long userId, @Valid CreateDirectOrderRequest request) {
        // 1. user 존재 여부 확인(탈퇴한 회원일 수 있음)
        userService.validateActiveUser(userId);

        // 2. product 조회 + product 상태 검증 + 재고 선차감
        Product product = productService.getOrderableProductAndDecreaseStock(
                request.getProductId(),
                request.getQuantity()
        );

        // 3. int usedCouponAmount = 0; 설정
        int totalProductAmount = product.getPrice() * request.getQuantity();
        int usedCouponAmount = 0;

        // 4. if CouponId 존재 -> Coupon 조회 (request.getCouponId 활용) -> usedCouponAmount 변경
        if (request.getUserCouponId() != null) {
            usedCouponAmount = userCouponService.calculateDiscountAmount(
                    userId,
                    request.getUserCouponId(),
                    totalProductAmount
            );
        }

        // 5. Order 생성
        Order order = orderService.createOrder(userId, totalProductAmount, usedCouponAmount);

        // 6. 쿠폰 사용처리
        if (request.getUserCouponId() != null) {
            userCouponService.useCoupon(userId, request.getUserCouponId(), order.getId());
        }

        // 8. OrderItem 생성
        OrderItem orderItem = orderService.createOrderItem(order, product, request.getQuantity());

        // 9. Payment 생성
        Payment payment = paymentService.createPayment(
                order.getId(),
                order.getTotalProductAmount(),
                order.getUsedCouponAmount()
        );

        // 10. List<OrderItemResponse> items 생성
        List<OrderItemResponse> items = List.of(OrderItemResponse.from(orderItem));

        return CreateOrderResponse.of(order, items, payment);
    }
}
