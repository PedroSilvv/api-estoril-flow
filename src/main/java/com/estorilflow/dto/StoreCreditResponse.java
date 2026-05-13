package com.estorilflow.dto;

import com.estorilflow.entity.StoreCreditStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record StoreCreditResponse(
        Long id,
        String buyerName,
        String buyerPhone,
        BigDecimal amount,
        LocalDate creditDate,
        LocalDate dueDate,
        StoreCreditStatus status,
        int itemCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<StoreCreditItemResponse> items
) {
}
