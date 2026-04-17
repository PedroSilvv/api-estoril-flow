package com.estorilflow.service;

import com.estorilflow.dto.ProductCreateRequest;
import com.estorilflow.dto.PageResponse;
import com.estorilflow.dto.ProductResponse;
import com.estorilflow.dto.ProductStatusUpdateRequest;
import com.estorilflow.dto.ProductUpdateRequest;
import com.estorilflow.entity.Product;
import com.estorilflow.exceptions.BusinessRuleException;
import com.estorilflow.exceptions.ResourceNotFoundException;
import com.estorilflow.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        Product product = Product.builder()
                .name(request.name().trim())
                .price(request.price())
                .active(true)
                .build();

        return toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAll(Pageable pageable, Boolean active) {
        Page<ProductResponse> page = productRepository.findAll(buildSpecification(active), pageable)
                .map(this::toResponse);

        return PageResponse.from(page);
    }

    private Specification<Product> buildSpecification(Boolean active) {
        Specification<Product> specification = Specification.unrestricted();

        if (active != null) {
            specification = specification.and((root, query, criteriaBuilder)
                    -> criteriaBuilder.equal(root.get("active"), active));
        }

        return specification;
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return toResponse(getProductById(id));
    }

    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        Product product = getProductById(id);
        product.setName(request.name().trim());
        product.setPrice(request.price());

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateStatus(Long id, ProductStatusUpdateRequest request) {
        Product product = getProductById(id);
        product.setActive(request.active());

        return toResponse(productRepository.save(product));
    }

    private Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
