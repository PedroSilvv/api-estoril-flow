package com.estorilflow.service;

import com.estorilflow.adapter.SaleResponseAdapter;
import com.estorilflow.dto.PageResponse;
import com.estorilflow.dto.SaleResponse;
import com.estorilflow.dto.SaleSummaryResponse;
import com.estorilflow.entity.Sale;
import com.estorilflow.entity.SaleItem;
import com.estorilflow.exceptions.BusinessRuleException;
import com.estorilflow.exceptions.ResourceNotFoundException;
import com.estorilflow.repository.SaleItemRepository;
import com.estorilflow.repository.SaleRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;

    public SaleService(SaleRepository saleRepository, SaleItemRepository saleItemRepository) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleSummaryResponse> findAll(
            Pageable pageable,
            Long orderId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);

        Page<Sale> salePage = saleRepository.findAll(buildSpecification(orderId, startDate, endDate), pageable);
        if (salePage.isEmpty()) {
            return PageResponse.from(salePage.map(sale -> SaleResponseAdapter.toSummaryResponse(sale, List.of())));
        }

        List<SaleItem> items = saleItemRepository.findAllBySaleIdIn(
                salePage.getContent().stream().map(Sale::getId).toList()
        );

        Map<Long, List<SaleItem>> itemsBySaleId = items.stream()
                .collect(Collectors.groupingBy(SaleItem::getSaleId));

        return PageResponse.from(salePage.map(sale
                -> SaleResponseAdapter.toSummaryResponse(sale, itemsBySaleId.getOrDefault(sale.getId(), List.of()))));
    }

    @Transactional(readOnly = true)
    public SaleResponse findById(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id " + id));

        List<SaleItem> items = saleItemRepository.findAllBySaleIdOrderByIdAsc(id);
        return SaleResponseAdapter.toResponse(sale, items);
    }

    private Specification<Sale> buildSpecification(Long orderId, LocalDate startDate, LocalDate endDate) {
        Specification<Sale> specification = Specification.unrestricted();

        if (orderId != null) {
            specification = specification.and((root, query, criteriaBuilder)
                    -> criteriaBuilder.equal(root.get("orderId"), orderId));
        }

        if (startDate != null) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            specification = specification.and((root, query, criteriaBuilder)
                    -> criteriaBuilder.greaterThanOrEqualTo(root.get("soldAt"), startDateTime));
        }

        if (endDate != null) {
            LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
            specification = specification.and((root, query, criteriaBuilder)
                    -> criteriaBuilder.lessThan(root.get("soldAt"), endDateTime));
        }

        return specification;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessRuleException("startDate cannot be after endDate");
        }
    }

}
