# 쿠폰 API

쿠폰 이벤트 등록, 조회, 선착순 발급, 보유 쿠폰 조회를 담당합니다.

성공/실패 응답은 모두 [공통 응답 wrapper](./common.md#공통-응답)를 사용합니다.

## 엔드포인트

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| `POST` | `/api/coupon-events` | 쿠폰 이벤트 등록 | 필요 (관리자) |
| `GET` | `/api/coupon-events` | 쿠폰 이벤트 목록 조회 | 불필요 |
| `POST` | `/api/coupon-events/{couponEventId}/issue` | 쿠폰 발급 | 필요 |
| `GET` | `/api/users/me/coupons` | 내 쿠폰 목록 조회 | 필요 |

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
| `validDays` | `int` | Y | 발급일로부터 사용 가능한 기간(일). 발급된 쿠폰의 `expiredAt`은 `issuedAt + validDays`로 계산됨 |

```json
{
  "name": "여름 시즌 쿠폰",
  "discountType": "PERCENT",
  "discountAmount": 10,
  "totalQuantity": 100,
  "startsAt": "2026-06-23T00:00:00",
  "endsAt": "2026-06-30T23:59:59",
  "validDays": 30
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
    "endsAt": "2026-06-30T23:59:59",
    "validDays": 30
  }
}
```

### Error

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| VALIDATION_FAILED | 400 | 요청 본문 형식 오류 또는 필수 값 누락 |

## GET `/api/coupon-events`

발급 중이면서 발급 기간(`startsAt` ~ `endsAt`) 내에 있는 쿠폰 이벤트 목록을 조회합니다.

- 인증: 불필요
- HTTP Status: `200 OK`
- 종료된(`CLOSED`) 쿠폰 이벤트는 목록에 노출되지 않습니다.
- `status`가 `OPEN`이어도 발급 시작일(`startsAt`)이 지나지 않았거나 발급 종료일(`endsAt`)이 지난 쿠폰 이벤트는 목록에 노출되지 않습니다.

### Query Parameter

| 이름 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `page` | `int` | `0` | 0부터 시작하는 페이지 번호 (`0` 이상) |
| `size` | `int` | `10` | 페이지당 조회할 쿠폰 이벤트 수 (`1` 이상 `100` 이하) |

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

### Error

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| VALIDATION_FAILED | 400 | `page`가 0 미만이거나, `size`가 1 미만 또는 100 초과인 경우 |

## POST `/api/coupon-events/{couponEventId}/issue`

로그인한 사용자가 특정 쿠폰 이벤트의 쿠폰을 발급받습니다.

- 인증: 필요
- HTTP Status: `201 Created`
- 이미 발급받은 쿠폰이거나, 재고가 소진됐거나, 발급 기간이 아니면 발급할 수 없습니다.

### Path Variable

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `couponEventId` | `Long` | 발급받을 쿠폰 이벤트 ID |

### Response Body
```json
{
  "status": 201,
  "message": "요청이 성공했습니다.",
  "data": {
    "id": 1,
    "couponEventId": 1,
    "status": "ISSUED",
    "issuedAt": "2026-06-30T10:00:00"
  }
}
```

### Error

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| UNAUTHORIZED | 401 | 인증되지 않은 요청 |
| COUPON_EVENT_NOT_FOUND | 404 | 쿠폰 이벤트가 존재하지 않는 경우 |
| COUPON_EVENT_CLOSED | 409 | 발급 기간이 아니거나 종료된 쿠폰 이벤트인 경우 |
| COUPON_ALREADY_ISSUED | 409 | 이미 발급받은 쿠폰인 경우 |
| COUPON_STOCK_EXHAUSTED | 409 | 재고가 소진된 경우 |

## GET `/api/users/me/coupons`

내가 보유한 쿠폰 중 사용 가능한(발급됨 + 사용기한이 지나지 않은) 쿠폰 목록을 조회합니다.

- 인증: 필요
- HTTP Status: `200 OK`
- 이미 사용한(`USED`) 쿠폰이나 사용기한(`expiredAt`)이 지난 쿠폰은 목록에 노출되지 않습니다.
- `issuedAt` 내림차순으로 정렬됩니다.

### Query Parameter

| 이름 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `page` | `int` | `0` | 0부터 시작하는 페이지 번호 (`0` 이상) |
| `size` | `int` | `10` | 페이지당 조회할 쿠폰 수 (`1` 이상 `100` 이하) |

### Response Body
```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 1,
        "couponEventId": 1,
        "status": "ISSUED",
        "issuedAt": "2026-06-23T10:00:00",
        "expiredAt": "2026-07-23T10:00:00"
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

### Error

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| VALIDATION_FAILED | 400 | `page`가 0 미만이거나, `size`가 1 미만 또는 100 초과인 경우 |
| UNAUTHORIZED | 401 | 인증되지 않은 요청 |
