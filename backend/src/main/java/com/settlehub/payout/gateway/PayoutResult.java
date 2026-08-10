package com.settlehub.payout.gateway;

import com.settlehub.payout.domain.PgProvider;

public record PayoutResult(
        boolean success,
        PgProvider provider,
        String pgTransactionId,
        String failureReason,
        String rawResponse
) {

    public static PayoutResult success(PgProvider provider, String pgTransactionId, String rawResponse) {
        return new PayoutResult(true, provider, pgTransactionId, null, rawResponse);
    }

    public static PayoutResult failure(PgProvider provider, String failureReason, String rawResponse) {
        return new PayoutResult(false, provider, null, failureReason, rawResponse);
    }

    public static PayoutResult success(String pgTransactionId, String rawResponse) {
        return success(PgProvider.MOCK, pgTransactionId, rawResponse);
    }

    public static PayoutResult failure(String failureReason, String rawResponse) {
        return failure(PgProvider.MOCK, failureReason, rawResponse);
    }
}
