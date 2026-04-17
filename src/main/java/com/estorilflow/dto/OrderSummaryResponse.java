package com.estorilflow.dto;

import com.estorilflow.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
        Long id,
        String code,
        String customerNameOrLabel,
        OrderStatus status,
        Long openedByUserId,
        Long closedByUserId,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        String notes,
        int itemCount,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
