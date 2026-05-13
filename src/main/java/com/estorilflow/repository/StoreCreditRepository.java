package com.estorilflow.repository;

import com.estorilflow.entity.StoreCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StoreCreditRepository extends JpaRepository<StoreCredit, Long>, JpaSpecificationExecutor<StoreCredit> {
}
