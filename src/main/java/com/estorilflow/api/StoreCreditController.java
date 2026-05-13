package com.estorilflow.api;

import com.estorilflow.dto.PageResponse;
import com.estorilflow.dto.StoreCreditCreateRequest;
import com.estorilflow.dto.StoreCreditResponse;
import com.estorilflow.dto.StoreCreditSummaryResponse;
import com.estorilflow.dto.StoreCreditUpdateRequest;
import com.estorilflow.entity.StoreCreditStatus;
import com.estorilflow.service.StoreCreditService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/store-credits")
public class StoreCreditController {

    private final StoreCreditService storeCreditService;

    public StoreCreditController(StoreCreditService storeCreditService) {
        this.storeCreditService = storeCreditService;
    }

    @PostMapping
    public ResponseEntity<StoreCreditResponse> create(@Valid @RequestBody StoreCreditCreateRequest request) {
        StoreCreditResponse response = storeCreditService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<StoreCreditSummaryResponse>> findAll(
            @RequestParam(required = false) StoreCreditStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 10, sort = {"creditDate", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(storeCreditService.findAll(pageable, status, startDate, endDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreCreditResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(storeCreditService.findById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<StoreCreditResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody StoreCreditUpdateRequest request
    ) {
        return ResponseEntity.ok(storeCreditService.update(id, request));
    }
}
