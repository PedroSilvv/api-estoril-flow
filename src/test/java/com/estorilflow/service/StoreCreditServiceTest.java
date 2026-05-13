package com.estorilflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.estorilflow.dto.PageResponse;
import com.estorilflow.dto.StoreCreditCreateRequest;
import com.estorilflow.dto.StoreCreditItemRequest;
import com.estorilflow.dto.StoreCreditResponse;
import com.estorilflow.dto.StoreCreditSummaryResponse;
import com.estorilflow.dto.StoreCreditUpdateRequest;
import com.estorilflow.entity.Product;
import com.estorilflow.entity.StoreCredit;
import com.estorilflow.entity.StoreCreditItem;
import com.estorilflow.entity.StoreCreditStatus;
import com.estorilflow.exceptions.BusinessRuleException;
import com.estorilflow.repository.ProductRepository;
import com.estorilflow.repository.StoreCreditItemRepository;
import com.estorilflow.repository.StoreCreditRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class StoreCreditServiceTest {

    @Mock
    private StoreCreditRepository storeCreditRepository;

    @Mock
    private StoreCreditItemRepository storeCreditItemRepository;

    @Mock
    private ProductRepository productRepository;

    private StoreCreditService storeCreditService;

    @BeforeEach
    void setUp() {
        storeCreditService = new StoreCreditService(
                storeCreditRepository,
                storeCreditItemRepository,
                productRepository
        );
    }

    @Test
    void shouldCreateStoreCreditWithLinkedItems() {
        Product product = product(10L, "Caipirinha", "25.90", true);
        StoreCredit savedStoreCredit = storeCredit(
                1L,
                "Maria",
                "11999999999",
                "51.80",
                LocalDate.parse("2026-05-12"),
                LocalDate.parse("2026-05-20"),
                StoreCreditStatus.PENDING_PAYMENT
        );
        StoreCreditItem savedItem = storeCreditItem(20L, 1L, 10L, "Caipirinha", "25.90", 2, "51.80");

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(storeCreditRepository.save(any(StoreCredit.class))).thenReturn(savedStoreCredit);
        when(storeCreditItemRepository.saveAll(any())).thenReturn(List.of(savedItem));

        StoreCreditResponse response = storeCreditService.create(new StoreCreditCreateRequest(
                " Maria ",
                " 11999999999 ",
                new BigDecimal("51.80"),
                List.of(new StoreCreditItemRequest(10L, 2)),
                LocalDate.parse("2026-05-12"),
                LocalDate.parse("2026-05-20"),
                StoreCreditStatus.PENDING_PAYMENT
        ));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.buyerName()).isEqualTo("Maria");
        assertThat(response.buyerPhone()).isEqualTo("11999999999");
        assertThat(response.amount()).isEqualByComparingTo("51.80");
        assertThat(response.status()).isEqualTo(StoreCreditStatus.PENDING_PAYMENT);
        assertThat(response.itemCount()).isEqualTo(1);
    }

    @Test
    void shouldReturnStoreCreditsAsPaginatedResponse() {
        StoreCredit firstCredit = storeCredit(
                1L,
                "Maria",
                "11999999999",
                "32.00",
                LocalDate.parse("2026-05-10"),
                LocalDate.parse("2026-05-20"),
                StoreCreditStatus.PENDING_PAYMENT
        );
        StoreCredit secondCredit = storeCredit(
                2L,
                "Joao",
                "11888888888",
                "14.20",
                LocalDate.parse("2026-05-11"),
                LocalDate.parse("2026-05-11"),
                StoreCreditStatus.PAID
        );
        StoreCreditItem firstItem = storeCreditItem(10L, 1L, 1L, "Agua", "8.00", 4, "32.00");
        StoreCreditItem secondItem = storeCreditItem(11L, 2L, 2L, "Suco", "14.20", 1, "14.20");
        Pageable pageable = PageRequest.of(0, 10);

        when(storeCreditRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<StoreCredit>>any(),
                org.mockito.ArgumentMatchers.eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(firstCredit, secondCredit), pageable, 2));
        when(storeCreditItemRepository.findAllByStoreCreditIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(firstItem, secondItem));

        PageResponse<StoreCreditSummaryResponse> response = storeCreditService.findAll(pageable, null, null, null);

        assertThat(response.content()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.content().getFirst().itemCount()).isEqualTo(1);
        assertThat(response.content().getFirst().amount()).isEqualByComparingTo("32.00");
    }

    @Test
    void shouldRejectInvalidDateRangeWhenListingStoreCredits() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> storeCreditService.findAll(
                pageable,
                StoreCreditStatus.PENDING_PAYMENT,
                LocalDate.parse("2026-05-20"),
                LocalDate.parse("2026-05-12")
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("startDate cannot be after endDate");
    }

    @Test
    void shouldRejectAmountMismatch() {
        Product product = product(10L, "Caipirinha", "25.90", true);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> storeCreditService.create(new StoreCreditCreateRequest(
                "Maria",
                "11999999999",
                new BigDecimal("50.00"),
                List.of(new StoreCreditItemRequest(10L, 2)),
                LocalDate.parse("2026-05-12"),
                LocalDate.parse("2026-05-20"),
                StoreCreditStatus.PENDING_PAYMENT
        )))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("amount must match the sum of linked items");
    }

    @Test
    void shouldUpdateStoreCreditAndReplaceItems() {
        Product firstProduct = product(10L, "Caipirinha", "25.90", true);
        Product secondProduct = product(11L, "Water", "5.00", true);
        StoreCredit existingStoreCredit = storeCredit(
                1L,
                "Maria",
                "11999999999",
                "25.90",
                LocalDate.parse("2026-05-12"),
                LocalDate.parse("2026-05-20"),
                StoreCreditStatus.PENDING_PAYMENT
        );
        StoreCreditItem savedItem = storeCreditItem(21L, 1L, 11L, "Water", "5.00", 3, "15.00");

        when(storeCreditRepository.findById(1L)).thenReturn(Optional.of(existingStoreCredit));
        when(productRepository.findById(11L)).thenReturn(Optional.of(secondProduct));
        when(storeCreditRepository.save(existingStoreCredit)).thenReturn(existingStoreCredit);
        when(storeCreditItemRepository.saveAll(any())).thenReturn(List.of(savedItem));

        StoreCreditResponse response = storeCreditService.update(1L, new StoreCreditUpdateRequest(
                "Maria Silva",
                "11777777777",
                new BigDecimal("15.00"),
                List.of(new StoreCreditItemRequest(11L, 3)),
                LocalDate.parse("2026-05-13"),
                LocalDate.parse("2026-05-25"),
                StoreCreditStatus.PENDING_PAYMENT
        ));

        assertThat(response.buyerName()).isEqualTo("Maria Silva");
        assertThat(response.buyerPhone()).isEqualTo("11777777777");
        assertThat(response.amount()).isEqualByComparingTo("15.00");
        verify(storeCreditItemRepository).deleteAllByStoreCreditId(1L);

        ArgumentCaptor<StoreCredit> storeCreditCaptor = ArgumentCaptor.forClass(StoreCredit.class);
        verify(storeCreditRepository).save(storeCreditCaptor.capture());
        assertThat(storeCreditCaptor.getValue().getDueDate()).isEqualTo(LocalDate.parse("2026-05-25"));
    }

    @Test
    void shouldReturnOverdueStatusWhenDueDateHasPassed() {
        StoreCredit overdueStoreCredit = storeCredit(
                3L,
                "Carlos",
                "11666666666",
                "12.00",
                LocalDate.parse("2026-05-01"),
                LocalDate.parse("2026-05-05"),
                StoreCreditStatus.PENDING_PAYMENT
        );
        StoreCreditItem item = storeCreditItem(30L, 3L, 2L, "Juice", "12.00", 1, "12.00");

        when(storeCreditRepository.findById(3L)).thenReturn(Optional.of(overdueStoreCredit));
        when(storeCreditItemRepository.findAllByStoreCreditIdOrderByIdAsc(3L)).thenReturn(List.of(item));

        StoreCreditResponse response = storeCreditService.findById(3L);

        assertThat(response.status()).isEqualTo(StoreCreditStatus.OVERDUE);
    }

    private StoreCredit storeCredit(
            Long id,
            String buyerName,
            String buyerPhone,
            String amount,
            LocalDate creditDate,
            LocalDate dueDate,
            StoreCreditStatus status
    ) {
        return StoreCredit.builder()
                .id(id)
                .buyerName(buyerName)
                .buyerPhone(buyerPhone)
                .amount(new BigDecimal(amount))
                .creditDate(creditDate)
                .dueDate(dueDate)
                .status(status)
                .createdAt(now())
                .updatedAt(now())
                .build();
    }

    private StoreCreditItem storeCreditItem(
            Long id,
            Long storeCreditId,
            Long productId,
            String productNameSnapshot,
            String unitPrice,
            Integer quantity,
            String subtotal
    ) {
        return StoreCreditItem.builder()
                .id(id)
                .storeCreditId(storeCreditId)
                .productId(productId)
                .productNameSnapshot(productNameSnapshot)
                .unitPrice(new BigDecimal(unitPrice))
                .quantity(quantity)
                .subtotal(new BigDecimal(subtotal))
                .createdAt(now())
                .updatedAt(now())
                .build();
    }

    private Product product(Long id, String name, String price, boolean active) {
        return Product.builder()
                .id(id)
                .name(name)
                .price(new BigDecimal(price))
                .active(active)
                .build();
    }

    private LocalDateTime now() {
        return LocalDateTime.parse("2026-05-12T05:00:00");
    }
}
