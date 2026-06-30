# Validation Convention

Request DTO에 기본 검증을 적용한다.

예시:

```java
public class CreateOrderRequest {

    @NotEmpty
    private List<OrderItemRequest> items;
}
```

검증 대상:

- 빈 상품 목록
- 0 이하 수량
- 음수 금액
- 빈 portOnePaymentId
- 잘못된 Enum 값
- 필수 값 누락
