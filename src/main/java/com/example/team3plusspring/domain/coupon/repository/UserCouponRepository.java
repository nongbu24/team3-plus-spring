package com.example.team3plusspring.domain.coupon.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.team3plusspring.domain.coupon.entity.UserCoupon;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long>, UserCouponRepositoryCustom{

	/**
	 * 같은 회원이 같은 쿠폰 이벤트를 중복으로 발급받았는지 확인한다.
	 * 발급 API에서 쿠폰을 발급해주기 전에 먼저 이 메서드로 체크해서,
	 * 이미 발급받은 적이 있으면 COUPON_ALREADY_ISSUED 예외를 던지는 데 사용한다.
	 */
	boolean existsByUserIdAndCouponEventId(Long userId, Long couponEventId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
    select uc
    from UserCoupon uc
    where uc.id = :userCouponId
      and uc.userId = :userId
""")
	Optional<UserCoupon> findByIdAndUserIdForUpdate(@Param("userCouponId") Long userCouponId, @Param("userId") Long userId);

	long countByCouponEventId(Long couponEventId);
}
