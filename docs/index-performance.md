# 상품 조회 인덱스 성능 분석

## 개요

상품 테이블에 100만 건의 데이터를 삽입한 후, **실제 ProductRepository.findByCondition()이
생성하는 SQL**을 기준으로 인덱스 적용 전후 성능을 분석했습니다.

- 테스트 데이터: 1,000,000건

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
   (가격 필터링 기능 자체가 구현되어 있지 않음)
2. ORDER BY created_at DESC 정렬이 분석에서 누락되어 있었음

이 차이 때문에 분석 결과가 실제 API 성능을 정확히 대변하지 못한다는
리뷰를 받았고, 아래부터는 실제 SQL 기준으로 재분석한 내용입니다.

---

## 실제 SQL 기준 분석

### 인덱스 없을 때

```sql
EXPLAIN
SELECT p1_0.id, p1_0.category_id, p1_0.created_at, p1_0.description,
       p1_0.name, p1_0.price, p1_0.status, p1_0.stock, p1_0.updated_at
FROM products p1_0
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
| Extra | Using where; **Using filesort** |
| 실행 시간 | 447.8ms |

---

### 시도 1. idx_product_best (status, category_id, price)

3개 조건(category_id, status 위주)으로 설계했던 기존 인덱스를 그대로 적용했습니다.

```sql
CREATE INDEX idx_product_best ON products(status, category_id, price);
```

| 항목 | 결과 |
|---|---|
| type | ref |
| key | idx_product_best |
| rows | 31,748 |
| Extra | Using index condition; Using where; **Using filesort** |
| 실행 시간 | **553.2ms (인덱스 없을 때보다 더 느려짐)** |

**분석:** rows는 953,029 → 31,748로 크게 줄었지만, 인덱스에 created_at이
없어서 정렬(filesort)을 별도로 수행해야 했습니다. 그 결과 실행 시간이
오히려 더 느려졌습니다. rows가 줄어든 것과 실제 실행 시간이 빨라지는 것은
별개라는 것을 확인했습니다.

---

### 시도 2. idx_product_real (status, category_id, created_at)

정렬에 사용되는 created_at을 인덱스에 포함시켜 재시도했습니다.

```sql
CREATE INDEX idx_product_real ON products(status, category_id, created_at);
```

| 항목 | 결과 |
|---|---|
| type | ref |
| key | idx_product_real |
| rows | 33,274 |
| Extra | Using where; **Backward index scan** |
| 실행 시간 | **214.1ms** |

**분석:** `Using filesort`가 `Backward index scan`으로 바뀌었습니다.
created_at이 인덱스에 포함되어 있어 별도 정렬 없이 인덱스를 역순으로
읽는 것만으로 ORDER BY가 해결되었습니다. 인덱스 없을 때(447.8ms) 대비
약 2.1배 빨라졌습니다.

---

## 최종 비교

| 케이스 | type | rows | Extra | 실행 시간 |
|---|---|---|---|---|
| 인덱스 없음 | ALL | 953,029 | Using where; Using filesort | 447.8ms |
| idx_product_best (status, category_id, price) | ref | 31,748 | Using where; Using filesort | 553.2ms (역효과) |
| idx_product_real (status, category_id, created_at) | ref | 33,274 | Using where; Backward index scan | 214.1ms |

---

## 결론

**최종 적용 인덱스: `(status, category_id, created_at)`**

```sql
CREATE INDEX idx_product_real ON products(status, category_id, created_at);
```

1. price는 실제 API에 없는 조건이라 인덱스에 포함시켜도 의미가 없었고,
   오히려 정렬 컬럼(created_at)을 인덱스에서 빠뜨려 filesort가 발생해
   인덱스 적용 전보다 더 느려지는 역효과가 있었습니다.
2. 인덱스 설계는 WHERE 조건뿐 아니라 ORDER BY에 쓰이는 컬럼까지
   포함해서 고려해야 한다는 것을 직접 확인했습니다.
3. rows가 줄어드는 것(EXPLAIN 상 좋아 보이는 지표)과 실제 실행 시간이
   빨라지는 것은 다를 수 있다는 것을 배웠습니다.
4. 직접 작성한 단순 쿼리가 아니라, show-sql로 확인한 실제 SQL을
   기준으로 분석해야 신뢰할 수 있는 결과가 나온다는 것을 배웠습니다.

idx_product_category_id도 EXPLAIN으로 단독 사용 여부를 확인했습니다.
category_id만 있는 단순 쿼리로 테스트했을 때는 이 인덱스가 단독으로
사용되는 것을 확인했지만, ProductService 코드를 다시 보니 status가
null이면 항상 ON_SALE로 채워진 뒤 Repository로 넘어가는 구조였습니다.
즉 실제 API에서는 status 조건이 항상 포함되어 idx_product_real로
충분히 처리되고, category_id가 단독으로 쓰이는 경우는 없었습니다.
따라서 idx_product_category_id도 불필요한 인덱스로 판단해 제거했습니다.
idx_product_status, idx_category_name도 각각 idx_product_real과
중복되거나 실제 코드에서 사용되지 않아 제거했습니다.