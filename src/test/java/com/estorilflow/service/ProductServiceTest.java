package com.estorilflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.estorilflow.dto.PageResponse;
import com.estorilflow.dto.ProductCreateRequest;
import com.estorilflow.dto.ProductResponse;
import com.estorilflow.dto.ProductStatusUpdateRequest;
import com.estorilflow.dto.ProductUpdateRequest;
import com.estorilflow.entity.Product;
import com.estorilflow.exceptions.ResourceNotFoundException;
import com.estorilflow.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    void shouldCreateProductAsActive() {
        Product savedProduct = product(
                1L,
                "Caipirinha",
                new BigDecimal("25.90"),
                true,
                LocalDateTime.parse("2026-04-17T04:00:00"),
                LocalDateTime.parse("2026-04-17T04:00:00")
        );

        when(productRepository.save(org.mockito.ArgumentMatchers.any(Product.class))).thenReturn(savedProduct);

        ProductResponse response = productService.create(new ProductCreateRequest("  Caipirinha  ", new BigDecimal("25.90")));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Caipirinha");
        assertThat(response.price()).isEqualByComparingTo("25.90");
        assertThat(response.active()).isTrue();
    }

    @Test
    void shouldReturnProductsAsPaginatedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Product>>any(), org.mockito.ArgumentMatchers.eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(
                product(1L, "Agua", new BigDecimal("5.00"), true, null, null),
                product(2L, "Suco", new BigDecimal("9.50"), false, null, null)
        ), pageable, 2));

        PageResponse<ProductResponse> response = productService.findAll(pageable, null);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().getFirst().name()).isEqualTo("Agua");
        assertThat(response.content().get(1).active()).isFalse();
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(2);
    }

    @Test
    void shouldReturnProductById() {
        Product product = product(3L, "Cafe", new BigDecimal("7.00"), true, null, null);
        when(productRepository.findById(3L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.findById(3L);

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.name()).isEqualTo("Cafe");
    }

    @Test
    void shouldUpdateProduct() {
        Product existingProduct = product(4L, "Refrigerante", new BigDecimal("8.00"), true, null, null);
        when(productRepository.findById(4L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(existingProduct)).thenReturn(existingProduct);

        ProductResponse response = productService.update(4L, new ProductUpdateRequest("  Refrigerante Zero  ", new BigDecimal("9.50")));

        assertThat(existingProduct.getName()).isEqualTo("Refrigerante Zero");
        assertThat(existingProduct.getPrice()).isEqualByComparingTo("9.50");
        assertThat(response.name()).isEqualTo("Refrigerante Zero");
    }

    @Test
    void shouldUpdateProductStatus() {
        Product existingProduct = product(5L, "Hamburguer", new BigDecimal("32.00"), true, null, null);
        when(productRepository.findById(5L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(existingProduct)).thenReturn(existingProduct);

        ProductResponse response = productService.updateStatus(5L, new ProductStatusUpdateRequest(false));

        assertThat(existingProduct.isActive()).isFalse();
        assertThat(response.active()).isFalse();
    }

    @Test
    void shouldThrowWhenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found with id 99");

        verify(productRepository).findById(99L);
    }

    private Product product(
            Long id,
            String name,
            BigDecimal price,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return Product.builder()
                .id(id)
                .name(name)
                .price(price)
                .active(active)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
