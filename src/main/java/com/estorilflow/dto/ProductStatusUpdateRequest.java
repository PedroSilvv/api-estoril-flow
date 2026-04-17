package com.estorilflow.dto;

import jakarta.validation.constraints.NotNull;

public record ProductStatusUpdateRequest(
        @NotNull(message = "active is required")
        Boolean active
) {
}
