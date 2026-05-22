package com.estorilflow.service;

import com.estorilflow.adapter.OrderResponseAdapter;
import com.estorilflow.dto.OrderCreateRequest;
import com.estorilflow.dto.OrderItemCreateRequest;
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
import com.estorilflow.support.ApplicationClock;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        Order order = Order.open(
                generateCode(),
                request.customerNameOrLabel(),
                openedByUserId,
                request.notes(),
                ApplicationClock.now()
        );

        Order savedOrder = orderRepository.save(order);
        return OrderResponseAdapter.toResponse(savedOrder, List.of());
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
            return PageResponse.from(orderPage.map(order -> OrderResponseAdapter.toSummaryResponse(order, List.of())));
        }

        List<OrderItem> items = orderItemRepository.findAllByOrderIdIn(
                orderPage.getContent().stream().map(Order::getId).toList()
        );

        Map<Long, List<OrderItem>> itemsByOrderId = items.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        return PageResponse.from(orderPage.map(order
                -> OrderResponseAdapter.toSummaryResponse(order, itemsByOrderId.getOrDefault(order.getId(), List.of()))));
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
        return OrderResponseAdapter.toResponse(order, items);
    }

    @Transactional
    public OrderResponse update(Long id, OrderUpdateRequest request) {
        Order order = getOrderById(id);
        order.updateHeader(request.customerNameOrLabel(), request.notes());

        Order savedOrder = orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findAllByOrderIdOrderByIdAsc(savedOrder.getId());
        return OrderResponseAdapter.toResponse(savedOrder, items);
    }

    @Transactional
    public OrderResponse cancel(Long id) {
        Order order = getOrderById(id);
        order.cancel();
        Order savedOrder = orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findAllByOrderIdOrderByIdAsc(savedOrder.getId());
        return OrderResponseAdapter.toResponse(savedOrder, items);
    }

    @Transactional
    public OrderResponse addItem(Long orderId, OrderItemCreateRequest request) {
        Order order = getOrderById(orderId);
        order.ensureCanChangeItems();

        Product product = getProductById(request.productId());
        OrderItem item = order.createItem(product, request.quantity());

        orderItemRepository.save(item);
        List<OrderItem> items = orderItemRepository.findAllByOrderIdOrderByIdAsc(order.getId());
        return OrderResponseAdapter.toResponse(order, items);
    }

    @Transactional
    public OrderResponse updateItem(Long orderId, Long itemId, OrderItemUpdateRequest request) {
        Order order = getOrderById(orderId);
        order.ensureCanChangeItems();

        OrderItem item = orderItemRepository.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order item not found with id " + itemId + " for order " + orderId
                ));

        Product product = getProductById(request.productId());
        order.updateItem(item, product, request.quantity());

        orderItemRepository.save(item);
        List<OrderItem> items = orderItemRepository.findAllByOrderIdOrderByIdAsc(order.getId());
        return OrderResponseAdapter.toResponse(order, items);
    }

    @Transactional
    public void removeItem(Long orderId, Long itemId) {
        Order order = getOrderById(orderId);
        order.ensureCanChangeItems();

        OrderItem item = orderItemRepository.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order item not found with id " + itemId + " for order " + orderId
                ));

        orderItemRepository.delete(item);
    }

    @Transactional
    public OrderResponse close(Long orderId, Long closedByUserId) {
        Order order = getOrderById(orderId);

        List<OrderItem> items = orderItemRepository.findAllByOrderIdOrderByIdAsc(orderId);
        LocalDateTime now = ApplicationClock.now();
        Sale sale = order.close(items, closedByUserId, now);

        Sale savedSale = saleRepository.save(sale);
        List<SaleItem> saleItems = items.stream()
                .map(item -> SaleItem.fromOrderItem(savedSale.getId(), item))
                .toList();
        saleItemRepository.saveAll(saleItems);

        Order savedOrder = orderRepository.save(order);
        return OrderResponseAdapter.toResponse(savedOrder, items);
    }

    private Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + id));
    }

    private Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + productId));
    }

    private String generateCode() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
