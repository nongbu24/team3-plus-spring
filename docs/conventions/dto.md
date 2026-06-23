# DTO Convention

Controller는 Entity를 직접 받거나 반환하지 않는다.

금지:

```java
@PostMapping("/orders")
public Order createOrder(@RequestBody Order order)
```

권장:

```java
@PostMapping("/orders")
public CreateOrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request)
```

Request/Response DTO는 역할을 분리한다.

```
CreateOrderRequest
CreateOrderResponse
ConfirmPaymentRequest
ConfirmPaymentResponse
RefundPaymentRequest
RefundPaymentResponse
```

DTO는 `class`를 사용한다.

```java
public class ConfirmPaymentRequest {

    @NotNull
    private Long paymentId;

    @NotBlank
    private String portOnePaymentId;
}
```
