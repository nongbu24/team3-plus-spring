package com.example.team3plusspring.domain.order.facade;

import com.example.team3plusspring.domain.cart.repository.CartItemRepository;
import com.example.team3plusspring.domain.cart.repository.CartRepository;
import com.example.team3plusspring.domain.coupon.entity.CouponEvent;
import com.example.team3plusspring.domain.coupon.entity.DiscountType;
import com.example.team3plusspring.domain.coupon.entity.UserCoupon;
import com.example.team3plusspring.domain.coupon.entity.UserCouponStatus;
import com.example.team3plusspring.domain.coupon.repository.CouponEventRepository;
import com.example.team3plusspring.domain.coupon.repository.UserCouponRepository;
import com.example.team3plusspring.domain.order.dto.CreateDirectOrderRequest;
import com.example.team3plusspring.domain.order.entity.Order;
import com.example.team3plusspring.domain.order.entity.OrderStatus;
import com.example.team3plusspring.domain.order.repository.OrderItemRepository;
import com.example.team3plusspring.domain.order.repository.OrderRepository;
import com.example.team3plusspring.domain.payment.entity.Payment;
import com.example.team3plusspring.domain.payment.entity.PaymentStatus;
import com.example.team3plusspring.domain.payment.repository.PaymentRepository;
import com.example.team3plusspring.domain.product.entity.Product;
import com.example.team3plusspring.domain.product.repository.ProductRepository;
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.repository.UserRepository;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import com.example.team3plusspring.support.RedisTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrderFacadeConcurrencyTest extends RedisTestSupport {

    @Autowired
    OrderFacade orderFacade;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CouponEventRepository couponEventRepository;

    @Autowired
    UserCouponRepository userCouponRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderItemRepository orderItemRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    CartRepository cartRepository;

    @Autowired
    CartItemRepository cartItemRepository;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        userCouponRepository.deleteAllInBatch();
        couponEventRepository.deleteAllInBatch();
        cartItemRepository.deleteAllInBatch();
        cartRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void 상품에서바로주문하면_주문상품결제대기정보를생성하고재고를차감한다() {
        // given
        User user = userRepository.save(User.create(uniqueEmail(), "password", "tester", "010-0000-0000"));
        Product product = productRepository.save(Product.create("keyboard", "mechanical keyboard", 10_000, 5, 1L));

        // when
        orderFacade.createDirectOrder(user.getId(), directOrderRequest(product.getId(), 2, null));

        // then
        Order order = orderRepository.findAll().get(0);
        Payment payment = paymentRepository.findAll().get(0);

        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(orderItemRepository.count()).isEqualTo(1);
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(order.getTotalProductAmount()).isEqualTo(20_000);
        assertThat(order.getUsedCouponAmount()).isZero();
        assertThat(order.getPaymentAmount()).isEqualTo(20_000);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getOrderId()).isEqualTo(order.getId());
        assertThat(payment.getPaymentAmount()).isEqualTo(20_000);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(3);
    }

    @Test
    void 쿠폰으로상품에서바로주문하면_할인금액을적용하고쿠폰을사용처리한다() {
        // given
        User user = userRepository.save(User.create(uniqueEmail(), "password", "tester", "010-0000-0000"));
        Product product = productRepository.save(Product.create("keyboard", "mechanical keyboard", 10_000, 5, 1L));
        CouponEvent couponEvent = couponEventRepository.save(CouponEvent.create(
                "order discount",
                DiscountType.FIXED,
                3_000,
                100,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        ));
        UserCoupon userCoupon = userCouponRepository.save(UserCoupon.issue(user.getId(), couponEvent.getId()));

        // when
        orderFacade.createDirectOrder(user.getId(), directOrderRequest(product.getId(), 2, userCoupon.getId()));

        // then
        Order order = orderRepository.findAll().get(0);
        Payment payment = paymentRepository.findAll().get(0);
        UserCoupon usedCoupon = userCouponRepository.findById(userCoupon.getId()).orElseThrow();

        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(orderItemRepository.count()).isEqualTo(1);
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(order.getTotalProductAmount()).isEqualTo(20_000);
        assertThat(order.getUsedCouponAmount()).isEqualTo(3_000);
        assertThat(order.getPaymentAmount()).isEqualTo(17_000);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaymentAmount()).isEqualTo(17_000);
        assertThat(usedCoupon.getStatus()).isEqualTo(UserCouponStatus.USED);
        assertThat(usedCoupon.getOrderId()).isEqualTo(order.getId());
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(3);
    }

    @Test
    void 같은상품을동시에주문하면_재고수량만큼만성공한다() throws Exception {
        // given
        User user = userRepository.save(User.create(uniqueEmail(), "password", "tester", "010-0000-0000"));
        Product product = productRepository.save(Product.create("keyboard", "mechanical keyboard", 10_000, 1, 1L));

        // when
        List<Throwable> results = runConcurrently(
                () -> orderFacade.createDirectOrder(user.getId(), directOrderRequest(product.getId(), 1, null)),
                () -> orderFacade.createDirectOrder(user.getId(), directOrderRequest(product.getId(), 1, null))
        );

        // then
        assertThat(successCount(results)).isEqualTo(1);
        assertThat(errorCount(results, ErrorCode.ORDER_STOCK_SHORTAGE)).isEqualTo(1);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isZero();
    }

    @Test
    void 같은쿠폰을동시에사용하면_한주문만성공한다() throws Exception {
        // given
        User user = userRepository.save(User.create(uniqueEmail(), "password", "tester", "010-0000-0000"));
        Product firstProduct = productRepository.save(Product.create("keyboard", "mechanical keyboard", 10_000, 1, 1L));
        Product secondProduct = productRepository.save(Product.create("mouse", "wireless mouse", 10_000, 1, 1L));
        CouponEvent couponEvent = couponEventRepository.save(CouponEvent.create(
                "order discount",
                DiscountType.FIXED,
                1_000,
                100,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        ));
        UserCoupon userCoupon = userCouponRepository.save(UserCoupon.issue(user.getId(), couponEvent.getId()));

        // when
        List<Throwable> results = runConcurrently(
                () -> orderFacade.createDirectOrder(user.getId(), directOrderRequest(firstProduct.getId(), 1, userCoupon.getId())),
                () -> orderFacade.createDirectOrder(user.getId(), directOrderRequest(secondProduct.getId(), 1, userCoupon.getId()))
        );

        // then
        assertThat(successCount(results)).isEqualTo(1);
        assertThat(errorCount(results, ErrorCode.COUPON_ALREADY_USED)).isEqualTo(1);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(userCouponRepository.findById(userCoupon.getId()).orElseThrow().getStatus()).isEqualTo(UserCouponStatus.USED);

        int remainingStock = productRepository.findById(firstProduct.getId()).orElseThrow().getStock()
                + productRepository.findById(secondProduct.getId()).orElseThrow().getStock();
        assertThat(remainingStock).isEqualTo(1);
    }

    @Test
    void 만료된회원쿠폰으로상품에서바로주문하면_주문생성에실패한다() {
        // given
        User user = userRepository.save(User.create(uniqueEmail(), "password", "tester", "010-0000-0000"));
        Product product = productRepository.save(Product.create("keyboard", "mechanical keyboard", 10_000, 5, 1L));
        CouponEvent couponEvent = couponEventRepository.save(CouponEvent.create(
                "order discount",
                DiscountType.FIXED,
                3_000,
                100,
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().plusDays(10)
        ));
        UserCoupon userCoupon = userCouponRepository.save(UserCoupon.issue(user.getId(), couponEvent.getId()));
        ReflectionTestUtils.setField(userCoupon, "expiredAt", LocalDateTime.now().minusDays(1));
        userCouponRepository.saveAndFlush(userCoupon);

        // when & then
        assertThatThrownBy(() -> orderFacade.createDirectOrder(
                user.getId(),
                directOrderRequest(product.getId(), 2, userCoupon.getId())
        ))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COUPON_EXPIRED));

        assertThat(orderRepository.count()).isZero();
        assertThat(orderItemRepository.count()).isZero();
        assertThat(paymentRepository.count()).isZero();
        assertThat(userCouponRepository.findById(userCoupon.getId()).orElseThrow().getStatus()).isEqualTo(UserCouponStatus.ISSUED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(5);
    }

    private CreateDirectOrderRequest directOrderRequest(Long productId, int quantity, Long userCouponId) {
        CreateDirectOrderRequest request = new CreateDirectOrderRequest();
        ReflectionTestUtils.setField(request, "productId", productId);
        ReflectionTestUtils.setField(request, "quantity", quantity);
        ReflectionTestUtils.setField(request, "userCouponId", userCouponId);
        return request;
    }

    private List<Throwable> runConcurrently(ThrowingRunnable... tasks) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(tasks.length);
        CountDownLatch ready = new CountDownLatch(tasks.length);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Throwable>> futures = new ArrayList<>();

            for (ThrowingRunnable task : tasks) {
                futures.add(executorService.submit(() -> {
                    ready.countDown();
                    start.await();

                    try {
                        task.run();
                        return null;
                    } catch (Throwable throwable) {
                        return throwable;
                    }
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Throwable> results = new ArrayList<>();
            for (Future<Throwable> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }

            return results;
        } finally {
            executorService.shutdownNow();
        }
    }

    private long successCount(List<Throwable> results) {
        return results.stream()
                .filter(result -> result == null)
                .count();
    }

    private long errorCount(List<Throwable> results, ErrorCode errorCode) {
        return results.stream()
                .filter(BusinessException.class::isInstance)
                .map(BusinessException.class::cast)
                .filter(exception -> exception.getErrorCode() == errorCode)
                .count();
    }

    private String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
