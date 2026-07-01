package com.example.team3plusspring.domain.product.service;

import com.example.team3plusspring.domain.category.entity.Category;
import com.example.team3plusspring.domain.category.repository.CategoryRepository;
import com.example.team3plusspring.domain.product.entity.Product;
import com.example.team3plusspring.domain.product.repository.ProductRepository;
import com.example.team3plusspring.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    CategoryRepository categoryRepository;

    @InjectMocks
    ProductService productService;

    private Category createTestCategory(Long id, String name) throws Exception {
        Category category = Category.class.getDeclaredConstructor().newInstance();

        Field idField = Category.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(category, id);

        Field nameField = Category.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(category, name);

        return category;
    }

    @Test
    @DisplayName("상품 상세 조회 시 조회수가 1 증가한다")
    void getProduct_조회수_증가() throws Exception {
        // given
        Product product = Product.create("테스트상품", "설명", 10000, 100, 1L);
        Category category = createTestCategory(1L, "테스트카테고리");

        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

        int beforeViewCount = product.getViewCount(); // 0

        // when
        productService.getProduct(1L);

        // then
        assertThat(product.getViewCount()).isEqualTo(beforeViewCount + 1);
        System.out.println("조회 전: " + beforeViewCount + " → 조회 후: " + product.getViewCount());
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 예외가 발생한다")
    void getProduct_없는상품_예외() {
        // given
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProduct(999L))
                .isInstanceOf(BusinessException.class);
    }
}