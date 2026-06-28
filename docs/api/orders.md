# 주문 API

주문 생성, 내 주문 목록 조회, 주문 상세 조회, 결제 전 주문 취소를 담당합니다.
주문 생성은 상품에서 바로 주문하는 방식과 장바구니 상품을 주문하는 방식으로 나뉘며, 두 방식 모두 주문 도메인에서 처리합니다.

성공/실패 응답은 모두 [공통 응답 wrapper](./common.md#공통-응답)를 사용합니다.
아래 `Response Body` 예시는 공통 응답 wrapper 전체를 보여줍니다.

## 엔드포인트

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| `POST` | `/api/orders/direct` | 상품에서 바로 주문 생성 | 필요 |
| `POST` | `/api/orders/carts` | 장바구니 상품 주문 생성 | 필요 |
| `GET` | `/api/orders` | 내 주문 목록 조회 | 필요 |
| `GET` | `/api/orders/{orderId}` | 주문 상세 조회 | 필요 |
| `POST` | `/api/orders/{orderId}/cancel` | 결제 전 주문 취소 | 필요 |

## POST `/api/orders/direct`

상품 상세/목록 화면에서 장바구니를 거치지 않고 바로 주문을 생성합니다. 주문 생성 시 상품명과 상품 가격은 주문 당시 값으로 복사합니다.

- 인증: 필요
- HTTP Status: `201 Created`

### Request Body

| 필드 | 타입     | 필수 | 설명 |
| --- |--------| --- | --- |
| `productId` | `Long` | Y | 바로 주문할 상품 ID |
| `quantity` | `int`  | Y | 주문 수량 |
| `userCouponId` | `Long` | N | 사용할 회원 쿠폰 ID. 쿠폰을 사용하지 않으면 `null` 또는 생략 |

```json
{
  "productId": 10,
  "quantity": 2,
  "userCouponId": 20
}
```

### Response Body

```json
{
  "status": 201,
  "message": "요청이 성공했습니다.",
  "data": {
    "orderId": 200,
    "paymentId": 300,
    "orderNumber": "ORD-20260622-000001",
    "portOnePaymentId": "pay_9381dde4-49d5-4079-af45-2ea490dbcc6d",
    "status": "PAYMENT_PENDING",
    "totalProductAmount": 78000,
    "usedCouponAmount": 10000,
    "paymentAmount": 68000,
    "items": [
      {
        "orderItemId": 400,
        "productId": 10,
        "productName": "무선 키보드",
        "quantity": 2,
        "unitPrice": 39000,
        "lineAmount": 78000
      }
    ],
    "createdAt": "2026-06-22T18:30:00+09:00"
  }
}
```

### 주문 상품 응답 필드

`items`는 `order_items` 테이블에 저장된 주문 상품 스냅샷입니다.

| 필드 | 타입       | 설명 |
| --- |----------| --- |
| `orderItemId` | `Long`   | 주문 상품 ID |
| `productId` | `Long`   | 상품 ID |
| `productName` | `String` | 주문 당시 상품명 |
| `quantity` | `int`    | 주문 수량 |
| `unitPrice` | `int`    | 주문 당시 상품 1개 가격 |
| `lineAmount` | `int`    | 상품 가격 * 주문 수량 |

### 주문 금액 응답 필드

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `totalProductAmount` | `int` | 주문 상품 총액 |
| `usedCouponAmount` | `int` | 쿠폰 할인 금액. 사용하지 않으면 `0` |
| `paymentAmount` | `int` | 최종 결제 금액. `totalProductAmount - usedCouponAmount` |

### 처리 규칙

- 토큰의 회원 ID를 기준으로 주문을 생성합니다.
- 주문 대상은 요청한 `productId` 상품 1개입니다.
- 장바구니에 담긴 상품 여부와 관계없이 주문을 생성합니다.
- 주문 생성 시 상품명과 상품 가격은 주문 상품에 스냅샷으로 저장합니다.
- `order_items`는 주문 생성 API 내부에서 함께 생성되며, 단독 생성 API를 만들지 않습니다.
- 최종 주문 금액과 결제 금액은 클라이언트 요청값을 신뢰하지 않고 서버에서 다시 계산합니다.
- 상품이 판매중이 아니거나 재고가 부족하면 주문을 생성하지 않습니다.
- `userCouponId`가 있으면 요청 회원이 보유한 사용 가능한 쿠폰인지 검증하고, 쿠폰 할인 금액은 서버에서 계산합니다.
- 쿠폰을 사용하지 않는 경우 `userCouponId`는 `null` 또는 생략할 수 있습니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `VALIDATION_FAILED` | 400 | 요청 본문 형식 오류 또는 필수 값 누락 |
| `USER_NOT_FOUND` | 404 | 인증 사용자를 찾을 수 없음 |
| `RESOURCE_NOT_FOUND` | 404 | 요청한 회원 쿠폰을 찾을 수 없음 |
| `FORBIDDEN` | 403 | 타인의 회원 쿠폰으로 주문 시도 |
| `PRODUCT_NOT_FOUND` | 404 | 상품 없음 |
| `PRODUCT_NOT_ON_SALE` | 400 | 판매중 상품이 아님 |
| `ORDER_STOCK_SHORTAGE` | 409 | 주문 생성 중 재고 부족 |
| `COUPON_ALREADY_USED` | 409 | 이미 사용된 쿠폰 |

## POST `/api/orders/carts`

장바구니 상품을 기준으로 주문을 생성합니다. 장바구니 상품을 사용하지만 주문 생성 책임은 주문 도메인에 있습니다.
주문 생성 시 상품명과 상품 가격은 주문 당시 값으로 복사합니다.

- 인증: 필요
- HTTP Status: `201 Created`

### Request Body

| 필드 | 타입            | 필수 | 설명 |
| --- |---------------| --- | --- |
| `cartItemIds` | `Array<Long>` | Y | 주문할 장바구니 상품 ID 목록 |
| `userCouponId` | `Long`        | N | 사용할 회원 쿠폰 ID. 쿠폰을 사용하지 않으면 `null` 또는 생략 |

```json
{
  "cartItemIds": [100, 101],
  "userCouponId": 20
}
```

### Response Body

`/api/orders/direct` 응답과 동일합니다.

### 처리 규칙

- 토큰의 회원 ID를 기준으로 주문을 생성합니다.
- 주문 대상은 요청한 `cartItemIds`에 포함된 장바구니 상품입니다.
- 장바구니 상품은 반드시 요청 회원의 장바구니에 담긴 상품이어야 합니다.
- 주문 생성 시 상품명과 상품 가격은 주문 상품에 스냅샷으로 저장합니다.
- `order_items`는 주문 생성 API 내부에서 함께 생성되며, 단독 생성 API를 만들지 않습니다.
- 최종 주문 금액과 결제 금액은 클라이언트 요청값을 신뢰하지 않고 서버에서 다시 계산합니다.
- 상품이 판매중이 아니거나 재고가 부족하면 주문을 생성하지 않습니다.
- `userCouponId`가 있으면 요청 회원이 보유한 사용 가능한 쿠폰인지 검증하고, 쿠폰 할인 금액은 서버에서 계산합니다.
- 쿠폰을 사용하지 않는 경우 `userCouponId`는 `null` 또는 생략할 수 있습니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `VALIDATION_FAILED` | 400 | 요청 본문 형식 오류 또는 필수 값 누락 |
| `USER_NOT_FOUND` | 404 | 인증 사용자를 찾을 수 없음 |
| `RESOURCE_NOT_FOUND` | 404 | 요청한 회원 쿠폰을 찾을 수 없음 |
| `FORBIDDEN` | 403 | 타인의 회원 쿠폰으로 주문 시도 |
| `CART_NOT_FOUND` | 404 | 회원의 장바구니가 없음 |
| `CART_ITEM_SELECTION_INVALID` | 400 | 선택한 장바구니 항목이 유효하지 않음 |
| `PRODUCT_NOT_FOUND` | 404 | 상품 없음 |
| `PRODUCT_NOT_ON_SALE` | 400 | 판매중 상품이 아님 |
| `ORDER_STOCK_SHORTAGE` | 409 | 주문 생성 중 재고 부족 |
| `COUPON_ALREADY_USED` | 409 | 이미 사용된 쿠폰 |

## GET `/api/orders`

인증된 회원 본인의 주문 목록을 조회합니다.

- 인증: 필요
- HTTP Status: `200 OK`

### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `page` | `Integer` | N | `0` | 페이지 번호 |
| `size` | `Integer` | N | `10` | 페이지 크기 |
| `status` | `String` | N | 없음 | 주문 상태 필터 |

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [
      {
        "orderId": 200,
        "orderNumber": "ORD-20260622-000001",
        "status": "PAYMENT_PENDING",
        "totalProductAmount": 78000,
        "usedCouponAmount": 10000,
        "paymentAmount": 68000,
        "orderedAt": "2026-06-22T18:30:00+09:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  }
}
```

### 처리 규칙

- 토큰의 회원 ID를 기준으로 본인 주문만 조회합니다.
- 기본 정렬은 주문 생성일 최신순입니다.
- 주문 목록 화면에 필요한 요약 정보만 반환합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `INVALID_PAGINATION` | 400 | 페이지 번호 또는 크기 오류 |

## GET `/api/orders/{orderId}`

특정 주문의 상세 정보를 조회합니다. 주문 기본 정보와 주문 상품 목록을 함께 반환합니다.

- 인증: 필요
- HTTP Status: `200 OK`

### Path Variables

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `orderId` | `Long` | 조회할 주문 ID |

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "orderId": 200,
    "orderNumber": "ORD-20260622-000001",
    "status": "PAYMENT_PENDING",
    "totalProductAmount": 78000,
    "usedCouponAmount": 10000,
    "paymentAmount": 68000,
    "items": [
      {
        "orderItemId": 400,
        "productId": 10,
        "productName": "무선 키보드",
        "quantity": 2,
        "unitPrice": 39000,
        "lineAmount": 78000
      }
    ],
    "orderedAt": "2026-06-22T18:30:00+09:00",
    "canceledAt": null
  }
}
```

### 처리 규칙

- 주문 소유자만 상세 조회할 수 있습니다.
- 주문 상품의 `productName`, `unitPrice`는 주문 생성 시점에 저장된 스냅샷 값입니다.
- 결제 확정 요청에는 주문 생성 응답의 `paymentId`와 `portOnePaymentId`를 사용합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `ORDER_NOT_FOUND` | 404 | 주문 없음 |
| `ORDER_ACCESS_DENIED` | 403 | 타인의 주문 조회 |

## POST `/api/orders/{orderId}/cancel`

결제 전 주문을 취소합니다. 결제 완료 이후 취소는 환불 API에서 처리합니다.

- 인증: 필요
- HTTP Status: `200 OK`

### Path Variables

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `orderId` | `Long` | 취소할 주문 ID |

### Request Body

없음

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "orderId": 200,
    "orderNumber": "ORD-20260622-000001",
    "previousStatus": "PAYMENT_PENDING",
    "currentStatus": "CANCELED",
    "canceledAt": "2026-06-22T18:40:00+09:00"
  }
}
```

### 처리 규칙

- 주문 소유자만 취소할 수 있습니다.
- 주문 상태가 `PAYMENT_PENDING`인 결제 전 주문만 직접 취소할 수 있습니다.
- 취소 시 주문 상태는 `CANCELED`로 변경합니다.
- 주문 생성 시 차감하거나 예약한 재고와 쿠폰이 있다면 함께 복구합니다.
- 결제 완료 이후 취소는 `/api/refunds`를 사용합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `ORDER_NOT_FOUND` | 404 | 주문 없음 |
| `ORDER_ACCESS_DENIED` | 403 | 타인의 주문 취소 |
| `ORDER_CANCEL_NOT_ALLOWED` | 409 | 결제 전 취소가 불가능한 주문 상태 |

## 설계 메모

- 주문 생성 시 상품명과 상품 가격은 주문 당시 값으로 복사합니다.
- 결제 전 취소는 주문 도메인에서 처리하고, 결제 후 취소는 환불 도메인에서 처리합니다.
- 최종 결제 금액은 상품 총액에서 쿠폰 할인 금액을 차감해 서버에서 다시 계산해야 합니다.
- 주문 생성과 재고 차감은 하나의 트랜잭션으로 처리합니다.
- 쿠폰 할인 금액은 클라이언트가 전달하지 않고 `userCouponId`를 기준으로 서버에서 계산합니다.
- 장바구니 주문 생성 후 장바구니 상품 삭제 시점은 결제 흐름과 함께 팀 정책으로 정합니다.
