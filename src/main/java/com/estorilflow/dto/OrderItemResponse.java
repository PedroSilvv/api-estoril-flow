package com.estorilflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productNameSnapshot,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
