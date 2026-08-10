package com.settlehub.settlement.domain;

/**
 * 정산 계산 입력값.
 *
 * @param orderAmount     음식 주문금액(원), 배달팁 제외
 * @param deliveryTip     배달팁(원)
 * @param platformFeeBps  플랫폼 수수료율 (500 = 5%)
 * @param agencyFeeBps    대행사 수수료율 (1000 = 10%)
 * @param riderFee        건당 라이더 비용(원)
 */
public record SettlementCalculateCommand(
        long orderAmount,
        long deliveryTip,
        int platformFeeBps,
        int agencyFeeBps,
        long riderFee
) {
}
