package com.example.team3plusspring.domain.order.service;

import com.example.team3plusspring.domain.order.entity.Order;
import com.example.team3plusspring.domain.order.entity.OrderStatus;
import com.example.team3plusspring.domain.order.repository.OrderItemRepository;
import com.example.team3plusspring.domain.order.repository.OrderRepository;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderItemRepository orderItemRepository;

    @InjectMocks
    OrderService orderService;

    @Test
    void 주문목록조회_상태가없으면_사용자의전체주문을최신순으로조회한다() {
        // given
        Order order = org.mockito.Mockito.mock(Order.class);
        Page<Order> orders = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(orderRepository.findAllByUserId(eq(USER_ID), pageableCaptor.capture())).thenReturn(orders);

        // when
        Page<Order> result = orderService.findOrders(USER_ID, null, 0, 10);

        // then
        assertThat(result.getContent()).containsExactly(order);
        assertThat(result.getTotalElements()).isEqualTo(1);

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
        verify(orderRepository, never()).findAllByUserIdAndStatus(eq(USER_ID), eq(OrderStatus.COMPLETED), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 주문목록조회_상태가있으면_사용자의해당상태주문만조회한다() {
        // given
        Order order = org.mockito.Mockito.mock(Order.class);
        Page<Order> orders = new PageImpl<>(List.of(order), PageRequest.of(1, 5), 6);

        when(orderRepository.findAllByUserIdAndStatus(
                eq(USER_ID),
                eq(OrderStatus.COMPLETED),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(orders);

        // when
        Page<Order> result = orderService.findOrders(USER_ID, OrderStatus.COMPLETED, 1, 5);

        // then
        assertThat(result.getContent()).containsExactly(order);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(6);
        verify(orderRepository, never()).findAllByUserId(eq(USER_ID), org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @ParameterizedTest
    @CsvSource({
            "-1, 10",
            "0, 0",
            "0, 101"
    })
    void 주문목록조회_페이지조건이잘못되면_실패한다(int page, int size) {
        // when & then
        assertThatThrownBy(() -> orderService.findOrders(USER_ID, null, page, size))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PAGINATION));
        verifyNoInteractions(orderRepository, orderItemRepository);
    }
}
