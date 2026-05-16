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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
@Table(name = "store_credits")
public class StoreCredit {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "buyer_name", nullable = false, length = 255)
    private String buyerName;

    @Column(name = "buyer_phone", nullable = false, length = 50)
    private String buyerPhone;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "credit_date", nullable = false)
    private LocalDate creditDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StoreCreditStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static StoreCredit open(
            String buyerName,
            String buyerPhone,
            BigDecimal requestedAmount,
            List<StoreCreditItem> items,
            LocalDate creditDate,
            LocalDate dueDate,
            StoreCreditStatus requestedStatus,
            LocalDate today
    ) {
        validateDueDate(creditDate, dueDate);

        BigDecimal calculatedAmount = totalAmount(items);
        validateAmount(requestedAmount, calculatedAmount);

        return StoreCredit.builder()
                .buyerName(normalizeRequired(buyerName, "buyerName"))
                .buyerPhone(normalizeRequired(buyerPhone, "buyerPhone"))
                .amount(calculatedAmount)
                .creditDate(creditDate)
                .dueDate(dueDate)
                .status(resolveStatus(requestedStatus, dueDate, today))
                .build();
    }

    public void update(
            String buyerName,
            String buyerPhone,
            BigDecimal requestedAmount,
            List<StoreCreditItem> items,
            LocalDate creditDate,
            LocalDate dueDate,
            StoreCreditStatus requestedStatus,
            LocalDate today
    ) {
        validateDueDate(creditDate, dueDate);

        BigDecimal calculatedAmount = totalAmount(items);
        validateAmount(requestedAmount, calculatedAmount);

        this.buyerName = normalizeRequired(buyerName, "buyerName");
        this.buyerPhone = normalizeRequired(buyerPhone, "buyerPhone");
        this.amount = calculatedAmount;
        this.creditDate = creditDate;
        this.dueDate = dueDate;
        this.status = resolveStatus(requestedStatus, dueDate, today);
    }

    public StoreCreditStatus effectiveStatus(LocalDate today) {
        if (status == StoreCreditStatus.PAID) {
            return StoreCreditStatus.PAID;
        }

        return dueDate.isBefore(today) ? StoreCreditStatus.OVERDUE : StoreCreditStatus.PENDING_PAYMENT;
    }

    public static BigDecimal totalAmount(List<StoreCreditItem> items) {
        return items.stream()
                .map(StoreCreditItem::getSubtotal)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
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

    private static void validateDueDate(LocalDate creditDate, LocalDate dueDate) {
        if (creditDate.isAfter(dueDate)) {
            throw new BusinessRuleException("creditDate cannot be after dueDate");
        }
    }

    private static void validateAmount(BigDecimal requestedAmount, BigDecimal calculatedAmount) {
        if (requestedAmount.setScale(2, RoundingMode.HALF_UP).compareTo(calculatedAmount) != 0) {
            throw new BusinessRuleException("amount must match the sum of linked items");
        }
    }

    private static StoreCreditStatus resolveStatus(
            StoreCreditStatus requestedStatus,
            LocalDate dueDate,
            LocalDate today
    ) {
        if (requestedStatus == StoreCreditStatus.PAID) {
            return StoreCreditStatus.PAID;
        }

        return dueDate.isBefore(today) ? StoreCreditStatus.OVERDUE : StoreCreditStatus.PENDING_PAYMENT;
    }

    private static String normalizeRequired(String value, String fieldName) {
        String normalizedValue = value == null ? null : value.trim();
        if (normalizedValue == null || normalizedValue.isEmpty()) {
            throw new BusinessRuleException(fieldName + " is required");
        }
        return normalizedValue;
    }
}
