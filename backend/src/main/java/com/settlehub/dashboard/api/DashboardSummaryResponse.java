package com.settlehub.dashboard.api;

public record DashboardSummaryResponse(
        long orderCount,
        long orderAmountSum,
        long settlementPendingCount,
        long heldCount,
        long readyForPayoutCount,
        long paidAmountSum
) {
}
