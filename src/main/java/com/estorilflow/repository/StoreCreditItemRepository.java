package com.estorilflow.repository;

import com.estorilflow.entity.StoreCreditItem;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreCreditItemRepository extends JpaRepository<StoreCreditItem, Long> {

    List<StoreCreditItem> findAllByStoreCreditIdOrderByIdAsc(Long storeCreditId);

    List<StoreCreditItem> findAllByStoreCreditIdIn(Collection<Long> storeCreditIds);

    void deleteAllByStoreCreditId(Long storeCreditId);
}
