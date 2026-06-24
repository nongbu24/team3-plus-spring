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
| --- |----| --- | --- |
| `name` | `String` | Y | 쿠폰 이벤트명 |
| `discountType` | `String` | Y | `FIXED`, `PERCENT` |
| `discountAmount` | `int` | Y | 할인 금액 또는 할인율 |
| `totalQuantity` | `int` | Y | 총 발급 수량 |
| `startsAt` | `String` | Y | 발급 시작일시 |
| `endsAt` | `String` | Y | 발급 종료일시 |

```json
{
  "name": "여름 시즌 쿠폰",
  "discountType": "PERCENT",
  "discountAmount": 10,
  "totalQuantity": 100,
  "startsAt": "2026-06-23T00:00:00",
  "endsAt": "2026-06-30T23:59:59"
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
    "startsAt": "2026-06-23T00:00:00",
    "endsAt": "2026-06-30T23:59:59"
  }
}
```

### Error

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| VALIDATION_FAILED | 400 | 요청 본문 형식 오류 또는 필수 값 누락 |

## GET `/api/coupon-events`

발급 중인(`OPEN`) 쿠폰 이벤트 목록을 조회합니다.

- 인증: 불필요
- HTTP Status: `200 OK`
- 종료된(`CLOSED`) 쿠폰 이벤트는 목록에 노출되지 않습니다.

### Query Parameter

| 이름 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `page` | `int` | `0` | 0부터 시작하는 페이지 번호 |
| `size` | `int` | `10` | 페이지당 조회할 쿠폰 이벤트 수 |

### Response Body
```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "여름 시즌 쿠폰",
        "discountType": "PERCENT",
        "discountAmount": 10,
        "totalQuantity": 100,
        "issuedQuantity": 0,
        "status": "OPEN",
        "startsAt": "2026-06-23T00:00:00",
        "endsAt": "2026-06-30T23:59:59"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 10,
    "number": 0,
    "first": true,
    "last": true,
    "numberOfElements": 1,
    "empty": false
  }
}
```
