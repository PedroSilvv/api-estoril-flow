package com.estorilflow.repository;

import com.estorilflow.entity.OrderItem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findAllByOrderIdOrderByIdAsc(Long orderId);

    List<OrderItem> findAllByOrderIdIn(Collection<Long> orderIds);

    Optional<OrderItem> findByIdAndOrderId(Long id, Long orderId);
}
