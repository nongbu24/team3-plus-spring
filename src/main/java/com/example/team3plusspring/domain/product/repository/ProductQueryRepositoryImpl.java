package com.example.team3plusspring.domain.product.repository;

import com.example.team3plusspring.domain.product.entity.Product;
import com.example.team3plusspring.domain.product.entity.ProductStatus;
import com.example.team3plusspring.domain.product.entity.QProduct;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ProductQueryRepositoryImpl implements ProductQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final QProduct product = QProduct.product;

    @Override
    public Page<Product> findByCondition(Long categoryId, String keyword, ProductStatus status, Pageable pageable) {

        List<Product> content = queryFactory
                .selectFrom(product)
                .where(
                        categoryIdEq(categoryId),
                        keywordLike(keyword),
                        statusEq(status)
                )
                .orderBy(getOrderSpecifier(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(product.count())
                .from(product)
                .where(
                        categoryIdEq(categoryId),
                        keywordLike(keyword),
                        statusEq(status)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    @Override
    public List<Product> findPopularProducts(List<ProductStatus> statuses, Pageable pageable) {
        return queryFactory
                .selectFrom(product)
                .where(product.status.in(statuses))
                .orderBy(product.viewCount.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public Optional<Product> findByIdForUpdate(Long productId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(product)
                        .where(product.id.eq(productId))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }

    @Override
    public List<Product> findAllByIdInForUpdate(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectFrom(product)
                .where(product.id.in(productIds))
                .orderBy(product.id.asc())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetch();
    }

    @Override
    public Page<Product> findByChatbotKeyword(String keyword, ProductStatus status, Pageable pageable) {
        List<Product> content = queryFactory
                .selectFrom(product)
                .where(
                        chatbotKeywordLike(keyword),
                        statusEq(status)
                )
                .orderBy(getOrderSpecifier(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(product.count())
                .from(product)
                .where(
                        chatbotKeywordLike(keyword),
                        statusEq(status)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    @Override
    public Page<Product> findByCategoryIdInAndStatus(List<Long> categoryIds, ProductStatus status, Pageable pageable) {
        List<Product> content = queryFactory
                .selectFrom(product)
                .where(
                        categoryIdIn(categoryIds),
                        statusEq(status)
                )
                .orderBy(getOrderSpecifier(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(product.count())
                .from(product)
                .where(
                        categoryIdIn(categoryIds),
                        statusEq(status)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    @Override
    public void incrementViewCount(Long id) {
        queryFactory
                .update(product)
                .set(product.viewCount, product.viewCount.add(1))
                .where(product.id.eq(id))
                .execute();
    }

    private OrderSpecifier<?> getOrderSpecifier(Pageable pageable) {
        if (!pageable.getSort().isEmpty()) {
            Sort.Order order = pageable.getSort().iterator().next();
            if (order.getProperty().equals("price")) {
                return order.isAscending()
                        ? product.price.asc()
                        : product.price.desc();
            }
        }
        return product.createdAt.desc();
    }

    private BooleanExpression categoryIdEq(Long categoryId) {
        return categoryId != null ? product.categoryId.eq(categoryId) : null;
    }

    private BooleanExpression categoryIdIn(List<Long> categoryIds) {
        return categoryIds != null && !categoryIds.isEmpty() ? product.categoryId.in(categoryIds) : null;
    }

    private BooleanExpression keywordLike(String keyword) {
        return keyword != null ? product.name.contains(keyword) : null;
    }

    private BooleanExpression chatbotKeywordLike(String keyword) {
        return keyword != null
                ? product.name.contains(keyword).or(product.description.contains(keyword))
                : null;
    }

    private BooleanExpression statusEq(ProductStatus status) {
        return status != null ? product.status.eq(status) : null;
    }
}
