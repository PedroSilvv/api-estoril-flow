package com.estorilflow.dto;

import com.estorilflow.entity.StoreCreditStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record StoreCreditSummaryResponse(
        Long id,
        String buyerName,
        String buyerPhone,
        BigDecimal amount,
        LocalDate creditDate,
        LocalDate dueDate,
        StoreCreditStatus status,
        int itemCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
