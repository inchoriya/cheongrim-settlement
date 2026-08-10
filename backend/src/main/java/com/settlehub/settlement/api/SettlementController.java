package com.settlehub.settlement.api;

import com.settlehub.auth.security.AuthUser;
import com.settlehub.common.api.ApiResponse;
import com.settlehub.common.api.PageResponse;
import com.settlehub.settlement.domain.SettlementStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementBatchService settlementBatchService;
    private final SettlementService settlementService;

    @PostMapping("/settlements/batch")
    public ApiResponse<SettlementBatchResponse> runBatch(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody SettlementBatchRequest request
    ) {
        return ApiResponse.ok(settlementBatchService.runBatch(authUser, request));
    }

    @GetMapping("/batch-jobs/{id}")
    public ApiResponse<SettlementBatchResponse> getBatchJob(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(settlementBatchService.getBatchJob(authUser, id));
    }

    @GetMapping("/settlements")
    public ApiResponse<PageResponse<SettlementResponse>> list(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(required = false) Long agencyId,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime periodStart,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime periodEnd,
            @PageableDefault(size = 20, sort = "periodStart", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.ok(settlementService.list(
                authUser, status, agencyId, merchantId, periodStart, periodEnd, pageable
        ));
    }

    @GetMapping("/settlements/{id}")
    public ApiResponse<SettlementResponse> get(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(settlementService.get(authUser, id));
    }

    @PostMapping("/settlements/{id}/hold")
    public ApiResponse<SettlementResponse> hold(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @Valid @RequestBody SettlementHoldRequest request
    ) {
        return ApiResponse.ok(settlementService.hold(authUser, id, request));
    }

    @PostMapping("/settlements/{id}/confirm")
    public ApiResponse<SettlementResponse> confirm(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(settlementService.confirm(authUser, id));
    }

    @PostMapping("/settlements/{id}/ready-for-payout")
    public ApiResponse<SettlementResponse> readyForPayout(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(settlementService.readyForPayout(authUser, id));
    }
}
