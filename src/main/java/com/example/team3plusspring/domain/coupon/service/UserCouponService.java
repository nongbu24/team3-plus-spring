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

@Service
@RequiredArgsConstructor
public class UserCouponService {

    private final UserCouponRepository userCouponRepository;
    private final CouponEventRepository couponEventRepository;

    @Transactional(readOnly = true)
    public int calculateDiscountAmount(Long userId, Long userCouponId, int totalProductAmount) {
        UserCoupon userCoupon = findOwnedUserCoupon(userId, userCouponId);

        CouponEvent couponEvent = couponEventRepository.findById(userCoupon.getCouponEventId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_EVENT_NOT_FOUND));

        long discountAmount = couponEvent.calculateDiscountAmount(totalProductAmount);

        return (int) Math.min(discountAmount, totalProductAmount);
    }

    @Transactional
    public void useCoupon(Long userId, Long userCouponId, Long orderId) {
        UserCoupon userCoupon = findOwnedUserCoupon(userId, userCouponId);
        userCoupon.markAsUsed(orderId);
    }

    private UserCoupon findOwnedUserCoupon(Long userId, Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!userCoupon.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return userCoupon;
    }
}
