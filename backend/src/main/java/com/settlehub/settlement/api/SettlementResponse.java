package com.settlehub.settlement.api;

import com.settlehub.settlement.domain.Settlement;
import com.settlehub.settlement.domain.SettlementStatus;

import java.time.LocalDateTime;
import java.util.List;

public record SettlementResponse(
        Long id,
        Long agencyId,
        String agencyCode,
        Long merchantId,
        String merchantCode,
        String merchantName,
        LocalDateTime periodStart,
        LocalDateTime periodEnd,
        SettlementStatus status,
        int orderCount,
        long totalOrderAmount,
        long totalPlatformFeeAmount,
        long totalAgencySettlementAmount,
        long totalRiderFeeAmount,
        long totalTipAmount,
        long totalMerchantSettlementAmount,
        String anomalyFlags,
        String holdReason,
        LocalDateTime confirmedAt,
        List<SettlementLineResponse> lines
) {

    public static SettlementResponse summary(Settlement settlement) {
        return from(settlement, false);
    }

    public static SettlementResponse detail(Settlement settlement) {
        return from(settlement, true);
    }

    private static SettlementResponse from(Settlement settlement, boolean includeLines) {
        return new SettlementResponse(
                settlement.getId(),
                settlement.getAgency().getId(),
                settlement.getAgency().getCode(),
                settlement.getMerchant().getId(),
                settlement.getMerchant().getCode(),
                settlement.getMerchant().getName(),
                settlement.getPeriodStart(),
                settlement.getPeriodEnd(),
                settlement.getStatus(),
                settlement.getOrderCount(),
                settlement.getTotalOrderAmount(),
                settlement.getTotalPlatformFeeAmount(),
                settlement.getTotalAgencySettlementAmount(),
                settlement.getTotalRiderFeeAmount(),
                settlement.getTotalTipAmount(),
                settlement.getTotalMerchantSettlementAmount(),
                settlement.getAnomalyFlags(),
                settlement.getHoldReason(),
                settlement.getConfirmedAt(),
                includeLines
                        ? settlement.getLines().stream().map(SettlementLineResponse::from).toList()
                        : List.of()
        );
    }
}
