package com.estorilflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleSummaryResponse(
        Long id,
        Long orderId,
        BigDecimal totalAmount,
        LocalDateTime soldAt,
        Long openedByUserId,
        Long closedByUserId,
        int itemCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
