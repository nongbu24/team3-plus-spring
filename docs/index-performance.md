# 상품 조회 인덱스 성능 분석

## 개요

상품 테이블에 100만 건의 데이터를 삽입한 후, **실제 ProductRepository.findByCondition()이
생성하는 SQL**을 기준으로 인덱스 적용 전후 성능을 분석했습니다.

- 테스트 데이터: 1,000,000건

---

## 분석 범위 한정

상품 목록 API는 sort 파라미터로 LATEST(최신순), PRICE_ASC, PRICE_DESC
(가격순)를 지원하지만, 이번 분석은 LATEST(최신순) 조회 최적화에
한정했습니다. created_at 기준으로 설계된 인덱스(idx_product_real,
idx_product_status_created)는 가격순 정렬(PRICE_ASC/DESC) 요청에는
적용되지 않으며, 가격순 정렬 시에는 별도의 정렬 비용(filesort)이
발생할 수 있습니다. 가격순 정렬 최적화는 향후 별도로 분석이
필요합니다.

---

## 측정 방식에 대한 안내

이번 PR에서 측정한 실행 시간은 동일 세션 내에서 인덱스를 만들고
바로 이어서 측정한 값으로, InnoDB 버퍼 풀 캐싱 영향을 배제하지
못했습니다. 따라서 절대적인 ms 수치보다는 EXPLAIN의 type, key,
Extra(특히 Using filesort 발생 여부) 변화를 핵심 근거로 판단했고,
실행 시간은 참고 자료로만 표기했습니다.

---

## 실제 API가 실행하는 SQL

`GET /api/v1/products?categoryId=1&keyword=Galaxy&status=ON_SALE` 호출 시
Hibernate가 생성한 실제 SQL입니다.

```sql
SELECT p1_0.id, p1_0.category_id, p1_0.created_at, p1_0.description,
       p1_0.name, p1_0.price, p1_0.status, p1_0.stock, p1_0.updated_at
FROM products p1_0
WHERE (? IS NULL OR p1_0.category_id = ?)
  AND (? IS NULL OR p1_0.name LIKE ? ESCAPE '')
  AND (? IS NULL OR p1_0.status = ?)
ORDER BY p1_0.created_at DESC
LIMIT ?
```

ProductRepository의 @Query JPQL이 모든 조건을 `(:param IS NULL OR ...)` 형태로
감싸고 있어, 파라미터 유무와 관계없이 SQL 구조가 동일하게 생성됩니다.

---

## 1차 시행착오: 단순 쿼리로 분석했던 것의 문제

리뷰 전, 다음과 같은 단순화된 쿼리로 인덱스 효과를 분석했습니다.

```sql
SELECT * FROM products WHERE category_id = 1 AND status = 'ON_SALE';
```

이 쿼리 기준으로는 `(status, category_id, price)` 인덱스가 가장 효율적이라는
결론을 냈으나, 다음 두 가지가 실제 API와 달랐습니다.

1. price 조건은 실제 ProductRepository.findByCondition()에 존재하지 않음
2. ORDER BY created_at DESC 정렬이 분석에서 누락되어 있었음

아래부터는 실제 SQL 기준으로 재분석한 내용입니다.

---

## categoryId가 있을 때 (예: categoryId=1)

### 인덱스 없을 때

```sql
EXPLAIN
SELECT * FROM products p1_0
WHERE (1 IS NULL OR p1_0.category_id = 1)
  AND ('Galaxy' IS NULL OR p1_0.name LIKE '%Galaxy%')
  AND ('ON_SALE' IS NULL OR p1_0.status = 'ON_SALE')
ORDER BY p1_0.created_at DESC
LIMIT 10;
```

| 항목 | 결과 |
|---|---|
| type | ALL |
| key | 없음 |
| rows | 953,029 |
| Extra | Using where; Using filesort |
| 실행 시간 (참고) | 447.8ms |

### 시도 1. idx_product_best (status, category_id, price) — 실패

```sql
CREATE INDEX idx_product_best ON products(status, category_id, price);
```

| 항목 | 결과 |
|---|---|
| type | ref |
| rows | 31,748 |
| Extra | Using where; Using filesort |
| 실행 시간 (참고) | 553.2ms (인덱스 없을 때보다 더 느려짐) |

**분석:** rows는 줄었지만 created_at이 인덱스에 없어 filesort가 발생해
오히려 더 느려졌습니다. EXPLAIN상 Extra에 Using filesort가 그대로
남아있다는 것이 핵심 근거이며, 실행 시간 역전도 이를 뒷받침합니다.

### 시도 2. idx_product_real (status, category_id, created_at) — 성공

```sql
CREATE INDEX idx_product_real ON products(status, category_id, created_at);
```

| 항목 | 결과 |
|---|---|
| type | ref |
| rows | 33,274 |
| Extra | Using where; Backward index scan |
| 실행 시간 (참고) | 214.1ms |

**분석:** created_at을 인덱스에 포함시키니 Extra가 Using filesort에서
Backward index scan으로 바뀌었습니다. 정렬을 위한 별도 작업 없이
인덱스를 역순으로 읽는 것만으로 ORDER BY가 처리된다는 뜻이며, 이것이
핵심 개선입니다.

---

## categoryId가 없을 때 (예: 기본 목록 조회 sort=LATEST)

리뷰에서 categoryId 없이 호출되는 케이스(실제 트래픽에서 더 흔할 수 있는
패턴)도 확인해야 한다는 지적을 받아 추가로 검증했습니다.

### idx_product_real 만으로 처리했을 때

```sql
EXPLAIN
SELECT * FROM products
WHERE status = 'ON_SALE'
ORDER BY created_at DESC
LIMIT 10;
```

| 항목 | 결과 |
|---|---|
| type | ref |
| key | idx_product_real |
| rows | 497,345 |
| Extra | Using index condition; **Using filesort** |
| 실행 시간 (참고) | 2.44초 |

**분석:** category_id 조건이 빠지면서 인덱스 가운데 컬럼이 비어,
세 번째 컬럼인 created_at의 정렬 이점을 전혀 활용하지 못했습니다.
Extra에 Using filesort가 다시 나타난 것이 핵심 근거입니다.

### idx_product_status_created (status, created_at) 추가 후

```sql
CREATE INDEX idx_product_status_created ON products(status, created_at);
```

| 항목 | 결과 |
|---|---|
| type | range |
| key | idx_product_status_created |
| rows | 497,345 |
| Extra | Using index condition; **Backward index scan** |

**분석:** category_id 없이 status + created_at만으로 구성된 인덱스를
추가하니 Extra의 Using filesort가 사라지고 Backward index scan으로
바뀌었습니다. 실행 시간은 반복 측정 시 0.415ms ~ 2.0ms로 변동이 커서
(InnoDB 버퍼 풀 캐싱 영향으로 판단), 구체적인 배수는 표기하지
않았습니다. 핵심 근거는 filesort 제거라는 EXPLAIN상의 구조적 변화입니다.

### 두 인덱스 동시 존재 시 충돌 여부 확인

```sql
EXPLAIN
SELECT * FROM products
WHERE status = 'ON_SALE' AND category_id = 1
ORDER BY created_at DESC
LIMIT 10;
```

| 항목 | 결과 |
|---|---|
| type | ref |
| key | idx_product_real |
| rows | 28,868 |
| Extra | Using where; Backward index scan |
| 실행 시간 (참고) | 2.72ms |

**분석:** categoryId가 있을 때는 idx_product_real을, 없을 때는
idx_product_status_created를 옵티마이저가 상황에 맞게 자동으로
선택하는 것을 확인했습니다. 두 인덱스가 서로 다른 쿼리 패턴을
커버하므로 함께 유지하기로 했습니다.

---

## 최종 적용 인덱스

```sql
CREATE INDEX idx_product_real ON products(status, category_id, created_at);
CREATE INDEX idx_product_status_created ON products(status, created_at);
```

---

## 결론

1. price는 실제 API에 없는 조건이라 인덱스에 포함시켜도 의미가 없었고,
   오히려 정렬 컬럼(created_at)을 인덱스에서 빠뜨려 filesort가 발생해
   인덱스 적용 전보다 더 느려지는 역효과가 있었습니다.
2. 인덱스 설계는 WHERE 조건뿐 아니라 ORDER BY에 쓰이는 컬럼까지
   포함해서 고려해야 한다는 것을 직접 확인했습니다.
3. 복합 인덱스는 중간 컬럼이 조건에 없으면 그 뒤 컬럼(정렬용 컬럼 포함)의
   이점을 활용하지 못한다는 것을 categoryId 유무 비교로 확인했습니다.
4. rows가 줄어드는 것과 실제 실행 시간이 빨라지는 것은 다를 수 있다는
   것을 배웠습니다.
5. 직접 작성한 단순 쿼리가 아니라, show-sql로 확인한 실제 SQL을
   기준으로 분석해야 신뢰할 수 있는 결과가 나온다는 것을 배웠습니다.
6. 동일 세션 반복 측정은 캐싱 영향을 받을 수 있어, 실행 시간은
   절대값이 아닌 참고 지표로 다루고 EXPLAIN의 구조적 변화(type, Extra)를
   핵심 판단 근거로 삼아야 한다는 것을 배웠습니다.

idx_product_category_id도 EXPLAIN으로 단독 사용 여부를 확인했습니다.
ProductService 코드를 보면 status가 null이면 항상 ON_SALE로 채워진 뒤
Repository로 넘어가는 구조라, 실제 API에서는 status 조건이 항상
포함되어 category_id가 단독으로 쓰이는 경우는 없었습니다. 따라서
idx_product_category_id는 불필요한 인덱스로 판단해 제거했습니다.
idx_product_status, idx_category_name도 각각 다른 인덱스와 중복되거나
실제 코드에서 사용되지 않아 제거했습니다.