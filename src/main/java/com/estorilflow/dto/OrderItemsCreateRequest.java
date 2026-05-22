package com.estorilflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrderItemsCreateRequest(
        @NotEmpty(message = "items is required")
        List<@Valid OrderItemCreateRequest> items
) {
}
