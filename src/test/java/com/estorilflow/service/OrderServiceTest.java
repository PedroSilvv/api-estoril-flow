package com.estorilflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.estorilflow.dto.PageResponse;
import com.estorilflow.dto.OrderCreateRequest;
import com.estorilflow.dto.OrderItemCreateRequest;
import com.estorilflow.dto.OrderItemsCreateRequest;
import com.estorilflow.dto.OrderResponse;
import com.estorilflow.dto.OrderUpdateRequest;
import com.estorilflow.entity.Order;
import com.estorilflow.entity.OrderItem;
import com.estorilflow.entity.OrderStatus;
import com.estorilflow.entity.Product;
import com.estorilflow.entity.Sale;
import com.estorilflow.entity.SaleItem;
import com.estorilflow.exceptions.BusinessRuleException;
import com.estorilflow.repository.OrderItemRepository;
import com.estorilflow.repository.OrderRepository;
import com.estorilflow.repository.ProductRepository;
import com.estorilflow.repository.SaleItemRepository;
import com.estorilflow.repository.SaleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private SaleItemRepository saleItemRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                orderItemRepository,
                productRepository,
                saleRepository,
                saleItemRepository
        );
    }

    @Test
    void shouldCreateOpenOrder() {
        Order savedOrder = order(
                1L,
                "ORD-ABCD1234",
                "Mesa 12",
                OrderStatus.OPEN,
                7L,
                null,
                LocalDateTime.parse("2026-04-17T05:00:00"),
                null,
                "Sem gelo",
                LocalDateTime.parse("2026-04-17T05:00:00"),
                LocalDateTime.parse("2026-04-17T05:00:00")
        );

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.create(new OrderCreateRequest(" Mesa 12 ", " Sem gelo "), 7L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.code()).isEqualTo("ORD-ABCD1234");
        assertThat(response.status()).isEqualTo(OrderStatus.OPEN);
        assertThat(response.openedByUserId()).isEqualTo(7L);
        assertThat(response.customerNameOrLabel()).isEqualTo("Mesa 12");
        assertThat(response.itemCount()).isZero();
        assertThat(response.totalAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldReturnOrdersAsPaginatedResponse() {
        Order firstOrder = order(1L, "ORD-A", "Mesa 1", OrderStatus.OPEN, null, null, now(), null, null, now(), now());
        Order secondOrder = order(2L, "ORD-B", "Mesa 2", OrderStatus.CLOSED, null, null, now(), now(), null, now(), now());
        OrderItem firstItem = orderItem(10L, 1L, 1L, "Agua", "5.00", 2, "10.00");
        OrderItem secondItem = orderItem(11L, 2L, 2L, "Suco", "14.20", 1, "14.20");
        Pageable pageable = PageRequest.of(0, 10);

        when(orderRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Order>>any(), org.mockito.ArgumentMatchers.eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(firstOrder, secondOrder), pageable, 2));
        when(orderItemRepository.findAllByOrderIdIn(List.of(1L, 2L))).thenReturn(List.of(firstItem, secondItem));

        PageResponse<com.estorilflow.dto.OrderSummaryResponse> response = orderService.findAll(
                pageable,
                null,
                null,
                null
        );

        assertThat(response.content()).hasSize(2);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.numberOfElements()).isEqualTo(2);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
        assertThat(response.empty()).isFalse();
        assertThat(response.content().getFirst().itemCount()).isEqualTo(1);
        assertThat(response.content().getFirst().totalAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    void shouldRejectInvalidDateRangeWhenListingOrders() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> orderService.findAll(
                pageable,
                OrderStatus.OPEN,
                LocalDate.parse("2026-04-18"),
                LocalDate.parse("2026-04-17")
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("startDate cannot be after endDate");
    }

    @Test
    void shouldAddItemUsingCurrentProductSnapshot() {
        Order order = order(2L, "ORD-XYZ12345", null, OrderStatus.OPEN, 7L, null, now(), null, null, now(), now());
        Product product = product(10L, "Caipirinha", "25.90", true);
        OrderItem createdItem = orderItem(20L, 2L, 10L, "Caipirinha", "25.90", 2, "51.80");

        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(createdItem);
        when(orderItemRepository.findAllByOrderIdOrderByIdAsc(2L)).thenReturn(List.of(createdItem));

        OrderResponse response = orderService.addItem(2L, new OrderItemCreateRequest(10L, 2));

        assertThat(response.itemCount()).isEqualTo(1);
        assertThat(response.totalAmount()).isEqualByComparingTo("51.80");
        assertThat(response.items().getFirst().productNameSnapshot()).isEqualTo("Caipirinha");
        assertThat(response.items().getFirst().subtotal()).isEqualByComparingTo("51.80");
    }

    @Test
    void shouldAddMultipleItemsUsingCurrentProductSnapshots() {
        Order order = order(2L, "ORD-XYZ12345", null, OrderStatus.OPEN, 7L, null, now(), null, null, now(), now());
        Product firstProduct = product(10L, "Caipirinha", "25.90", true);
        Product secondProduct = product(11L, "Suco", "12.50", true);
        OrderItem firstItem = orderItem(20L, 2L, 10L, "Caipirinha", "25.90", 2, "51.80");
        OrderItem secondItem = orderItem(21L, 2L, 11L, "Suco", "12.50", 1, "12.50");

        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));
        when(productRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(firstProduct, secondProduct));
        when(orderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findAllByOrderIdOrderByIdAsc(2L)).thenReturn(List.of(firstItem, secondItem));

        OrderResponse response = orderService.addItems(2L, new OrderItemsCreateRequest(List.of(
                new OrderItemCreateRequest(10L, 2),
                new OrderItemCreateRequest(11L, 1)
        )));

        assertThat(response.itemCount()).isEqualTo(2);
        assertThat(response.totalAmount()).isEqualByComparingTo("64.30");
        assertThat(response.items()).extracting(com.estorilflow.dto.OrderItemResponse::productNameSnapshot)
                .containsExactly("Caipirinha", "Suco");

        ArgumentCaptor<List<OrderItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(orderItemRepository).saveAll(itemsCaptor.capture());
        assertThat(itemsCaptor.getValue()).hasSize(2);
        assertThat(itemsCaptor.getValue().getFirst().getSubtotal()).isEqualByComparingTo("51.80");
        assertThat(itemsCaptor.getValue().get(1).getSubtotal()).isEqualByComparingTo("12.50");
    }

    @Test
    void shouldRejectItemChangeWhenOrderIsClosed() {
        Order order = order(3L, "ORD-CLOSED", null, OrderStatus.CLOSED, 7L, 8L, now(), now(), null, now(), now());
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.addItem(3L, new OrderItemCreateRequest(10L, 1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Order closed cannot receive new items");
    }

    @Test
    void shouldUpdateOrderHeaderOnlyWhenOpen() {
        Order order = order(4L, "ORD-OPEN", null, OrderStatus.OPEN, 7L, null, now(), null, null, now(), now());
        when(orderRepository.findById(4L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderItemRepository.findAllByOrderIdOrderByIdAsc(4L)).thenReturn(List.of());

        OrderResponse response = orderService.update(4L, new OrderUpdateRequest(" Cliente XPTO ", " Observacao "));

        assertThat(response.customerNameOrLabel()).isEqualTo("Cliente XPTO");
        assertThat(response.notes()).isEqualTo("Observacao");
    }

    @Test
    void shouldRejectCloseWithoutItems() {
        Order order = order(5L, "ORD-EMPTY", null, OrderStatus.OPEN, 7L, null, now(), null, null, now(), now());
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderIdOrderByIdAsc(5L)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.close(5L, 9L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Order cannot be closed without items");
    }

    @Test
    void shouldCloseOrderAndGenerateSaleSnapshot() {
        Order order = order(6L, "ORD-CLOSE", "Mesa 9", OrderStatus.OPEN, 7L, null, now(), null, null, now(), now());
        OrderItem item = orderItem(30L, 6L, 10L, "Suco", "12.50", 2, "25.00");
        Sale savedSale = Sale.builder()
                .id(100L)
                .orderId(6L)
                .totalAmount(new BigDecimal("25.00"))
                .soldAt(now())
                .openedByUserId(7L)
                .closedByUserId(9L)
                .build();

        when(orderRepository.findById(6L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderIdOrderByIdAsc(6L)).thenReturn(List.of(item));
        when(saleRepository.save(any(Sale.class))).thenReturn(savedSale);
        when(saleItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(order)).thenReturn(order);

        OrderResponse response = orderService.close(6L, 9L);

        assertThat(response.status()).isEqualTo(OrderStatus.CLOSED);
        assertThat(response.closedByUserId()).isEqualTo(9L);
        assertThat(response.totalAmount()).isEqualByComparingTo("25.00");

        ArgumentCaptor<Sale> saleCaptor = ArgumentCaptor.forClass(Sale.class);
        verify(saleRepository).save(saleCaptor.capture());
        assertThat(saleCaptor.getValue().getOrderId()).isEqualTo(6L);
        assertThat(saleCaptor.getValue().getTotalAmount()).isEqualByComparingTo("25.00");

        ArgumentCaptor<List<SaleItem>> saleItemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(saleItemRepository).saveAll(saleItemsCaptor.capture());
        assertThat(saleItemsCaptor.getValue()).hasSize(1);
        assertThat(saleItemsCaptor.getValue().getFirst().getProductNameSnapshot()).isEqualTo("Suco");
    }

    private Order order(
            Long id,
            String code,
            String customerNameOrLabel,
            OrderStatus status,
            Long openedByUserId,
            Long closedByUserId,
            LocalDateTime openedAt,
            LocalDateTime closedAt,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return Order.builder()
                .id(id)
                .code(code)
                .customerNameOrLabel(customerNameOrLabel)
                .status(status)
                .openedByUserId(openedByUserId)
                .closedByUserId(closedByUserId)
                .openedAt(openedAt)
                .closedAt(closedAt)
                .notes(notes)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    private OrderItem orderItem(
            Long id,
            Long orderId,
            Long productId,
            String productNameSnapshot,
            String unitPrice,
            Integer quantity,
            String subtotal
    ) {
        return OrderItem.builder()
                .id(id)
                .orderId(orderId)
                .productId(productId)
                .productNameSnapshot(productNameSnapshot)
                .unitPrice(new BigDecimal(unitPrice))
                .quantity(quantity)
                .subtotal(new BigDecimal(subtotal))
                .createdAt(now())
                .updatedAt(now())
                .build();
    }

    private Product product(Long id, String name, String price, boolean active) {
        return Product.builder()
                .id(id)
                .name(name)
                .price(new BigDecimal(price))
                .active(active)
                .build();
    }

    private LocalDateTime now() {
        return LocalDateTime.parse("2026-04-17T05:00:00");
    }
}
