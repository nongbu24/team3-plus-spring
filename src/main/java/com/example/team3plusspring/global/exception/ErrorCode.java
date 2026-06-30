package com.example.team3plusspring.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Common
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 본문, 쿼리 파라미터, 경로 변수 검증에 실패했습니다."),
    INVALID_ENUM_VALUE(HttpStatus.BAD_REQUEST, "허용하지 않는 Enum 값입니다."),
    MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "필수 값이 누락되었습니다."),
    INVALID_PAGINATION(HttpStatus.BAD_REQUEST, "페이지 번호 또는 크기가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증 토큰이 누락되었거나 인증에 실패했습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "잘못된 JWT입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 JWT입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한 또는 소유권이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP method입니다."),
    CONFLICT(HttpStatus.CONFLICT, "현재 상태와 충돌하는 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),
    EXTERNAL_API_FAILED(HttpStatus.BAD_GATEWAY, "외부 API 호출에 실패했습니다."),

    // Auth
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    INVALID_LOGIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    USER_ALREADY_DELETED(HttpStatus.CONFLICT, "이미 탈퇴한 회원입니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    PRODUCT_NOT_ON_SALE(HttpStatus.BAD_REQUEST, "판매중이 아닌 상품입니다."),
    PRODUCT_OUT_OF_STOCK(HttpStatus.CONFLICT, "상품 재고가 부족합니다."),

    // Category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),

    // Cart
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니를 찾을 수 없습니다."),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니 상품을 찾을 수 없습니다."),
    CART_ITEM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "타인의 장바구니 상품에 접근할 수 없습니다."),
    CART_STOCK_EXCEEDED(HttpStatus.CONFLICT, "장바구니 수량이 재고를 초과했습니다."),
    CART_ITEM_QUANTITY_INVALID(HttpStatus.BAD_REQUEST, "장바구니 상품 수량은 1개 이상이어야 합니다."),
    CART_ITEM_SELECTION_INVALID(HttpStatus.BAD_REQUEST, "선택한 장바구니 항목이 유효하지 않습니다."),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "타인의 주문에 접근할 수 없습니다."),
    ORDER_CANCEL_NOT_ALLOWED(HttpStatus.CONFLICT, "PG 결제 시작 전 상태가 아니라 직접 취소할 수 없습니다."),
    ORDER_STOCK_SHORTAGE(HttpStatus.CONFLICT, "주문 생성 중 재고가 부족합니다."),
    ORDER_STATUS_INVALID(HttpStatus.CONFLICT, "올바르지 않은 주문 상태 변경입니다."),
    ORDER_DISCOUNT_AMOUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "할인 금액은 상품 총액을 초과할 수 없습니다."),

    // Coupon
    COUPON_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "쿠폰 이벤트를 찾을 수 없습니다."),
    COUPON_EVENT_CLOSED(HttpStatus.CONFLICT, "발급 종료된 쿠폰 이벤트입니다."),
    COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "이미 발급받은 쿠폰입니다."),
    COUPON_STOCK_EXHAUSTED(HttpStatus.CONFLICT, "쿠폰 발급 수량이 소진되었습니다."),
    COUPON_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용된 쿠폰입니다."),
    COUPON_NOT_USED(HttpStatus.CONFLICT, "사용되지 않은 쿠폰은 복구할 수 없습니다."),
    COUPON_EXPIRED(HttpStatus.CONFLICT, "만료된 쿠폰입니다."),
    INVALID_DISCOUNT_AMOUNT(HttpStatus.BAD_REQUEST, "퍼센트 할인은 100을 초과할 수 없습니다."),
    INVALID_COUPON_EVENT_PERIOD(HttpStatus.BAD_REQUEST, "발급 시작일시는 종료일시보다 빠르거나 같아야 합니다."),
    COUPON_EVENT_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 쿠폰 이벤트 이름입니다."),
    COUPON_ISSUE_LOCK_FAILED(HttpStatus.CONFLICT, "쿠폰 발급이 몰리고 있습니다. 잠시 후 다시 시도해주세요."),

    // Payment
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제를 찾을 수 없습니다."),
    PAYMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "타인의 결제에 접근할 수 없습니다."),
    PAYMENT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 결제입니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "결제 승인 금액이 일치하지 않습니다."),
    PAYMENT_STATUS_NOT_PAID(HttpStatus.BAD_REQUEST, "PortOne 결제 상태가 성공 상태가 아닙니다."),
    PAYMENT_NOT_STARTED(HttpStatus.CONFLICT, "결제가 아직 시작되지 않았습니다."),
    PAYMENT_NOT_COMPLETED(HttpStatus.CONFLICT, "결제가 아직 완료되지 않았습니다."),
    PAYMENT_CANCEL_PENDING(HttpStatus.CONFLICT, "결제 취소가 처리 중입니다."),
    PAYMENT_REVIEW_REQUIRED(HttpStatus.CONFLICT, "결제 취소 결과를 확인해야 합니다."),
    PAYMENT_WEBHOOK_INVALID(HttpStatus.BAD_REQUEST, "결제 웹훅 요청이 올바르지 않습니다."),
    PAYMENT_STATUS_INVALID(HttpStatus.CONFLICT, "올바르지 않은 결제 상태 변경입니다."),

    // Chat
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 채팅방에 접근할 수 없습니다."),
    CHAT_ROOM_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 완료된 채팅방입니다."),
    INVALID_CHAT_STATUS_TRANSITION(HttpStatus.CONFLICT, "변경할 수 없는 문의 상태입니다."),
    ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND, "관리자를 찾을 수 없습니다."),

    // Search
    POPULAR_SEARCH_KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "인기 검색어를 찾을 수 없습니다."),

    // Webhook
    WEBHOOK_SIGNATURE_INVALID(HttpStatus.BAD_REQUEST, "웹훅 서명 검증에 실패했습니다."),
    WEBHOOK_PAYLOAD_INVALID(HttpStatus.BAD_REQUEST, "웹훅 본문 파싱에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
