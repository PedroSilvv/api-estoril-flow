package com.estorilflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StoreCreditItemRequest(
        @NotNull(message = "productId is required")
        @Positive(message = "productId must be greater than zero")
        Long productId,
        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than zero")
        Integer quantity
) {
}
