package com.example.team3plusspring.domain.cart.service;

import com.example.team3plusspring.domain.cart.dto.AddCartItemRequest;
import com.example.team3plusspring.domain.cart.dto.AddCartItemResponse;
import com.example.team3plusspring.domain.cart.dto.CartItemDetailResponse;
import com.example.team3plusspring.domain.cart.dto.GetCartResponse;
import com.example.team3plusspring.domain.cart.entity.Cart;
import com.example.team3plusspring.domain.cart.entity.CartItem;
import com.example.team3plusspring.domain.cart.repository.CartItemRepository;
import com.example.team3plusspring.domain.cart.repository.CartRepository;
import com.example.team3plusspring.domain.product.entity.Product;
import com.example.team3plusspring.domain.product.repository.ProductRepository;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import com.example.team3plusspring.global.security.jwt.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Transactional
    public AddCartItemResponse add(CustomUserDetails userDetails, AddCartItemRequest request) {
        Cart cart = cartRepository.findByUserIdForUpdate(userDetails.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_NOT_FOUND));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 판매중인 상품인지 확인
        product.validateStatus();

        // 장바구니에 이미 있는 상품인지 조회
        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .map(existingCartItem -> {
                    int newQuantity = existingCartItem.getQuantity() + request.getQuantity();

                    if (!product.hasEnoughStock(newQuantity)) { // 합산 수량이 재고 초과하는지 검증
                        throw new BusinessException(ErrorCode.CART_STOCK_EXCEEDED);
                    }

                    existingCartItem.addQuantity(request.getQuantity());

                    return existingCartItem;
                })
                .orElseGet(() -> {  // 장바구니에 존재하지 않는 상품인 경우 재고 초과하는지 확인 후 새로 담기
                    if (!product.hasEnoughStock(request.getQuantity())) {
                        throw new BusinessException(ErrorCode.CART_STOCK_EXCEEDED);
                    }

                    return CartItem.create(cart, product.getId(), request.getQuantity());
                });

        CartItem savedCartItem = cartItemRepository.save(cartItem);

        return AddCartItemResponse.of(savedCartItem, product);
    }

    @Transactional(readOnly = true)
    public GetCartResponse getMyCart(CustomUserDetails userDetails) {
        // 1. 장바구니 조회
        Cart cart = cartRepository.findByUserId(userDetails.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_NOT_FOUND));

        // 2. 장바구니 아이템 조회
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        // 3. 상품 ID 목록 추출
        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .toList(); // distinct() 제거

        // 4. 상품들을 한 번에 조회
        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        // 5. 상세 정보 매핑
        List<CartItemDetailResponse> itemDetails = cartItems.stream()
                .map(item -> {
                    Product product = productMap.get(item.getProductId());
                    if (product == null) {
                        throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
                    }
                    return CartItemDetailResponse.of(item, product);
                })
                .toList();

        return GetCartResponse.of(
                cart.getId(),
                itemDetails,
                cartItems.stream().mapToInt(CartItem::getQuantity).sum(),
                itemDetails.stream().mapToInt(CartItemDetailResponse::getLineAmount).sum()
        );
    }
}
