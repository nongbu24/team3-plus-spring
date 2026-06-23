# 공통 API 규칙

이 문서는 전체 API에서 공통으로 사용하는 Base URL, 인증 방식, 응답 형식, 페이지네이션, Enum, 에러 코드를 정리합니다.

## Base URL

- 로컬 실행 기준: `http://localhost:8080`
- 모든 API prefix: `/api`
- 요청/응답 Content-Type: `application/json; charset=UTF-8`
- 금액 단위: 원화 정수
- 일시 형식: ISO 8601 문자열. 예: `2026-06-22T18:30:00+09:00`

## 인증

JWT Bearer 토큰을 사용합니다.

```http
Authorization: Bearer {accessToken}
```

인증이 필요 없는 API는 다음과 같습니다.

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/api/auth/signup` | 회원가입 |
| `POST` | `/api/auth/login` | 로그인, JWT 발급 |
| `GET` | `/api/products/{productId}` | 상품 상세 조회 |
| `GET` | `/api/v1/products` | 상품 목록 조회 |
| `GET` | `/api/v2/products` | Local Cache 적용 상품 검색 |
| `GET` | `/api/v1/search/popular` | 인기 검색어 조회 |

관리자 API는 JWT 인증 후 관리자 권한을 추가로 확인합니다.

```text
/api/admin/**
```

PortOne 웹훅은 JWT가 아니라 웹훅 서명을 검증합니다.

```http
POST /api/payments/webhook
```

## 공통 응답

성공 응답은 `status`, `message`, `data`를 사용합니다.

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

실패 응답은 `status`, `code`, `message`, `data`를 사용합니다.

```json
{
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "요청 본문, 쿼리 파라미터, 경로 변수 검증에 실패했습니다.",
  "data": [
    "email은 필수입니다."
  ]
}
```

- `status`는 실제 HTTP 상태 코드와 동일하게 작성합니다.
- `data`가 없으면 `null`을 사용하거나 응답에서 생략할 수 있습니다.
- 도메인별 응답 데이터 구조는 각 도메인 문서에 작성합니다.

## 페이지네이션

목록 조회 API는 기본적으로 다음 query parameter를 사용합니다.

| 이름 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `page` | `Integer` | `0` | 0부터 시작하는 페이지 번호 |
| `size` | `Integer` | `10` | 페이지 크기 |
| `sort` | `String` | API별 기본값 | 정렬 기준 |

페이지 응답 형식:

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 120,
  "totalPages": 12,
  "hasNext": true
}
```

## 멱등성

- 결제 승인 검증은 같은 결제 식별자로 여러 번 요청되어도 최종 결과가 같아야 합니다.
- PortOne 웹훅은 같은 이벤트가 여러 번 들어올 수 있으므로 중복 처리를 방지해야 합니다.
- 쿠폰 발급 요청은 같은 회원이 같은 쿠폰 이벤트에 중복 발급받지 못하도록 처리해야 합니다.

## Enum

### ProductStatus

| 값 | 설명 |
| --- |----|
| `ON_SALE` | 판매중 |
| `SOLD_OUT` | 품절 |
| `DISCONTINUED` | 단종 |

### OrderStatus

| 값 | 설명 |
| --- | --- |
| `PAYMENT_PENDING` | 결제 대기 |
| `COMPLETED` | 주문 완료 |
| `CANCELED` | 결제 전 주문 취소 |
| `REFUND_REQUESTED` | 환불 요청 |
| `REFUNDED` | 환불 완료 |

### PaymentStatus

| 값 | 설명 |
| --- | --- |
| `PENDING` | 결제 대기 |
| `PAID` | 결제 완료 |
| `FAILED` | 결제 실패 |
| `CANCELED` | 결제 취소 |
| `REFUNDED` | 환불 완료 |

### PointHistoryType

| 값         | 설명        |
|-----------|-----------|
| `EARN`    | 포인트 적립    |
| `USE`     | 포인트 사용    |
| `RESTORE` | 사용 포인트 복구 |
| `REVOKE`  | 적립 포인트 회수 |


### RefundStatus

| 값 | 설명 |
| --- | --- |
| `REQUESTED` | 환불 요청 |
| `APPROVED` | 환불 승인 |
| `REJECTED` | 환불 거절 |
| `COMPLETED` | 환불 완료 |
| `FAILED` | 환불 실패 |

### WebhookStatus

| 값 | 설명 |
| --- | --- |
| `RECEIVED` | 웹훅 수신 |
| `PROCESSED` | 처리 완료 |
| `FAILED` | 처리 실패 |

## 공통 에러 코드

| 코드 | HTTP | 설명 |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | 요청 본문, 쿼리 파라미터, 경로 변수 검증 실패 |
| `INVALID_ENUM_VALUE` | 400 | 허용하지 않는 Enum 값 |
| `MISSING_REQUIRED_FIELD` | 400 | 필수 값 누락 |
| `INVALID_PAGINATION` | 400 | 페이지 번호 또는 크기 오류 |
| `UNAUTHORIZED` | 401 | 인증 토큰 누락 또는 인증 실패 |
| `INVALID_TOKEN` | 401 | 잘못된 JWT |
| `EXPIRED_TOKEN` | 401 | 만료된 JWT |
| `FORBIDDEN` | 403 | 권한 또는 소유권 없음 |
| `RESOURCE_NOT_FOUND` | 404 | 리소스 없음 |
| `METHOD_NOT_ALLOWED` | 405 | 지원하지 않는 HTTP method |
| `CONFLICT` | 409 | 현재 상태와 충돌하는 요청 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류 |
| `EXTERNAL_API_FAILED` | 502 | 외부 API 호출 실패 |

## 도메인 에러 코드

| 코드 | HTTP | 설명 |
| --- | --- | --- |
| `EMAIL_ALREADY_EXISTS` | 409 | 이미 가입된 이메일 |
| `INVALID_LOGIN_CREDENTIALS` | 401 | 이메일 또는 비밀번호 불일치 |
| `USER_NOT_FOUND` | 404 | 회원 없음 |
| `USER_ALREADY_DELETED` | 409 | 이미 탈퇴한 회원 |
| `PRODUCT_NOT_FOUND` | 404 | 상품 없음 |
| `PRODUCT_NOT_ON_SALE` | 400 | 판매중이 아닌 상품 |
| `PRODUCT_OUT_OF_STOCK` | 409 | 상품 재고 부족 |
| `CATEGORY_NOT_FOUND` | 404 | 카테고리 없음 |
| `CART_NOT_FOUND` | 404 | 장바구니 없음 |
| `CART_ITEM_NOT_FOUND` | 404 | 장바구니 상품 없음 |
| `CART_ITEM_ACCESS_DENIED` | 403 | 타인의 장바구니 상품 접근 |
| `CART_STOCK_EXCEEDED` | 409 | 장바구니 수량이 재고 초과 |
| `ORDER_NOT_FOUND` | 404 | 주문 없음 |
| `ORDER_ACCESS_DENIED` | 403 | 타인의 주문 접근 |
| `ORDER_CANCEL_NOT_ALLOWED` | 409 | 결제 전 취소가 불가능한 주문 상태 |
| `ORDER_STOCK_SHORTAGE` | 409 | 주문 생성 중 재고 부족 |
| `COUPON_EVENT_NOT_FOUND` | 404 | 쿠폰 이벤트 없음 |
| `COUPON_EVENT_CLOSED` | 409 | 발급 종료된 쿠폰 이벤트 |
| `COUPON_ALREADY_ISSUED` | 409 | 이미 발급받은 쿠폰 |
| `COUPON_STOCK_EXHAUSTED` | 409 | 쿠폰 발급 수량 소진 |
| `COUPON_ALREADY_USED` | 409 | 이미 사용된 쿠폰 |
| `COUPON_NOT_USED` | 409 | 사용되지 않은 쿠폰은 복구할 수 없음 |
| `PAYMENT_NOT_FOUND` | 404 | 결제 없음 |
| `PAYMENT_ACCESS_DENIED` | 403 | 타인의 결제 접근 |
| `PAYMENT_ALREADY_PROCESSED` | 409 | 이미 처리된 결제 |
| `PAYMENT_AMOUNT_MISMATCH` | 400 | 결제 승인 금액 불일치 |
| `PAYMENT_STATUS_NOT_PAID` | 400 | 외부 결제 상태가 성공 상태가 아님 |
| `PAYMENT_WEBHOOK_INVALID` | 400 | 결제 웹훅 요청이 올바르지 않음 |
| `REFUND_NOT_ALLOWED` | 409 | 환불 가능한 결제 상태가 아님 |
| `REFUND_NOT_FOUND` | 404 | 환불 내역 없음 |
| `REFUND_AMOUNT_INVALID` | 400 | 환불 금액 오류 |
| `POINT_ACCOUNT_NOT_FOUND` | 404 | 포인트 계정 없음 |
| `INSUFFICIENT_POINT` | 400 | 포인트 잔액 부족 |
| `POINT_HISTORY_NOT_FOUND` | 404 | 포인트 이력 없음 |
| `POPULAR_SEARCH_KEYWORD_NOT_FOUND` | 404 | 인기 검색어 없음 |
| `WEBHOOK_SIGNATURE_INVALID` | 400 | 웹훅 서명 검증 실패 |
| `WEBHOOK_PAYLOAD_INVALID` | 400 | 웹훅 본문 파싱 실패 |
