package com.estorilflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.estorilflow.exceptions.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Product create(String name, BigDecimal price) {
        return Product.builder()
                .name(normalizeProductName(name))
                .price(price)
                .active(true)
                .build();
    }

    public void updateDetails(String name, BigDecimal price) {
        this.name = normalizeProductName(name);
        this.price = price;
    }

    public void activate(boolean active) {
        this.active = active;
    }

    public void ensureActivate() {
        if (!active) {
            throw new BusinessRuleException("Inactive product cannot be used in orders");
        }
    }

    public void ensureActiveForStoreCredits() {
        if (!active) {
            throw new BusinessRuleException("Inactive product cannot be used in store credits");
        }
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    private static String normalizeProductName(String name) {
        String normalizedName = name == null ? null : name.trim();
        if (normalizedName == null || normalizedName.isEmpty()) {
            throw new BusinessRuleException("name is required");
        }
        return normalizedName;
    }
}
