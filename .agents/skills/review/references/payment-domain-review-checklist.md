# Payment Domain Review Checklist

## Order

* 주문 생성 시 상태가 `PENDING` 또는 `결제 대기`인지
* 주문 완료는 결제 검증 이후에만 되는지
* 주문 상품에 주문 당시 상품명/가격 스냅샷이 저장되는지
* 다중 상품 주문에서 총액 계산이 정확한지
* 주문 소유권 검증이 있는지

## Payment

* 결제 시도 레코드가 먼저 생성되는지
* `portonePaymentId`가 unique인지
* 클라이언트 결제 성공값을 그대로 신뢰하지 않는지
* PortOne 결제 조회 결과를 기준으로 확정하는지
* 주문 금액과 PG 승인 금액을 비교하는지
* 결제 상태와 주문 상태가 분리되어 있는지
* 중복 confirm 요청에 안전한지

## Webhook

* PortOne 이벤트 식별자를 안정적으로 받을 수 있는 경우에만 `webhookId`를 unique하게 저장하고, 기본 멱등성은 `portonePaymentId`와 상태 기반으로 보장하는지
* Webhook payload를 최종 근거로 믿지 않는지
* Webhook 수신 후에도 PortOne 결제 조회를 수행하는지
* Client Confirm과 동일한 결제 확정 로직을 재사용하는지
* 이미 처리된 이벤트에 대해 안전하게 200 OK를 반환하는지