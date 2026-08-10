package com.settlehub.settlement.api;

import jakarta.validation.constraints.NotBlank;

public record SettlementHoldRequest(
        @NotBlank String reason
) {
}
