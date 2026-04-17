package com.estorilflow.repository;

import com.estorilflow.entity.SaleItem;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    List<SaleItem> findAllBySaleIdOrderByIdAsc(Long saleId);

    List<SaleItem> findAllBySaleIdIn(Collection<Long> saleIds);
}
