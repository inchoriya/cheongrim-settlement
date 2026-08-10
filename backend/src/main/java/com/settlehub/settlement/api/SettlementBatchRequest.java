package com.settlehub.settlement.api;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SettlementBatchRequest(
        @NotNull LocalDateTime periodStart,
        @NotNull LocalDateTime periodEnd,
        Long agencyId
) {
}
