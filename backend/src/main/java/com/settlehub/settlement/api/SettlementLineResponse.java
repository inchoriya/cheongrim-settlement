package com.settlehub.settlement.api;

import com.settlehub.settlement.domain.AnomalyFlag;
import com.settlehub.settlement.domain.SettlementLine;

public record SettlementLineResponse(
        Long id,
        Long orderId,
        String externalOrderId,
        long orderAmount,
        long deliveryTip,
        int platformFeeBps,
        int agencyFeeBps,
        long riderFee,
        long platformFeeAmount,
        long agencyFeeAmount,
        long riderFeeAmount,
        long tipToRiderAmount,
        long merchantSettlementAmount,
        long agencySettlementAmount,
        AnomalyFlag anomalyFlag
) {

    public static SettlementLineResponse from(SettlementLine line) {
        return new SettlementLineResponse(
                line.getId(),
                line.getOrder().getId(),
                line.getOrder().getExternalOrderId(),
                line.getOrderAmount(),
                line.getDeliveryTip(),
                line.getPlatformFeeBps(),
                line.getAgencyFeeBps(),
                line.getRiderFee(),
                line.getPlatformFeeAmount(),
                line.getAgencyFeeAmount(),
                line.getRiderFeeAmount(),
                line.getTipToRiderAmount(),
                line.getMerchantSettlementAmount(),
                line.getAgencySettlementAmount(),
                line.getAnomalyFlag()
        );
    }
}
