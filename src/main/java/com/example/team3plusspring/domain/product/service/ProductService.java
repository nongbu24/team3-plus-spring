package com.example.team3plusspring.domain.product.service;

import com.example.team3plusspring.domain.category.entity.Category;
import com.example.team3plusspring.domain.category.repository.CategoryRepository;
import com.example.team3plusspring.domain.order.entity.OrderItem;
import com.example.team3plusspring.domain.product.dto.GetOneProductResponse;
import com.example.team3plusspring.domain.product.dto.GetProductsResponse;
import com.example.team3plusspring.domain.product.entity.Product;
import com.example.team3plusspring.domain.product.entity.ProductStatus;
import com.example.team3plusspring.domain.product.repository.ProductRepository;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // 상품 상세 조회
    public GetOneProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        Category category = categoryRepository.findById(product.getCategoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        return GetOneProductResponse.of(product, category);
    }

    // 상품 목록 조회
    public Page<GetProductsResponse> getProducts(
            Long categoryId, String keyword, ProductStatus status,
            String sort, int page, int size) {

        // 페이지 유효성 검증
        if (page < 0 || size <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PAGINATION);
        }

        // 정렬 조건
        Sort sorting = switch (sort) {
            case "LATEST" -> Sort.by("createdAt").descending();
            case "PRICE_ASC" -> Sort.by("price").ascending();
            case "PRICE_DESC" -> Sort.by("price").descending();
            default -> throw new BusinessException(ErrorCode.INVALID_ENUM_VALUE);
        };

        // 페이지 조건
        Pageable pageable = PageRequest.of(page, size, sorting);

        // status 기본값 적용
        if (status == null) {
            status = ProductStatus.ON_SALE;
        }

        // 상품 목록 조회
        Page<Product> products = productRepository.findByCondition(
                categoryId, keyword, status, pageable
        );

        // categoryId 목록으로 Category 한 번에 조회
        List<Long> categoryIds = products.getContent()
                .stream()
                .map(Product::getCategoryId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Category> categoryMap = categoryRepository.findAllById(categoryIds)
                .stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        // DTO 변환
        return products.map(product ->
                GetProductsResponse.of(product, categoryMap.get(product.getCategoryId()))
        );
    }

    @Transactional
    public Product getOrderableProductAndDecreaseStock(Long productId, int quantity) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.decreaseStock(quantity);

        return product;
    }

    @Transactional
    public List<Product> getOrderableProductsForUpdate(List<Long> productIds) {
        List<Product> products = productRepository.findAllByIdInForUpdate(productIds);

        if (products.size() != productIds.size()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        return products;
    }

    @Transactional
    public void restoreStocks(List<OrderItem> orderItems) {
        Map<Long, Integer> quantitiesByProductId = orderItems.stream()
                .collect(Collectors.toMap(
                        OrderItem::getProductId,
                        OrderItem::getQuantity,
                        Integer::sum
                ));

        List<Long> productIds = quantitiesByProductId.keySet().stream()
                .sorted()
                .toList();
        List<Product> products = productRepository.findAllByIdInForUpdate(productIds);

        if (products.size() != productIds.size()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        Map<Long, Product> productsById = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        quantitiesByProductId.forEach((productId, quantity) ->
                productsById.get(productId).increaseStock(quantity)
        );
    }
}
