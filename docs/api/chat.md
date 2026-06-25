# 채팅 API

고객의 1:1 문의 채팅방 생성, 채팅방 목록 조회, 문의 상태 변경, 채팅 메시지 조회, 실시간 채팅 송수신을 담당합니다.
채팅은 REST API와 STOMP WebSocket을 함께 사용합니다.

성공/실패 응답은 REST API에서 모두 [공통 응답 wrapper](./common.md#공통-응답)를 사용합니다.
아래 `Response Body` 예시는 공통 응답 wrapper 전체를 보여줍니다.

## 엔드포인트

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| `POST` | `/api/chat/rooms/me` | 내 1:1 문의 채팅방 생성 | 필요 |
| `GET` | `/api/chat/rooms` | 채팅방 목록 조회 | 필요 |
| `PATCH` | `/api/chat/rooms/{roomId}/status` | 문의 상태 변경 | 필요 (관리자) |
| `GET` | `/api/chat/rooms/{roomId}/messages` | 채팅방 최근 메시지 조회 | 필요 |
| `GET` | `/api/chat/rooms/{roomId}/messages/before/{lastMessageId}` | 특정 메시지 이전 메시지 조회 | 필요 |
| `GET` | `/api/chat/rooms/{roomId}/messages/after/{lastReceivedMessageId}` | 재연결 후 미수신 메시지 조회 | 필요 |
| `GET` | `/api/chat/messages` | 전체 최근 메시지 조회 | 필요 (관리자) |

## WebSocket/STOMP 엔드포인트

| 구분 | Destination | 설명 | 인증 |
| --- | --- | --- | --- |
| 연결 | `/ws` | SockJS STOMP 연결 엔드포인트 | 필요 |
| Subscribe | `/sub/chat/{roomId}` | 채팅방 메시지 구독 | 필요 |
| Publish | `/pub/chat.enter` | 채팅방 입장 이벤트 발행 | 필요 |
| Publish | `/pub/chat.send` | 채팅 메시지 발행 | 필요 |
| Publish | `/pub/chat.leave` | 채팅방 퇴장 이벤트 발행 | 필요 |

## POST `/api/chat/rooms/me`

인증된 고객 본인의 1:1 문의 채팅방을 생성합니다.

- 인증: 필요
- HTTP Status: `200 OK`

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "roomId": 1,
    "name": "홍길동님의 1:1 문의",
    "customerId": 10,
    "customerName": "홍길동",
    "adminId": null,
    "adminName": null,
    "status": "WAITING",
    "createdAt": "2026-06-25T10:30:00"
  }
}
```

### 채팅방 응답 필드

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `roomId` | `Long` | 채팅방 ID |
| `name` | `String` | 채팅방 이름 |
| `customerId` | `Long` | 문의 고객 ID |
| `customerName` | `String` | 문의 고객 이름 |
| `adminId` | `Long` | 담당 관리자 ID. 아직 배정되지 않으면 `null` |
| `adminName` | `String` | 담당 관리자 이름. 아직 배정되지 않으면 `null` |
| `status` | `String` | 문의 상태. `WAITING`, `IN_PROGRESS`, `COMPLETED` |
| `createdAt` | `String` | 채팅방 생성 일시 |

### 처리 규칙

- 토큰의 회원 ID를 기준으로 채팅방을 생성합니다.
- 일반 고객 권한만 채팅방을 생성할 수 있습니다.
- 채팅방 이름은 `{회원 이름}님의 1:1 문의` 형식으로 생성합니다.
- 최초 상태는 `WAITING`입니다.
- 담당 관리자는 처음에는 배정되지 않습니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `FORBIDDEN` | 403 | 일반 고객이 아닌 사용자가 채팅방 생성 시도 |

## GET `/api/chat/rooms`

채팅방 목록을 조회합니다.

- 인증: 필요
- HTTP Status: `200 OK`

### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `status` | `String` | N | 없음 | 관리자 조회 시 문의 상태 필터. `WAITING`, `IN_PROGRESS`, `COMPLETED` |

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": [
    {
      "roomId": 1,
      "name": "홍길동님의 1:1 문의",
      "customerId": 10,
      "customerName": "홍길동",
      "adminId": 1,
      "adminName": "관리자",
      "status": "IN_PROGRESS",
      "createdAt": "2026-06-25T10:30:00"
    }
  ]
}
```

### 처리 규칙

- 관리자는 전체 채팅방을 조회합니다.
- 관리자가 `status`를 전달하면 해당 상태의 채팅방만 조회합니다.
- 일반 고객은 본인이 생성한 채팅방만 조회합니다.
- 일반 고객 조회에서는 `status` 필터를 적용하지 않습니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `INVALID_ENUM_VALUE` | 400 | 잘못된 `status` 값 |

## PATCH `/api/chat/rooms/{roomId}/status`

문의 상태를 변경합니다.

- 인증: 필요 (관리자)
- HTTP Status: `200 OK`

### Path Variables

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `roomId` | `Long` | 상태를 변경할 채팅방 ID |

### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `status` | `String` | Y | 변경할 문의 상태. `IN_PROGRESS` 또는 `COMPLETED` |

```json
{
  "status": "IN_PROGRESS"
}
```

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "roomId": 1,
    "name": "홍길동님의 1:1 문의",
    "customerId": 10,
    "customerName": "홍길동",
    "adminId": 1,
    "adminName": "관리자",
    "status": "IN_PROGRESS",
    "createdAt": "2026-06-25T10:30:00"
  }
}
```

### 처리 규칙

- 관리자만 문의 상태를 변경할 수 있습니다.
- 상태는 `WAITING -> IN_PROGRESS -> COMPLETED` 순서로만 변경할 수 있습니다.
- `COMPLETED` 상태에서는 더 이상 다른 상태로 변경할 수 없습니다.
- `WAITING -> IN_PROGRESS` 상태 변경 시 해당 관리자가 담당자로 배정됩니다.
- 메시지 조회만으로는 담당 관리자가 배정되지 않습니다.
- 이미 담당 관리자가 배정된 채팅방은 해당 관리자만 접근할 수 있습니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `VALIDATION_FAILED` | 400 | 요청 본문 형식 오류 또는 필수 값 누락 |
| `INVALID_ENUM_VALUE` | 400 | 잘못된 `status` 값 |
| `FORBIDDEN` | 403 | 관리자가 아닌 사용자가 상태 변경 시도 |
| `CHAT_ROOM_NOT_FOUND` | 404 | 채팅방이 없음 |
| `CHAT_ROOM_ACCESS_DENIED` | 403 | 다른 관리자가 담당 중인 채팅방에 접근 |
| `INVALID_CHAT_STATUS_TRANSITION` | 409 | 허용되지 않는 상태 변경 |

## GET `/api/chat/rooms/{roomId}/messages`

채팅방의 최근 메시지를 조회합니다.

- 인증: 필요
- HTTP Status: `200 OK`

### Path Variables

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `roomId` | `Long` | 메시지를 조회할 채팅방 ID |

### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `size` | `Integer` | N | `50` | 조회할 메시지 개수. 1 이상 100 이하 |

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": [
    {
      "messageId": 100,
      "content": "상품 배송은 언제 시작되나요?",
      "senderId": 10,
      "senderName": "홍길동",
      "createdAt": "2026-06-25T10:35:00"
    }
  ]
}
```

### 채팅 메시지 응답 필드

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `messageId` | `Long` | 채팅 메시지 ID |
| `content` | `String` | 메시지 내용 |
| `senderId` | `Long` | 보낸 회원 ID |
| `senderName` | `String` | 보낸 회원 이름 |
| `createdAt` | `String` | 메시지 생성 일시 |

### 처리 규칙

- 채팅방 고객 또는 담당 관리자만 메시지를 조회할 수 있습니다.
- 담당 관리자가 없는 채팅방은 관리자도 조회할 수 있지만, 조회만으로 담당자가 배정되지는 않습니다.
- 메시지는 최신 메시지부터 `messageId` 내림차순으로 반환합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `VALIDATION_FAILED` | 400 | `size`가 1 미만 또는 100 초과 |
| `CHAT_ROOM_NOT_FOUND` | 404 | 채팅방이 없음 |
| `CHAT_ROOM_ACCESS_DENIED` | 403 | 접근 권한이 없는 채팅방 |

## GET `/api/chat/rooms/{roomId}/messages/before/{lastMessageId}`

특정 메시지보다 이전 메시지를 조회합니다.

- 인증: 필요
- HTTP Status: `200 OK`

### Path Variables

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `roomId` | `Long` | 메시지를 조회할 채팅방 ID |
| `lastMessageId` | `Long` | 기준 메시지 ID. 이 값보다 작은 메시지 ID를 조회 |

### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `size` | `Integer` | N | `50` | 조회할 메시지 개수. 1 이상 100 이하 |

### Response Body

`/api/chat/rooms/{roomId}/messages` 응답과 동일합니다.

### 처리 규칙

- 채팅방 고객 또는 담당 관리자만 메시지를 조회할 수 있습니다.
- `lastMessageId`보다 작은 메시지 ID만 조회합니다.
- 메시지는 최신 메시지부터 `messageId` 내림차순으로 반환합니다.
- 이전 메시지가 없으면 빈 배열을 반환합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `VALIDATION_FAILED` | 400 | `size`가 1 미만 또는 100 초과 |
| `CHAT_ROOM_NOT_FOUND` | 404 | 채팅방이 없음 |
| `CHAT_ROOM_ACCESS_DENIED` | 403 | 접근 권한이 없는 채팅방 |

## GET `/api/chat/rooms/{roomId}/messages/after/{lastReceivedMessageId}`

STOMP 재연결 후 클라이언트가 받지 못한 메시지를 복구합니다.

- 인증: 필요
- HTTP Status: `200 OK`

### Path Variables

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `roomId` | `Long` | 메시지를 조회할 채팅방 ID |
| `lastReceivedMessageId` | `Long` | 클라이언트가 마지막으로 수신한 메시지 ID. 이 값보다 큰 메시지를 조회 |

### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `size` | `Integer` | N | `100` | 조회할 메시지 개수. 1 이상 500 이하 |

### Response Body

`/api/chat/rooms/{roomId}/messages` 응답과 동일합니다.

### 처리 규칙

- 채팅방 고객 또는 담당 관리자만 메시지를 조회할 수 있습니다.
- `lastReceivedMessageId`보다 큰 메시지 ID만 조회합니다.
- 클라이언트가 화면에 순서대로 붙일 수 있도록 메시지는 `messageId` 오름차순으로 반환합니다.
- 미수신 메시지가 없으면 빈 배열을 반환합니다.

### 재연결 복구 흐름

- 클라이언트는 마지막으로 받은 `messageId`를 저장합니다.
- 네트워크가 끊기면 STOMP를 자동 재연결합니다.
- 재연결에 성공하면 이 API로 `lastReceivedMessageId` 이후 메시지를 요청합니다.
- 서버는 `messageId > lastReceivedMessageId`인 메시지를 오래된 순서부터 반환합니다.
- 클라이언트는 반환된 메시지를 화면에 붙이고 다시 실시간 구독을 이어갑니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `VALIDATION_FAILED` | 400 | `size`가 1 미만 또는 500 초과 |
| `CHAT_ROOM_NOT_FOUND` | 404 | 채팅방이 없음 |
| `CHAT_ROOM_ACCESS_DENIED` | 403 | 접근 권한이 없는 채팅방 |

## GET `/api/chat/messages`

전체 채팅방의 최근 메시지를 조회합니다.

- 인증: 필요 (관리자)
- HTTP Status: `200 OK`

### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `size` | `Integer` | N | `50` | 조회할 메시지 개수. 1 이상 100 이하 |

### Response Body

`/api/chat/rooms/{roomId}/messages` 응답과 동일합니다.

### 처리 규칙

- 관리자만 전체 최근 메시지를 조회할 수 있습니다.
- 메시지는 최신 메시지부터 `messageId` 내림차순으로 반환합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `FORBIDDEN` | 403 | 관리자가 아닌 사용자가 조회 시도 |
| `VALIDATION_FAILED` | 400 | `size`가 1 미만 또는 100 초과 |

## STOMP 연결

SockJS STOMP 클라이언트는 `/ws`로 연결합니다.
연결 시 native header에 JWT를 전달합니다.

```http
Authorization: Bearer {accessToken}
```

### 처리 규칙

- STOMP `CONNECT` 단계에서 JWT를 검증합니다.
- 토큰이 없거나 올바르지 않으면 연결 인증에 실패합니다.
- `/sub/chat/{roomId}` 구독 시 채팅방 접근 권한을 검증합니다.
- 담당자가 없는 채팅방을 관리자가 구독해도 담당자로 배정되지는 않습니다.
- 구독 대상 `roomId`가 숫자가 아니거나 존재하지 않으면 `CHAT_ROOM_NOT_FOUND`가 발생합니다.

### Errors

| 코드 | 발생 조건 |
| --- | --- |
| `UNAUTHORIZED` | STOMP 연결 토큰 누락 또는 인증 실패 |
| `CHAT_ROOM_NOT_FOUND` | 구독 대상 채팅방이 없음 |
| `CHAT_ROOM_ACCESS_DENIED` | 접근 권한이 없는 채팅방 구독 시도 |
| `CHAT_ROOM_ALREADY_COMPLETED` | 이미 완료된 채팅방에 메시지 발행 시도 |

## Subscribe `/sub/chat/{roomId}`

채팅방 메시지를 실시간으로 구독합니다.

### Path Variables

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `roomId` | `Long` | 구독할 채팅방 ID |

### Message Body

서버가 구독자에게 전달하는 메시지는 `ChatMessageResponse` 형식입니다.

```json
{
  "messageId": 100,
  "content": "상품 배송은 언제 시작되나요?",
  "senderId": 10,
  "senderName": "홍길동",
  "createdAt": "2026-06-25T10:35:00"
}
```

## Publish `/pub/chat.enter`

채팅방 입장 이벤트를 발행합니다.

### Message Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `roomId` | `Long` | Y | 입장할 채팅방 ID |

```json
{
  "roomId": 1
}
```

### 처리 규칙

- 채팅방 접근 권한을 검증합니다.
- 이미 완료된 채팅방에는 입장 메시지를 저장하지 않습니다.
- 입장 메시지는 `{회원 이름}님이 입장했습니다` 형식으로 저장됩니다.
- 저장된 입장 메시지는 `/sub/chat/{roomId}` 구독자에게 발행됩니다.

## Publish `/pub/chat.send`

채팅 메시지를 발행합니다.

### Message Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `roomId` | `Long` | Y | 메시지를 보낼 채팅방 ID |
| `content` | `String` | Y | 메시지 내용. 1000자 이하 |

```json
{
  "roomId": 1,
  "content": "상품 배송은 언제 시작되나요?"
}
```

### 처리 규칙

- 채팅방 접근 권한을 검증합니다.
- 이미 완료된 채팅방에는 메시지를 저장하지 않습니다.
- 담당자가 없는 대기 상태 채팅방에 관리자가 메시지를 보내면, 해당 관리자를 담당자로 배정하고 상태를 `IN_PROGRESS`로 변경합니다.
- 메시지 내용은 비어 있을 수 없고 1000자 이하여야 합니다.
- 저장된 메시지는 `/sub/chat/{roomId}` 구독자에게 발행됩니다.

## Publish `/pub/chat.leave`

채팅방 퇴장 이벤트를 발행합니다.

### Message Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `roomId` | `Long` | Y | 퇴장할 채팅방 ID |

```json
{
  "roomId": 1
}
```

### 처리 규칙

- 채팅방 접근 권한을 검증합니다.
- 이미 완료된 채팅방에는 퇴장 메시지를 저장하지 않습니다.
- 퇴장 메시지는 `{회원 이름}님이 퇴장했습니다` 형식으로 저장됩니다.
- 저장된 퇴장 메시지는 `/sub/chat/{roomId}` 구독자에게 발행됩니다.
- WebSocket 연결이 끊기면 해당 세션이 입장한 모든 채팅방에 퇴장 메시지를 발행합니다.

## 설계 메모

- 채팅 REST API는 `/api/chat` 하위에서 채팅방과 메시지 조회를 담당합니다.
- 실시간 채팅은 SockJS STOMP를 사용하며, 클라이언트 발행 prefix는 `/pub`, 서버 구독 prefix는 `/sub`입니다.
- 기본 프로필에서는 서버 내부 WebSocket broker로 메시지를 전달하고, `redis-chat` 프로필 사용 시 Redis Pub/Sub으로 발행한 뒤 `/sub/chat/{roomId}` 구독자에게 전달합니다.
- 관리자는 `WAITING -> IN_PROGRESS` 상태 변경 시 담당자로 배정됩니다.
- 담당자가 없는 채팅방을 관리자가 STOMP 구독해도 담당자로 배정되지는 않습니다.
- 담당자가 없는 대기 상태 채팅방에 관리자가 메시지를 보내도 해당 관리자가 담당자로 배정되고 상태가 `IN_PROGRESS`로 변경됩니다.
- 담당 관리자가 이미 배정된 채팅방은 다른 관리자가 접근할 수 없습니다.
- 문의 상태는 `WAITING -> IN_PROGRESS -> COMPLETED` 단방향 흐름으로 관리합니다.
