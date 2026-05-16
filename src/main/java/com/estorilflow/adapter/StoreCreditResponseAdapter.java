package com.estorilflow.adapter;

import com.estorilflow.dto.StoreCreditItemResponse;
import com.estorilflow.dto.StoreCreditResponse;
import com.estorilflow.dto.StoreCreditSummaryResponse;
import com.estorilflow.entity.StoreCredit;
import com.estorilflow.entity.StoreCreditItem;
import java.time.LocalDate;
import java.util.List;

public final class StoreCreditResponseAdapter {

    private StoreCreditResponseAdapter() {
    }

    public static StoreCreditSummaryResponse toSummaryResponse(
            StoreCredit storeCredit,
            List<StoreCreditItem> items,
            LocalDate referenceDate
    ) {
        return new StoreCreditSummaryResponse(
                storeCredit.getId(),
                storeCredit.getBuyerName(),
                storeCredit.getBuyerPhone(),
                storeCredit.getAmount(),
                storeCredit.getCreditDate(),
                storeCredit.getDueDate(),
                storeCredit.effectiveStatus(referenceDate),
                items.size(),
                storeCredit.getCreatedAt(),
                storeCredit.getUpdatedAt()
        );
    }

    public static StoreCreditResponse toResponse(
            StoreCredit storeCredit,
            List<StoreCreditItem> items,
            LocalDate referenceDate
    ) {
        List<StoreCreditItemResponse> itemResponses = items.stream()
                .map(StoreCreditResponseAdapter::toItemResponse)
                .toList();

        return new StoreCreditResponse(
                storeCredit.getId(),
                storeCredit.getBuyerName(),
                storeCredit.getBuyerPhone(),
                storeCredit.getAmount(),
                storeCredit.getCreditDate(),
                storeCredit.getDueDate(),
                storeCredit.effectiveStatus(referenceDate),
                itemResponses.size(),
                storeCredit.getCreatedAt(),
                storeCredit.getUpdatedAt(),
                itemResponses
        );
    }

    public static StoreCreditItemResponse toItemResponse(StoreCreditItem item) {
        return new StoreCreditItemResponse(
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
