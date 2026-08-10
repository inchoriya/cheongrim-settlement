package com.settlehub.settlement.domain;

import java.util.Optional;

/**
 * 주문 1건에 대한 정산 breakdown.
 * 금액 단위: 원(Long).
 */
public record SettlementBreakdown(
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
        Optional<AnomalyFlag> anomalyFlag
) {

    public long riderTotalAmount() {
        return riderFeeAmount + tipToRiderAmount;
    }

    public boolean hasAnomaly() {
        return anomalyFlag.isPresent();
    }

    /**
     * 적용 정책이 없을 때 사용하는 스냅샷. 헤더 상태는 HELD로 둔다.
     */
    public static SettlementBreakdown policyMissing(long orderAmount, long deliveryTip) {
        return new SettlementBreakdown(
                orderAmount,
                deliveryTip,
                0,
                0,
                0,
                0,
                0,
                0,
                deliveryTip,
                orderAmount,
                0,
                Optional.of(AnomalyFlag.POLICY_MISSING)
        );
    }
}
