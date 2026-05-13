package com.estorilflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StoreCreditItemResponse(
        Long id,
        Long productId,
        String productNameSnapshot,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
