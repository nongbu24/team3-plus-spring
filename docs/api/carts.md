# 장바구니 API

인증된 회원의 장바구니 조회와 장바구니 상품 추가, 수량 변경, 삭제를 담당합니다.
장바구니는 항상 토큰의 회원 기준으로 동작하므로 URL에 `cartId`를 받지 않습니다.

성공/실패 응답은 모두 [공통 응답 wrapper](./common.md#공통-응답)를 사용합니다.
아래 `Response Body` 예시는 공통 응답 wrapper 전체를 보여줍니다.

## 엔드포인트

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| `POST` | `/api/carts/items` | 장바구니 상품 추가 | 필요 |
| `GET` | `/api/carts` | 내 장바구니 조회 | 필요 |
| `PATCH` | `/api/carts/items/{cartItemId}` | 장바구니 수량 변경 | 필요 |
| `DELETE` | `/api/carts/items/{cartItemId}` | 장바구니 상품 삭제 | 필요 |

## POST `/api/carts/items`

장바구니에 상품을 추가합니다. 이미 담긴 상품이면 기존 장바구니 상품의 수량을 증가시킵니다.

- 인증: 필요
- HTTP Status: `201 Created`

### Request Body

| 필드 | 타입     | 필수 | 설명 |
| --- |--------| --- | --- |
| `productId` | `Long` | Y | 장바구니에 담을 상품 ID |
| `quantity` | `int`  | Y | 담을 수량. 1 이상 |

```json
{
  "productId": 10,
  "quantity": 2
}
```

### Response Body

```json
{
  "status": 201,
  "message": "요청이 성공했습니다.",
  "data": {
    "cartItemId": 100,
    "productId": 10,
    "productName": "무선 키보드",
    "quantity": 2,
    "unitPrice": 39000,
    "lineAmount": 78000
  }
}
```

### 장바구니 상품 응답 필드

| 필드 | 타입       | 설명 |
| --- |----------| --- |
| `cartItemId` | `Long`   | 장바구니 상품 ID |
| `productId` | `Long`   | 상품 ID |
| `productName` | `String` | 상품명 |
| `quantity` | `int`    | 장바구니에 담긴 수량 |
| `unitPrice` | `int`    | 상품 1개 가격 |
| `lineAmount` | `int`    | 해당 상품의 총 금액 |

### 처리 규칙

- 토큰의 회원 ID를 기준으로 본인 장바구니에 상품을 추가합니다.
- 같은 장바구니에 같은 상품은 중복 row로 담지 않습니다.
- 이미 담긴 상품을 다시 추가하면 기존 `cart_items`의 수량을 증가시킵니다.
- 상품 상태와 현재 재고를 검증합니다.
- 수량은 1 이상이어야 합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `VALIDATION_FAILED` | 400 | 요청 본문 형식 오류 또는 수량이 1 미만 |
| `PRODUCT_NOT_FOUND` | 404 | 상품이 없음 |
| `PRODUCT_NOT_ON_SALE` | 400 | 판매중 상품이 아님 |
| `CART_NOT_FOUND` | 404 | 회원의 장바구니가 없음 |
| `CART_STOCK_EXCEEDED` | 409 | 요청 수량이 현재 재고를 초과 |

## GET `/api/carts`

인증된 회원 본인의 장바구니를 조회합니다.

- 인증: 필요
- HTTP Status: `200 OK`

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "cartId": 1,
    "items": [
      {
        "cartItemId": 100,
        "productId": 10,
        "productName": "무선 키보드",
        "quantity": 2,
        "unitPrice": 39000,
        "lineAmount": 78000,
        "stock": 12,
        "status": "ON_SALE"
      }
    ],
    "totalQuantity": 2,
    "totalAmount": 78000
  }
}
```

### 장바구니 응답 필드

| 필드 | 타입        | 설명 |
| --- |-----------| --- |
| `cartId` | `Long`    | 장바구니 ID |
| `items` | `List`    | 장바구니 상품 목록 |
| `totalQuantity` | `Integer` | 장바구니 전체 상품 수량 |
| `totalAmount` | `Integer` | 장바구니 전체 금액 |

### 처리 규칙

- 토큰의 회원 ID를 기준으로 본인 장바구니만 조회합니다.
- 장바구니 전체 수량과 금액은 서버에서 계산합니다.
- 장바구니 상품의 현재 상품 상태와 재고를 함께 반환합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `CART_NOT_FOUND` | 404 | 회원의 장바구니가 없음 |

## PATCH `/api/carts/items/{cartItemId}`

장바구니 상품의 수량을 변경합니다.

- 인증: 필요
- HTTP Status: `200 OK`

### Path Variables

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `cartItemId` | `Long` | 변경할 장바구니 상품 ID |

### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `quantity` | `Integer` | Y | 변경할 수량. 1 이상 |

```json
{
  "quantity": 3
}
```

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "cartItemId": 100,
    "productId": 10,
    "productName": "무선 키보드",
    "quantity": 3,
    "unitPrice": 39000,
    "lineAmount": 117000,
    "stock": 12,
    "status": "ON_SALE"
  }
}
```

### 처리 규칙

- 장바구니 상품 소유자만 수량을 변경할 수 있습니다.
- 상품 상태와 현재 재고를 검증합니다.
- 수량은 1 이상이어야 합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `VALIDATION_FAILED` | 400 | 요청 본문 형식 오류 또는 수량이 1 미만 |
| `PRODUCT_NOT_ON_SALE` | 400 | 판매중 상품이 아님 |
| `CART_ITEM_NOT_FOUND` | 404 | 장바구니 상품이 없음 |
| `CART_ITEM_ACCESS_DENIED` | 403 | 타인의 장바구니 상품에 접근 |
| `CART_STOCK_EXCEEDED` | 409 | 요청 수량이 현재 재고를 초과 |

## DELETE `/api/carts/items/{cartItemId}`

장바구니 상품을 삭제합니다.

- 인증: 필요
- HTTP Status: `200 OK`

### Path Variables

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `cartItemId` | `Long` | 삭제할 장바구니 상품 ID |

### Request Body

없음

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "cartItemId": 100,
    "deleted": true
  }
}
```

### 처리 규칙

- 장바구니 상품 소유자만 삭제할 수 있습니다.
- 삭제 후 장바구니 총액은 다음 조회 시 서버에서 다시 계산합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `CART_ITEM_NOT_FOUND` | 404 | 장바구니 상품이 없음 |
| `CART_ITEM_ACCESS_DENIED` | 403 | 타인의 장바구니 상품에 접근 |

## 설계 메모

- 회원가입 성공 시 회원별 기본 장바구니를 생성합니다.
- 같은 장바구니에 같은 상품은 중복으로 담지 않습니다.
- 이미 담긴 상품을 다시 추가하면 새 row를 만들지 않고 기존 `cart_items`의 수량을 증가시킵니다.
- 장바구니 상품 추가와 수량 변경 시 현재 상품 상태와 재고를 검증합니다.
- 수량은 1 이상이어야 합니다.
