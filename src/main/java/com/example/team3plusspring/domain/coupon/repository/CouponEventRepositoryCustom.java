package com.example.team3plusspring.domain.coupon.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.team3plusspring.domain.coupon.entity.CouponEvent;

public interface CouponEventRepositoryCustom {

	/**
	 * 재고가 남아있을 때만(issuedQuantity < totalQuantity) issuedQuantity를 1 증가시키는
	 * 원자적 UPDATE 쿼리. DB 차원에서 조건 체크와 증가가 한 번에 처리되어,
	 * 락(분산 락 등)이 어떤 이유로 깨져도 재고 초과 발급을 막아주는 최종 방어선 역할을 한다.
	 *
	 * @param id 쿠폰 이벤트 ID
	 * @return 업데이트된 row 수 (0이면 재고 소진으로 증가하지 못한 것)
	 */
	long increaseIssuedQuantity(Long id);

	Page<CouponEvent> findOpenCouponEvents(LocalDateTime now, Pageable pageable);
}
