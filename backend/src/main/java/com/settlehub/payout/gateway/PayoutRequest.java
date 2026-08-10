package com.settlehub.payout.gateway;

public record PayoutRequest(
        Long settlementId,
        long amount,
        Long beneficiaryId,
        String refSellerId,
        String merchantName,
        String bankCode,
        String accountNumber,
        String accountHolder,
        String tossSellerId,
        boolean forceFail
) {
}
