# Package Structure Convention

기본 구조는 **도메인 중심 패키지 구조**를 권장한다.

```
domain
 └─ {domain-name}
     ├─ controller
     ├─ service
     ├─ repository
     ├─ entity
     ├─ dto
     └─ facade
```

도메인 성격에 따라 필요한 하위 패키지만 생성한다.

예시:

```
com.example.team3plusspring
 ├─ global
 │   ├─ config
 │   ├─ entity
 │   ├─ exception
 │   ├─ response
 │   └─ security
 │       └─ jwt
 └─ domain
     ├─ auth
     ├─ cart
     ├─ coupon
     ├─ user
     └─ product
```

도메인별 내부 구조:

```
domain/{domain-name}
 ├─ controller
 ├─ service
 ├─ repository
 ├─ entity
 ├─ dto
 └─ facade
```

`global`에는 특정 도메인에 속하지 않는 공통 설정만 둔다.

예시:

- Security 설정
- 공통 예외
- 공통 응답
- Jackson 설정
- Auditing 설정
