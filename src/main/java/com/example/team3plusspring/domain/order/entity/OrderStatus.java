package com.example.team3plusspring.domain.order.entity;

/**
 * PAYMENT_PENDING → COMPLETED : 결제 대기 → 주문 완료(결제 성공 = 주문 성공)
 * PAYMENT_PENDING → CANCELED : 결제 대기 → 주문 취소(결제 실패 or 주문 취소)
 * COMPLETED → REFUND_REQUESTED : 주문 완료 → 환불 요청
 * REFUND_REQUESTED → REFUNDED : 환불 요청 → 환불 완료
 *
 * 이 이외의 상태 변화 불가
 **/
public enum OrderStatus {

    PAYMENT_PENDING {
        @Override
        public boolean canTransitTo(OrderStatus target) {
            return target == COMPLETED || target == CANCELED;
        }
    },
    COMPLETED {
        @Override
        public boolean canTransitTo(OrderStatus target) {
            return target == REFUND_REQUESTED;
        }
    },
    CANCELED {
        @Override
        public boolean canTransitTo(OrderStatus target) {
            return false;
        }
    },
    REFUND_REQUESTED {
        @Override
        public boolean canTransitTo(OrderStatus target) {
            return target == REFUNDED;
        }
    },
    REFUNDED {
        @Override
        public boolean canTransitTo(OrderStatus target) {
            return false;
        }
    };

    public abstract boolean canTransitTo(OrderStatus target);
}
