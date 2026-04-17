package com.estorilflow.dto;

import com.estorilflow.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
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
        LocalDateTime updatedAt,
        List<OrderItemResponse> items
) {
}
