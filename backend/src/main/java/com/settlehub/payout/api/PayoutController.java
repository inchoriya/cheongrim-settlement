package com.settlehub.payout.api;

import com.settlehub.auth.security.AuthUser;
import com.settlehub.common.api.ApiResponse;
import com.settlehub.payout.domain.PayoutStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payouts")
@RequiredArgsConstructor
public class PayoutController {

    private final PayoutService payoutService;

    @PostMapping
    public ApiResponse<PayoutResponse> create(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PayoutCreateRequest request
    ) {
        return ApiResponse.ok(payoutService.requestPayout(authUser, request));
    }

    @GetMapping
    public ApiResponse<List<PayoutResponse>> list(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) Long settlementId,
            @RequestParam(required = false) PayoutStatus status
    ) {
        return ApiResponse.ok(payoutService.list(authUser, settlementId, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<PayoutResponse> get(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(payoutService.get(authUser, id));
    }
}
