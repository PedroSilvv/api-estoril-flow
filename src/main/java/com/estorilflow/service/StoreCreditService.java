package com.estorilflow.service;

import com.estorilflow.adapter.StoreCreditResponseAdapter;
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
import com.estorilflow.exceptions.ResourceNotFoundException;
import com.estorilflow.repository.ProductRepository;
import com.estorilflow.repository.StoreCreditItemRepository;
import com.estorilflow.repository.StoreCreditRepository;
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
        List<StoreCreditItem> items = buildItems(request.items());
        StoreCredit storeCredit = StoreCredit.open(
                request.buyerName(),
                request.buyerPhone(),
                request.amount(),
                items,
                request.creditDate(),
                request.dueDate(),
                request.status(),
                today()
        );

        StoreCredit savedStoreCredit = storeCreditRepository.save(storeCredit);
        List<StoreCreditItem> itemsToSave = items.stream()
                .peek(item -> item.linkToStoreCredit(savedStoreCredit.getId()))
                .toList();

        List<StoreCreditItem> savedItems = storeCreditItemRepository.saveAll(itemsToSave);
        return StoreCreditResponseAdapter.toResponse(savedStoreCredit, savedItems, today());
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
            return PageResponse.from(storeCreditPage.map(storeCredit
                    -> StoreCreditResponseAdapter.toSummaryResponse(storeCredit, List.of(), today())));
        }

        List<StoreCreditItem> items = storeCreditItemRepository.findAllByStoreCreditIdIn(
                storeCreditPage.getContent().stream().map(StoreCredit::getId).toList()
        );
        Map<Long, List<StoreCreditItem>> itemsByStoreCreditId = items.stream()
                .collect(Collectors.groupingBy(StoreCreditItem::getStoreCreditId));

        return PageResponse.from(storeCreditPage.map(storeCredit -> StoreCreditResponseAdapter.toSummaryResponse(
                storeCredit,
                itemsByStoreCreditId.getOrDefault(storeCredit.getId(), List.of()),
                today()
        )));
    }

    @Transactional(readOnly = true)
    public StoreCreditResponse findById(Long id) {
        StoreCredit storeCredit = getStoreCreditById(id);
        List<StoreCreditItem> items = storeCreditItemRepository.findAllByStoreCreditIdOrderByIdAsc(id);
        return StoreCreditResponseAdapter.toResponse(storeCredit, items, today());
    }

    @Transactional
    public StoreCreditResponse update(Long id, StoreCreditUpdateRequest request) {
        StoreCredit storeCredit = getStoreCreditById(id);
        List<StoreCreditItem> items = buildItems(request.items());
        storeCredit.update(
                request.buyerName(),
                request.buyerPhone(),
                request.amount(),
                items,
                request.creditDate(),
                request.dueDate(),
                request.status(),
                today()
        );

        StoreCredit savedStoreCredit = storeCreditRepository.save(storeCredit);

        storeCreditItemRepository.deleteAllByStoreCreditId(savedStoreCredit.getId());
        List<StoreCreditItem> itemsToSave = items.stream()
                .peek(item -> item.linkToStoreCredit(savedStoreCredit.getId()))
                .toList();
        List<StoreCreditItem> savedItems = storeCreditItemRepository.saveAll(itemsToSave);

        return StoreCreditResponseAdapter.toResponse(savedStoreCredit, savedItems, today());
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
        Product product = getProductById(request.productId());

        return StoreCreditItem.fromProduct(product, request.quantity());
    }

    private Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + productId));
    }

    private LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}
