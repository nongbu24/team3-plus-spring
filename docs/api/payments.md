# 결제 API

결제 승인 검증, 결제 상세 조회, PortOne 웹훅 수신을 담당합니다.

성공/실패 응답은 모두 [공통 응답 wrapper](./common.md#공통-응답)를 사용합니다.
아래 `Response Body` 예시는 공통 응답 wrapper 전체를 보여줍니다.

## 엔드포인트

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| `POST` | `/api/payments/confirm` | 결제 승인 검증 | 필요 |
| `GET` | `/api/payments/{paymentId}` | 결제 상세 조회 | 필요 |
| `POST` | `/api/payments/webhook` | PortOne 웹훅 수신 | 웹훅 검증 |

## POST `/api/payments/confirm`

클라이언트가 PortOne 결제 완료 후 서버에 결제 승인을 검증 요청합니다. 서버는 PortOne 결제 정보와 주문/결제 정보를 비교한 뒤 결제 상태를 확정합니다.

- 인증: 필요
- HTTP Status: `200 OK`

### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `paymentId` | `Long` | Y | 서버에 저장된 결제 ID |
| `portonePaymentId` | `String` | Y | PortOne 결제 ID |

```json
{
  "paymentId": 300,
  "portonePaymentId": "pay_9381dde4-49d5-4079-af45-2ea490dbcc6d"
}
```

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "paymentId": 300,
    "orderId": 200,
    "portonePaymentId": "pay_9381dde4-49d5-4079-af45-2ea490dbcc6d",
    "status": "PAID",
    "totalProductAmount": 78000,
    "usedPointAmount": 5000,
    "paymentAmount": 73000,
    "approvedAt": "2026-06-22T18:35:00+09:00"
  }
}
```

### 처리 규칙

- 인증된 회원의 결제 건만 승인 검증할 수 있습니다.
- 결제 건에 연결된 주문이 결제 대기 상태인지 확인합니다.
- 요청한 `portonePaymentId`가 서버에 저장된 결제 식별자와 일치하는지 확인합니다.
- PortOne 결제 단건 조회 결과가 결제 성공 상태인지 확인합니다.
- PortOne 승인 금액과 서버가 계산한 결제 금액이 일치해야 합니다.
- 검증에 성공하면 결제 상태를 `PAID`, 주문 상태를 `COMPLETED`로 변경합니다.
- 같은 결제 승인 요청이 중복으로 들어와도 최종 상태가 같도록 멱등하게 처리합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `VALIDATION_FAILED` | 400 | 요청 본문 형식 오류 또는 필수 값 누락 |
| `PAYMENT_NOT_FOUND` | 404 | 결제 없음 |
| `PAYMENT_ACCESS_DENIED` | 403 | 타인의 결제 승인 검증 |
| `PAYMENT_ALREADY_PROCESSED` | 409 | 이미 처리된 결제 |
| `PAYMENT_AMOUNT_MISMATCH` | 400 | PortOne 승인 금액과 서버 결제 금액 불일치 |
| `PAYMENT_STATUS_NOT_PAID` | 400 | PortOne 결제 상태가 성공 상태가 아님 |
| `ORDER_NOT_FOUND` | 404 | 결제에 연결된 주문 없음 |
| `ORDER_ACCESS_DENIED` | 403 | 타인의 주문에 연결된 결제 |
| `EXTERNAL_API_FAILED` | 502 | PortOne API 호출 실패 |

## GET `/api/payments/{paymentId}`

결제 상세 정보를 조회합니다.

- 인증: 필요
- HTTP Status: `200 OK`

### Path Variables

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `paymentId` | `Long` | 조회할 결제 ID |

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "paymentId": 300,
    "orderId": 200,
    "orderNumber": "ORD-20260622-000001",
    "portonePaymentId": "pay_9381dde4-49d5-4079-af45-2ea490dbcc6d",
    "status": "PAID",
    "totalProductAmount": 78000,
    "usedPointAmount": 5000,
    "paymentAmount": 73000,
    "approvedAt": "2026-06-22T18:35:00+09:00",
    "createdAt": "2026-06-22T18:30:00+09:00"
  }
}
```

### 처리 규칙

- 인증된 회원 본인의 결제 건만 조회할 수 있습니다.
- 결제 금액은 서버에서 계산해 저장한 값을 반환합니다.
- 주문 상세 정보가 필요하면 `/api/orders/{orderId}`를 사용합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `PAYMENT_NOT_FOUND` | 404 | 결제 없음 |
| `PAYMENT_ACCESS_DENIED` | 403 | 타인의 결제 조회 |

## POST `/api/payments/webhook`

PortOne에서 전송한 결제 이벤트를 수신합니다. 웹훅은 클라이언트 결제 승인 요청보다 먼저 도착하거나, 같은 이벤트가 여러 번 도착할 수 있습니다.

- 인증: 웹훅 서명 검증
- HTTP Status: `200 OK`

### Request Header

| 헤더 | 필수 | 설명 |
| --- | --- | --- |
| `webhook-id` | Y | PortOne 웹훅 ID |
| `webhook-signature` | Y | PortOne 웹훅 서명 |
| `webhook-timestamp` | Y | PortOne 웹훅 전송 시각 |

### Request Body

PortOne에서 전달하는 웹훅 payload를 그대로 받습니다.

```json
{
  "type": "Transaction.Paid",
  "data": {
    "paymentId": "pay_9381dde4-49d5-4079-af45-2ea490dbcc6d"
  }
}
```

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "received": true
  }
}
```

### 처리 규칙

- JWT 인증을 사용하지 않고 PortOne 웹훅 서명을 검증합니다.
- 웹훅 본문에서 PortOne 결제 ID를 추출합니다.
- 같은 웹훅 이벤트가 여러 번 들어와도 중복 처리되지 않도록 멱등하게 처리합니다.
- 결제 성공 이벤트라면 결제 승인 검증과 같은 기준으로 결제 금액과 상태를 확인합니다.
- 웹훅 원문과 처리 결과는 웹훅 이벤트 기록으로 남길 수 있습니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `WEBHOOK_SIGNATURE_INVALID` | 400 | 웹훅 서명 검증 실패 |
| `WEBHOOK_PAYLOAD_INVALID` | 400 | 웹훅 본문 파싱 실패 |
| `PAYMENT_WEBHOOK_INVALID` | 400 | 결제 웹훅 요청이 올바르지 않음 |
| `PAYMENT_NOT_FOUND` | 404 | 웹훅에 해당하는 결제 없음 |
| `PAYMENT_AMOUNT_MISMATCH` | 400 | PortOne 승인 금액과 서버 결제 금액 불일치 |
| `EXTERNAL_API_FAILED` | 502 | PortOne API 호출 실패 |

## 설계 메모

- 결제 승인 검증 API와 PortOne 웹훅은 같은 결제 검증 기준을 사용해야 합니다.
- 클라이언트 요청과 웹훅은 순서가 보장되지 않으므로 결제 처리는 멱등해야 합니다.
- 결제 금액은 주문 생성 시 서버가 계산한 금액을 기준으로 검증합니다.
- PortOne API Secret과 웹훅 Secret은 응답에 포함하지 않습니다.
- 결제 완료 이후 취소는 환불 API인 `/api/refunds`에서 처리합니다.
