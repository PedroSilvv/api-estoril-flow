package com.estorilflow.service;

import com.estorilflow.dto.PageResponse;
import com.estorilflow.dto.StoreCreditCreateRequest;
import com.estorilflow.dto.StoreCreditItemRequest;
import com.estorilflow.dto.StoreCreditItemResponse;
import com.estorilflow.dto.StoreCreditResponse;
import com.estorilflow.dto.StoreCreditSummaryResponse;
import com.estorilflow.dto.StoreCreditUpdateRequest;
import com.estorilflow.entity.Product;
import com.estorilflow.entity.StoreCredit;
import com.estorilflow.entity.StoreCreditItem;
import com.estorilflow.entity.StoreCreditStatus;
import com.estorilflow.exceptions.BusinessRuleException;
import com.estorilflow.exceptions.ResourceNotFoundException;
import com.estorilflow.repository.ProductRepository;
import com.estorilflow.repository.StoreCreditItemRepository;
import com.estorilflow.repository.StoreCreditRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreCreditService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final StoreCreditRepository storeCreditRepository;
    private final StoreCreditItemRepository storeCreditItemRepository;
    private final ProductRepository productRepository;

    public StoreCreditService(
            StoreCreditRepository storeCreditRepository,
            StoreCreditItemRepository storeCreditItemRepository,
            ProductRepository productRepository
    ) {
        this.storeCreditRepository = storeCreditRepository;
        this.storeCreditItemRepository = storeCreditItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public StoreCreditResponse create(StoreCreditCreateRequest request) {
        validateDueDate(request.creditDate(), request.dueDate());

        List<StoreCreditItem> items = buildItems(request.items());
        BigDecimal calculatedAmount = calculateTotal(items);
        validateAmount(request.amount(), calculatedAmount);

        StoreCredit storeCredit = StoreCredit.builder()
                .buyerName(normalizeRequired(request.buyerName(), "buyerName"))
                .buyerPhone(normalizeRequired(request.buyerPhone(), "buyerPhone"))
                .amount(calculatedAmount)
                .creditDate(request.creditDate())
                .dueDate(request.dueDate())
                .status(resolveStatus(request.status(), request.dueDate()))
                .build();

        StoreCredit savedStoreCredit = storeCreditRepository.save(storeCredit);
        List<StoreCreditItem> itemsToSave = items.stream()
                .peek(item -> item.setStoreCreditId(savedStoreCredit.getId()))
                .toList();

        List<StoreCreditItem> savedItems = storeCreditItemRepository.saveAll(itemsToSave);
        return toStoreCreditResponse(savedStoreCredit, savedItems);
    }

    @Transactional(readOnly = true)
    public PageResponse<StoreCreditSummaryResponse> findAll(
            Pageable pageable,
            StoreCreditStatus status,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);

        Page<StoreCredit> storeCreditPage = storeCreditRepository.findAll(
                buildSpecification(status, startDate, endDate),
                pageable
        );
        if (storeCreditPage.isEmpty()) {
            return PageResponse.from(storeCreditPage.map(storeCredit -> toStoreCreditSummaryResponse(storeCredit, List.of())));
        }

        List<StoreCreditItem> items = storeCreditItemRepository.findAllByStoreCreditIdIn(
                storeCreditPage.getContent().stream().map(StoreCredit::getId).toList()
        );
        Map<Long, List<StoreCreditItem>> itemsByStoreCreditId = items.stream()
                .collect(Collectors.groupingBy(StoreCreditItem::getStoreCreditId));

        return PageResponse.from(storeCreditPage.map(storeCredit -> toStoreCreditSummaryResponse(
                storeCredit,
                itemsByStoreCreditId.getOrDefault(storeCredit.getId(), List.of())
        )));
    }

    @Transactional(readOnly = true)
    public StoreCreditResponse findById(Long id) {
        StoreCredit storeCredit = getStoreCreditById(id);
        List<StoreCreditItem> items = storeCreditItemRepository.findAllByStoreCreditIdOrderByIdAsc(id);
        return toStoreCreditResponse(storeCredit, items);
    }

    @Transactional
    public StoreCreditResponse update(Long id, StoreCreditUpdateRequest request) {
        validateDueDate(request.creditDate(), request.dueDate());

        StoreCredit storeCredit = getStoreCreditById(id);
        List<StoreCreditItem> items = buildItems(request.items());
        BigDecimal calculatedAmount = calculateTotal(items);
        validateAmount(request.amount(), calculatedAmount);

        storeCredit.setBuyerName(normalizeRequired(request.buyerName(), "buyerName"));
        storeCredit.setBuyerPhone(normalizeRequired(request.buyerPhone(), "buyerPhone"));
        storeCredit.setAmount(calculatedAmount);
        storeCredit.setCreditDate(request.creditDate());
        storeCredit.setDueDate(request.dueDate());
        storeCredit.setStatus(resolveStatus(request.status(), request.dueDate()));

        StoreCredit savedStoreCredit = storeCreditRepository.save(storeCredit);

        storeCreditItemRepository.deleteAllByStoreCreditId(savedStoreCredit.getId());
        List<StoreCreditItem> itemsToSave = items.stream()
                .peek(item -> item.setStoreCreditId(savedStoreCredit.getId()))
                .toList();
        List<StoreCreditItem> savedItems = storeCreditItemRepository.saveAll(itemsToSave);

        return toStoreCreditResponse(savedStoreCredit, savedItems);
    }

    private Specification<StoreCredit> buildSpecification(
            StoreCreditStatus status,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Specification<StoreCredit> specification = Specification.unrestricted();
        LocalDate today = today();

        if (status != null) {
            specification = switch (status) {
                case PAID -> specification.and((root, query, criteriaBuilder)
                        -> criteriaBuilder.equal(root.get("status"), StoreCreditStatus.PAID));
                case PENDING_PAYMENT -> specification.and((root, query, criteriaBuilder) -> criteriaBuilder.and(
                        criteriaBuilder.notEqual(root.get("status"), StoreCreditStatus.PAID),
                        criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), today)
                ));
                case OVERDUE -> specification.and((root, query, criteriaBuilder) -> criteriaBuilder.and(
                        criteriaBuilder.notEqual(root.get("status"), StoreCreditStatus.PAID),
                        criteriaBuilder.lessThan(root.get("dueDate"), today)
                ));
            };
        }

        if (startDate != null) {
            specification = specification.and((root, query, criteriaBuilder)
                    -> criteriaBuilder.greaterThanOrEqualTo(root.get("creditDate"), startDate));
        }

        if (endDate != null) {
            specification = specification.and((root, query, criteriaBuilder)
                    -> criteriaBuilder.lessThanOrEqualTo(root.get("creditDate"), endDate));
        }

        return specification;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessRuleException("startDate cannot be after endDate");
        }
    }

    private void validateDueDate(LocalDate creditDate, LocalDate dueDate) {
        if (creditDate.isAfter(dueDate)) {
            throw new BusinessRuleException("creditDate cannot be after dueDate");
        }
    }

    private void validateAmount(BigDecimal amount, BigDecimal calculatedAmount) {
        if (amount.setScale(2, RoundingMode.HALF_UP).compareTo(calculatedAmount) != 0) {
            throw new BusinessRuleException("amount must match the sum of linked items");
        }
    }

    private StoreCredit getStoreCreditById(Long id) {
        return storeCreditRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store credit not found with id " + id));
    }

    private List<StoreCreditItem> buildItems(List<StoreCreditItemRequest> requests) {
        return requests.stream()
                .map(this::toStoreCreditItem)
                .toList();
    }

    private StoreCreditItem toStoreCreditItem(StoreCreditItemRequest request) {
        Product product = getActiveProduct(request.productId());

        return StoreCreditItem.builder()
                .productId(product.getId())
                .productNameSnapshot(product.getName())
                .unitPrice(product.getPrice())
                .quantity(request.quantity())
                .subtotal(calculateSubtotal(product.getPrice(), request.quantity()))
                .build();
    }

    private Product getActiveProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + productId));

        if (!product.isActive()) {
            throw new BusinessRuleException("Inactive product cannot be used in store credits");
        }

        return product;
    }

    private StoreCreditSummaryResponse toStoreCreditSummaryResponse(StoreCredit storeCredit, List<StoreCreditItem> items) {
        return new StoreCreditSummaryResponse(
                storeCredit.getId(),
                storeCredit.getBuyerName(),
                storeCredit.getBuyerPhone(),
                storeCredit.getAmount(),
                storeCredit.getCreditDate(),
                storeCredit.getDueDate(),
                effectiveStatus(storeCredit),
                items.size(),
                storeCredit.getCreatedAt(),
                storeCredit.getUpdatedAt()
        );
    }

    private StoreCreditResponse toStoreCreditResponse(StoreCredit storeCredit, List<StoreCreditItem> items) {
        List<StoreCreditItemResponse> itemResponses = items.stream()
                .map(this::toStoreCreditItemResponse)
                .toList();

        return new StoreCreditResponse(
                storeCredit.getId(),
                storeCredit.getBuyerName(),
                storeCredit.getBuyerPhone(),
                storeCredit.getAmount(),
                storeCredit.getCreditDate(),
                storeCredit.getDueDate(),
                effectiveStatus(storeCredit),
                itemResponses.size(),
                storeCredit.getCreatedAt(),
                storeCredit.getUpdatedAt(),
                itemResponses
        );
    }

    private StoreCreditItemResponse toStoreCreditItemResponse(StoreCreditItem item) {
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

    private StoreCreditStatus resolveStatus(StoreCreditStatus requestedStatus, LocalDate dueDate) {
        if (requestedStatus == StoreCreditStatus.PAID) {
            return StoreCreditStatus.PAID;
        }

        return dueDate.isBefore(today()) ? StoreCreditStatus.OVERDUE : StoreCreditStatus.PENDING_PAYMENT;
    }

    private StoreCreditStatus effectiveStatus(StoreCredit storeCredit) {
        if (storeCredit.getStatus() == StoreCreditStatus.PAID) {
            return StoreCreditStatus.PAID;
        }

        return storeCredit.getDueDate().isBefore(today()) ? StoreCreditStatus.OVERDUE : StoreCreditStatus.PENDING_PAYMENT;
    }

    private BigDecimal calculateSubtotal(BigDecimal unitPrice, Integer quantity) {
        return unitPrice
                .multiply(BigDecimal.valueOf(quantity.longValue()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotal(List<StoreCreditItem> items) {
        return items.stream()
                .map(StoreCreditItem::getSubtotal)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalizedValue = value == null ? null : value.trim();
        if (normalizedValue == null || normalizedValue.isEmpty()) {
            throw new BusinessRuleException(fieldName + " is required");
        }
        return normalizedValue;
    }

    private LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}
