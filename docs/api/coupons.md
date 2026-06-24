# 쿠폰 API

쿠폰 이벤트 등록, 조회, 선착순 발급, 보유 쿠폰 조회를 담당합니다.

성공/실패 응답은 모두 [공통 응답 wrapper](./common.md#공통-응답)를 사용합니다.

## 엔드포인트

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| `POST` | `/api/coupon-events` | 쿠폰 이벤트 등록 | 필요 (관리자) |
| `GET` | `/api/coupon-events` | 쿠폰 이벤트 목록 조회 | 불필요 |

## POST `/api/coupon-events`

쿠폰 이벤트를 등록합니다.

- 인증: 필요 
- HTTP Status: `201 Created`

### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `name` | `String` | Y | 쿠폰 이벤트명 |
| `discountType` | `String` | Y | `FIXED`, `PERCENT` |
| `discountAmount` | `Long` | Y | 할인 금액 또는 할인율 |
| `totalQuantity` | `Integer` | Y | 총 발급 수량 |
| `startsAt` | `String` | Y | 발급 시작일시 |
| `endsAt` | `String` | Y | 발급 종료일시 |

```json
{
  "name": "여름 시즌 쿠폰",
  "discountType": "PERCENT",
  "discountAmount": 10,
  "totalQuantity": 100,
  "startsAt": "2026-06-23T00:00:00+09:00",
  "endsAt": "2026-06-30T23:59:59+09:00"
}
```

### Response Body
```json
{
  "status": 201,
  "message": "요청이 성공했습니다.",
  "data": {
    "id": 1,
    "name": "여름 시즌 쿠폰",
    "discountType": "PERCENT",
    "discountAmount": 10,
    "totalQuantity": 100,
    "issuedQuantity": 0,
    "status": "OPEN",
    "startsAt": "2026-06-23T00:00:00+09:00",
    "endsAt": "2026-06-30T23:59:59+09:00"
  }
}
```

### Error

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| VALIDATION_FAILED | 400 | 요청 본문 형식 오류 또는 필수 값 누락 |
