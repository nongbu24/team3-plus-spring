# 웹훅 API

PortOne 웹훅 수신과 처리 결과 기록을 담당합니다.

웹훅은 JWT 인증을 사용하지 않고 PortOne 웹훅 서명을 검증합니다.
웹훅 본문은 그대로 신뢰하지 않고, 필요한 결제 식별자를 추출한 뒤 서버 기준 데이터와 PortOne 조회 결과를 비교해 처리합니다.

성공/실패 응답은 모두 [공통 응답 wrapper](./common.md#공통-응답)를 사용합니다.
아래 `Response Body` 예시는 공통 응답 wrapper 전체를 보여줍니다.

## 엔드포인트

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| `POST` | `/api/payments/webhook` | PortOne 웹훅 수신 | 웹훅 검증 |

## POST `/api/payments/webhook`

PortOne에서 전송한 결제 이벤트를 수신합니다.
클라이언트의 결제 승인 요청보다 웹훅이 먼저 도착하거나 같은 이벤트가 여러 번 도착할 수 있으므로 멱등하게 처리합니다.

- 인증: 웹훅 서명 검증
- HTTP Status: `200 OK`
- Content-Type: `application/json`

### Request Header

PortOne 웹훅 서명 검증에 필요한 헤더입니다.

| 헤더 | 필수 | 설명 |
| --- | --- | --- |
| `webhook-id` | Y | 웹훅 이벤트 ID |
| `webhook-signature` | Y | 웹훅 서명 |
| `webhook-timestamp` | Y | 웹훅 전송 시각 |

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
    "received": true,
    "processed": true,
    "portonePaymentId": "pay_9381dde4-49d5-4079-af45-2ea490dbcc6d",
    "reason": "PROCESSED"
  }
}
```

처리 대상이 아니거나 이미 처리한 웹훅도 재전송 방지를 위해 `200 OK`로 응답할 수 있습니다.

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "received": true,
    "processed": false,
    "portonePaymentId": null,
    "reason": "DUPLICATE_OR_IGNORED"
  }
}
```

### 처리 규칙

- JWT 인증을 사용하지 않고 PortOne 웹훅 서명을 검증합니다.
- 웹훅 원문과 처리 결과를 `webhook_events`에 기록합니다.
- 웹훅 본문에서 PortOne 결제 ID를 추출합니다.
- 결제 성공 이벤트라면 결제 승인 검증 API와 같은 기준으로 결제 상태와 금액을 확인합니다.
- 같은 `webhook-id`가 여러 번 들어와도 중복 처리하지 않습니다.
- 본문 금액이나 상태는 최종 신뢰하지 않고 PortOne API 조회 결과와 서버 저장 값을 기준으로 처리합니다.
- 현재 처리하지 않는 이벤트는 실패로 보지 않고 수신 기록만 남긴 뒤 정상 응답할 수 있습니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `WEBHOOK_SIGNATURE_INVALID` | 400 | 웹훅 서명 검증 실패 |
| `WEBHOOK_PAYLOAD_INVALID` | 400 | 웹훅 본문 파싱 실패 |
| `PAYMENT_WEBHOOK_INVALID` | 400 | 결제 웹훅 요청이 올바르지 않음 |
| `PAYMENT_NOT_FOUND` | 404 | 웹훅에 해당하는 결제 없음 |
| `PAYMENT_AMOUNT_MISMATCH` | 400 | PortOne 승인 금액과 서버 결제 금액 불일치 |
| `PAYMENT_STATUS_NOT_PAID` | 400 | PortOne 결제 상태가 성공 상태가 아님 |
| `EXTERNAL_API_FAILED` | 502 | PortOne API 호출 실패 |

## 설계 메모

- 웹훅은 재전송될 수 있으므로 항상 멱등하게 처리합니다.
- 결제 승인 검증 API와 PortOne 웹훅은 같은 결제 검증 기준을 사용해야 합니다.
- 클라이언트 결제 승인 요청과 웹훅은 도착 순서가 보장되지 않습니다.
- 웹훅 Secret은 서버 환경변수로 관리하고 응답에 포함하지 않습니다.
- 웹훅 이벤트 기록은 장애 분석과 중복 처리 방지에 사용합니다.
