# API 명세

이 폴더는 커머스 프로젝트의 API 명세를 도메인별로 나눈 문서입니다.

공통 응답 형식, 인증 방식, 공통 Enum, 공통 에러 코드는 [common.md](./common.md)에 정의합니다.
각 도메인 문서에는 해당 API의 엔드포인트, 요청/응답 데이터, 인증 필요 여부, 설계 메모를 정리합니다.

## 문서 목록

| 문서 | 설명 |
| --- | --- |
| [common.md](./common.md) | Base URL, 인증, 응답 wrapper, 페이지네이션, Enum, 에러 코드 |
| [auth.md](./auth.md) | 회원가입, 로그인/JWT 발급, 로그아웃 |
| [users.md](./users.md) | 내 정보 조회, 회원 탈퇴 |
| [products.md](./products.md) | 상품 상세 조회, 상품 목록 조회, 상품 검색 |
| [carts.md](./carts.md) | 장바구니 상품 추가, 내 장바구니 조회, 수량 변경, 삭제 |
| [coupons.md](./coupons.md) | 쿠폰 이벤트 등록/조회, 선착순 발급, 보유 쿠폰 조회 |
| [orders.md](./orders.md) | 상품 바로 주문 생성, 장바구니 상품 주문 생성, 주문 상세 조회, 내 주문 목록 조회, 결제 전 주문 취소 |
| [payments.md](./payments.md) | 결제 승인 검증, PortOne 웹훅 수신 |
| [refunds.md](./refunds.md) | 결제 후 환불 요청, 내 환불 목록 조회 |
| [webhooks.md](./webhooks.md) | PortOne 웹훅 수신 |

## 엔드포인트 요약

| 도메인 | 기능 | Method | Path | 인증 |
| --- | --- | --- | --- | --- |
| 인증 | 회원가입 | POST | `/api/auth/signup` | 불필요 |
| 인증 | 로그인, JWT 발급 | POST | `/api/auth/login` | 불필요 |
| 인증 | 로그아웃 | POST | `/api/auth/logout` | 필요 |
| 회원 | 내 정보 조회 | GET | `/api/users/me` | 필요 |
| 회원 | 회원 탈퇴 | POST | `/api/users/delete` | 필요 |
| 상품 | 상품 상세 조회 | GET | `/api/products/{productId}` | 불필요 |
| 상품 | 상품 목록 조회 | GET | `/api/v1/products` | 불필요 |
| 상품 | Local Cache 적용 상품 검색 | GET | `/api/v2/products` | 불필요 |
| 검색 | 인기 검색어 조회 | GET | `/api/v1/search/popular` | 불필요 |
| 장바구니 | 장바구니 상품 추가 | POST | `/api/carts/items` | 필요 |
| 장바구니 | 내 장바구니 조회 | GET | `/api/carts` | 필요 |
| 장바구니 | 장바구니 수량 변경 | PATCH | `/api/carts/items/{cartItemId}` | 필요 |
| 장바구니 | 장바구니 상품 삭제 | DELETE | `/api/carts/items/{cartItemId}` | 필요 |
| 주문 | 상품에서 바로 주문 생성 | POST | `/api/orders/direct` | 필요 |
| 주문 | 장바구니 상품 주문 생성 | POST | `/api/orders/carts` | 필요 |
| 주문 | 주문 상세 조회 | GET | `/api/orders/{orderId}` | 필요 |
| 주문 | 내 주문 목록 조회 | GET | `/api/orders` | 필요 |
| 주문 | 결제 전 주문 취소 | POST | `/api/orders/{orderId}/cancel` | 필요 |
| 쿠폰 | 쿠폰 이벤트 등록 | POST | `/api/coupon-events` | 필요 (관리자) |
| 쿠폰 | 쿠폰 이벤트 목록 조회 | GET | `/api/coupon-events` | 불필요 |
| 쿠폰 | 선착순 쿠폰 발급 요청 | POST | `/api/coupon-events/{eventId}/issue` | 필요 |
| 쿠폰 | 내가 보유한 쿠폰 목록 조회 | GET | `/api/users/me/coupons` | 필요 |
| 결제 | 결제 승인 검증 | POST | `/api/payments/confirm` | 필요 |
| 결제 | PortOne 웹훅 수신 | POST | `/api/payments/webhook` | 웹훅 검증 |
| 환불 | 결제 후 환불 요청 | POST | `/api/refunds` | 필요 |
| 환불 | 내 환불 목록 조회 | GET | `/api/refunds` | 필요 |

## 설계 메모

- 인증 API는 `/api/auth` 하위에서 회원가입, 로그인/JWT 발급, 로그아웃을 담당합니다.
- 상품 목록 조회는 기본 조회 API인 `/api/v1/products`와 Local Cache가 적용된 검색 API인 `/api/v2/products`를 구분합니다.
- 장바구니 API는 인증된 회원의 장바구니를 기준으로 동작하므로 URL에 `cartId`를 노출하지 않습니다.
- 주문 취소는 결제 전 주문에 대해서만 허용합니다.
- 결제 승인 검증과 PortOne 웹훅 처리는 중복 요청이 들어올 수 있으므로 멱등성을 고려해야 합니다.
