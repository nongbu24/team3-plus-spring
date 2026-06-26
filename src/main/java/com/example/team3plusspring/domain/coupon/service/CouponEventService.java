package com.example.team3plusspring.domain.coupon.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.team3plusspring.domain.coupon.dto.CouponEventResponse;
import com.example.team3plusspring.domain.coupon.dto.CreateCouponEventRequest;
import com.example.team3plusspring.domain.coupon.dto.GetCouponEventListResponse;
import com.example.team3plusspring.domain.coupon.dto.IssueCouponResponse;
import com.example.team3plusspring.domain.coupon.entity.CouponEvent;
import com.example.team3plusspring.domain.coupon.entity.CouponEventStatus;
import com.example.team3plusspring.domain.coupon.entity.UserCoupon;
import com.example.team3plusspring.domain.coupon.repository.CouponEventRepository;
import com.example.team3plusspring.domain.coupon.repository.UserCouponRepository;
import com.example.team3plusspring.domain.user.entity.UserRole;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import com.example.team3plusspring.global.lock.RedisLock;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponEventService {

	private final CouponEventRepository couponEventRepository;
	private final UserCouponRepository userCouponRepository;

	/**
	 * 쿠폰 이벤트를 등록하는 메서드
	 * 관리자(ADMIN)만 등록할 수 있고, 발급 중(OPEN)인 쿠폰 이벤트와 이름이 중복되면 등록할 수 없음
	 * 할인율 100 초과, 발급 시작일시가 종료일시보다 늦은 경우는 엔티티 생성 단계에서 검증됨
	 *
	 * @param role 요청한 사용자의 권한
	 * @param request 쿠폰 이벤트 등록 요청 DTO
	 * @return 등록된 쿠폰 이벤트 응답 DTO
	 */

	@Transactional
	public CouponEventResponse createCouponEvent(UserRole role, CreateCouponEventRequest request) {

		if (role != UserRole.ADMIN) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}

		if (couponEventRepository.existsByNameAndStatus(request.getName(), CouponEventStatus.OPEN)) {
			throw new BusinessException(ErrorCode.COUPON_EVENT_NAME_ALREADY_EXISTS);
		}

		CouponEvent couponEvent = CouponEvent.create(
			request.getName(),
			request.getDiscountType(),
			request.getDiscountAmount(),
			request.getTotalQuantity(),
			request.getStartsAt(),
			request.getEndsAt(),
			request.getValidDays()
		);

		CouponEvent savedCouponEvent = couponEventRepository.save(couponEvent);

		return CouponEventResponse.from(savedCouponEvent);
	}

	/**
	 * 발급 중(OPEN)인 쿠폰 이벤트 목록을 페이지 단위로 조회하는 메서드
	 * 종료(CLOSED)된 쿠폰 이벤트는 결과에 포함되지 않음
	 *
	 * @param page 0부터 시작하는 페이지 번호
	 * @param size 페이지당 조회할 쿠폰 이벤트 수
	 * @return 쿠폰 이벤트 목록 응답 DTO를 담은 페이지
	 */

	@Transactional(readOnly = true)
	public Page<GetCouponEventListResponse> getCouponEvents(int page, int size) {

		Pageable pageable = PageRequest.of(page, size);

		return couponEventRepository.findOpenCouponEvents(LocalDateTime.now(), pageable)
			.map(GetCouponEventListResponse::from);
	}

	/**
	 * 쿠폰을 발급하는 메서드
	 * 쿠폰 이벤트가 발급 가능 상태(OPEN, 발급 기간 내)인지, 이미 발급받은 적이 있는지 확인한 뒤
	 * 발급 수량을 1 증가시키고 UserCoupon을 생성함
	 * 동시에 여러 요청이 들어와도 재고를 초과해서 발급되지 않도록, 분산 락(@RedisLock)으로
	 * 같은 쿠폰 이벤트에 대한 동시 접근을 한 번에 하나씩만 허용함
	 *
	 * @param userId 쿠폰을 발급받는 사용자 ID
	 * @param couponEventId 발급받을 쿠폰 이벤트 ID
	 * @return 발급된 쿠폰 응답 DTO
	 */

	@Transactional
	@RedisLock(key = "lock:coupon:", argIndex = 1)
	public IssueCouponResponse issueCoupon(Long userId, Long couponEventId) {

		CouponEvent couponEvent = couponEventRepository.findById(couponEventId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COUPON_EVENT_NOT_FOUND));

		if (!couponEvent.isIssuable(LocalDateTime.now())) {
			throw new BusinessException(ErrorCode.COUPON_EVENT_CLOSED);
		}

		if (userCouponRepository.existsByUserIdAndCouponEventId(userId, couponEventId)) {
			throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
		}

		long updatedRows = couponEventRepository.increaseIssuedQuantity(couponEventId);
		if (updatedRows == 0) {
			throw new BusinessException(ErrorCode.COUPON_STOCK_EXHAUSTED);
		}

		UserCoupon userCoupon = UserCoupon.issue(userId, couponEventId, couponEvent.getValidDays());
		UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

		return IssueCouponResponse.from(savedUserCoupon);
	}
}
