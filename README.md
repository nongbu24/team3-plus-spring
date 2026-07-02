<div align="center">

# 전자제품 커머스 플랫폼

캐싱 · 선착순 쿠폰 발급 동시성 제어 · 실시간 채팅 · PG 결제 웹훅/멱등성 처리를 갖춘 Spring Boot 이커머스 백엔드

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-00000F?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white)

</div>

## 프로젝트 소개

Spring Boot 기반 전자제품 판매 이커머스 백엔드로, 대용량 트래픽에서도 안전한 선착순 쿠폰 발급, 캐시 기반 상품 검색 성능 개선, PortOne 결제 웹훅 멱등성 처리, WebSocket 기반 실시간 1:1 상담 채팅을 구현하는 데 초점을 맞춘 프로젝트입니다.

### 핵심 기능

| 도메인 | 기능 |
|---|---|
| 회원 | 회원가입, JWT 로그인/로그아웃, 내 정보 조회, 회원 탈퇴 |
| 상품 | 상품 목록/상세 조회, Local Cache 적용 검색(v1/v2), 조회수 기반 인기 상품 조회 |
| 장바구니 | 상품 담기, 수량 변경, 삭제, 내 장바구니 조회 |
| 주문/결제 | 바로 주문/장바구니 주문 생성, PortOne 결제 연동, 웹훅 수신, 결제 전 주문 취소 |
| 쿠폰 | 쿠폰 이벤트 등록, 선착순 발급, 보유 쿠폰 조회 |
| 채팅/AI 상담 | STOMP 기반 1:1 문의 채팅, Redis Pub/Sub 다중 서버 브로드캐스팅, Claude 기반 AI 챗봇 |

## 기술 스택

| 카테고리 | 스택 |
|---|---|
| Backend | Java 17, Spring Boot 4.1.0, Spring Security, Spring Data JPA, QueryDSL, Spring Validation, Spring AI(Anthropic) |
| Database | MySQL, Redis 7 (캐싱 · 쿠폰 재고 카운터 · Pub/Sub) |
| 인증/결제 | JWT(JJWT), PortOne PG 연동 |
| 실행 환경 | Docker Compose (Redis 로컬 실행) |
| 협업 | Git, GitHub (Issue/PR 템플릿) |

> 실제 배포된 인프라가 없는 로컬 실행 전용 프로젝트라 아키텍처(인프라 구성도) 섹션은 생략했습니다.

## 팀 소개

| 이름 | 역할 | GitHub |
|---|---|---|
| 박정원 | Backend (인증/인가, 실시간 채팅) | [@nongbu24](https://github.com/nongbu24) |
| 정예진 | Backend (쿠폰: 동시성/캐싱) | [@yxejxnn](https://github.com/yxejxnn) |
| 라예실 | Backend (주문/결제) | [@sirisiyesiri](https://github.com/sirisiyesiri) |
| 이재석 | Backend (상품/인덱싱) | [@Sole02](https://github.com/Sole02) |
| 이동희 | Backend (장바구니/캐싱) | [@20LDH](https://github.com/20LDH) |

## 주요 기능

### 선착순 쿠폰 발급
한정 수량의 쿠폰 이벤트를 선착순으로 발급받을 수 있습니다. 동시에 여러 사용자가 요청해도 총 발급 수량을 넘지 않습니다.

발급 요청이 들어오면 먼저 Redis에 저장된 재고 카운터를 Lua 스크립트로 원자적으로 감소시켜(`DECR`) 1차로 동시성을 제어하고, 통과한 요청만 DB에서 `issuedQuantity < totalQuantity` 조건이 걸린 UPDATE 쿼리로 발급 수량을 증가시켜 2차로 검증합니다. DB 반영이나 `UserCoupon` 저장에 실패하면 감소시켰던 Redis 재고를 다시 복구(`restoreStock`)해 실제 발급 수보다 재고가 줄어드는 일이 없도록 처리했습니다. 이미 발급받은 이력은 `(user_id, coupon_event_id)` UNIQUE 제약과 사전 조회로 중복 발급을 막습니다.

### 상품 검색 캐싱 (Local Cache v1/v2)
카테고리, 키워드, 상태로 상품을 필터링/정렬/페이지네이션하여 검색할 수 있습니다.

캐시 효과를 비교할 수 있도록 캐시가 없는 `/api/v1/products`와 Spring `@Cacheable` 기반 Redis 캐시가 적용된 `/api/v2/products`를 분리해서 제공합니다. 캐시는 TTL 30초로 짧게 설정해 상품 정보가 오래 stale 되지 않도록 하면서도, 반복 조회가 몰리는 구간에서 DB 부하를 줄이는 효과를 확인할 수 있게 구성했습니다.

### 주문 조회 인덱스 최적화
주문/상품 목록 조회 API가 대량 데이터에서도 빠르게 응답하도록 인덱스를 설계했습니다.

100만 건의 더미 데이터를 넣고 실제 `ProductRepository` 쿼리를 `EXPLAIN`으로 분석한 결과, 단순히 WHERE 조건 컬럼만으로 인덱스를 구성하면 `ORDER BY created_at`에서 `Using filesort`가 발생해 오히려 느려지는 것을 확인했습니다. 정렬 컬럼까지 포함한 복합 인덱스(`status, category_id, created_at` / `status, created_at`)로 재설계해 `Backward index scan`으로 정렬을 처리하도록 바꿔 filesort를 제거했습니다. 자세한 분석 과정은 [index-performance.md](docs/index-performance.md)에 정리되어 있습니다.

### 주문/결제 (PortOne 연동)
상품을 바로 주문하거나 장바구니에 담긴 상품을 한 번에 주문하고, PortOne을 통해 결제할 수 있습니다.

결제 승인 검증(`/api/payments/confirm`)과 PortOne 웹훅(`/api/payments/webhook`)은 도착 순서가 보장되지 않고 중복으로 들어올 수 있어 동일한 검증 기준으로 멱등하게 처리됩니다. 서버가 계산한 결제 금액과 PortOne 승인 금액이 다르면 주문을 취소하고 재고·쿠폰을 복구한 뒤 PortOne 결제 취소를 요청하며, 취소 처리 중 상태(`CANCEL_REQUESTED`)를 별도로 두어 같은 요청이 취소를 중복 호출하지 않도록 했습니다. 결제 시작과 주문 취소처럼 같은 주문을 동시에 건드릴 수 있는 요청은 비관적 락으로 먼저 획득한 요청만 처리되도록 했습니다.

### 실시간 1:1 문의 채팅
고객이 판매자(관리자)와 1:1로 실시간 채팅 상담을 할 수 있습니다.

SockJS/STOMP(`/ws` 연결, `/pub` 발행, `/sub` 구독)로 실시간 메시지를 주고받고, STOMP `CONNECT` 프레임에서 JWT를 검증합니다. 문의 상태는 `WAITING → IN_PROGRESS → COMPLETED` 단방향으로만 전이되며, 담당자가 없는 대기방에 관리자가 입장하거나 메시지를 보내면 그 관리자가 자동으로 담당자로 배정됩니다. 재연결 시 유실 메시지를 복구할 수 있도록 `messageId` 기준 커서 페이징 API(`/before`, `/after`)를 제공하고, 마지막 활동 후 5분간 입력이 없으면 자동 퇴장 처리합니다. `redis-chat` 프로필을 켜면 Redis Pub/Sub으로 여러 서버 인스턴스에 걸쳐 메시지를 브로드캐스팅해 다중 서버 환경에서도 실시간 채팅이 동작하도록 확장했습니다.

### AI 챗봇 상담
상품에 대한 질문을 하면 Claude 기반 챗봇이 실제 상품 DB 조회 결과를 바탕으로 답변합니다.

사용자 질문에서 검색 키워드 후보를 뽑아 상품 DB를 먼저 조회하고, 그 결과(상품명·설명·가격·재고·카테고리)를 질문과 함께 Claude API로 전달해 근거 있는 답변을 생성합니다. 세션별 대화 기록은 메모리에 최대 30분 보관하며, 같은 IP·세션 기준 분당 최대 10회로 요청을 제한해 과도한 외부 API 호출을 막습니다.

## ERD

전체 ERD(Mermaid 다이어그램)는 [docs/ERD.md](docs/ERD.md)에서 확인할 수 있습니다.

### 핵심 엔티티

- **User**: 회원 정보를 담당하며, Cart와 1:1, Order/UserCoupon/ChatMember와 1:N 관계로 연결된다.
- **Product**: 상품 정보를 담당하며, Category에 N:1로 속하고 CartItem/OrderItem에서 참조된다.
- **Order / OrderItem**: 주문 시점의 상품명·단가를 스냅샷으로 저장해 이후 상품 가격이 바뀌어도 과거 주문 금액은 보존된다. Order는 Payment와 1:1로 연결된다.
- **Payment / WebhookEvent**: 결제 승인 결과를 저장하며, PortOne 웹훅 이벤트를 WebhookEvent에 원문 그대로 남겨 중복 수신을 판별한다.
- **CouponEvent / UserCoupon**: CouponEvent는 발급 수량을 관리하는 쿠폰 이벤트, UserCoupon은 회원별 발급 내역이다. `(user_id, coupon_event_id)` UNIQUE로 중복 발급을 막는다.
- **ChatRoom / ChatMessage / ChatMember**: ChatRoom은 고객-관리자 1:1 문의방이며 ChatMessage(대화 내용), ChatMember(참여자 스냅샷)와 1:N 관계로 연결된다.

## API 명세

전체 API 문서는 도메인별로 정리되어 있으며 [docs/api](docs/api/README.md)에서 확인할 수 있습니다. (별도 Swagger/Postman 문서는 아직 구성되어 있지 않습니다.)

### 주요 엔드포인트

| Method | URL | 설명 |
|---|---|---|
| POST | `/api/auth/signup` | 회원가입 |
| POST | `/api/auth/login` | 로그인(JWT 발급) |
| GET | `/api/v1/products` | 상품 목록/검색 조회 |
| GET | `/api/v2/products` | Local Cache 적용 상품 검색 |
| POST | `/api/carts/items` | 장바구니 상품 추가 |
| POST | `/api/orders/direct` | 상품 바로 주문 생성 |
| POST | `/api/coupon-events/{couponEventId}/issue` | 선착순 쿠폰 발급 |
| POST | `/api/payments/confirm` | 결제 승인 검증 |
| POST | `/api/payments/webhook` | PortOne 웹훅 수신 |
| POST | `/api/chat/rooms/me` | 1:1 문의 채팅방 생성 |
| POST | `/api/chatbot` | AI 챗봇 질문 |

## 프로젝트 구조

```
src
└── main
    └── java
        └── com/example/team3plusspring
            ├── domain
            │   ├── auth       # 회원가입, 로그인/로그아웃
            │   ├── user       # 내 정보 조회, 회원 탈퇴
            │   ├── category   # 카테고리 조회
            │   ├── product    # 상품 조회, 검색(v1/v2), 인기 상품
            │   ├── cart       # 장바구니
            │   ├── order      # 주문 생성/조회/취소
            │   ├── coupon     # 쿠폰 이벤트, 선착순 발급
            │   ├── payment    # 결제 시작/승인/중단/웹훅
            │   ├── portone    # PortOne 결제 클라이언트 연동
            │   └── chat       # 1:1 채팅, AI 챗봇, Redis Pub/Sub
            └── global
                ├── config     # Security, Redis, QueryDSL, WebSocket, Cache 설정
                ├── entity     # 공통 Base 엔티티
                ├── exception  # ErrorCode, BusinessException, GlobalExceptionHandler
                ├── lock       # Redis 분산락(@RedisLock)
                ├── response   # 공통 응답 wrapper
                └── security   # JWT 인증/인가
```

## 시작하기

### 요구사항
- Java 17
- MySQL 8.x
- Docker / Docker Compose (Redis 실행용)

### 설치 및 실행

```bash
git clone https://github.com/nongbu24/team3-plus-spring.git
cd team3-plus-spring

# Redis 실행 (docker-compose.yml에는 Redis만 정의되어 있어 MySQL은 별도로 준비해야 합니다)
docker-compose up -d

cp .env.example .env
# .env 파일에 DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET,
# ANTHROPIC_API_KEY, PORTONE_API_SECRET 등 필요한 값 채우기

./gradlew bootRun --args='--spring.profiles.active=local'
```

애플리케이션은 기본적으로 `http://localhost:8080`에서 확인할 수 있습니다.

테스트 실행:
```bash
./gradlew test
```

## 컨벤션

### 커밋 메시지
`type: subject` 형식을 따릅니다.

| Type | 설명 |
|---|---|
| feat | 새 기능 추가 |
| fix | 버그 수정 |
| refactor | 리팩토링 |
| docs | 문서 수정 |
| test | 테스트 코드 |
| chore | 빌드/설정 변경 |

### 브랜치 전략
Git Flow를 단순화한 전략을 따르며, 브랜치는 다음 규칙으로 생성합니다.

- `feature/기능명`: 기능 개발
- `fix/버그명`: 버그 수정
- `refactor/기능명`: 리팩토링
- `docs/문서명`: 문서 작업

`main` → `dev` → `feature/*`(`fix/*`, `refactor/*`, `docs/*`) 순서로 브랜치를 나누고, PR을 통해 `dev`에 병합합니다.

### PR & 코드 리뷰
- PR 작성 시 [PR 템플릿](.github/pull_request_template.md)의 작업 내용 / 변경 이유 / 주요 변경 사항 / 테스트 및 확인 / 리뷰 포인트 / AI 활용 기록 항목을 채웁니다.
- 별도로 강제된 branch protection 규칙은 없지만, 실제로는 팀원 2명 이상의 승인을 받은 뒤 병합합니다.
- 머지 방식: Merge commit (`Merge pull request #N from ...` 형태로 병합 커밋을 남기는 방식, Squash/Rebase 아님)

### 코드 컨벤션

패키지는 도메인 중심 구조를 따르며, `global`은 특정 도메인에 속하지 않는 공통 설정만 둡니다. 도메인 내부는 `controller / service / repository / entity / dto / (facade)`로 구성합니다.

| 대상 | 규칙 | 예시 |
|---|---|---|
| Controller | `{Domain}Controller` | `PaymentController` |
| Service | `{Domain}Service` | `PaymentService` |
| Repository | `{Entity}Repository` | `PaymentRepository` |
| Enum | `{Domain}Status`/`{Domain}Type` | `PaymentStatus` |
| Request/Response DTO | `{Action}{Domain}Request/Response` | `ConfirmPaymentRequest` |
| Exception | `{Domain}Exception` 또는 `BusinessException` | `PaymentException` |

- Controller는 Entity를 직접 받거나 반환하지 않고 항상 Request/Response DTO를 경유합니다.
- Entity는 DB 매핑과 상태 변경 책임만 가집니다. Setter는 금지하고 의미 있는 상태 변경 메서드를 사용합니다(`payment.markAsPaid()` O, `payment.setStatus(...)` X).
- Service가 유스케이스를 표현하고 트랜잭션 경계를 가집니다(`@Transactional(readOnly = true)` 조회 / `@Transactional` 변경).
- 여러 도메인이 얽히는 흐름(주문 생성, 결제 확정 등)은 Facade에서 orchestration하고, 단일 도메인 로직은 Facade 없이 처리합니다.
- 외부 API 연동은 별도 Client 클래스로 분리합니다(`PaymentService` ↔ `PortOneClient`).
- 동적 쿼리와 락 쿼리는 QueryDSL로 통일합니다.
- 예외는 `ErrorCode` + `BusinessException` + `GlobalExceptionHandler` 공통 구조를 따르고, `RuntimeException`을 직접 던지지 않습니다.
- 로그에는 JWT, password, billingKey, secret key, access/refresh token, 카드 전체번호, 개인정보 전체값을 남기지 않습니다.
