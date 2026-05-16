package com.estorilflow.service;

import com.estorilflow.adapter.ProductResponseAdapter;
import com.estorilflow.dto.ProductCreateRequest;
import com.estorilflow.dto.PageResponse;
import com.estorilflow.dto.ProductResponse;
import com.estorilflow.dto.ProductStatusUpdateRequest;
import com.estorilflow.dto.ProductUpdateRequest;
import com.estorilflow.entity.Product;
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
        Product product = Product.create(request.name(), request.price());

        return ProductResponseAdapter.toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAll(Pageable pageable, Boolean active) {
        Page<ProductResponse> page = productRepository.findAll(buildSpecification(active), pageable)
                .map(ProductResponseAdapter::toResponse);

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
        return ProductResponseAdapter.toResponse(getProductById(id));
    }

    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        Product product = getProductById(id);
        product.updateDetails(request.name(), request.price());

        return ProductResponseAdapter.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateStatus(Long id, ProductStatusUpdateRequest request) {
        Product product = getProductById(id);
        product.activate(request.active());

        return ProductResponseAdapter.toResponse(productRepository.save(product));
    }

    private Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
    }
}
