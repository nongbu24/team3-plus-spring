# 인증 API

회원가입, 로그인/JWT 발급, 로그아웃을 담당합니다.

성공/실패 응답은 모두 [공통 응답 wrapper](./common.md#공통-응답)를 사용합니다.
아래 `Response Body` 예시는 공통 응답 wrapper 전체를 보여줍니다.

## 엔드포인트

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| `POST` | `/api/auth/signup` | 회원가입 | 불필요 |
| `POST` | `/api/auth/login` | 로그인, JWT 발급 | 불필요 |
| `POST` | `/api/auth/logout` | 로그아웃 | 필요 |

## POST `/api/auth/signup`

회원을 생성합니다. 회원가입 성공 시 기본 장바구니를 생성합니다.

- 인증: 불필요
- HTTP Status: `201 Created`

### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `email` | string | Y | 로그인 이메일. UNIQUE |
| `password` | string | Y | 비밀번호. 서버에서 암호화 저장 |
| `name` | string | Y | 회원 이름 |
| `phone` | string | Y | 전화번호 |

```json
{
  "email": "customer@example.com",
  "password": "Password123",
  "name": "홍길동",
  "phone": "010-1234-5678"
}
```

### Response Body

```json
{
  "status": 201,
  "message": "요청이 성공했습니다.",
  "data": {
    "userId": 1,
    "email": "customer@example.com",
    "name": "홍길동"
  }
}
```

### 처리 규칙

- 이메일은 중복 가입을 허용하지 않습니다.
- 탈퇴한 회원의 이메일도 재가입에 사용할 수 없습니다.
- 비밀번호는 서버에서 암호화해서 저장합니다.
- 회원가입 성공 시 기본 장바구니를 생성합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | 이메일, 비밀번호, 이름, 전화번호 형식 오류 |
| `EMAIL_ALREADY_EXISTS` | 409 | 이미 가입된 이메일 |

## POST `/api/auth/login`

이메일과 비밀번호를 검증하고 JWT access token을 발급합니다.

- 인증: 불필요
- HTTP Status: `200 OK`

### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `email` | string | Y | 로그인 이메일 |
| `password` | string | Y | 비밀번호 |

```json
{
  "email": "customer@example.com",
  "password": "Password123"
}
```

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOi...",
    "expiresIn": 3600,
    "user": {
      "userId": 1,
      "email": "customer@example.com",
      "name": "홍길동"
    }
  }
}
```

### 처리 규칙

- 이메일과 비밀번호를 검증합니다.
- 탈퇴한 회원은 로그인할 수 없습니다.
- 인증에 성공하면 JWT access token을 발급합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | 이메일 또는 비밀번호 누락 |
| `INVALID_LOGIN_CREDENTIALS` | 401 | 이메일 또는 비밀번호 불일치 |

## POST `/api/auth/logout`

로그아웃을 처리합니다. 서버에서 토큰 blocklist를 운영하는 경우 현재 access token을 만료 처리하고, 운영하지 않는 경우 클라이언트가 보관 중인 토큰을 폐기하는 방식으로 처리합니다.

- 인증: 필요
- HTTP Status: `200 OK`

### Request Header

| 헤더 | 필수 | 설명 |
| --- | --- | --- |
| `Authorization` | Y | `Bearer {accessToken}` 형식의 JWT |

### Request Body

없음

### Response Body

```json
{
  "status": 200,
  "message": "요청이 성공했습니다.",
  "data": {
    "message": "로그아웃이 완료되었습니다."
  }
}
```

### 처리 규칙

- 서버에서 토큰 blocklist를 운영하는 경우 현재 access token을 만료 처리합니다.
- 토큰 blocklist를 운영하지 않는 경우 클라이언트가 보관 중인 토큰을 폐기하는 방식으로 처리합니다.

### Errors

| 코드 | HTTP | 발생 조건 |
| --- | --- | --- |
| `UNAUTHORIZED` | 401 | 토큰 누락 |
| `INVALID_TOKEN` | 401 | 잘못된 토큰 |
| `EXPIRED_TOKEN` | 401 | 만료된 토큰 |

## 설계 메모

- 인증 API는 `/api/auth` 하위에서 회원가입, 로그인/JWT 발급, 로그아웃을 담당합니다.
- 비밀번호는 반드시 암호화해서 저장하고 응답에는 포함하지 않습니다.
- 탈퇴한 회원은 로그인할 수 없도록 `deleted_at IS NULL` 조건을 적용합니다.
- 탈퇴한 회원의 이메일은 영구적으로 재가입에 사용할 수 없습니다.
- 로그아웃 정책은 토큰 blocklist 운영 여부에 따라 서버 처리 방식이 달라질 수 있습니다.
