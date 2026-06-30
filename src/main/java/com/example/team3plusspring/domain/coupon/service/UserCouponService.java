package com.example.team3plusspring.domain.coupon.service;

import com.example.team3plusspring.domain.coupon.dto.GetUserCouponListResponse;
import com.example.team3plusspring.domain.coupon.entity.CouponEvent;
import com.example.team3plusspring.domain.coupon.entity.UserCoupon;
import com.example.team3plusspring.domain.coupon.repository.CouponEventRepository;
import com.example.team3plusspring.domain.coupon.repository.UserCouponRepository;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserCouponService {

    private final UserCouponRepository userCouponRepository;
    private final CouponEventRepository couponEventRepository;

    /**
     * 내가 보유한 쿠폰 중 사용 가능한(ISSUED 상태이면서 사용기한이 지나지 않은) 쿠폰 목록을 페이지 단위로 조회하는 메서드
     * 사용기한(expiredAt)이 지난 쿠폰은 결과에 포함되지 않음
     *
     * @param userId 조회할 회원 ID
     * @param page 0부터 시작하는 페이지 번호
     * @param size 페이지당 조회할 쿠폰 수
     * @return 사용 가능한 쿠폰 목록 응답 DTO를 담은 페이지
     */

    @Transactional(readOnly = true)
    public Page<GetUserCouponListResponse> getMyCoupons(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userCouponRepository.findUsableUserCoupons(userId, LocalDateTime.now(), pageable)
            .map(GetUserCouponListResponse::from);
    }

    @Transactional
    public UserCoupon getUsableCouponForUpdate(Long userId, Long userCouponId) {
        return userCouponRepository.findByIdAndUserIdForUpdate(userCouponId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public int calculateDiscountAmount(UserCoupon userCoupon, int totalProductAmount) {
        userCoupon.validateUsablePeriod(LocalDateTime.now());

        CouponEvent couponEvent = couponEventRepository.findById(userCoupon.getCouponEventId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_EVENT_NOT_FOUND));

        int discountAmount = couponEvent.calculateDiscountAmount(totalProductAmount);

        return Math.min(discountAmount, totalProductAmount);
    }

    @Transactional
    public void useCoupon(UserCoupon userCoupon, Long orderId) {
        userCoupon.markAsUsed(orderId);
    }

    @Transactional
    public void restoreCouponByOrderId(Long orderId) {
        userCouponRepository.findByOrderIdForUpdate(orderId)
                .ifPresent(UserCoupon::restore);
    }

    public boolean existsByUserIdAndCouponEventId(Long userId, Long couponEventId) {
        return userCouponRepository.existsByUserIdAndCouponEventId(userId, couponEventId);
    }

    @Transactional
    public UserCoupon issue(Long userId, Long couponEventId, int validDays) {
        UserCoupon userCoupon = UserCoupon.issue(userId, couponEventId, validDays);
        return userCouponRepository.save(userCoupon);
    }
}
