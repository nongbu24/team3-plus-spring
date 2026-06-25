package com.example.team3plusspring.domain.coupon.service;

import com.example.team3plusspring.domain.coupon.entity.CouponEvent;
import com.example.team3plusspring.domain.coupon.entity.UserCoupon;
import com.example.team3plusspring.domain.coupon.repository.CouponEventRepository;
import com.example.team3plusspring.domain.coupon.repository.UserCouponRepository;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserCouponService {

    private final UserCouponRepository userCouponRepository;
    private final CouponEventRepository couponEventRepository;

    @Transactional
    public UserCoupon getUsableCouponForUpdate(Long userId, Long userCouponId) {
        return userCouponRepository.findByIdAndUserIdForUpdate(userCouponId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    public int calculateDiscountAmount(UserCoupon userCoupon, int totalProductAmount) {
        CouponEvent couponEvent = couponEventRepository.findById(userCoupon.getCouponEventId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_EVENT_NOT_FOUND));

        if (!couponEvent.isIssuable(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.COUPON_EVENT_CLOSED);
        }

        int discountAmount = couponEvent.calculateDiscountAmount(totalProductAmount);

        return Math.min(discountAmount, totalProductAmount);
    }

    @Transactional
    public void useCoupon(UserCoupon userCoupon, Long orderId) {
        userCoupon.markAsUsed(orderId);
    }
}
