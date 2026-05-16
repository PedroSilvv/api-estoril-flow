package com.estorilflow.adapter;

import com.estorilflow.dto.ProductResponse;
import com.estorilflow.entity.Product;

public final class ProductResponseAdapter {

    private ProductResponseAdapter() {
    }

    public static ProductResponse toResponse(Product product) {
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
