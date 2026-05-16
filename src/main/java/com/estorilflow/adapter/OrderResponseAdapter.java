package com.estorilflow.adapter;

import com.estorilflow.dto.OrderItemResponse;
import com.estorilflow.dto.OrderResponse;
import com.estorilflow.dto.OrderSummaryResponse;
import com.estorilflow.entity.Order;
import com.estorilflow.entity.OrderItem;
import java.util.List;

public final class OrderResponseAdapter {

    private OrderResponseAdapter() {
    }

    public static OrderSummaryResponse toSummaryResponse(Order order, List<OrderItem> items) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getCode(),
                order.getCustomerNameOrLabel(),
                order.getStatus(),
                order.getOpenedByUserId(),
                order.getClosedByUserId(),
                order.getOpenedAt(),
                order.getClosedAt(),
                order.getNotes(),
                items.size(),
                Order.totalAmount(items),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public static OrderResponse toResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(OrderResponseAdapter::toItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCode(),
                order.getCustomerNameOrLabel(),
                order.getStatus(),
                order.getOpenedByUserId(),
                order.getClosedByUserId(),
                order.getOpenedAt(),
                order.getClosedAt(),
                order.getNotes(),
                itemResponses.size(),
                Order.totalAmount(items),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemResponses
        );
    }

    public static OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductNameSnapshot(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
