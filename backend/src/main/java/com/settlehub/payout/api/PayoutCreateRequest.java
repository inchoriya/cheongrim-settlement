package com.settlehub.payout.api;

import jakarta.validation.constraints.NotNull;

public record PayoutCreateRequest(
        @NotNull Long settlementId,
        Boolean forceFail
) {
}
