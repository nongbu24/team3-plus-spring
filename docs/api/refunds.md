# 환불 API

결제 완료 이후 환불 요청을 담당합니다.

성공/실패 응답은 모두 [공통 응답 wrapper](./common.md#공통-응답)를 사용합니다.
아래 `Response Body` 예시는 공통 응답 wrapper 전체를 보여줍니다.

## 엔드포인트

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| `POST` | `/api/refunds` | 결제 후 환불 요청 | 필요 |
| `GET` | `/api/refunds` | 내 환불 목록 조회 | 필요 |

## POST `/api/refunds`

결제 완료된 주문 또는 결제 건에 대해 환불을 요청합니다.

- 인증: 필요
- HTTP Status: `201 Created`

### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `paymentId` | `Long` | Y | 환불할 결제 ID |
| `reason` | `String` | Y | 환불 사유 |

```json
{
  "paymentId": 300,
  "reason": "단순 변심으로 인한 환불 요청"
}
```

### Response Body

```json
{
  "status": 201,
  "message": "요청이 성공했습니다.",
  "data": {
    "refundId": 500,
    "paymentId": 300,
    "orderId": 200,
    "status": "REQUESTED",
    "refundAmount": 68000,
    "reason": "단순 변심으로 인한 환불 요청",
    "requestedAt": "2026-06-22T19:00:00+09:00",
    "completedAt": null
  }
}
```

### 처리 규칙

- 인증된 회원 본인의 결제 건만 환불 요청할 수 있습니다.
- 결제 상태가 환불 가능한 상태인지 확인합니다.
- 환불 금액은 클라이언트가 직접 입력하지 않고 서버가 결제 금액을 기준으로 계산합니다.
- 환불 완료 시 주문, 결제, 재고 상태를 함께 정리합니다.
- 결제 전 주문 취소는 환불 API가 아니라 `/api/orders/{orderId}/cancel`에서 처리합니다.
- PortOne 결제 취소가 필요한 경우 서버에서 PortOne API를 호출합니다. 클라이언트는 PortOne 취소 API를 직접 호출하지 않습니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `VALIDATION_FAILED` | 400 | 요청 본문 형식 오류 또는 필수 값 누락 |
| `PAYMENT_NOT_FOUND` | 404 | 결제 없음 |
| `PAYMENT_ACCESS_DENIED` | 403 | 타인의 결제 환불 요청 |
| `REFUND_NOT_ALLOWED` | 409 | 환불 가능한 결제 상태가 아님 |
| `REFUND_AMOUNT_INVALID` | 400 | 환불 금액 계산 오류 |
| `EXTERNAL_API_FAILED` | 502 | PortOne 환불 API 호출 실패 |

## GET `/api/refunds`

인증된 회원 본인의 환불 목록을 조회합니다.

- 인증: 필요
- HTTP Status: `200 OK`

### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `page` | `Integer` | N | `0` | 페이지 번호 |
| `size` | `Integer` | N | `10` | 페이지 크기 |
| `status` | `String` | N | 없음 | 환불 상태 필터 |

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [
      {
        "refundId": 500,
        "paymentId": 300,
        "orderId": 200,
        "status": "REQUESTED",
        "refundAmount": 68000,
        "reason": "단순 변심으로 인한 환불 요청",
        "requestedAt": "2026-06-22T19:00:00+09:00",
        "completedAt": null
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

- 토큰의 회원 ID를 기준으로 본인 환불 내역만 조회합니다.
- 기본 정렬은 환불 요청일 최신순입니다.
- 환불 목록 화면에 필요한 요약 정보만 반환합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `INVALID_PAGINATION` | 400 | 페이지 번호 또는 크기 오류 |

## 설계 메모

- 환불은 결제 완료 이후 취소 흐름입니다.
- 결제 전 취소는 주문 도메인에서 처리하고, 결제 후 취소는 환불 도메인에서 처리합니다.
- 환불 금액은 서버에서 계산합니다.
- 환불 완료 시 재고 복구가 함께 필요할 수 있습니다.
- 같은 결제 건에 중복 환불 요청이 들어오지 않도록 상태를 확인해야 합니다.
