package com.settlehub.settlement.domain;

import java.util.Optional;

/**
 * 정산 순수 계산기. 외부 I/O 없음.
 * 규칙: docs/settlement-rules.md
 */
public final class SettlementCalculator {

    private SettlementCalculator() {
    }

    public static SettlementBreakdown calculate(SettlementCalculateCommand command) {
        validate(command);

        long orderAmount = command.orderAmount();
        long deliveryTip = command.deliveryTip();
        int platformFeeBps = command.platformFeeBps();
        int agencyFeeBps = command.agencyFeeBps();
        long riderFee = command.riderFee();

        long platformFeeAmount = feeAmount(orderAmount, platformFeeBps);
        long agencyFeeAmount = feeAmount(orderAmount, agencyFeeBps);
        long riderFeeAmount = riderFee;
        long tipToRiderAmount = deliveryTip;

        long merchantSettlementAmount = orderAmount
                - platformFeeAmount
                - agencyFeeAmount
                - riderFeeAmount;

        Optional<AnomalyFlag> anomaly = resolveAnomaly(orderAmount, merchantSettlementAmount);

        return new SettlementBreakdown(
                orderAmount,
                deliveryTip,
                platformFeeBps,
                agencyFeeBps,
                riderFee,
                platformFeeAmount,
                agencyFeeAmount,
                riderFeeAmount,
                tipToRiderAmount,
                merchantSettlementAmount,
                agencyFeeAmount,
                anomaly
        );
    }

    /**
     * feeAmount = floor(orderAmount * feeBps / 10000)
     */
    public static long feeAmount(long orderAmount, int feeBps) {
        return Math.floorDiv(orderAmount * feeBps, 10_000L);
    }

    private static void validate(SettlementCalculateCommand command) {
        if (command.orderAmount() < 0) {
            throw new IllegalArgumentException("orderAmount must be >= 0");
        }
        if (command.deliveryTip() < 0) {
            throw new IllegalArgumentException("deliveryTip must be >= 0");
        }
        if (command.riderFee() < 0) {
            throw new IllegalArgumentException("riderFee must be >= 0");
        }
        validateBps(command.platformFeeBps(), "platformFeeBps");
        validateBps(command.agencyFeeBps(), "agencyFeeBps");
    }

    private static void validateBps(int bps, String field) {
        if (bps < 0 || bps > 10_000) {
            throw new IllegalArgumentException(field + " must be between 0 and 10000");
        }
    }

    private static Optional<AnomalyFlag> resolveAnomaly(long orderAmount, long merchantSettlementAmount) {
        if (merchantSettlementAmount < 0) {
            return Optional.of(AnomalyFlag.NEGATIVE_MERCHANT_AMOUNT);
        }
        if (orderAmount == 0) {
            return Optional.of(AnomalyFlag.ZERO_ORDER_AMOUNT);
        }
        return Optional.empty();
    }
}
