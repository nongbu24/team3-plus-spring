package com.example.team3plusspring.domain.order.facade;

import com.example.team3plusspring.domain.cart.entity.Cart;
import com.example.team3plusspring.domain.cart.entity.CartItem;
import com.example.team3plusspring.domain.cart.service.CartService;
import com.example.team3plusspring.domain.coupon.entity.UserCoupon;
import com.example.team3plusspring.domain.coupon.service.UserCouponService;
import com.example.team3plusspring.domain.order.dto.CreateDirectOrderRequest;
import com.example.team3plusspring.domain.order.dto.CreateOrderFromCartRequest;
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
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final UserService userService;
    private final ProductService productService;
    private final UserCouponService userCouponService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final CartService cartService;

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
        UserCoupon userCoupon = null;
        int totalProductAmount = product.getPrice() * request.getQuantity();
        int usedCouponAmount = 0;

        // 4. if CouponId 존재 -> Coupon 조회 (request.getCouponId 활용) -> usedCouponAmount 변경
        if (request.getUserCouponId() != null) {
            userCoupon = userCouponService.getUsableCouponForUpdate(userId, request.getUserCouponId());
            usedCouponAmount = userCouponService.calculateDiscountAmount(userCoupon, totalProductAmount);
        }

        // 5. Order 생성
        Order order = orderService.createOrder(userId, totalProductAmount, usedCouponAmount);

        // 6. 쿠폰 사용처리
        if (userCoupon != null) {
            userCouponService.useCoupon(userCoupon, order.getId());
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

    @Transactional
    public CreateOrderResponse createOrderFromCart(Long userId, @Valid CreateOrderFromCartRequest request) {
        // 1. user 존재 여부 확인(탈퇴한 회원일 수 있음)
        userService.validateActiveUser(userId);

        // 2. userId를 통해 cart 조회
        Cart cart = cartService.findByUser(userId);

        // 3. 주문할 장바구니 상품 조회(데드락 방지를 위해 productId가 작은 순으로 조회)
        List<CartItem> cartItems = cartService.findOrderCartItemsByCartId(cart.getId(), request.getCartItemIds());

        // 4. 요청한 cartItemId가 모두 내 장바구니에 있는지 검증
        int requestItemCount = request.getCartItemIds().stream()
                .collect(Collectors.toSet())
                .size();

        if (cartItems.size() != requestItemCount) {
            throw new BusinessException(ErrorCode.CART_ITEM_SELECTION_INVALID);
        }

        // 5. product 조회 + product 상태 검증 + 재고 선차감
        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .toList();
        List<Product> products = productService.getOrderableProductsForUpdate(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        cartItems.forEach(cartItem -> productMap.get(cartItem.getProductId()).decreaseStock(cartItem.getQuantity()));

        // 6. 총 상품 금액 계산
        int totalProductAmount = cartItems.stream()
                .mapToInt(cartItem -> {
                    Product product = productMap.get(cartItem.getProductId());
                    return product.getPrice() * cartItem.getQuantity();
                })
                .sum();

        // 7. int usedCouponAmount = 0; 설정
        UserCoupon userCoupon = null;
        int usedCouponAmount = 0;

        // 8. if CouponId 존재 -> Coupon 조회 -> usedCouponAmount 변경
        if (request.getUserCouponId() != null) {
            userCoupon = userCouponService.getUsableCouponForUpdate(userId, request.getUserCouponId());
            usedCouponAmount = userCouponService.calculateDiscountAmount(userCoupon, totalProductAmount);
        }

        // 9. Order 생성
        Order order = orderService.createOrder(userId, totalProductAmount, usedCouponAmount);

        // 10. 쿠폰 사용처리
        if (userCoupon != null) {
            userCouponService.useCoupon(userCoupon, order.getId());
        }

        // 11. CartItem -> OrderItem으로 변환
        List<OrderItem> orderItems = cartItems.stream()
                .map(cartItem -> orderService.createOrderItem(
                        order,
                        productMap.get(cartItem.getProductId()),
                        cartItem.getQuantity()
                ))
                .toList();

        // 12. Payment 생성
        Payment payment = paymentService.createPayment(
                order.getId(),
                order.getTotalProductAmount(),
                order.getUsedCouponAmount()
        );

        // 13. List<OrderItemResponse> items 생성
        List<OrderItemResponse> items = orderItems.stream()
                .map(OrderItemResponse::from)
                .toList();

        return CreateOrderResponse.of(order, items, payment);
    }
}
