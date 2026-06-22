# 포인트 API

회원의 현재 포인트 잔액 조회와 포인트 이력 조회를 담당합니다.

성공/실패 응답은 모두 [공통 응답 wrapper](./common.md#공통-응답)를 사용합니다.
아래 `Response Body` 예시는 공통 응답 wrapper 전체를 보여줍니다.

## 엔드포인트

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| `GET` | `/api/points/me` | 내 포인트 잔액 조회 | 필요 |
| `GET` | `/api/points/histories` | 내 포인트 이력 조회 | 필요 |

## GET `/api/points/me`

인증된 회원의 현재 포인트 잔액을 조회합니다.

- 인증: 필요
- HTTP Status: `200 OK`

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "userId": 1,
    "balance": 5000
  }
}
```

### 처리 규칙

- 토큰의 회원 ID를 기준으로 본인 포인트만 조회합니다.
- 포인트 잔액은 주문 생성, 주문 취소, 결제 완료, 환불 처리 과정에서 변경될 수 있습니다.
- 포인트 이력은 별도 테이블인 `point_histories`에 기록합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `POINT_ACCOUNT_NOT_FOUND` | 404 | 포인트 계정 없음 |

## GET `/api/points/histories`

인증된 회원의 포인트 적립, 사용, 복구, 만료 이력을 최신순으로 조회합니다.

- 인증: 필요
- HTTP Status: `200 OK`

### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `type` | `String` | N | 없음 | 이력 타입 |
| `page` | `Integer` | N | `0` | 페이지 번호 |
| `size` | `Integer` | N | `10` | 페이지 크기 |

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "content": [
      {
        "pointHistoryId": 900,
        "type": "EARN",
        "amount": 730,
        "balanceAfter": 5730,
        "description": "결제 완료 포인트 적립",
        "createdAt": "2026-06-22T18:35:00+09:00"
      },
      {
        "pointHistoryId": 899,
        "type": "USE",
        "amount": -5000,
        "balanceAfter": 5000,
        "description": "주문 포인트 사용",
        "createdAt": "2026-06-22T18:30:00+09:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 2,
    "totalPages": 1,
    "hasNext": false
  }
}
```

### 처리 규칙

- 토큰의 회원 ID를 기준으로 본인 포인트 이력만 조회합니다.
- 기본 정렬은 생성일 최신순입니다.
- `type`이 있으면 해당 포인트 이력 타입만 필터링합니다.
- 포인트 이력은 돈의 흐름과 비슷하므로 삭제하지 않습니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `INVALID_ENUM_VALUE` | 400 | 잘못된 포인트 이력 타입 |
| `INVALID_PAGINATION` | 400 | 페이지 번호 또는 크기 오류 |
| `POINT_ACCOUNT_NOT_FOUND` | 404 | 포인트 계정 없음 |
| `POINT_HISTORY_NOT_FOUND` | 404 | 포인트 이력 없음 |

## 설계 메모

- 포인트 잔액은 회원별로 하나의 현재 잔액을 관리합니다.
- 포인트 적립, 사용, 복구, 만료 같은 변경 내역은 `point_histories`에 기록합니다.
- 주문 생성 시 사용할 포인트가 있으면 현재 잔액을 검증합니다.
- 결제 전 주문 취소나 결제 후 환불로 포인트 복구가 필요한 경우 잔액을 함께 갱신하고 이력을 남깁니다.
- 포인트 이력은 삭제하지 않고, 필요한 경우 취소/복구 이력을 추가로 남기는 방식으로 추적합니다.
