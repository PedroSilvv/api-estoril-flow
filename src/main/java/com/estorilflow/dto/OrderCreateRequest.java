package com.estorilflow.dto;

import jakarta.validation.constraints.Size;

public record OrderCreateRequest(
        @Size(max = 255, message = "customerNameOrLabel must have at most 255 characters")
        String customerNameOrLabel,
        @Size(max = 500, message = "notes must have at most 500 characters")
        String notes
) {
}
