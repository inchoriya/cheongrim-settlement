package com.settlehub.order.api;

import com.settlehub.auth.security.AuthUser;
import com.settlehub.common.api.ApiResponse;
import com.settlehub.common.api.PageResponse;
import com.settlehub.order.domain.OrderStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ApiResponse<OrderResponse> create(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        return ApiResponse.ok(orderService.create(authUser, request));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<OrderUploadResponse> upload(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(value = "agencyId", required = false) Long agencyId,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.ok(orderService.upload(authUser, agencyId, file));
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> list(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(value = "merchantId", required = false) Long merchantId,
            @RequestParam(value = "status", required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "orderedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.ok(orderService.list(authUser, from, to, merchantId, status, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> get(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(orderService.get(authUser, id));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancel(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(orderService.cancel(authUser, id));
    }
}
