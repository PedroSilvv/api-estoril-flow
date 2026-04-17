package com.estorilflow.service;

import com.estorilflow.dto.OrderCreateRequest;
import com.estorilflow.dto.OrderItemCreateRequest;
import com.estorilflow.dto.OrderItemResponse;
import com.estorilflow.dto.OrderItemUpdateRequest;
import com.estorilflow.dto.OrderResponse;
import com.estorilflow.dto.OrderSummaryResponse;
import com.estorilflow.dto.OrderUpdateRequest;
import com.estorilflow.dto.PageResponse;
import com.estorilflow.entity.Order;
import com.estorilflow.entity.OrderItem;
import com.estorilflow.entity.OrderStatus;
import com.estorilflow.entity.Product;
import com.estorilflow.entity.Sale;
import com.estorilflow.entity.SaleItem;
import com.estorilflow.exceptions.BusinessRuleException;
import com.estorilflow.exceptions.ResourceNotFoundException;
import com.estorilflow.repository.OrderItemRepository;
import com.estorilflow.repository.OrderRepository;
import com.estorilflow.repository.ProductRepository;
import com.estorilflow.repository.SaleItemRepository;
import com.estorilflow.repository.SaleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            SaleRepository saleRepository,
            SaleItemRepository saleItemRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
    }

    @Transactional
    public OrderResponse create(OrderCreateRequest request, Long openedByUserId) {
        Order order = Order.builder()
                .code(generateCode())
                .customerNameOrLabel(normalizeNullable(request.customerNameOrLabel()))
                .status(OrderStatus.OPEN)
                .openedByUserId(openedByUserId)
                .openedAt(LocalDateTime.now(ZoneOffset.UTC))
                .notes(normalizeNullable(request.notes()))
                .build();

        Order savedOrder = orderRepository.save(order);
        return toOrderResponse(savedOrder, List.of());
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> findAll(
            Pageable pageable,
            OrderStatus status,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);

        Page<Order> orderPage = orderRepository.findAll(buildSpecification(status, startDate, endDate), pageable);
        if (orderPage.isEmpty()) {
            return PageResponse.from(orderPage.map(order -> toOrderSummaryResponse(order, List.of())));
        }

        List<OrderItem> items = orderItemRepository.findAllByOrderIdIn(
                orderPage.getContent().stream().map(Order::getId).toList()
        );

        Map<Long, List<OrderItem>> itemsByOrderId = items.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        return PageResponse.from(orderPage.map(order
                -> toOrderSummaryResponse(order, itemsByOrderId.getOrDefault(order.getId(), List.of()))));
    }

    private Specification<Order> buildSpecification(
            OrderStatus status,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Specification<Order> specification = Specification.unrestricted();

        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder)
                    -> criteriaBuilder.equal(root.get("status"), status));
        }

        if (startDate != null) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            specification = specification.and((root, query, criteriaBuilder)
                    -> criteriaBuilder.greaterThanOrEqualTo(root.get("openedAt"), startDateTime));
        }

        if (endDate != null) {
            LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
            specification = specification.and((root, query, criteriaBuilder)
                    -> criteriaBuilder.lessThan(root.get("openedAt"), endDateTime));
        }

        return specification;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessRuleException("startDate cannot be after endDate");
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        Order order = getOrderById(id);
        List<OrderItem> items = orderItemRepository.findAllByOrderIdOrderByIdAsc(order.getId());
        return toOrderResponse(order, items);
    }

    @Transactional
    public OrderResponse update(Long id, OrderUpdateRequest request) {
        Order order = getOrderById(id);
        ensureOrderIsOpenForModification(order);

        order.setCustomerNameOrLabel(normalizeNullable(request.customerNameOrLabel()));
        order.setNotes(normalizeNullable(request.notes()));

        Order savedOrder = orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findAllByOrderIdOrderByIdAsc(savedOrder.getId());
        return toOrderResponse(savedOrder, items);
    }

    @Transactional
    public OrderResponse cancel(Long id) {
        Order order = getOrderById(id);

        if (order.getStatus() == OrderStatus.CLOSED) {
            throw new BusinessRuleException("Closed order cannot be cancelled");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Order is already cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findAllByOrderIdOrderByIdAsc(savedOrder.getId());
        return toOrderResponse(savedOrder, items);
    }

    @Transactional
    public OrderResponse addItem(Long orderId, OrderItemCreateRequest request) {
        Order order = getOrderById(orderId);
        ensureOrderCanReceiveItems(order);

        Product product = getActiveProduct(request.productId());
        OrderItem item = OrderItem.builder()
                .orderId(order.getId())
                .productId(product.getId())
                .productNameSnapshot(product.getName())
                .unitPrice(product.getPrice())
                .quantity(request.quantity())
                .subtotal(calculateSubtotal(product.getPrice(), request.quantity()))
                .build();

        orderItemRepository.save(item);
        List<OrderItem> items = orderItemRepository.findAllByOrderIdOrderByIdAsc(order.getId());
        return toOrderResponse(order, items);
    }

    @Transactional
    public OrderResponse updateItem(Long orderId, Long itemId, OrderItemUpdateRequest request) {
        Order order = getOrderById(orderId);
        ensureOrderCanReceiveItems(order);

        OrderItem item = orderItemRepository.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order item not found with id " + itemId + " for order " + orderId
                ));

        Product product = getActiveProduct(request.productId());
        item.setProductId(product.getId());
        item.setProductNameSnapshot(product.getName());
        item.setUnitPrice(product.getPrice());
        item.setQuantity(request.quantity());
        item.setSubtotal(calculateSubtotal(product.getPrice(), request.quantity()));

        orderItemRepository.save(item);
        List<OrderItem> items = orderItemRepository.findAllByOrderIdOrderByIdAsc(order.getId());
        return toOrderResponse(order, items);
    }

    @Transactional
    public void removeItem(Long orderId, Long itemId) {
        Order order = getOrderById(orderId);
        ensureOrderCanReceiveItems(order);

        OrderItem item = orderItemRepository.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order item not found with id " + itemId + " for order " + orderId
                ));

        orderItemRepository.delete(item);
    }

    @Transactional
    public OrderResponse close(Long orderId, Long closedByUserId) {
        Order order = getOrderById(orderId);

        if (order.getStatus() == OrderStatus.CLOSED) {
            throw new BusinessRuleException("Order is already closed");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled order cannot be closed");
        }

        List<OrderItem> items = orderItemRepository.findAllByOrderIdOrderByIdAsc(orderId);
        if (items.isEmpty()) {
            throw new BusinessRuleException("Order cannot be closed without items");
        }

        BigDecimal totalAmount = calculateTotal(items);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        Sale sale = Sale.builder()
                .orderId(order.getId())
                .totalAmount(totalAmount)
                .soldAt(now)
                .openedByUserId(order.getOpenedByUserId())
                .closedByUserId(closedByUserId)
                .build();

        Sale savedSale = saleRepository.save(sale);
        List<SaleItem> saleItems = items.stream()
                .map(item -> SaleItem.builder()
                        .saleId(savedSale.getId())
                        .productId(item.getProductId())
                        .productNameSnapshot(item.getProductNameSnapshot())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();
        saleItemRepository.saveAll(saleItems);

        order.setStatus(OrderStatus.CLOSED);
        order.setClosedByUserId(closedByUserId);
        order.setClosedAt(now);

        Order savedOrder = orderRepository.save(order);
        return toOrderResponse(savedOrder, items);
    }

    private Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + id));
    }

    private Product getActiveProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + productId));

        if (!product.isActive()) {
            throw new BusinessRuleException("Inactive product cannot be used in orders");
        }

        return product;
    }

    private void ensureOrderIsOpenForModification(Order order) {
        if (order.getStatus() != OrderStatus.OPEN) {
            throw new BusinessRuleException("Only open orders can be updated");
        }
    }

    private void ensureOrderCanReceiveItems(Order order) {
        if (order.getStatus() == OrderStatus.CLOSED) {
            throw new BusinessRuleException("Order closed cannot receive new items");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled order cannot be changed");
        }
    }

    private OrderSummaryResponse toOrderSummaryResponse(Order order, List<OrderItem> items) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getCode(),
                order.getCustomerNameOrLabel(),
                order.getStatus(),
                order.getOpenedByUserId(),
                order.getClosedByUserId(),
                order.getOpenedAt(),
                order.getClosedAt(),
                order.getNotes(),
                items.size(),
                calculateTotal(items),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private OrderResponse toOrderResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(this::toOrderItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCode(),
                order.getCustomerNameOrLabel(),
                order.getStatus(),
                order.getOpenedByUserId(),
                order.getClosedByUserId(),
                order.getOpenedAt(),
                order.getClosedAt(),
                order.getNotes(),
                itemResponses.size(),
                calculateTotal(items),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemResponses
        );
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductNameSnapshot(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private BigDecimal calculateSubtotal(BigDecimal unitPrice, Integer quantity) {
        return unitPrice
                .multiply(BigDecimal.valueOf(quantity.longValue()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateCode() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
