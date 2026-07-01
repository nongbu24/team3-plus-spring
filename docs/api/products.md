# 상품 API

상품은 조회와 검색 중심 도메인입니다.

성공/실패 응답은 모두 [공통 응답 wrapper](./common.md#공통-응답)를 사용합니다.
아래 `Response Body` 예시는 공통 응답 wrapper 전체를 보여줍니다.

## 엔드포인트

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/products` | 상품 목록 조회 | 불필요 |
| `GET` | `/api/products/{productId}` | 상품 상세 조회 | 불필요 |
| `GET` | `/api/v2/products` | Local Cache 적용 상품 검색 | 불필요 |
| `GET` | `/api/products/popular` | 인기 상품 목록 조회 (조회수 기준) | 불필요 |

## GET `/api/v1/products`

상품 목록을 필터링, 정렬, 페이지네이션하여 조회합니다.

- Method: `GET`
- Path: `/api/v1/products`
- 인증: 불필요
- HTTP Status: `200 OK`

### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `categoryId` | `Long` | N | 없음 | 카테고리 ID |
| `keyword` | `String` | N | 없음 | 상품명 검색어 |
| `status` | `String` | N | 없음 | `ON_SALE`, `SOLD_OUT`, `DISCONTINUED` |
| `sort` | `String` | N | `LATEST` | 정렬 기준 |
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
        "id": 10,
        "name": "무선 키보드",
        "price": 39000,
        "stock": 12,
        "status": "ON_SALE",
        "categoryId": 1,
        "categoryName": "키보드"
      }
    ],
    "totalElements": 45,
    "totalPages": 5,
    "number": 0,
    "size": 10,
    "first": true,
    "last": false
  }
}
```

### 처리 규칙

- 사용자 상품 목록에는 `ON_SALE` 상태 조건을 기본 적용합니다.
- `categoryId`, `keyword`, `status` 조건이 있으면 해당 조건으로 필터링합니다.
- 존재하지 않는 categoryId는 빈 목록을 반환합니다.
- 기본 정렬은 최신순입니다.
- 페이지 번호와 페이지 크기는 서버에서 검증합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | 쿼리 파라미터 형식 오류 |
| `INVALID_ENUM_VALUE` | 400 | 잘못된 `status` 또는 `sort` |
| `INVALID_PAGINATION` | 400 | 페이지 번호 또는 크기 오류 |

## GET `/api/products/{productId}`

상품 상세 정보를 조회합니다.

- Method: `GET`
- Path: `/api/products/{productId}`
- 인증: 불필요
- HTTP Status: `200 OK`

### Path Variables

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `productId` | `Long` | 상품 ID |

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "id": 10,
    "name": "무선 키보드",
    "description": "저소음 무선 키보드입니다.",
    "price": 39000,
    "stock": 12,
    "status": "ON_SALE",
    "categoryId": 6,
    "categoryName": "키보드/마우스"
  }
}
```

### 처리 규칙

- 삭제되지 않은 상품만 조회합니다.
- 상품 상세 화면에 필요한 설명, 가격, 재고, 판매 상태를 반환합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `PRODUCT_NOT_FOUND` | 404 | 상품이 없음 |

## GET `/api/v2/products`

Local Cache가 적용된 상품 검색 API입니다.

- Method: `GET`
- Path: `/api/v2/products`
- 인증: 불필요
- HTTP Status: `200 OK`

### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `keyword` | `String` | N | 없음 | 상품명 검색어 |
| `categoryId` | `Long` | N | 없음 | 카테고리 ID |
| `page` | `Integer` | N | `0` | 페이지 번호 |
| `size` | `Integer` | N | `10` | 페이지 크기 |

### Response Body

상품 목록 조회와 같은 페이지 응답 형식을 사용합니다.

### 처리 규칙

- Local Cache가 적용된 상품 검색 API입니다.
- 검색 조건과 페이지 응답 형식은 기본 상품 목록 조회와 동일하게 유지합니다.
- 캐시 적용 여부와 무관하게 응답 데이터의 의미는 `/api/v1/products`와 일관되게 유지합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | 쿼리 파라미터 형식 오류 |
| `INVALID_PAGINATION` | 400 | 페이지 번호 또는 크기 오류 |

## 설계 메모

- 사용자 상품 목록에는 `ON_SALE` 상태 조건을 기본 적용합니다.
- 상품 가격은 주문 생성 시 주문 상품에 스냅샷으로 저장합니다. 따라서 주문 생성 후 상품 가격이 바뀌어도 과거 주문 금액은 바뀌지 않습니다.
- 상품 검색 API는 기본 조회 API인 `/api/v1/products`와 Local Cache 적용 API인 `/api/v2/products`를 구분합니다.

## GET `/api/products/popular`

조회수(view_count) 기준 내림차순으로 인기 상품 목록을 조회합니다.

- Method: `GET`
- Path: `/api/products/popular`
- 인증: 불필요
- HTTP Status: `200 OK`

### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `limit` | `Integer` | N | `10` | 조회할 상품 개수 (1~100) |

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": [
    {
      "id": 10,
      "name": "무선 키보드",
      "price": 39000,
      "stock": 12,
      "status": "ON_SALE",
      "categoryId": 1,
      "categoryName": "키보드"
    }
  ]
}
```

### 처리 규칙

- 상품의 `view_count`를 내림차순으로 정렬하여 상위 `limit`개를 반환합니다.
- 페이지네이션 없이 단일 리스트로 반환합니다.
- `limit`은 1 이상 100 이하만 허용합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `INVALID_PAGINATION` | 400 | `limit`이 0 이하이거나 상한을 초과한 경우 |