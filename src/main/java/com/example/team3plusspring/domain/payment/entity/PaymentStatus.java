package com.example.team3plusspring.domain.payment.entity;

/**
 * PENDING → PAID : 결제 대기 → 결제 성공(=결제 완료)
 * PENDING → FAILED : 결제 대기 → 결제 실패
 * PENDING → CANCEL_REQUESTED : 결제 대기 → PG 취소 요청 접수
 * PENDING → CANCELED : 결제 대기 → PG 취소 완료
 * CANCEL_REQUESTED → CANCELED : PG 취소 요청 접수 → PG 취소 완료
 * CANCEL_REQUESTED → REVIEW_REQUIRED : PG 취소 요청 접수 → PG 취소 결과 수동 확인 필요
 * REVIEW_REQUIRED → CANCELED : 수동 확인 필요 → PG 취소 완료 확인
 * PAID → REFUNDED : 결제 성공(=결제 완료) → 환불 완료
 *
 * 이 이외의 상태 변화 불가
 **/

public enum PaymentStatus {
    PENDING {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return target == PAID
                    || target == FAILED
                    || target == CANCEL_REQUESTED
                    || target == CANCELED;
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
    CANCEL_REQUESTED {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return target == CANCELED || target == REVIEW_REQUIRED;
        }
    },
    CANCELED {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return false;
        }
    },
    REVIEW_REQUIRED {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return target == CANCELED;
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
