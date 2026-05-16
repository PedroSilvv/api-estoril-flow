package com.estorilflow.adapter;

import com.estorilflow.dto.SaleItemResponse;
import com.estorilflow.dto.SaleResponse;
import com.estorilflow.dto.SaleSummaryResponse;
import com.estorilflow.entity.Sale;
import com.estorilflow.entity.SaleItem;
import java.util.List;

public final class SaleResponseAdapter {

    private SaleResponseAdapter() {
    }

    public static SaleSummaryResponse toSummaryResponse(Sale sale, List<SaleItem> items) {
        return new SaleSummaryResponse(
                sale.getId(),
                sale.getOrderId(),
                sale.getTotalAmount(),
                sale.getSoldAt(),
                sale.getOpenedByUserId(),
                sale.getClosedByUserId(),
                items.size(),
                sale.getCreatedAt(),
                sale.getUpdatedAt()
        );
    }

    public static SaleResponse toResponse(Sale sale, List<SaleItem> items) {
        List<SaleItemResponse> itemResponses = items.stream()
                .map(SaleResponseAdapter::toItemResponse)
                .toList();

        return new SaleResponse(
                sale.getId(),
                sale.getOrderId(),
                sale.getTotalAmount(),
                sale.getSoldAt(),
                sale.getOpenedByUserId(),
                sale.getClosedByUserId(),
                itemResponses.size(),
                sale.getCreatedAt(),
                sale.getUpdatedAt(),
                itemResponses
        );
    }

    public static SaleItemResponse toItemResponse(SaleItem item) {
        return new SaleItemResponse(
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
