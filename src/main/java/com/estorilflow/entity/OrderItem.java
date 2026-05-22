package com.estorilflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.estorilflow.support.ApplicationClock;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name_snapshot", nullable = false)
    private String productNameSnapshot;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static OrderItem fromProduct(Long orderId, Product product, Integer quantity) {
        product.ensureActivate();

        return OrderItem.builder()
                .orderId(orderId)
                .productId(product.getId())
                .productNameSnapshot(product.getName())
                .unitPrice(product.getPrice())
                .quantity(quantity)
                .subtotal(calculateSubtotal(product.getPrice(), quantity))
                .build();
    }

    public void changeProduct(Product product, Integer quantity) {
        product.ensureActivate();

        this.productId = product.getId();
        this.productNameSnapshot = product.getName();
        this.unitPrice = product.getPrice();
        this.quantity = quantity;
        this.subtotal = calculateSubtotal(product.getPrice(), quantity);
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

    private static BigDecimal calculateSubtotal(BigDecimal unitPrice, Integer quantity) {
        return unitPrice
                .multiply(BigDecimal.valueOf(quantity.longValue()))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
