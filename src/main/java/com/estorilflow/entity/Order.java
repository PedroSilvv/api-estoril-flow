package com.estorilflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.estorilflow.exceptions.BusinessRuleException;
import com.estorilflow.support.ApplicationClock;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "customer_name_or_label")
    private String customerNameOrLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "opened_by_user_id")
    private Long openedByUserId;

    @Column(name = "closed_by_user_id")
    private Long closedByUserId;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Order open(
            String code,
            String customerNameOrLabel,
            Long openedByUserId,
            String notes,
            LocalDateTime openedAt
    ) {
        return Order.builder()
                .code(code)
                .customerNameOrLabel(normalizeNullable(customerNameOrLabel))
                .status(OrderStatus.OPEN)
                .openedByUserId(openedByUserId)
                .openedAt(openedAt)
                .notes(normalizeNullable(notes))
                .build();
    }

    public void updateHeader(String customerNameOrLabel, String notes) {
        ensureOpenForModification();

        this.customerNameOrLabel = normalizeNullable(customerNameOrLabel);
        this.notes = normalizeNullable(notes);
    }

    public void cancel() {
        if (status == OrderStatus.CLOSED) {
            throw new BusinessRuleException("Closed order cannot be cancelled");
        }
        if (status == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Order is already cancelled");
        }

        this.status = OrderStatus.CANCELLED;
    }

    public OrderItem createItem(Product product, Integer quantity) {
        ensureCanReceiveItems();
        return OrderItem.fromProduct(id, product, quantity);
    }

    public void updateItem(OrderItem item, Product product, Integer quantity) {
        ensureCanReceiveItems();
        item.changeProduct(product, quantity);
    }

    public void ensureCanChangeItems() {
        ensureCanReceiveItems();
    }

    public Sale close(List<OrderItem> items, Long closedByUserId, LocalDateTime closedAt) {
        ensureCanClose(items);

        this.status = OrderStatus.CLOSED;
        this.closedByUserId = closedByUserId;
        this.closedAt = closedAt;

        return Sale.fromClosedOrder(this, totalAmount(items), closedAt);
    }

    public static BigDecimal totalAmount(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = ApplicationClock.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = ApplicationClock.now();
    }

    private void ensureOpenForModification() {
        if (status != OrderStatus.OPEN) {
            throw new BusinessRuleException("Only open orders can be updated");
        }
    }

    private void ensureCanReceiveItems() {
        if (status == OrderStatus.CLOSED) {
            throw new BusinessRuleException("Order closed cannot receive new items");
        }
        if (status == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled order cannot be changed");
        }
    }

    private void ensureCanClose(List<OrderItem> items) {
        if (status == OrderStatus.CLOSED) {
            throw new BusinessRuleException("Order is already closed");
        }
        if (status == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled order cannot be closed");
        }
        if (items.isEmpty()) {
            throw new BusinessRuleException("Order cannot be closed without items");
        }
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
