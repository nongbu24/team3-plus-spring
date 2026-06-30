# 상품 조회 인덱스 성능 분석

## 개요

상품 테이블에 100만 건의 데이터를 삽입한 후, 인덱스 적용 전후의 성능을 EXPLAIN과 실행 시간으로 비교 분석했습니다.

- 테스트 데이터: 1,000,000건
- 분석 도구: MySQL EXPLAIN, SET profiling = 1
- 테스트 쿼리 조건: 가격 범위(BETWEEN), 카테고리(=), 판매 상태(=), 상품명 검색(LIKE) — 총 4개 조건

---

## 테스트 쿼리

```sql
SELECT p.id, p.name, p.price, p.status, c.name as category_name
FROM products p
LEFT JOIN categories c ON p.category_id = c.id
WHERE p.price BETWEEN 100000 AND 500000
AND p.category_id = 1
AND p.status = 'ON_SALE'
AND p.name LIKE '%Galaxy%';
```

실제 상품 목록 API에서 사용 가능한 카테고리, 가격대, 판매 상태, 검색어 4가지 조건을 모두 포함했으며, 인덱스 컬럼 순서 비교(A/B/C)도 이 4개 조건 그대로 진행했습니다.

---

## 카디널리티 분석

| 컬럼 | 카디널리티 | 조건 타입 |
|---|---|---|
| status | 3 | 등호(=) |
| category_id | 20 | 등호(=) |
| price | 730,694 | 범위(BETWEEN) |
| name | 거의 전체 고유값 | LIKE '%...%' (인덱스 활용 불가) |

---

## 가설

카디널리티가 높은 컬럼을 인덱스 앞에 두면 무조건 빠를 거라고 생각해서
(price, category_id, status) 순서를 생각했고

AI에게 "카디널리티만 보면 되는지, 조건 종류는 상관없는지" 물어봤고,
"등호 조건은 인덱스로 정확히 하나의 값으로 좁혀지지만, 범위 조건은
탐색 구간이 넓어 그 뒤 컬럼은 인덱스 효과를 못 받는다"는 답을 들었다

세 가지 순서(A: category_id 우선, B: status 우선, C: price 우선)를
모두 인덱스로 만들어서 EXPLAIN과 실행시간을 비교했다

결과적으로 C(price를 맨 앞에 둔 경우)만 type=ALL로 돌아가고 65.7ms로
느려진 것을 직접 확인했다

---

## 인덱스 없을 때 실행 계획

| 테이블 | type | key | rows | Extra |
|---|---|---|---|---|
| p (products) | ALL | 없음 | 976,537 | Using where |
| c (categories) | const | PRIMARY | 1 | - |

실행 시간: **226.0ms**

---

## 복합 인덱스 순서 비교

### 순서 A. (category_id, status, price) — 카디널리티 순서대로 배치

```sql
CREATE INDEX idx_test_a ON products(category_id, status, price);
```

| type | key | rows | Extra | 실행 시간 |
|---|---|---|---|---|
| range | idx_test_a | 3,267 | Using index condition; Using where | 10.3ms |

### 순서 B. (status, category_id, price) — 등호 조건 우선 배치

```sql
CREATE INDEX idx_test_b ON products(status, category_id, price);
```

| type | key | rows | Extra | 실행 시간 |
|---|---|---|---|---|
| range | idx_test_b | 3,267 | Using index condition; Using where | 8.6ms |

### 순서 C. (price, category_id, status) — 범위 조건을 맨 앞에 배치

```sql
CREATE INDEX idx_test_c ON products(price, category_id, status);
```

| type | key | rows | Extra | 실행 시간 |
|---|---|---|---|---|
| ALL | 없음 | 976,537 | Using where | 49.3ms |

**검증:** 가설대로 price(범위 조건)를 맨 앞에 두자 인덱스를 전혀 타지 못하고
type이 다시 ALL로 돌아갔다. A, B는 둘 다 range로 동일했지만 B가 더 빨랐다.

**name 조건의 영향:** A, B, C 모두 `name LIKE '%Galaxy%'`는 인덱스에 없는
컬럼이라 `Using where`가 공통으로 붙었다. 즉 name 조건은 인덱스 컬럼
순서(A/B/C)의 우열에는 영향을 주지 않고, rows를 좁힌 이후 결과를
한 번 더 거르는 별도의 비용으로 동작했다.

---

## 최종 비교

| 케이스 | type | rows | 실행 시간 |
|---|---|---|---|
| 인덱스 없음 (4조건) | ALL | 976,537 | 226.0ms |
| A (category_id, status, price) | range | 3,267 | 10.3ms |
| B (status, category_id, price) | range | 3,267 | 8.6ms |
| C (price, category_id, status) | ALL | 976,537 | 49.3ms |

---

## 결론

**최적 인덱스: `(status, category_id, price)`**

```sql
CREATE INDEX idx_product_best ON products(status, category_id, price);
```

1. 등호(=) 조건(status, category_id)을 먼저, 범위(BETWEEN) 조건(price)을 마지막에 배치
2. price를 맨 앞에 두면 인덱스가 무력화됨을 직접 검증함(C 케이스)
3. name LIKE '%keyword%' 조건은 어떤 인덱스 순서를 쓰든 인덱스로 해결되지 않으며
   (B-Tree 인덱스는 앞부분 일치만 빠르게 찾을 수 있음), 인덱스로 좁힌 결과를
   추가로 한 줄씩 검사하는 비용이 항상 발생함. 별도의 검색 전략(Full-Text Index,
   검색엔진 연동 등)이 필요함

---

## 회고

1. 범위 검색 조건은 인덱스 마지막에 배치해야 한다는 것을
   테스트로 검증함 (price를 앞에 뒀을 때 type=ALL로 바뀜)

2. 카디널리티만 보고 세운 첫 가설은 틀렸고,
   조건의 종류가 카디널리티보다 더 중요한 기준이라는 것을
   AI 도움을 받아 이해하고 가설을 수정함

3. LIKE '%keyword%' 검색은 인덱스로 해결할 수 없다는 한계를 직접 확인했고,
   이런 경우 인덱스 설계만으로는 부족하며 다른 검색 기술이 필요하다는 것을 배움

4. 인덱스 설계는 카디널리티만으로 판단하지 말고
   실제 EXPLAIN과 실행 시간으로 검증해야 한다는 것을 배움