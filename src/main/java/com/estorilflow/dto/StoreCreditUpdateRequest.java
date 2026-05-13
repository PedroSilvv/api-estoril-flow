package com.estorilflow.dto;

import com.estorilflow.entity.StoreCreditStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StoreCreditUpdateRequest(
        @NotBlank(message = "buyerName is required")
        @Size(max = 255, message = "buyerName must have at most 255 characters")
        String buyerName,
        @NotBlank(message = "buyerPhone is required")
        @Size(max = 50, message = "buyerPhone must have at most 50 characters")
        String buyerPhone,
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,
        @NotEmpty(message = "items must not be empty")
        List<@Valid StoreCreditItemRequest> items,
        @NotNull(message = "creditDate is required")
        LocalDate creditDate,
        @NotNull(message = "dueDate is required")
        LocalDate dueDate,
        @NotNull(message = "status is required")
        StoreCreditStatus status
) {
}
