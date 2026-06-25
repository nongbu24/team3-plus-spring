package com.example.team3plusspring.domain.payment.entity;

/**
 * PENDING → PAID : 결제 대기 → 결제 성공(=결제 완료)
 * PENDING → FAILED : 결제 대기 → 결제 실패
 * PENDING → CANCELED : 결제 대기 → 결제 전 주문 취소
 * PAID → REFUNDED : 결제 성공(=결제 완료) → 환불 완료
 *
 * 이 이외의 상태 변화 불가
 **/

public enum PaymentStatus {
    PENDING {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return target == PAID || target == FAILED || target == CANCELED;
        }
    },
    PAID {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return target == REFUNDED;
        }
    },
    FAILED {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return false;
        }
    },
    CANCELED {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return false;
        }
    },
    REFUNDED {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return false;
        }
    };

    public abstract boolean canTransitTo(PaymentStatus target);
}
