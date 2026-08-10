package com.settlehub.policy.api;

import com.settlehub.auth.security.AuthUser;
import com.settlehub.common.api.ApiResponse;
import com.settlehub.settlement.domain.SettlementBreakdown;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class FeePolicyController {

    private final FeePolicyService feePolicyService;

    @PostMapping
    public ApiResponse<FeePolicyResponse> create(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody FeePolicyRequest request
    ) {
        return ApiResponse.ok(feePolicyService.create(authUser, request));
    }

    @GetMapping
    public ApiResponse<List<FeePolicyResponse>> list(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(feePolicyService.list(authUser));
    }

    @GetMapping("/resolve")
    public ApiResponse<FeePolicyResponse> resolve(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam Long agencyId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime orderedAt
    ) {
        return ApiResponse.ok(feePolicyService.resolve(authUser, agencyId, orderedAt));
    }

    @PostMapping("/preview")
    public ApiResponse<Map<String, Object>> preview(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PreviewRequest request
    ) {
        SettlementBreakdown breakdown = feePolicyService.preview(
                authUser,
                request.orderAmount(),
                request.deliveryTip() == null ? 0L : request.deliveryTip(),
                request.agencyId(),
                request.orderedAt()
        );
        return ApiResponse.ok(Map.of(
                "orderAmount", breakdown.orderAmount(),
                "deliveryTip", breakdown.deliveryTip(),
                "platformFeeAmount", breakdown.platformFeeAmount(),
                "agencyFeeAmount", breakdown.agencyFeeAmount(),
                "riderFeeAmount", breakdown.riderFeeAmount(),
                "merchantSettlementAmount", breakdown.merchantSettlementAmount(),
                "tipToRiderAmount", breakdown.tipToRiderAmount(),
                "anomalyFlag", breakdown.anomalyFlag().map(Enum::name).orElse("")
        ));
    }

    public record PreviewRequest(
            @NotNull @Min(0) Long orderAmount,
            Long deliveryTip,
            Long agencyId,
            LocalDateTime orderedAt
    ) {
    }
}
