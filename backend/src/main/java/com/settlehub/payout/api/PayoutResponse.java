package com.settlehub.payout.api;

import com.settlehub.payout.domain.PgProvider;
import com.settlehub.payout.domain.PayoutStatus;
import com.settlehub.payout.domain.PayoutTransaction;

import java.time.LocalDateTime;

public record PayoutResponse(
        Long payoutId,
        Long settlementId,
        long amount,
        PayoutStatus status,
        PgProvider pgProvider,
        String pgTransactionId,
        String failureReason,
        LocalDateTime requestedAt,
        LocalDateTime completedAt
) {

    public static PayoutResponse from(PayoutTransaction tx) {
        return new PayoutResponse(
                tx.getId(),
                tx.getSettlement().getId(),
                tx.getAmount(),
                tx.getStatus(),
                tx.getPgProvider(),
                tx.getPgTransactionId(),
                tx.getFailureReason(),
                tx.getRequestedAt(),
                tx.getCompletedAt()
        );
    }
}
