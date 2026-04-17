package com.estorilflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.estorilflow.dto.PageResponse;
import com.estorilflow.dto.SaleResponse;
import com.estorilflow.dto.SaleSummaryResponse;
import com.estorilflow.entity.Sale;
import com.estorilflow.entity.SaleItem;
import com.estorilflow.exceptions.BusinessRuleException;
import com.estorilflow.exceptions.ResourceNotFoundException;
import com.estorilflow.repository.SaleItemRepository;
import com.estorilflow.repository.SaleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private SaleItemRepository saleItemRepository;

    private SaleService saleService;

    @BeforeEach
    void setUp() {
        saleService = new SaleService(saleRepository, saleItemRepository);
    }

    @Test
    void shouldReturnSalesAsPaginatedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Sale firstSale = sale(1L, 10L, "38.40");
        Sale secondSale = sale(2L, 11L, "25.00");
        SaleItem firstItem = saleItem(100L, 1L, 1L, "Agua", "5.00", 2, "10.00");
        SaleItem secondItem = saleItem(101L, 2L, 2L, "Suco", "25.00", 1, "25.00");

        when(saleRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Sale>>any(), org.mockito.ArgumentMatchers.eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(firstSale, secondSale), pageable, 2));
        when(saleItemRepository.findAllBySaleIdIn(List.of(1L, 2L))).thenReturn(List.of(firstItem, secondItem));

        PageResponse<SaleSummaryResponse> response = saleService.findAll(pageable, null, null, null);

        assertThat(response.content()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.content().getFirst().itemCount()).isEqualTo(1);
        assertThat(response.content().getFirst().orderId()).isEqualTo(10L);
    }

    @Test
    void shouldRejectInvalidDateRangeWhenListingSales() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> saleService.findAll(
                pageable,
                null,
                LocalDate.parse("2026-04-18"),
                LocalDate.parse("2026-04-17")
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("startDate cannot be after endDate");
    }

    @Test
    void shouldReturnSaleByIdWithItems() {
        Sale sale = sale(3L, 12L, "63.50");
        SaleItem item = saleItem(102L, 3L, 3L, "Caipirinha", "31.75", 2, "63.50");

        when(saleRepository.findById(3L)).thenReturn(Optional.of(sale));
        when(saleItemRepository.findAllBySaleIdOrderByIdAsc(3L)).thenReturn(List.of(item));

        SaleResponse response = saleService.findById(3L);

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.orderId()).isEqualTo(12L);
        assertThat(response.itemCount()).isEqualTo(1);
        assertThat(response.items().getFirst().productNameSnapshot()).isEqualTo("Caipirinha");
    }

    @Test
    void shouldThrowWhenSaleDoesNotExist() {
        when(saleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Sale not found with id 99");
    }

    private Sale sale(Long id, Long orderId, String totalAmount) {
        return Sale.builder()
                .id(id)
                .orderId(orderId)
                .totalAmount(new BigDecimal(totalAmount))
                .soldAt(now())
                .openedByUserId(null)
                .closedByUserId(null)
                .createdAt(now())
                .updatedAt(now())
                .build();
    }

    private SaleItem saleItem(
            Long id,
            Long saleId,
            Long productId,
            String productNameSnapshot,
            String unitPrice,
            Integer quantity,
            String subtotal
    ) {
        return SaleItem.builder()
                .id(id)
                .saleId(saleId)
                .productId(productId)
                .productNameSnapshot(productNameSnapshot)
                .unitPrice(new BigDecimal(unitPrice))
                .quantity(quantity)
                .subtotal(new BigDecimal(subtotal))
                .createdAt(now())
                .updatedAt(now())
                .build();
    }

    private LocalDateTime now() {
        return LocalDateTime.parse("2026-04-17T05:00:00");
    }
}
