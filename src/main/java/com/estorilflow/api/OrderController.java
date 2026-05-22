package com.estorilflow.api;

import com.estorilflow.dto.OrderCreateRequest;
import com.estorilflow.dto.OrderItemCreateRequest;
import com.estorilflow.dto.OrderItemUpdateRequest;
import com.estorilflow.dto.PageResponse;
import com.estorilflow.dto.OrderResponse;
import com.estorilflow.dto.OrderSummaryResponse;
import com.estorilflow.dto.OrderUpdateRequest;
import com.estorilflow.entity.OrderStatus;
import com.estorilflow.service.OrderService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request) {
        OrderResponse response = orderService.create(request, null);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderSummaryResponse>> findAll(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 10, sort = {"openedAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(orderService.findAll(pageable, status, startDate, endDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OrderResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody OrderUpdateRequest request
    ) {
        return ResponseEntity.ok(orderService.update(id, request));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancel(id));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<OrderResponse> addItem(
            @PathVariable Long id,
            @Valid @RequestBody OrderItemCreateRequest request
    ) {
        log.info("Received request to add item {}", request);
        return ResponseEntity.ok(orderService.addItem(id, request));
    }

    @PutMapping("/{id}/items/{itemId}")
    public ResponseEntity<OrderResponse> updateItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody OrderItemUpdateRequest request
    ) {
        return ResponseEntity.ok(orderService.updateItem(id, itemId, request));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long id,
            @PathVariable Long itemId
    ) {
        orderService.removeItem(id, itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<OrderResponse> close(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.close(id, null));
    }
}
