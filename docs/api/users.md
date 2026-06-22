# 회원 API

내 정보 조회와 회원 탈퇴를 담당합니다.

성공/실패 응답은 모두 [공통 응답 wrapper](./common.md#공통-응답)를 사용합니다.
아래 `Response Body` 예시는 공통 응답 wrapper 전체를 보여줍니다.

## 엔드포인트

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| `GET` | `/api/users/me` | 내 정보 조회 | 필요 |
| `POST` | `/api/users/delete` | 회원 탈퇴 | 필요 |

## GET `/api/users/me`

인증된 회원 본인의 정보를 조회합니다.

- 인증: 필요
- HTTP Status: `200 OK`

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "userId": 1,
    "email": "customer@example.com",
    "name": "홍길동",
    "phone": "010-1234-5678",
    "role": "USER",
    "pointBalance": 5000
  }
}
```

### 회원 응답 필드

| 필드 | 타입       | 설명 |
| --- |----------| --- |
| `userId` | `Long`   | 회원 ID |
| `email` | `String` | 로그인 이메일 |
| `name` | `String` | 회원 이름 |
| `phone` | `String` | 휴대폰 번호 |
| `role` | `String` | 권한 |
| `pointBalance` | `Long`   | 보유 포인트 |

### 처리 규칙

- 토큰의 회원 ID를 기준으로 본인 정보만 조회합니다.
- 탈퇴한 회원은 조회되지 않습니다.
- 비밀번호는 응답에 포함하지 않습니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `INVALID_TOKEN` | 401 | 탈퇴한 회원의 기존 토큰으로 요청 |
| `USER_NOT_FOUND` | 404 | 인증 사용자를 찾을 수 없음 |

## POST `/api/users/delete`

인증된 회원 본인의 계정을 탈퇴 처리합니다.

- 인증: 필요
- HTTP Status: `200 OK`

### Request Body

없음

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "message": "회원 탈퇴가 완료되었습니다."
  }
}
```

### 처리 규칙

- 토큰의 회원 ID를 기준으로 본인 계정만 탈퇴 처리합니다.
- 회원 데이터는 실제 삭제하지 않고 `deleted_at`에 탈퇴 일시를 저장합니다.
- 탈퇴 이후 로그인과 내 정보 조회에서는 해당 회원을 제외합니다.
- 탈퇴한 회원의 이메일은 영구적으로 재가입에 사용할 수 없습니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 또는 인증 실패 |
| `INVALID_TOKEN` | 401 | 탈퇴한 회원의 기존 토큰으로 요청 |
| `USER_NOT_FOUND` | 404 | 인증 사용자를 찾을 수 없음 |

## 설계 메모

- 회원 탈퇴는 실제 삭제하지 않고 `deleted_at`에 탈퇴 일시를 저장합니다.
- 로그인과 회원 조회에서는 `deleted_at IS NULL` 조건을 적용합니다.
- 탈퇴한 회원의 이메일은 재사용하지 않습니다.
- 비밀번호는 반드시 암호화해서 저장하고 응답에는 포함하지 않습니다.
